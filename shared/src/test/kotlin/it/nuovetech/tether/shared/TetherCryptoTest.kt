package it.nuovetech.tether.shared

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import java.security.GeneralSecurityException

class TetherCryptoTest {

    @Test
    fun ecdhAgreesOnBothSides() {
        val slave = TetherCrypto.generateKeyPair()
        val master = TetherCrypto.generateKeyPair()
        val salt = "pairing-id-test".toByteArray()

        val masterKey = TetherCrypto.deriveSessionKey(master.private, slave.public, salt)
        val slaveKey = TetherCrypto.deriveSessionKey(slave.private, master.public, salt)

        assertArrayEquals(masterKey, slaveKey)
        assertEquals(32, masterKey.size)
    }

    @Test
    fun differentSaltGivesDifferentKey() {
        val slave = TetherCrypto.generateKeyPair()
        val master = TetherCrypto.generateKeyPair()
        val k1 = TetherCrypto.deriveSessionKey(master.private, slave.public, byteArrayOf(1))
        val k2 = TetherCrypto.deriveSessionKey(master.private, slave.public, byteArrayOf(2))
        assertFalse(k1.contentEquals(k2))
    }

    @Test
    fun publicKeyRoundTripsThroughEncoding() {
        val pair = TetherCrypto.generateKeyPair()
        val decoded = TetherCrypto.decodePublicKey(TetherCrypto.encodePublicKey(pair.public))
        assertArrayEquals(pair.public.encoded, decoded.encoded)
    }

    @Test
    fun sealThenOpenReturnsPlaintext() {
        val key = ByteArray(32) { it.toByte() }
        val aad = "pid-1".toByteArray()
        val message = "{\"type\":\"EVENT\"}".toByteArray()

        val sealed = TetherCrypto.seal(key, message, aad)
        assertArrayEquals(message, TetherCrypto.open(key, sealed, aad))
    }

    @Test
    fun tamperedCiphertextIsRejected() {
        val key = ByteArray(32) { 7 }
        val aad = byteArrayOf(1)
        val sealed = TetherCrypto.seal(key, "secret".toByteArray(), aad)
        sealed[sealed.size - 1] = (sealed[sealed.size - 1].toInt() xor 1).toByte()

        assertThrows(GeneralSecurityException::class.java) { TetherCrypto.open(key, sealed, aad) }
    }

    @Test
    fun wrongAadIsRejected() {
        val key = ByteArray(32) { 3 }
        val sealed = TetherCrypto.seal(key, "x".toByteArray(), "a".toByteArray())

        assertThrows(GeneralSecurityException::class.java) { TetherCrypto.open(key, sealed, "b".toByteArray()) }
    }

    @Test
    fun hmacMatchesRfc4231TestCase2() {
        // RFC 4231, caso di test 2: HMAC-SHA256("Jefe", "what do ya want for nothing?")
        val mac = TetherCrypto.hmac("Jefe".toByteArray(), "what do ya want for nothing?".toByteArray())
        val hex = mac.joinToString("") { "%02x".format(it) }
        assertEquals("5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843", hex)
    }
}
