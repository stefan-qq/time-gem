package com.dragonpi.timegem.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
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
    val appearance: Appearance = Appearance(),
    val homeOptions: HomeOptions = HomeOptions(),
    val interactions: InteractionOptions = InteractionOptions(),
)

data class InteractionOptions(
    val askBeforeSaving: Boolean = false,
    val haptics: Boolean = true,
    val themedLogo: Boolean = true,
)

enum class NoteSort(val label: String) { MODIFIED("Date modified"), CREATED("Date created"), TITLE("Title A to Z") }

data class HomeOptions(
    val gridLayout: Boolean = true,
    val bottomSearch: Boolean = false,
    val sort: NoteSort = NoteSort.MODIFIED,
    val showToday: Boolean = true,
    val showCalendar: Boolean = true,
    val showWeek: Boolean = true,
)

data class SetupChoices(
    val calendarEnabled: Boolean,
    val routinesEnabled: Boolean,
    val wellbeingEnabled: Boolean,
    val reflectionsEnabled: Boolean,
    val bottomSearch: Boolean = false,
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
        val themeMode = stringPreferencesKey("theme_mode")
        val colorSource = stringPreferencesKey("color_source")
        val palette = stringPreferencesKey("color_palette")
        val gridLayout = booleanPreferencesKey("home_grid_layout")
        val bottomSearch = booleanPreferencesKey("bottom_search")
        val sort = stringPreferencesKey("note_sort")
        val showToday = booleanPreferencesKey("home_show_today")
        val showCalendar = booleanPreferencesKey("home_show_calendar")
        val showWeek = booleanPreferencesKey("home_show_week")
        val askBeforeSaving = booleanPreferencesKey("ask_before_saving")
        val haptics = booleanPreferencesKey("subtle_haptics")
        val themedLogo = booleanPreferencesKey("themed_search_logo")
    }

    val preferences: Flow<TimeGemPreferences> = context.timeGemDataStore.data.map { values ->
        TimeGemPreferences(
            setupCompleted = values[Keys.setupCompleted] ?: false,
            calendarEnabled = values[Keys.calendarEnabled] ?: true,
            routinesEnabled = values[Keys.routinesEnabled] ?: true,
            wellbeingEnabled = values[Keys.wellbeingEnabled] ?: true,
            reflectionsEnabled = values[Keys.reflectionsEnabled] ?: true,
            appearance = Appearance(
                mode = ThemeMode.entries.firstOrNull { it.name == values[Keys.themeMode] } ?: ThemeMode.SYSTEM,
                source = ColorSource.entries.firstOrNull { it.name == values[Keys.colorSource] }
                    ?: if (values[Keys.dynamicColorEnabled] == true) ColorSource.WALLPAPER else ColorSource.TIME_GEM,
                palette = Palette.entries.firstOrNull { it.name == values[Keys.palette] } ?: Palette.BLUE,
            ),
            interactions = InteractionOptions(
                askBeforeSaving = values[Keys.askBeforeSaving] ?: false,
                haptics = values[Keys.haptics] ?: true,
                themedLogo = values[Keys.themedLogo] ?: true,
            ),
            homeOptions = HomeOptions(
                gridLayout = values[Keys.gridLayout] ?: true,
                bottomSearch = values[Keys.bottomSearch] ?: false,
                sort = NoteSort.entries.firstOrNull { it.name == values[Keys.sort] } ?: NoteSort.MODIFIED,
                showToday = values[Keys.showToday] ?: true,
                showCalendar = values[Keys.showCalendar] ?: (values[Keys.calendarEnabled] ?: true),
                showWeek = values[Keys.showWeek] ?: true,
            ),
        )
    }

    suspend fun completeSetup(choices: SetupChoices) {
        context.timeGemDataStore.edit { values ->
            values[Keys.calendarEnabled] = choices.calendarEnabled
            values[Keys.routinesEnabled] = choices.routinesEnabled
            values[Keys.wellbeingEnabled] = choices.wellbeingEnabled
            values[Keys.reflectionsEnabled] = choices.reflectionsEnabled
            values[Keys.showToday] = choices.routinesEnabled || choices.wellbeingEnabled
            values[Keys.showCalendar] = choices.calendarEnabled
            values[Keys.showWeek] = choices.reflectionsEnabled
            values[Keys.bottomSearch] = choices.bottomSearch
            values[Keys.setupCompleted] = true
        }
    }

    suspend fun updateAppearance(appearance: Appearance) {
        context.timeGemDataStore.edit { values ->
            values[Keys.themeMode] = appearance.mode.name
            values[Keys.colorSource] = appearance.source.name
            values[Keys.palette] = appearance.palette.name
        }
        com.dragonpi.timegem.widget.QuickCaptureWidget.refresh(context)
    }

    suspend fun updateFeatures(routines: Boolean, wellbeing: Boolean, reflections: Boolean) {
        context.timeGemDataStore.edit { values ->
            values[Keys.routinesEnabled] = routines
            values[Keys.wellbeingEnabled] = wellbeing
            values[Keys.reflectionsEnabled] = reflections
        }
    }

    suspend fun updateInteractions(options: InteractionOptions) {
        context.timeGemDataStore.edit { values ->
            values[Keys.askBeforeSaving] = options.askBeforeSaving
            values[Keys.haptics] = options.haptics
            values[Keys.themedLogo] = options.themedLogo
        }
        com.dragonpi.timegem.widget.QuickCaptureWidget.refresh(context)
    }

    suspend fun updateHomeOptions(options: HomeOptions) {
        context.timeGemDataStore.edit { values ->
            values[Keys.bottomSearch] = options.bottomSearch
            values[Keys.gridLayout] = options.gridLayout
            values[Keys.sort] = options.sort.name
            values[Keys.showToday] = options.showToday
            values[Keys.showCalendar] = options.showCalendar
            values[Keys.showWeek] = options.showWeek
        }
    }
}
