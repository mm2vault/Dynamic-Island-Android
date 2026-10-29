package com.mm2vault.dynamicisland.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class IslandSettings(
    val widthDp: Int = DEFAULT_WIDTH,
    val heightDp: Int = DEFAULT_HEIGHT,
    val offsetDp: Int = DEFAULT_OFFSET,
    val animation: Float = DEFAULT_ANIMATION,
    val glow: Float = DEFAULT_GLOW,
) {
    companion object {
        const val DEFAULT_WIDTH = 126
        const val DEFAULT_HEIGHT = 36
        const val DEFAULT_OFFSET = 8
        const val DEFAULT_ANIMATION = 0.6f
        const val DEFAULT_GLOW = 0.5f
        val WIDTH_RANGE = 100..180
        val HEIGHT_RANGE = 30..48
        val OFFSET_RANGE = 0..48
    }

    fun sanitized() = copy(
        widthDp = widthDp.coerceIn(WIDTH_RANGE),
        heightDp = heightDp.coerceIn(HEIGHT_RANGE),
        offsetDp = offsetDp.coerceIn(OFFSET_RANGE),
        animation = animation.coerceIn(0f, 1f),
        glow = glow.coerceIn(0f, 1f),
    )
}

private val Context.store by preferencesDataStore(name = "island_settings")

class SettingsRepository(private val context: Context) {
    private object K {
        val width = intPreferencesKey("width")
        val height = intPreferencesKey("height")
        val offset = intPreferencesKey("offset")
        val animation = floatPreferencesKey("animation")
        val glow = floatPreferencesKey("glow")
    }

    val flow: Flow<IslandSettings> = context.store.data.map { it.toSettings() }

    suspend fun update(transform: (IslandSettings) -> IslandSettings) {
        context.store.edit { p ->
            val n = transform(p.toSettings()).sanitized()
            p[K.width] = n.widthDp
            p[K.height] = n.heightDp
            p[K.offset] = n.offsetDp
            p[K.animation] = n.animation
            p[K.glow] = n.glow
        }
    }

    suspend fun reset() = context.store.edit { it.clear() }

    private fun Preferences.toSettings() = IslandSettings(
        widthDp = this[K.width] ?: IslandSettings.DEFAULT_WIDTH,
        heightDp = this[K.height] ?: IslandSettings.DEFAULT_HEIGHT,
        offsetDp = this[K.offset] ?: IslandSettings.DEFAULT_OFFSET,
        animation = this[K.animation] ?: IslandSettings.DEFAULT_ANIMATION,
        glow = this[K.glow] ?: IslandSettings.DEFAULT_GLOW,
    ).sanitized()
}
