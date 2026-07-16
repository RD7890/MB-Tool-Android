package com.rohan.mbtool.engine

import android.content.ContentResolver
import android.net.Uri
import android.util.Base64
import androidx.documentfile.provider.DocumentFile
import com.google.gson.GsonBuilder
import java.io.InputStream
import java.io.OutputStream

/**
 * High-level Android API for decompiling and recompiling .material.bin files.
 * All operations are pure Kotlin — no native binaries or Termux required.
 */
object MaterialBinEngine {

    private val gson = GsonBuilder().setPrettyPrinting().create()

    // ── Decompile ─────────────────────────────────────────────────────────────
    data class DecompileResult(
        val materialName: String,
        val version: Long,
        val encryption: String,
        val passCount: Int,
        val variantCount: Int,
        val shaderCount: Int,
        val glslCount: Int,
        val outputDirName: String,
    )

    /**
     * Decompile a .material.bin file into [outputDir].
     * Creates:
     *   - _meta.json    : full metadata + opaque blobs as Base64
     *   - <passName>/   : sub-directories with per-shader files
     *   - *.bin         : raw bgfx shader blobs
     *   - *.glsl        : GLSL/ESSL source text (GLSL/ESSL platforms only)
     */
    fun decompile(
        inputStream: InputStream,
        outputDir: DocumentFile,
        resolver: ContentResolver,
    ): DecompileResult {
        val bytes = inputStream.readBytes()
        val buf   = ByteBuf(bytes)
        val cmd   = CompiledMaterialDefinition()
        cmd.loadFrom(buf)

        var variantTotal = 0
        var shaderTotal  = 0
        var glslTotal    = 0

        val meta = mutableMapOf<String, Any>()
        meta["name"]        = cmd.name
        meta["version"]     = cmd.version
        meta["encryption"]  = cmd.encryptionVariant.name
        if (cmd.hasParentName) meta["parentName"] = cmd.parentName
        // Store opaque blobs for perfect round-trip recompile
        meta["_samplerBlob"]  = Base64.encodeToString(cmd.rawSamplerBlob, Base64.NO_WRAP)
        meta["_propertyBlob"] = Base64.encodeToString(cmd.rawPropertyBlob, Base64.NO_WRAP)

        val passesMeta = mutableListOf<Map<String, Any>>()

        for ((passName, pass) in cmd.passMap) {
            val passDir = outputDir.createDirectory(passName)
                ?: error("Cannot create directory: $passName")

            val passMeta = mutableMapOf<String, Any>()
            passMeta["name"]         = passName
            passMeta["bitSet"]       = pass.bitSet
            passMeta["fallback"]     = pass.fallback
            passMeta["flagDefaults"] = pass.flagDefaultValues

            val variantsMeta = mutableListOf<Map<String, Any>>()
            pass.variantList.forEachIndexed { varIdx, variant ->
                variantTotal++
                val vMeta = mutableMapOf<String, Any>()
                vMeta["index"]       = varIdx
                vMeta["isSupported"] = variant.isSupported
                vMeta["flags"]       = variant.flags

                val shadersMeta = mutableListOf<Map<String, Any>>()
                for ((pss, shaderCode) in variant.shaderCodeMap) {
                    shaderTotal++
                    val baseName = "${passName}_${varIdx}_${pss.stageName}_${pss.platformName}"

                    // Write raw bgfx shader blob
                    val binFile = passDir.createFile("application/octet-stream", "$baseName.bin")
                    binFile?.let { f ->
                        resolver.openOutputStream(f.uri)?.use { os -> os.write(shaderCode.bgfxShaderData) }
                    }

                    val sMeta = mutableMapOf<String, Any>()
                    sMeta["stage"]            = pss.stageName
                    sMeta["platform"]         = pss.platformName
                    sMeta["stageOrdinal"]     = pss.stageOrdinal.toInt() and 0xFF
                    sMeta["platformOrdinal"]  = pss.platformOrdinal.toInt() and 0xFF
                    sMeta["sourceHash"]       = shaderCode.sourceHash
                    sMeta["rawBinFile"]       = "$baseName.bin"
                    sMeta["_shaderInputBlob"] = Base64.encodeToString(shaderCode.rawShaderInputBlob, Base64.NO_WRAP)

                    // For GLSL/ESSL, extract readable source text
                    if (pss.platform.isGlsl() && shaderCode.bgfxShaderData.isNotEmpty()) {
                        try {
                            val shader = BgfxShader()
                            shader.read(shaderCode.bgfxShaderData)
                            val glslFile = passDir.createFile("text/plain", "$baseName.glsl")
                            glslFile?.let { f ->
                                resolver.openOutputStream(f.uri)?.use { os -> os.write(shader.code) }
                                glslTotal++
                            }
                            sMeta["glslFile"]      = "$baseName.glsl"
                            sMeta["uniformCount"]  = shader.uniforms.size
                        } catch (e: Exception) {
                            sMeta["glslError"] = e.message ?: "parse error"
                        }
                    }

                    shadersMeta.add(sMeta)
                }
                vMeta["shaders"] = shadersMeta
                variantsMeta.add(vMeta)
            }
            passMeta["variants"] = variantsMeta
            passesMeta.add(passMeta)
        }
        meta["passes"] = passesMeta

        // Write _meta.json
        outputDir.createFile("application/json", "_meta.json")?.let { f ->
            resolver.openOutputStream(f.uri)?.use { os ->
                os.write(gson.toJson(meta).toByteArray(Charsets.UTF_8))
            }
        }

        return DecompileResult(
            materialName  = cmd.name,
            version       = cmd.version,
            encryption    = cmd.encryptionVariant.name,
            passCount     = cmd.passMap.size,
            variantCount  = variantTotal,
            shaderCount   = shaderTotal,
            glslCount     = glslTotal,
            outputDirName = outputDir.name ?: "",
        )
    }

    // ── Recompile ─────────────────────────────────────────────────────────────
    data class RecompileResult(
        val materialName: String,
        val passCount: Int,
        val variantCount: Int,
        val shaderCount: Int,
    )

    /**
     * Recompile a decompiled directory back into a .material.bin file.
     * The directory must contain _meta.json (produced by [decompile]).
     * Edited .glsl files are re-packed into the bgfx blob automatically.
     * Unedited shaders use the original .bin blob (bit-exact).
     */
    fun recompile(
        inputDir: DocumentFile,
        outputStream: OutputStream,
        resolver: ContentResolver,
    ): RecompileResult {
        val metaFile = inputDir.findFile("_meta.json")
            ?: error("_meta.json not found — is this a valid decompiled folder?")
        val metaJson = resolver.openInputStream(metaFile.uri)
            ?.readBytes()?.toString(Charsets.UTF_8)
            ?: error("Cannot read _meta.json")

        @Suppress("UNCHECKED_CAST")
        val meta = gson.fromJson(metaJson, Map::class.java) as Map<String, Any>

        val cmd = CompiledMaterialDefinition()
        cmd.name = meta["name"] as? String ?: ""
        cmd.version = (meta["version"] as? Double)?.toLong()
            ?: (meta["version"] as? Long) ?: 0L
        cmd.encryptionVariant = EncryptionVariants.values()
            .find { it.name == meta["encryption"] } ?: EncryptionVariants.None
        cmd.hasParentName = meta.containsKey("parentName")
        if (cmd.hasParentName) cmd.parentName = meta["parentName"] as? String ?: ""

        // Restore opaque blobs for perfect structural round-trip
        (meta["_samplerBlob"] as? String)?.let {
            cmd.rawSamplerBlob = Base64.decode(it, Base64.NO_WRAP)
        }
        (meta["_propertyBlob"] as? String)?.let {
            cmd.rawPropertyBlob = Base64.decode(it, Base64.NO_WRAP)
        }

        var variantTotal = 0
        var shaderTotal  = 0

        @Suppress("UNCHECKED_CAST")
        val passesMeta = meta["passes"] as? List<Map<String, Any>> ?: emptyList()

        for (passMeta in passesMeta) {
            val passName = passMeta["name"] as? String ?: continue
            val passDir  = inputDir.findFile(passName) ?: continue

            val pass = CompiledMaterialDefinition.Pass()
            pass.bitSet  = passMeta["bitSet"]  as? String ?: ""
            pass.fallback = passMeta["fallback"] as? String ?: ""
            @Suppress("UNCHECKED_CAST")
            (passMeta["flagDefaults"] as? Map<String, String>)
                ?.forEach { (k, v) -> pass.flagDefaultValues[k] = v }

            @Suppress("UNCHECKED_CAST")
            val variantsMeta = passMeta["variants"] as? List<Map<String, Any>> ?: emptyList()
            for (vMeta in variantsMeta) {
                variantTotal++
                val variant = CompiledMaterialDefinition.Variant()
                variant.isSupported = vMeta["isSupported"] as? Boolean ?: false
                @Suppress("UNCHECKED_CAST")
                (vMeta["flags"] as? Map<String, String>)
                    ?.forEach { (k, v) -> variant.flags[k] = v }

                @Suppress("UNCHECKED_CAST")
                val shadersMeta = vMeta["shaders"] as? List<Map<String, Any>> ?: emptyList()
                for (sMeta in shadersMeta) {
                    shaderTotal++
                    val pss = CompiledMaterialDefinition.PlatformShaderStage()
                    pss.stageName       = sMeta["stage"]    as? String ?: ""
                    pss.platformName    = sMeta["platform"] as? String ?: ""
                    pss.stageOrdinal    = ((sMeta["stageOrdinal"]    as? Double)?.toInt() ?: 0).toByte()
                    pss.platformOrdinal = ((sMeta["platformOrdinal"] as? Double)?.toInt() ?: 0).toByte()

                    val sc = CompiledMaterialDefinition.ShaderCode()
                    sc.sourceHash = (sMeta["sourceHash"] as? Double)?.toLong()
                        ?: (sMeta["sourceHash"] as? Long) ?: 0L

                    // Restore shaderInput opaque blob
                    (sMeta["_shaderInputBlob"] as? String)?.let {
                        sc.rawShaderInputBlob = Base64.decode(it, Base64.NO_WRAP)
                    }
                    if (sc.rawShaderInputBlob.isEmpty()) {
                        // Fallback: 0 shader inputs (2 bytes = count of 0)
                        sc.rawShaderInputBlob = ByteArray(2)
                    }

                    // Load shader blob: prefer edited .glsl, else use original .bin
                    val glslFileName = sMeta["glslFile"] as? String
                    val binFileName  = sMeta["rawBinFile"] as? String

                    val glslFile = glslFileName?.let { passDir.findFile(it) }
                    val binFile  = binFileName?.let  { passDir.findFile(it) }

                    sc.bgfxShaderData = when {
                        glslFile != null && glslFile.exists() && binFile != null -> {
                            // Re-pack edited GLSL source back into the bgfx blob
                            val origBlob = resolver.openInputStream(binFile.uri)?.readBytes() ?: ByteArray(0)
                            val glslSrc  = resolver.openInputStream(glslFile.uri)?.readBytes() ?: ByteArray(0)
                            val shader   = BgfxShader()
                            shader.read(origBlob)
                            shader.code  = glslSrc
                            shader.toByteArray()
                        }
                        binFile != null ->
                            resolver.openInputStream(binFile.uri)?.readBytes() ?: ByteArray(0)
                        else -> ByteArray(0)
                    }

                    variant.shaderCodeMap[pss] = sc
                }
                pass.variantList.add(variant)
            }
            cmd.passMap[passName] = pass
        }

        val outBuf = ByteBuf()
        cmd.saveTo(outBuf)
        outputStream.write(outBuf.toByteArray())

        return RecompileResult(
            materialName = cmd.name,
            passCount    = cmd.passMap.size,
            variantCount = variantTotal,
            shaderCount  = shaderTotal,
        )
    }
}
