package com.rohan.mbtool.engine

import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/** AES-256-CBC encryption/decryption for SimplePassphrase variant. */
object AesUtil {

    /** SHA-256 of the MaterialBinTool passphrase (the fixed "those are not the shaders..." key). */
    val SIMPLE_PASSPHRASE_KEY: ByteArray by lazy {
        sha256("dGhvc2UgYXJlIG5vdCB0aGUgc2hhZGVycyB5b3UgYXJlIGxvb2tpbmcgZm9yISA=".toByteArray())
    }

    fun sha256(data: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(data)

    fun decrypt(key: ByteArray, iv: ByteArray, data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        return cipher.doFinal(data)
    }

    fun encrypt(key: ByteArray, iv: ByteArray, data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        return cipher.doFinal(data)
    }
}
