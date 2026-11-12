package com.narcy.notesai.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ThemeMode { System, Light, Dark }

data class AppSettings(
    val theme: ThemeMode = ThemeMode.System,
    val grid: Boolean = true,
    val dynamicColor: Boolean = true
)

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<AppSettings> = _state

    private fun load(): AppSettings = AppSettings(
        theme = runCatching { ThemeMode.valueOf(prefs.getString("theme", "System") ?: "System") }
            .getOrDefault(ThemeMode.System),
        grid = prefs.getBoolean("grid", true),
        dynamicColor = prefs.getBoolean("dynamic", true)
    )

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_state.value)
        _state.value = next
        prefs.edit()
            .putString("theme", next.theme.name)
            .putBoolean("grid", next.grid)
            .putBoolean("dynamic", next.dynamicColor)
            .apply()
    }
}
