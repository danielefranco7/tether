package it.nuovetech.tether.shared

import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Primitive di pairing e cifratura usando solo le API di sistema (compatibili con Android 6 / API 23):
 * ECDH P-256, HKDF-SHA256 (costruito su HMAC) e AES-256-GCM.
 */
object TetherCrypto {

    private const val CURVE = "secp256r1"
    private const val GCM_TAG_BITS = 128
    private const val NONCE_BYTES = 12
    private const val KEY_BYTES = 32
    private val random = SecureRandom()

    /** Coppia di chiavi P-256 per lo scambio ECDH. */
    fun generateKeyPair(): KeyPair =
        KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec(CURVE)) }.generateKeyPair()

    /** Chiave pubblica in formato X.509 DER (il chiamante la codifica in Base64 per il QR). */
    fun encodePublicKey(publicKey: PublicKey): ByteArray = publicKey.encoded

    fun decodePublicKey(encoded: ByteArray): PublicKey =
        KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(encoded))

    /** Segreto condiviso ECDH, poi derivato con HKDF-SHA256 in una chiave AES-256 di sessione. */
    fun deriveSessionKey(privateKey: java.security.PrivateKey, peerPublic: PublicKey, salt: ByteArray): ByteArray {
        val shared = KeyAgreement.getInstance("ECDH").run {
            init(privateKey)
            doPhase(peerPublic, true)
            generateSecret()
        }
        return hkdf(shared, salt, "tether-v1-session".toByteArray(), KEY_BYTES)
    }

    /** HMAC-SHA256 del messaggio con la chiave: usato per la prova di conoscenza del codice di pairing. */
    fun hmac(key: ByteArray, message: ByteArray): ByteArray =
        Mac.getInstance("HmacSHA256").run {
            init(SecretKeySpec(key, "HmacSHA256"))
            doFinal(message)
        }

    /** Cifra con AES-256-GCM; restituisce nonce (12 byte) seguito dal testo cifrato con tag. */
    fun seal(key: ByteArray, plaintext: ByteArray, aad: ByteArray): ByteArray {
        val nonce = ByteArray(NONCE_BYTES).also { random.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, nonce))
        cipher.updateAAD(aad)
        return nonce + cipher.doFinal(plaintext)
    }

    /** Decifra il formato prodotto da [seal]. Lancia [java.security.GeneralSecurityException] se l'autenticazione fallisce. */
    fun open(key: ByteArray, sealed: ByteArray, aad: ByteArray): ByteArray {
        require(sealed.size > NONCE_BYTES) { "messaggio cifrato troppo corto" }
        val nonce = sealed.copyOfRange(0, NONCE_BYTES)
        val body = sealed.copyOfRange(NONCE_BYTES, sealed.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, nonce))
        cipher.updateAAD(aad)
        return cipher.doFinal(body)
    }

    /** Confronto a tempo costante di due array di byte. */
    fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean = MessageDigest.isEqual(a, b)

    /** HKDF-SHA256 (RFC 5869) con estrazione ed espansione. */
    private fun hkdf(ikm: ByteArray, salt: ByteArray, info: ByteArray, length: Int): ByteArray {
        val prk = hmac(salt, ikm)
        val okm = ByteArray(length)
        var previous = ByteArray(0)
        var offset = 0
        var counter = 1
        while (offset < length) {
            previous = hmac(prk, previous + info + byteArrayOf(counter.toByte()))
            val n = minOf(previous.size, length - offset)
            previous.copyInto(okm, offset, 0, n)
            offset += n
            counter++
        }
        return okm
    }
}
