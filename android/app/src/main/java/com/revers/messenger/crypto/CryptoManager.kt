package com.revers.messenger.crypto

import android.util.Base64
import com.google.crypto.tink.Aead
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AeadKeyTemplates
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.google.crypto.tink.subtle.Hex
import com.google.crypto.tink.subtle.X25519
import java.security.SecureRandom

class CryptoManager(private val context: android.content.Context) {
    private val aead: Aead
    private val keyStoreManager = KeyStoreManager()

    init {
        AeadConfig.register()

        val keysetManager = AndroidKeysetManager.Builder()
            .withSharedPref(context, "revers_messages", "revers_prefs")
            .withKeyTemplate(AeadKeyTemplates.AES256_GCM)
            .withMasterKeyUri("android-keystore://revers_master")
            .build()

        aead = keysetManager.keysetHandle.getPrimitive(Aead::class.java)
    }

    fun generateIdentity(): java.security.KeyPair {
        return keyStoreManager.generateKeyPair()
    }

    fun getIdentityPublicKeyHex(): String {
        return keyStoreManager.getPublicKeyHex()
    }

    fun deriveSharedSecret(theirPublicKeyHex: String): ByteArray {
        val myKeyPair = keyStoreManager.getKeyPair()
            ?: throw IllegalStateException("Ключи не найдены")

        val theirBytes = Hex.decode(theirPublicKeyHex)
        return X25519.computeSharedSecret(
            myKeyPair.private.encoded,
            theirBytes
        )
    }

    fun encryptMessage(sharedSecret: ByteArray, plaintext: String): EncryptedData {
        val nonce = ByteArray(12)
        SecureRandom().nextBytes(nonce)

        val ciphertext = aead.encrypt(
            plaintext.toByteArray(Charsets.UTF_8),
            sharedSecret + nonce
        )

        return EncryptedData(
            nonce = Base64.encodeToString(nonce, Base64.NO_WRAP),
            ciphertext = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        )
    }

    fun decryptMessage(sharedSecret: ByteArray, encryptedData: EncryptedData): String {
        val nonce = Base64.decode(encryptedData.nonce, Base64.NO_WRAP)
        val ciphertext = Base64.decode(encryptedData.ciphertext, Base64.NO_WRAP)

        val plaintext = aead.decrypt(ciphertext, sharedSecret + nonce)
        return String(plaintext, Charsets.UTF_8)
    }

    data class EncryptedData(
        val nonce: String,
        val ciphertext: String
    )
}
