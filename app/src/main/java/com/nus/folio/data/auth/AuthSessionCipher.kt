package com.nus.folio.data.auth

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Authenticated encryption for session payloads using AES-GCM with a key
 * that never leaves the Android Keystore.
 */
interface AuthSessionCipher {
    fun encrypt(plaintext: ByteArray): ByteArray
    fun decrypt(ciphertext: ByteArray): ByteArray
}

class AndroidKeystoreAuthSessionCipher(
    private val keyAlias: String = DEFAULT_KEY_ALIAS,
) : AuthSessionCipher {

    override fun encrypt(plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext)
        // IV length (1 byte) + IV + ciphertext+tag
        return byteArrayOf(iv.size.toByte()) + iv + ciphertext
    }

    override fun decrypt(ciphertext: ByteArray): ByteArray {
        require(ciphertext.isNotEmpty()) { "Ciphertext is empty" }
        val ivSize = ciphertext[0].toInt() and 0xFF
        require(ivSize in 1..ciphertext.lastIndex) { "Invalid IV size" }
        val iv = ciphertext.copyOfRange(1, 1 + ivSize)
        val encrypted = ciphertext.copyOfRange(1 + ivSize, ciphertext.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateSecretKey(),
            GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv),
        )
        return cipher.doFinal(encrypted)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(keyAlias, null) as? SecretKey)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE,
        )
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE_BITS)
                .build(),
        )
        return keyGenerator.generateKey()
    }

    companion object {
        const val DEFAULT_KEY_ALIAS = "folio_auth_session_aes"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val KEY_SIZE_BITS = 256

        fun encodeToString(bytes: ByteArray): String =
            Base64.encodeToString(bytes, Base64.NO_WRAP)

        fun decodeFromString(value: String): ByteArray =
            Base64.decode(value, Base64.NO_WRAP)
    }
}
