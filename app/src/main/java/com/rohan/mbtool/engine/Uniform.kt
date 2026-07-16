package com.rohan.mbtool.engine

import java.nio.charset.StandardCharsets

/** Ported from MaterialBinTool's Uniform.java (MIT License). */
class Uniform {
    var name: String = ""
    var type: Byte = 0
    var num: Byte = 0
    var regIndex: Short = 0
    var regCount: Short = 0

    fun readFrom(buf: ByteBuf) {
        val nameLen = buf.readByte().toInt() and 0xFF
        name = String(buf.readBytes(nameLen), StandardCharsets.UTF_8)
        type = buf.readByte()
        num = buf.readByte()
        regIndex = buf.readShortLE()
        regCount = buf.readShortLE()
    }

    fun writeTo(buf: ByteBuf) {
        val nameBytes = name.toByteArray(StandardCharsets.UTF_8)
        buf.writeByte(nameBytes.size)
        buf.writeBytes(nameBytes)
        buf.writeByte(type)
        buf.writeByte(num)
        buf.writeShortLE(regIndex)
        buf.writeShortLE(regCount)
    }
}
