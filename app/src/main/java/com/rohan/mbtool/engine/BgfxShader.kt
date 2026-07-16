package com.rohan.mbtool.engine

/**
 * Ported from MaterialBinTool's BgfxShader family (MIT License).
 * Handles GL/ESSL, D3D, Metal, and Vulkan shader blobs.
 */
class BgfxShader {
    var magic: Int = 0
    var hash: Int = 0
    var uniforms: MutableList<Uniform> = mutableListOf()
    var code: ByteArray = ByteArray(0)

    /**
     * Parse a raw bgfxShaderData blob.
     * Note: D3D shaders have an extra 4-byte outputHash after the trailing 0x00 byte,
     * but we don't need it since we preserve the raw blob on repack.
     */
    fun read(buf: ByteBuf) {
        magic = buf.readInt()
        hash = buf.readIntLE()
        val count = buf.readShortLE().toInt() and 0xFFFF
        uniforms = ArrayList(count)
        repeat(count) {
            val u = Uniform()
            u.readFrom(buf)
            uniforms.add(u)
        }
        code = buf.readByteArrayLE()
        buf.readByte() // trailing 0x00
    }

    fun read(bytes: ByteArray) = read(ByteBuf(bytes))

    fun write(buf: ByteBuf) {
        buf.writeInt(magic)
        buf.writeIntLE(hash)
        buf.writeShortLE(uniforms.size)
        uniforms.forEach { it.writeTo(buf) }
        buf.writeByteArrayLE(code)
        buf.writeByte(0)
    }

    fun toByteArray(): ByteArray {
        val buf = ByteBuf()
        write(buf)
        return buf.toByteArray()
    }

    /** Returns GLSL/ESSL source as text (valid only for GL/ESSL platform shaders). */
    fun getGlslSource(): String = String(code, Charsets.UTF_8)

    /** Replace GLSL source (for GL/ESSL platform shaders only). */
    fun setGlslSource(src: String) { code = src.toByteArray(Charsets.UTF_8) }
}
