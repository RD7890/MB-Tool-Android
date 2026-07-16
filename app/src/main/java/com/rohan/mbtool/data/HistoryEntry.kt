package com.rohan.mbtool.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class OpType { DECOMPILE, RECOMPILE }

data class HistoryEntry(
    val id: Long,
    val opType: OpType,
    val fileName: String,         // input file/folder name
    val outputName: String,       // output folder/file name
    val timestamp: Long,
    val success: Boolean,
    val detail: String = "",      // shader count, error message, etc.
) {
    fun formattedTime(): String = SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault())
        .format(Date(timestamp))

    fun toRecord(): String =
        listOf(id, opType.name, fileName, outputName, timestamp, success, detail)
            .joinToString(DELIM)

    companion object {
        private const val DELIM = "|||"

        fun fromRecord(s: String): HistoryEntry? {
            val p = s.split(DELIM)
            if (p.size < 7) return null
            return runCatching {
                HistoryEntry(
                    id         = p[0].toLong(),
                    opType     = OpType.valueOf(p[1]),
                    fileName   = p[2],
                    outputName = p[3],
                    timestamp  = p[4].toLong(),
                    success    = p[5].toBoolean(),
                    detail     = p[6],
                )
            }.getOrNull()
        }
    }
}
