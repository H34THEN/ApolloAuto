package com.apolloauto.player.echohunt

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Local-only AcoustID application API key storage.
 * Do not store MusicBrainz account passwords here.
 */
class EchoHuntCredentialStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _acoustIdKey = MutableStateFlow(prefs.getString(KEY_ACOUSTID, null))
    val acoustIdApplicationKey: StateFlow<String?> = _acoustIdKey.asStateFlow()

    fun setAcoustIdApplicationKey(key: String?) {
        val cleaned = key?.trim()?.ifBlank { null }
        _acoustIdKey.value = cleaned
        prefs.edit().putString(KEY_ACOUSTID, cleaned).apply()
    }

    fun asCredentials(appVersion: String): EchoHuntCredentials =
        DefaultEchoHuntCredentials(
            acoustIdKeyProvider = { _acoustIdKey.value },
            appVersion = appVersion
        )

    companion object {
        private const val PREFS = "apollo_echohunt"
        private const val KEY_ACOUSTID = "acoustid_application_key"
    }
}
