package com.dragonpi.timegem.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object HomeDestination : NavKey

@Serializable
data object CalendarDestination : NavKey

@Serializable
data object RoutinesDestination : NavKey

@Serializable
data object WellbeingDestination : NavKey

@Serializable
data object SettingsDestination : NavKey
