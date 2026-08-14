package com.revers.messenger.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import javax.crypto.KeyAgreement

class KeyStoreManager {
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    private val ALIAS = "revers_identity"

    fun generateKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            "AndroidKeyStore"
        )

        keyPairGenerator.initialize(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_AGREE_KEY or KeyProperties.PURPOSE_SIGN
            )
                .setAlgorithmParameterSpec(
                    java.security.spec.ECGenParameterSpec("secp256r1")
                )
                .setUserAuthenticationRequired(false)
                .build()
        )

        return keyPairGenerator.generateKeyPair()
    }

    fun getKeyPair(): KeyPair? {
        return try {
            val privateKey = keyStore.getKey(ALIAS, null) as? PrivateKey ?: return null
            val publicKey = keyStore.getCertificate(ALIAS)?.publicKey ?: return null
            object : KeyPair(publicKey, privateKey) {}
        } catch (e: Exception) {
            null
        }
    }

    fun getPublicKeyHex(): String {
        return keyStore.getCertificate(ALIAS)?.publicKey?.encoded?.joinToString("") {
            "%02x".format(it)
        } ?: ""
    }

    fun deleteKey() {
        keyStore.deleteEntry(ALIAS)
    }
}
