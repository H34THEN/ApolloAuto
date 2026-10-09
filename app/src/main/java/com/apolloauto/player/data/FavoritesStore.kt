package com.apolloauto.player.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FavoritesStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _ids = MutableStateFlow(prefs.getStringSet(KEY, emptySet())?.toSet() ?: emptySet())
    val ids: StateFlow<Set<String>> = _ids.asStateFlow()

    fun isFavorite(id: String): Boolean = id in _ids.value

    fun toggle(id: String) {
        val next = _ids.value.toMutableSet()
        if (!next.add(id)) next.remove(id)
        _ids.value = next
        prefs.edit().putStringSet(KEY, next).apply()
    }

    companion object {
        private const val PREFS = "apollo_favorites"
        private const val KEY = "ids"
    }
}
