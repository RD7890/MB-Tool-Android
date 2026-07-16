package com.rohan.mbtool.engine

/** Ported from MaterialBinTool's EncryptionVariants.java (MIT License). */
enum class EncryptionVariants(val signature: Int) {
    None            (0x4E4F4E45), // "NONE"
    SimplePassphrase(0x534D504C), // "SMPL"
    KeyPair         (0x4B595052), // "KYPR"
    Unknown         (0);

    companion object {
        fun getBySignature(sig: Int) =
            values().firstOrNull { it.signature == sig } ?: Unknown
    }
}
