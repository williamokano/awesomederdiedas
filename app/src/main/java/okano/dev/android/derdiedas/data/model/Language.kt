package okano.dev.android.derdiedas.data.model

/**
 * Supported languages in the app
 */
enum class Language(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    PORTUGUESE("pt", "Português");

    companion object {
        fun fromCode(code: String): Language {
            return entries.find { it.code == code } ?: ENGLISH
        }

        fun fromSystemLocale(localeLanguage: String): Language {
            return when (localeLanguage.lowercase()) {
                "pt" -> PORTUGUESE
                else -> ENGLISH
            }
        }
    }
}
