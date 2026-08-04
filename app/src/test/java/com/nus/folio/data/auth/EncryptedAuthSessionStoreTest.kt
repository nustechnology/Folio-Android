package com.nus.folio.data.auth

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.nus.folio.domain.model.AuthSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class EncryptedAuthSessionStoreTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var scope: CoroutineScope

    @Before
    fun setUp() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `write and read round trip encrypts session`() = runTest {
        val store = createStore()
        val session = AuthSession(
            email = "jordan@folio.app",
            displayName = "Jordan Lee",
            userId = "user-1",
            accessToken = "access",
            refreshToken = "refresh",
        )

        store.write(session)

        assertEquals(session, store.read())
    }

    @Test
    fun `clear removes encrypted session`() = runTest {
        val store = createStore()
        store.write(
            AuthSession(
                email = "jordan@folio.app",
                accessToken = "access",
                refreshToken = "refresh",
            ),
        )

        store.clear()

        assertNull(store.read())
    }

    @Test
    fun `migrates legacy plaintext session then clears legacy`() = runTest {
        var legacyCleared = false
        val legacySession = AuthSession(
            email = "legacy@folio.app",
            displayName = "Legacy User",
            accessToken = "legacy-access",
            refreshToken = "legacy-refresh",
        )
        val store = createStore(
            legacyReader = { legacySession.takeUnless { legacyCleared } },
            legacyClearer = { legacyCleared = true },
        )

        assertEquals(legacySession, store.read())
        assertTrue(legacyCleared)
    }

    @Test
    fun `failed legacy migration keeps plaintext and retries`() = runTest {
        var legacyCleared = false
        val legacySession = AuthSession(
            email = "legacy@folio.app",
            displayName = "Legacy User",
            accessToken = "legacy-access",
            refreshToken = "legacy-refresh",
        )
        val swappable = SwappableCipher(
            object : AuthSessionCipher {
                override fun encrypt(plaintext: ByteArray): ByteArray {
                    error("encrypt failed")
                }

                override fun decrypt(ciphertext: ByteArray): ByteArray = error("unused")
            },
        )
        val store = createStore(
            cipher = swappable,
            legacyReader = { legacySession.takeUnless { legacyCleared } },
            legacyClearer = { legacyCleared = true },
        )

        try {
            store.read()
            org.junit.Assert.fail("Expected migration encrypt to fail")
        } catch (expected: IllegalStateException) {
            assertEquals("encrypt failed", expected.message)
        }
        assertFalse(legacyCleared)

        swappable.delegate = AesGcmTestCipher()

        assertEquals(legacySession, store.read())
        assertTrue(legacyCleared)
    }

    @Test
    fun `corrupt ciphertext clears store`() = runTest {
        val swappable = SwappableCipher(AesGcmTestCipher())
        val store = createStore(cipher = swappable)
        store.write(
            AuthSession(
                email = "jordan@folio.app",
                accessToken = "access",
                refreshToken = "refresh",
            ),
        )
        swappable.delegate = BrokenCipher()

        assertNull(store.read())
    }

    @Test
    fun `cancellation during decrypt does not clear session`() = runTest {
        val workingCipher = AesGcmTestCipher()
        val swappable = SwappableCipher(workingCipher)
        val store = createStore(cipher = swappable)
        val session = AuthSession(
            email = "jordan@folio.app",
            accessToken = "access",
            refreshToken = "refresh",
        )
        store.write(session)
        swappable.delegate = object : AuthSessionCipher {
            override fun encrypt(plaintext: ByteArray): ByteArray = error("unused")
            override fun decrypt(ciphertext: ByteArray): ByteArray {
                throw kotlin.coroutines.cancellation.CancellationException("cancelled")
            }
        }

        try {
            store.read()
            org.junit.Assert.fail("Expected CancellationException")
        } catch (_: kotlin.coroutines.cancellation.CancellationException) {
            // Expected: cancellation must propagate, not clear storage.
        }

        swappable.delegate = workingCipher
        assertEquals(session, store.read())
    }

    private fun createStore(
        cipher: AuthSessionCipher = AesGcmTestCipher(),
        legacyReader: () -> AuthSession? = { null },
        legacyClearer: () -> Unit = {},
    ): EncryptedAuthSessionStore {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = {
                File(temporaryFolder.root, "auth-${System.nanoTime()}.preferences_pb")
            },
        )
        return EncryptedAuthSessionStore(
            cipher = cipher,
            dataStore = dataStore,
            legacyReader = legacyReader,
            legacyClearer = legacyClearer,
            // JVM unit tests cannot use android.util.Base64 (API stubs are not mocked).
            encodePayload = { java.util.Base64.getEncoder().encodeToString(it) },
            decodePayload = { java.util.Base64.getDecoder().decode(it) },
        )
    }

    private class SwappableCipher(
        var delegate: AuthSessionCipher,
    ) : AuthSessionCipher {
        override fun encrypt(plaintext: ByteArray): ByteArray = delegate.encrypt(plaintext)
        override fun decrypt(ciphertext: ByteArray): ByteArray = delegate.decrypt(ciphertext)
    }

    /** JVM-safe AES-GCM stand-in for Android Keystore in unit tests. */
    private class AesGcmTestCipher : AuthSessionCipher {
        private val key: SecretKey = KeyGenerator.getInstance("AES").apply {
            init(256)
        }.generateKey()

        override fun encrypt(plaintext: ByteArray): ByteArray {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            return byteArrayOf(iv.size.toByte()) + iv + cipher.doFinal(plaintext)
        }

        override fun decrypt(ciphertext: ByteArray): ByteArray {
            val ivSize = ciphertext[0].toInt() and 0xFF
            val iv = ciphertext.copyOfRange(1, 1 + ivSize)
            val encrypted = ciphertext.copyOfRange(1 + ivSize, ciphertext.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            return cipher.doFinal(encrypted)
        }
    }

    private class BrokenCipher : AuthSessionCipher {
        override fun encrypt(plaintext: ByteArray): ByteArray = plaintext
        override fun decrypt(ciphertext: ByteArray): ByteArray {
            error("cannot decrypt")
        }
    }
}
