package com.dragonpi.timegem.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.dragonpi.timegem.data.preferences.TimeGemPreferences
import com.dragonpi.timegem.feature.home.HomeScreen
import com.dragonpi.timegem.feature.placeholder.PlaceholderFeatureScreen

@Composable
fun TimeGemNavigation(
    preferences: TimeGemPreferences,
) {
    val backStack = rememberNavBackStack(HomeDestination)

    fun open(destination: androidx.navigation3.runtime.NavKey) {
        if (backStack.lastOrNull() != destination) {
            backStack.add(destination)
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<HomeDestination> {
                HomeScreen(
                    preferences = preferences,
                    onCalendarClick = { open(CalendarDestination) },
                    onRoutinesClick = { open(RoutinesDestination) },
                    onWellbeingClick = { open(WellbeingDestination) },
                    onSettingsClick = { open(SettingsDestination) },
                )
            }

            entry<CalendarDestination> {
                PlaceholderFeatureScreen(
                    title = "Calendar",
                    message = "Next milestone: events, reminders, birthdays, holidays and note-to-date suggestions.",
                    onBack = { backStack.removeLastOrNull() },
                )
            }

            entry<RoutinesDestination> {
                PlaceholderFeatureScreen(
                    title = "Routines",
                    message = "Next milestone: daily routines, objectives, streaks and completion history.",
                    onBack = { backStack.removeLastOrNull() },
                )
            }

            entry<WellbeingDestination> {
                PlaceholderFeatureScreen(
                    title = "Wellbeing",
                    message = "Later: sleep nudges, focus controls, mood check-ins and weekly reflections.",
                    onBack = { backStack.removeLastOrNull() },
                )
            }

            entry<SettingsDestination> {
                PlaceholderFeatureScreen(
                    title = "Settings",
                    message = "This will become the control center for appearance, features, privacy and reminders.",
                    onBack = { backStack.removeLastOrNull() },
                )
            }
        },
    )
}
