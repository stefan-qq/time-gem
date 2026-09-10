package com.dragonpi.timegem.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.timeGemDataStore by preferencesDataStore(name = "time_gem_preferences")

data class TimeGemPreferences(
    val setupCompleted: Boolean = false,
    val calendarEnabled: Boolean = true,
    val routinesEnabled: Boolean = true,
    val wellbeingEnabled: Boolean = true,
    val reflectionsEnabled: Boolean = true,
    val dynamicColorEnabled: Boolean = false,
)

data class SetupChoices(
    val calendarEnabled: Boolean,
    val routinesEnabled: Boolean,
    val wellbeingEnabled: Boolean,
    val reflectionsEnabled: Boolean,
    val dynamicColorEnabled: Boolean,
)

class AppPreferencesRepository(
    private val context: Context,
) {
    private object Keys {
        val setupCompleted = booleanPreferencesKey("setup_completed")
        val calendarEnabled = booleanPreferencesKey("feature_calendar")
        val routinesEnabled = booleanPreferencesKey("feature_routines")
        val wellbeingEnabled = booleanPreferencesKey("feature_wellbeing")
        val reflectionsEnabled = booleanPreferencesKey("feature_reflections")
        val dynamicColorEnabled = booleanPreferencesKey("dynamic_color")
    }

    val preferences: Flow<TimeGemPreferences> = context.timeGemDataStore.data.map { values ->
        TimeGemPreferences(
            setupCompleted = values[Keys.setupCompleted] ?: false,
            calendarEnabled = values[Keys.calendarEnabled] ?: true,
            routinesEnabled = values[Keys.routinesEnabled] ?: true,
            wellbeingEnabled = values[Keys.wellbeingEnabled] ?: true,
            reflectionsEnabled = values[Keys.reflectionsEnabled] ?: true,
            dynamicColorEnabled = values[Keys.dynamicColorEnabled] ?: false,
        )
    }

    suspend fun completeSetup(choices: SetupChoices) {
        context.timeGemDataStore.edit { values ->
            values[Keys.calendarEnabled] = choices.calendarEnabled
            values[Keys.routinesEnabled] = choices.routinesEnabled
            values[Keys.wellbeingEnabled] = choices.wellbeingEnabled
            values[Keys.reflectionsEnabled] = choices.reflectionsEnabled
            values[Keys.dynamicColorEnabled] = choices.dynamicColorEnabled
            values[Keys.setupCompleted] = true
        }
    }
}
