package com.kdelehoi.marshrutky.domain.model

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

/** Мова інтерфейсу. [tag] — тег IETF для `Locale`; у системної його немає, бо ми її не задаємо. */
enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    UKRAINIAN("uk"),
    ENGLISH("en"),
    RUSSIAN("ru")
}
