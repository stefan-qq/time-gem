package com.dragonpi.timegem.data.preferences

enum class ThemeMode(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark") }
enum class ColorSource { TIME_GEM, WALLPAPER, PRESET }
enum class Palette(val label: String, val seed: Long) {
    RED("Red", 0xFFF44336), PINK("Pink", 0xFFE91E63), PURPLE("Purple", 0xFF9C27B0),
    DEEP_PURPLE("Deep purple", 0xFF673AB7), INDIGO("Indigo", 0xFF3F51B5), BLUE("Blue", 0xFF2196F3),
    LIGHT_BLUE("Light blue", 0xFF03A9F4), CYAN("Cyan", 0xFF00BCD4), TEAL("Teal", 0xFF009688),
    GREEN("Green", 0xFF4CAF50), LIGHT_GREEN("Light green", 0xFF8BC34A), LIME("Lime", 0xFFCDDC39),
    YELLOW("Yellow", 0xFFFFEB3B), AMBER("Amber", 0xFFFFC107), ORANGE("Orange", 0xFFFF9800),
    DEEP_ORANGE("Deep orange", 0xFFFF5722), BROWN("Brown", 0xFF795548), GREY("Grey", 0xFF9E9E9E),
    BLUE_GREY("Blue grey", 0xFF607D8B),
}

data class Appearance(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val source: ColorSource = ColorSource.TIME_GEM,
    val palette: Palette = Palette.BLUE,
)
