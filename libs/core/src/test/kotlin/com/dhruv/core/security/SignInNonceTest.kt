package com.dhruv.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

/**
 * Guards the shared Google Sign-In nonce pair (`ONB-BR-001`'s exchange, reused by Settings ›
 * Account's sign-in since 0b.2). Extracted to `:libs:core` during the Phase 0b review pass, which
 * found it duplicated verbatim between `SignInScreen` and `AccountSettingsScreen` — a security
 * primitive on the auth path with two independently-maintained copies and no test on either.
 *
 * The failure this exists to catch is silent: swapping [SignInNonce.raw] and
 * [SignInNonce.sha256Hex] at either call site still compiles and still shows a Google account
 * picker — GoTrue just rejects the exchange afterwards with a nonce mismatch.
 */
class SignInNonceTest {
    @Test
    fun `sha256Hex is the SHA-256 hex digest of raw, not the raw value`() {
        val nonce = SignInNonce.generate()

        val expected =
            MessageDigest
                .getInstance("SHA-256")
                .digest(nonce.raw.toByteArray())
                .joinToString(separator = "") { "%02x".format(it) }

        assertEquals(expected, nonce.sha256Hex)
        assertNotEquals("raw and hashed forms must never be interchangeable", nonce.raw, nonce.sha256Hex)
    }

    @Test
    fun `raw is 32 bytes hex-encoded and sha256Hex is 32 bytes hex-encoded`() {
        val nonce = SignInNonce.generate()

        assertEquals(64, nonce.raw.length)
        assertEquals(64, nonce.sha256Hex.length)
        assertTrue(nonce.raw.all { it in "0123456789abcdef" })
        assertTrue(nonce.sha256Hex.all { it in "0123456789abcdef" })
    }

    @Test
    fun `every generate is fresh — a nonce is never reused across attempts`() {
        val generated = (1..100).map { SignInNonce.generate().raw }
        assertEquals("all 100 generated nonces must be distinct", 100, generated.toSet().size)
    }
}
