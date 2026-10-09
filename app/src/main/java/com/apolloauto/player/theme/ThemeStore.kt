package com.apolloauto.player.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemeStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _themeId = MutableStateFlow(
        runCatching {
            HudThemeId.valueOf(prefs.getString(KEY, HudThemeId.NEON_GRID.name)!!)
        }.getOrDefault(HudThemeId.NEON_GRID)
    )
    val themeId: StateFlow<HudThemeId> = _themeId.asStateFlow()

    fun setTheme(id: HudThemeId) {
        _themeId.value = id
        prefs.edit().putString(KEY, id.name).apply()
    }

    companion object {
        private const val PREFS = "apollo_theme"
        private const val KEY = "hud_theme"
    }
}
