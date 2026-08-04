package com.nus.folio.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nus.folio.domain.model.AuthSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException

interface AuthSessionStore {
    suspend fun read(): AuthSession?
    suspend fun write(session: AuthSession)
    suspend fun clear()
}

private val Context.authSessionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = EncryptedAuthSessionStore.DATA_STORE_NAME,
)

/**
 * Persists auth sessions as a Keystore-backed AES-GCM ciphertext in Preferences DataStore.
 * Migrates once from the legacy plaintext SharedPreferences file, then clears it.
 */
class EncryptedAuthSessionStore internal constructor(
    private val cipher: AuthSessionCipher,
    private val dataStore: DataStore<Preferences>,
    private val legacyReader: () -> AuthSession?,
    private val legacyClearer: () -> Unit,
    private val encodePayload: (ByteArray) -> String = AndroidKeystoreAuthSessionCipher::encodeToString,
    private val decodePayload: (String) -> ByteArray = AndroidKeystoreAuthSessionCipher::decodeFromString,
) : AuthSessionStore {

    constructor(
        context: Context,
        cipher: AuthSessionCipher = AndroidKeystoreAuthSessionCipher(),
    ) : this(
        cipher = cipher,
        dataStore = context.applicationContext.authSessionDataStore,
        legacyPrefs = context.applicationContext.getSharedPreferences(
            LEGACY_PREFS_NAME,
            Context.MODE_PRIVATE,
        ),
    )

    private constructor(
        cipher: AuthSessionCipher,
        dataStore: DataStore<Preferences>,
        legacyPrefs: SharedPreferences,
    ) : this(
        cipher = cipher,
        dataStore = dataStore,
        legacyReader = { readLegacyPlaintext(legacyPrefs) },
        legacyClearer = { legacyPrefs.edit().clear().apply() },
    )

    private val migrationMutex = Mutex()

    @Volatile
    private var legacyMigrationCompleted = false

    override suspend fun read(): AuthSession? {
        ensureLegacyMigrated()
        val payload = dataStore.data.first()[KEY_ENCRYPTED_SESSION] ?: return null
        return decodeSession(payload)
    }

    override suspend fun write(session: AuthSession) {
        ensureLegacyMigrated()
        val payload = encodeSession(session)
        dataStore.edit { prefs ->
            prefs[KEY_ENCRYPTED_SESSION] = payload
        }
    }

    override suspend fun clear() {
        migrationMutex.withLock {
            dataStore.edit { it.clear() }
            legacyClearer()
            legacyMigrationCompleted = true
        }
    }

    private suspend fun ensureLegacyMigrated() {
        if (legacyMigrationCompleted) return
        migrationMutex.withLock {
            if (legacyMigrationCompleted) return
            val legacySession = legacyReader()
            if (legacySession != null) {
                val payload = encodeSession(legacySession)
                dataStore.edit { prefs ->
                    prefs[KEY_ENCRYPTED_SESSION] = payload
                }
                // Only drop plaintext after the encrypted copy is persisted so a
                // failed migration can retry on the next read/write.
                legacyClearer()
            }
            legacyMigrationCompleted = true
        }
    }

    private fun encodeSession(session: AuthSession): String {
        val json = buildString {
            append('{')
            appendJsonField(JSON_EMAIL, session.email)
            append(',')
            appendJsonField(JSON_DISPLAY_NAME, session.displayName)
            append(',')
            appendJsonField(JSON_USER_ID, session.userId)
            append(',')
            appendJsonField(JSON_ACCESS_TOKEN, session.accessToken)
            append(',')
            appendJsonField(JSON_REFRESH_TOKEN, session.refreshToken)
            append('}')
        }
        val ciphertext = cipher.encrypt(json.toByteArray(Charsets.UTF_8))
        return encodePayload(ciphertext)
    }

    private fun StringBuilder.appendJsonField(key: String, value: String?) {
        append('"').append(escapeJson(key)).append('"').append(':')
        if (value == null) {
            append("null")
        } else {
            append('"').append(escapeJson(value)).append('"')
        }
    }

    private suspend fun decodeSession(payload: String): AuthSession? =
        try {
            val plaintext = cipher.decrypt(decodePayload(payload))
            val fields = parseJsonObject(String(plaintext, Charsets.UTF_8))
            val email = fields[JSON_EMAIL]?.takeIf { it.isNotBlank() } ?: return null
            AuthSession(
                email = email,
                displayName = fields[JSON_DISPLAY_NAME]?.takeIf { it.isNotBlank() }
                    ?: email.substringBefore("@"),
                userId = fields[JSON_USER_ID]?.takeIf { it.isNotBlank() },
                accessToken = fields[JSON_ACCESS_TOKEN]?.takeIf { it.isNotBlank() },
                refreshToken = fields[JSON_REFRESH_TOKEN]?.takeIf { it.isNotBlank() },
            )
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            clear()
            null
        }

    companion object {
        const val DATA_STORE_NAME = "folio_auth_session_encrypted"
        const val LEGACY_PREFS_NAME = "folio_auth_session"

        private val KEY_ENCRYPTED_SESSION = stringPreferencesKey("encrypted_session")

        private const val LEGACY_KEY_EMAIL = "email"
        private const val LEGACY_KEY_DISPLAY_NAME = "display_name"
        private const val LEGACY_KEY_USER_ID = "user_id"
        private const val LEGACY_KEY_ACCESS_TOKEN = "access_token"
        private const val LEGACY_KEY_REFRESH_TOKEN = "refresh_token"

        private const val JSON_EMAIL = "email"
        private const val JSON_DISPLAY_NAME = "displayName"
        private const val JSON_USER_ID = "userId"
        private const val JSON_ACCESS_TOKEN = "accessToken"
        private const val JSON_REFRESH_TOKEN = "refreshToken"

        private fun readLegacyPlaintext(prefs: SharedPreferences): AuthSession? {
            val email = prefs.getString(LEGACY_KEY_EMAIL, null)?.takeIf { it.isNotBlank() }
                ?: return null
            return AuthSession(
                email = email,
                displayName = prefs.getString(LEGACY_KEY_DISPLAY_NAME, null)
                    ?.takeIf { it.isNotBlank() }
                    ?: email.substringBefore("@"),
                userId = prefs.getString(LEGACY_KEY_USER_ID, null)?.takeIf { it.isNotBlank() },
                accessToken = prefs.getString(LEGACY_KEY_ACCESS_TOKEN, null)
                    ?.takeIf { it.isNotBlank() },
                refreshToken = prefs.getString(LEGACY_KEY_REFRESH_TOKEN, null)
                    ?.takeIf { it.isNotBlank() },
            )
        }

        private fun escapeJson(value: String): String =
            buildString(value.length) {
                for (ch in value) {
                    when (ch) {
                        '\\' -> append("\\\\")
                        '"' -> append("\\\"")
                        '\n' -> append("\\n")
                        '\r' -> append("\\r")
                        '\t' -> append("\\t")
                        else -> append(ch)
                    }
                }
            }

        /**
         * Minimal flat JSON object parser for session payloads we encode ourselves.
         */
        internal fun parseJsonObject(json: String): Map<String, String?> {
            val trimmed = json.trim()
            require(trimmed.startsWith("{") && trimmed.endsWith("}")) { "Invalid JSON object" }
            val body = trimmed.substring(1, trimmed.lastIndex).trim()
            if (body.isEmpty()) return emptyMap()

            val result = linkedMapOf<String, String?>()
            var index = 0
            while (index < body.length) {
                while (index < body.length && body[index].isWhitespace()) index++
                require(index < body.length && body[index] == '"') { "Expected key" }
                val keyEnd = findClosingQuote(body, index + 1)
                val key = unescapeJson(body.substring(index + 1, keyEnd))
                index = keyEnd + 1
                while (index < body.length && body[index].isWhitespace()) index++
                require(index < body.length && body[index] == ':') { "Expected colon" }
                index++
                while (index < body.length && body[index].isWhitespace()) index++
                require(index < body.length) { "Expected value" }
                when {
                    body.startsWith("null", index) -> {
                        result[key] = null
                        index += 4
                    }
                    body[index] == '"' -> {
                        val valueEnd = findClosingQuote(body, index + 1)
                        result[key] = unescapeJson(body.substring(index + 1, valueEnd))
                        index = valueEnd + 1
                    }
                    else -> error("Unsupported JSON value")
                }
                while (index < body.length && body[index].isWhitespace()) index++
                if (index < body.length && body[index] == ',') {
                    index++
                }
            }
            return result
        }

        private fun findClosingQuote(text: String, start: Int): Int {
            var i = start
            while (i < text.length) {
                when (text[i]) {
                    '\\' -> i += 2
                    '"' -> return i
                    else -> i++
                }
            }
            error("Unterminated string")
        }

        private fun unescapeJson(value: String): String =
            buildString(value.length) {
                var i = 0
                while (i < value.length) {
                    val ch = value[i]
                    if (ch == '\\' && i + 1 < value.length) {
                        when (value[i + 1]) {
                            '\\' -> append('\\')
                            '"' -> append('"')
                            'n' -> append('\n')
                            'r' -> append('\r')
                            't' -> append('\t')
                            else -> append(value[i + 1])
                        }
                        i += 2
                    } else {
                        append(ch)
                        i++
                    }
                }
            }
    }
}

class InMemoryAuthSessionStore : AuthSessionStore {
    private var session: AuthSession? = null

    override suspend fun read(): AuthSession? = session

    override suspend fun write(session: AuthSession) {
        this.session = session
    }

    override suspend fun clear() {
        session = null
    }
}
