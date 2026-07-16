package com.rohan.mbtool.engine

import java.nio.charset.StandardCharsets

/** Ported from MaterialBinTool's ByteBuf.java (MIT License). */
class ByteBuf {
    private var array: ByteArray
    var readerIndex = 0
    var writerIndex = 0

    constructor(size: Int = 1024) {
        array = ByteArray(size)
    }

    constructor(bytes: ByteArray) {
        array = bytes
        writerIndex = bytes.size
    }

    fun isReadable() = readerIndex < writerIndex

    fun toByteArray(): ByteArray = array.copyOfRange(readerIndex, writerIndex)

    /**
     * Return a defensive copy of the raw backing array slice [from, to).
     * Useful for capturing already-read bytes without re-reading them.
     */
    fun readSlice(from: Int, to: Int): ByteArray = array.copyOfRange(from, to)

    // ── Readers ──────────────────────────────────────────────────────────────
    fun readBoolean() = array[readerIndex++] != 0.toByte()
    fun readByte(): Byte = array[readerIndex++]
    fun readUnsignedByte(): Int = array[readerIndex++].toInt() and 0xFF

    fun readShortLE(): Short =
        ((array[readerIndex++].toInt() and 0xFF) or
         ((array[readerIndex++].toInt() and 0xFF) shl 8)).toShort()

    fun readIntLE(): Int =
        (array[readerIndex++].toInt() and 0xFF) or
        ((array[readerIndex++].toInt() and 0xFF) shl 8) or
        ((array[readerIndex++].toInt() and 0xFF) shl 16) or
        ((array[readerIndex++].toInt() and 0xFF) shl 24)

    fun readInt(): Int =
        ((array[readerIndex++].toInt() and 0xFF) shl 24) or
        ((array[readerIndex++].toInt() and 0xFF) shl 16) or
        ((array[readerIndex++].toInt() and 0xFF) shl 8) or
         (array[readerIndex++].toInt() and 0xFF)

    fun readFloatLE(): Float = java.lang.Float.intBitsToFloat(readIntLE())

    fun readLongLE(): Long =
        (array[readerIndex++].toLong() and 0xFF) or
        ((array[readerIndex++].toLong() and 0xFF) shl 8) or
        ((array[readerIndex++].toLong() and 0xFF) shl 16) or
        ((array[readerIndex++].toLong() and 0xFF) shl 24) or
        ((array[readerIndex++].toLong() and 0xFF) shl 32) or
        ((array[readerIndex++].toLong() and 0xFF) shl 40) or
        ((array[readerIndex++].toLong() and 0xFF) shl 48) or
        ((array[readerIndex++].toLong() and 0xFF) shl 56)

    fun readLong(): Long =
        ((array[readerIndex++].toLong() and 0xFF) shl 56) or
        ((array[readerIndex++].toLong() and 0xFF) shl 48) or
        ((array[readerIndex++].toLong() and 0xFF) shl 40) or
        ((array[readerIndex++].toLong() and 0xFF) shl 32) or
        ((array[readerIndex++].toLong() and 0xFF) shl 24) or
        ((array[readerIndex++].toLong() and 0xFF) shl 16) or
        ((array[readerIndex++].toLong() and 0xFF) shl 8) or
         (array[readerIndex++].toLong() and 0xFF)

    fun readBytes(len: Int): ByteArray {
        val result = array.copyOfRange(readerIndex, readerIndex + len)
        readerIndex += len
        return result
    }

    fun readByteArrayLE(): ByteArray = readBytes(readIntLE())
    fun readStringLE(): String = String(readByteArrayLE(), StandardCharsets.UTF_8)

    // ── Writers ──────────────────────────────────────────────────────────────
    private fun ensureWritable(needed: Int) {
        if (writerIndex + needed > array.size) {
            val newSize = maxOf(array.size * 2, writerIndex + needed)
            array = array.copyOf(newSize)
        }
    }

    fun writeBoolean(v: Boolean) { ensureWritable(1); array[writerIndex++] = if (v) 1 else 0 }
    fun writeByte(v: Int) { ensureWritable(1); array[writerIndex++] = v.toByte() }
    fun writeByte(v: Byte) { ensureWritable(1); array[writerIndex++] = v }

    fun writeShortLE(v: Short) {
        ensureWritable(2)
        val i = v.toInt()
        array[writerIndex++] = i.toByte()
        array[writerIndex++] = (i ushr 8).toByte()
    }
    fun writeShortLE(v: Int) = writeShortLE(v.toShort())

    fun writeIntLE(v: Int) {
        ensureWritable(4)
        array[writerIndex++] = v.toByte()
        array[writerIndex++] = (v ushr 8).toByte()
        array[writerIndex++] = (v ushr 16).toByte()
        array[writerIndex++] = (v ushr 24).toByte()
    }

    fun writeInt(v: Int) {
        ensureWritable(4)
        array[writerIndex++] = (v ushr 24).toByte()
        array[writerIndex++] = (v ushr 16).toByte()
        array[writerIndex++] = (v ushr 8).toByte()
        array[writerIndex++] = v.toByte()
    }

    fun writeFloatLE(v: Float) = writeIntLE(java.lang.Float.floatToRawIntBits(v))

    fun writeLongLE(v: Long) {
        ensureWritable(8)
        array[writerIndex++] = v.toByte()
        array[writerIndex++] = (v ushr 8).toByte()
        array[writerIndex++] = (v ushr 16).toByte()
        array[writerIndex++] = (v ushr 24).toByte()
        array[writerIndex++] = (v ushr 32).toByte()
        array[writerIndex++] = (v ushr 40).toByte()
        array[writerIndex++] = (v ushr 48).toByte()
        array[writerIndex++] = (v ushr 56).toByte()
    }

    fun writeLong(v: Long) {
        ensureWritable(8)
        array[writerIndex++] = (v ushr 56).toByte()
        array[writerIndex++] = (v ushr 48).toByte()
        array[writerIndex++] = (v ushr 40).toByte()
        array[writerIndex++] = (v ushr 32).toByte()
        array[writerIndex++] = (v ushr 24).toByte()
        array[writerIndex++] = (v ushr 16).toByte()
        array[writerIndex++] = (v ushr 8).toByte()
        array[writerIndex++] = v.toByte()
    }

    fun writeBytes(bytes: ByteArray) {
        ensureWritable(bytes.size)
        System.arraycopy(bytes, 0, array, writerIndex, bytes.size)
        writerIndex += bytes.size
    }

    fun writeByteArrayLE(bytes: ByteArray) {
        writeIntLE(bytes.size)
        writeBytes(bytes)
    }

    fun writeStringLE(s: String) = writeByteArrayLE(s.toByteArray(StandardCharsets.UTF_8))
}
