package com.rohan.mbtool.engine

import java.security.SecureRandom

/**
 * Ported from MaterialBinTool's CompiledMaterialDefinition.java (MIT License).
 * Reads and writes the RenderDragon .material.bin binary format.
 *
 * Complex sub-structures (SamplerDefinition, PropertyField, ShaderInput) are
 * read via opaque-blob capture: we record the exact byte range consumed and
 * write it back verbatim. This guarantees bit-exact round-trips even for fields
 * we don't fully understand.
 */
class CompiledMaterialDefinition {

    companion object {
        const val MAGIC = 0xA11DA1AL
        const val HEADER = "RenderDragon.CompiledMaterialDefinition"
    }

    var version: Long = 0
    var encryptionVariant: EncryptionVariants = EncryptionVariants.None
    var name: String = ""
    var hasParentName: Boolean = false
    var parentName: String = ""

    /** Raw bytes of the sampler section (count byte + all sampler data). */
    var rawSamplerBlob: ByteArray = ByteArray(0)
    /** Raw bytes of the property section (count short + all property data). */
    var rawPropertyBlob: ByteArray = ByteArray(0)

    val passMap: LinkedHashMap<String, Pass> = LinkedHashMap()

    // ── Load ─────────────────────────────────────────────────────────────────
    fun loadFrom(buf: ByteBuf) {
        val magic = buf.readLongLE()
        require(magic == MAGIC) { "Invalid magic: 0x${magic.toString(16)}" }
        val header = buf.readStringLE()
        require(header == HEADER) { "Invalid header: $header" }

        version = buf.readLongLE()
        require(version >= 0x16) { "Unsupported version: $version (min 22 / 0x16)" }

        encryptionVariant = EncryptionVariants.getBySignature(buf.readIntLE())
        when (encryptionVariant) {
            EncryptionVariants.None -> loadContent(buf)
            EncryptionVariants.SimplePassphrase -> {
                val key = buf.readBytes(32)      // fixed-size SHA-256 key (no length prefix)
                val iv  = buf.readBytes(16)      // fixed-size AES-CBC IV (no length prefix)
                val enc = buf.readByteArrayLE()  // length-prefixed encrypted payload
                loadContent(ByteBuf(AesUtil.decrypt(key, iv, enc)))
            }
            EncryptionVariants.KeyPair ->
                throw UnsupportedOperationException("KeyPair encryption is not supported")
            else ->
                throw UnsupportedOperationException("Unknown encryption variant: $encryptionVariant")
        }
    }

    private fun loadContent(buf: ByteBuf) {
        name = buf.readStringLE()
        hasParentName = buf.readBoolean()
        if (hasParentName) parentName = buf.readStringLE()

        // ── Sampler definitions (opaque blob capture) ─────────────────────
        val samplerBlobStart = buf.readerIndex
        val samplerCount = buf.readUnsignedByte()
        repeat(samplerCount) {
            buf.readStringLE()              // sampler name (key)
            readSamplerDefinitionOpaque(buf)
        }
        rawSamplerBlob = buf.readSlice(samplerBlobStart, buf.readerIndex)

        // ── Property fields (opaque blob capture) ─────────────────────────
        val propBlobStart = buf.readerIndex
        val propCount = buf.readShortLE().toInt() and 0xFFFF
        repeat(propCount) {
            buf.readStringLE()            // property name (key)
            readPropertyFieldOpaque(buf)
        }
        rawPropertyBlob = buf.readSlice(propBlobStart, buf.readerIndex)

        // ── Passes ────────────────────────────────────────────────────────
        val passCount = buf.readShortLE().toInt() and 0xFFFF
        repeat(passCount) {
            val passName = buf.readStringLE()
            val pass = Pass()
            pass.read(buf)
            passMap[passName] = pass
        }

        val endMagic = buf.readLongLE()
        if (endMagic != MAGIC) {
            android.util.Log.w("CMDef", "End magic mismatch: 0x${endMagic.toString(16)}")
        }
    }

    // ── Save ─────────────────────────────────────────────────────────────────
    fun saveTo(buf: ByteBuf, encryption: EncryptionVariants = encryptionVariant) {
        buf.writeLongLE(MAGIC)
        buf.writeStringLE(HEADER)
        buf.writeLongLE(version)
        buf.writeIntLE(encryption.signature)

        when (encryption) {
            EncryptionVariants.None -> saveContent(buf)
            EncryptionVariants.SimplePassphrase -> {
                val key = AesUtil.SIMPLE_PASSPHRASE_KEY
                val iv  = ByteArray(16).also { SecureRandom().nextBytes(it) }
                val contentBuf = ByteBuf()
                saveContent(contentBuf)
                buf.writeBytes(key)           // fixed-size, no length prefix
                buf.writeBytes(iv)            // fixed-size, no length prefix
                buf.writeByteArrayLE(AesUtil.encrypt(key, iv, contentBuf.toByteArray()))
            }
            else -> throw UnsupportedOperationException("Cannot save with $encryption")
        }
    }

    private fun saveContent(buf: ByteBuf) {
        buf.writeStringLE(name)
        buf.writeBoolean(hasParentName)
        if (hasParentName) buf.writeStringLE(parentName)

        // Write opaque blobs back verbatim — exact byte-for-byte round-trip
        buf.writeBytes(rawSamplerBlob)
        buf.writeBytes(rawPropertyBlob)

        buf.writeShortLE(passMap.size)
        for ((passName, pass) in passMap) {
            buf.writeStringLE(passName)
            pass.write(buf)
        }
        buf.writeLongLE(MAGIC)
    }

    // ── Opaque field readers ──────────────────────────────────────────────────
    private fun readSamplerDefinitionOpaque(buf: ByteBuf) {
        buf.readShortLE()  // reg
        buf.readByte()     // access ordinal
        buf.readByte()     // precision
        buf.readBoolean()  // allowUnorderedAccess
        buf.readByte()     // type ordinal
        buf.readStringLE() // textureFormat
        buf.readIntLE()    // unknownInt
        buf.readByte()     // unknownByte
        if (buf.readBoolean()) buf.readByte()     // hasUnknownByte2 → unknownByte2
        if (buf.readBoolean()) buf.readStringLE() // hasDefaultTexture → defaultTexture
        if (buf.readBoolean()) buf.readStringLE() // hasUnknownString → unknownString
        if (buf.readBoolean()) {                   // hasCustomTypeInfo
            buf.readStringLE()  // name
            buf.readIntLE()     // size
        }
    }

    private fun readPropertyFieldOpaque(buf: ByteBuf) {
        when (val type = buf.readShortLE().toInt() and 0xFFFF) {
            2 -> { buf.readIntLE(); if (buf.readBoolean()) buf.readBytes(16) }  // Vec4
            3 -> { buf.readIntLE(); if (buf.readBoolean()) buf.readBytes(36) }  // Mat3
            4 -> { buf.readIntLE(); if (buf.readBoolean()) buf.readBytes(64) }  // Mat4
            5 -> { /* ExternalUniformDeclaration — no extra bytes */ }
            else -> android.util.Log.w("CMDef", "Unknown PropertyField type: $type")
        }
    }

    // ── Inner classes ─────────────────────────────────────────────────────────
    class Pass {
        var bitSet: String = ""
        var fallback: String = ""
        var hasDefaultBlendMode: Boolean = false
        var defaultBlendMode: Short = 0
        val flagDefaultValues: LinkedHashMap<String, String> = LinkedHashMap()
        val variantList: MutableList<Variant> = mutableListOf()

        fun read(buf: ByteBuf) {
            bitSet  = buf.readStringLE()
            fallback = buf.readStringLE()
            hasDefaultBlendMode = buf.readBoolean()
            if (hasDefaultBlendMode) defaultBlendMode = buf.readShortLE()

            val flagCount    = buf.readShortLE().toInt() and 0xFFFF
            val variantCount = buf.readShortLE().toInt() and 0xFFFF
            repeat(flagCount) { flagDefaultValues[buf.readStringLE()] = buf.readStringLE() }
            repeat(variantCount) {
                val v = Variant(); v.read(buf); variantList.add(v)
            }
        }

        fun write(buf: ByteBuf) {
            buf.writeStringLE(bitSet)
            buf.writeStringLE(fallback)
            buf.writeBoolean(hasDefaultBlendMode)
            if (hasDefaultBlendMode) buf.writeShortLE(defaultBlendMode)
            buf.writeShortLE(flagDefaultValues.size)
            buf.writeShortLE(variantList.size)
            for ((k, v) in flagDefaultValues) { buf.writeStringLE(k); buf.writeStringLE(v) }
            variantList.forEach { it.write(buf) }
        }
    }

    class Variant {
        var isSupported: Boolean = false
        val flags: LinkedHashMap<String, String> = LinkedHashMap()
        val shaderCodeMap: LinkedHashMap<PlatformShaderStage, ShaderCode> = LinkedHashMap()

        fun read(buf: ByteBuf) {
            isSupported = buf.readBoolean()
            val flagCount   = buf.readShortLE().toInt() and 0xFFFF
            val shaderCount = buf.readShortLE().toInt() and 0xFFFF
            repeat(flagCount) { flags[buf.readStringLE()] = buf.readStringLE() }
            repeat(shaderCount) {
                val pss = PlatformShaderStage(); pss.read(buf)
                val sc  = ShaderCode();          sc.read(buf)
                shaderCodeMap[pss] = sc
            }
        }

        fun write(buf: ByteBuf) {
            buf.writeBoolean(isSupported)
            buf.writeShortLE(flags.size)
            buf.writeShortLE(shaderCodeMap.size)
            for ((k, v) in flags) { buf.writeStringLE(k); buf.writeStringLE(v) }
            for ((k, v) in shaderCodeMap) { k.write(buf); v.write(buf) }
        }
    }

    class PlatformShaderStage {
        var stageName: String = ""
        var platformName: String = ""
        var stageOrdinal: Byte = 0
        var platformOrdinal: Byte = 0

        val platform: ShaderCodePlatform get() =
            ShaderCodePlatform.get(platformOrdinal.toInt() and 0xFF)

        fun read(buf: ByteBuf) {
            stageName        = buf.readStringLE()
            platformName     = buf.readStringLE()
            stageOrdinal     = buf.readByte()
            platformOrdinal  = buf.readByte()
        }

        fun write(buf: ByteBuf) {
            buf.writeStringLE(stageName)
            buf.writeStringLE(platformName)
            buf.writeByte(stageOrdinal)
            buf.writeByte(platformOrdinal)
        }

        override fun hashCode() = 31 * stageName.hashCode() + platformName.hashCode()
        override fun equals(other: Any?) = other is PlatformShaderStage &&
            stageName == other.stageName && platformName == other.platformName
        override fun toString() = "$stageName/$platformName"
    }

    class ShaderCode {
        /** Raw bytes of the shaderInput section (includes count short + all entries). */
        var rawShaderInputBlob: ByteArray = ByteArray(0)
        var sourceHash: Long = 0
        var bgfxShaderData: ByteArray = ByteArray(0)

        fun read(buf: ByteBuf) {
            // Capture shaderInput section as opaque blob
            val inputBlobStart = buf.readerIndex
            val inputCount = buf.readShortLE().toInt() and 0xFFFF
            repeat(inputCount) { readShaderInputOpaque(buf) }
            rawShaderInputBlob = buf.readSlice(inputBlobStart, buf.readerIndex)

            sourceHash    = buf.readLong()
            bgfxShaderData = buf.readByteArrayLE()
        }

        fun write(buf: ByteBuf) {
            buf.writeBytes(rawShaderInputBlob)
            buf.writeLong(sourceHash)
            buf.writeByteArrayLE(bgfxShaderData)
        }

        private fun readShaderInputOpaque(buf: ByteBuf) {
            buf.readByte()    // type ordinal
            buf.readByte()    // attribute index
            buf.readByte()    // attribute subIndex
            buf.readBoolean() // isPerInstance
            if (buf.readBoolean()) buf.readUnsignedByte() // hasPrecisionConstraint → value
            if (buf.readBoolean()) buf.readUnsignedByte() // hasInterpolationConstraint → value
        }
    }
}
