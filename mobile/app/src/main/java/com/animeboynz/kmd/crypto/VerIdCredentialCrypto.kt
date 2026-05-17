package com.animeboynz.kmd.crypto

import android.util.Base64
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

object VerIdCredentialCrypto {
    private const val KEY_ALGORITHM = "EC"
    private const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
    private const val CURVE = "secp256r1"
    private const val BASE64_FLAGS = Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING

    data class StoredKeyPair(
        val publicKeyBase64: String,
        val privateKeyBase64: String,
    )

    fun generateKeyPair(): StoredKeyPair {
        val generator = KeyPairGenerator.getInstance(KEY_ALGORITHM)
        generator.initialize(ECGenParameterSpec(CURVE))
        val keyPair = generator.generateKeyPair()
        return StoredKeyPair(
            publicKeyBase64 = encode(keyPair.public.encoded),
            privateKeyBase64 = encode(keyPair.private.encoded),
        )
    }

    fun sign(privateKeyBase64: String, data: String): String {
        val signature = Signature.getInstance(SIGNATURE_ALGORITHM)
        signature.initSign(privateKeyFromBase64(privateKeyBase64))
        signature.update(data.toByteArray(Charsets.UTF_8))
        return encode(signature.sign())
    }

    fun verify(publicKeyBase64: String, data: String, signatureBase64: String): Boolean {
        return runCatching {
            val signature = Signature.getInstance(SIGNATURE_ALGORITHM)
            signature.initVerify(publicKeyFromBase64(publicKeyBase64))
            signature.update(data.toByteArray(Charsets.UTF_8))
            signature.verify(decode(signatureBase64))
        }.getOrDefault(false)
    }

    fun encodeText(value: String): String = encode(value.toByteArray(Charsets.UTF_8))

    fun decodeText(value: String): String = String(decode(value), Charsets.UTF_8)

    private fun privateKeyFromBase64(value: String): PrivateKey {
        val keySpec = PKCS8EncodedKeySpec(decode(value))
        return KeyFactory.getInstance(KEY_ALGORITHM).generatePrivate(keySpec)
    }

    private fun publicKeyFromBase64(value: String): PublicKey {
        val keySpec = X509EncodedKeySpec(decode(value))
        return KeyFactory.getInstance(KEY_ALGORITHM).generatePublic(keySpec)
    }

    private fun encode(bytes: ByteArray): String = Base64.encodeToString(bytes, BASE64_FLAGS)

    private fun decode(value: String): ByteArray = Base64.decode(value, BASE64_FLAGS)
}
