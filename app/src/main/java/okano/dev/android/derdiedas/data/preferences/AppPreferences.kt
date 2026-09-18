package okano.dev.android.derdiedas.data.preferences

import android.content.Context
import android.content.SharedPreferences
import okano.dev.android.derdiedas.data.model.CEFRLevel
import okano.dev.android.derdiedas.data.model.Language
import java.util.Locale

/**
 * Manager for app preferences (language, level, etc.)
 */
class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "derdiedas_prefs",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_LANGUAGE = "language"
        private const val KEY_CEFR_LEVEL = "cefr_level"
    }

    /**
     * Get the selected language, defaulting to system locale if not set
     */
    fun getLanguage(): Language {
        val savedLanguage = prefs.getString(KEY_LANGUAGE, null)
        return if (savedLanguage != null) {
            Language.fromCode(savedLanguage)
        } else {
            // Default to system locale
            val systemLanguage = Locale.getDefault().language
            Language.fromSystemLocale(systemLanguage)
        }
    }

    /**
     * Save the selected language
     */
    fun setLanguage(language: Language) {
        prefs.edit().putString(KEY_LANGUAGE, language.code).apply()
    }

    /**
     * Get the selected CEFR level, defaulting to ALL
     */
    fun getCEFRLevel(): CEFRLevel {
        val savedLevel = prefs.getString(KEY_CEFR_LEVEL, CEFRLevel.ALL.displayName)
        return CEFRLevel.fromDisplayName(savedLevel ?: CEFRLevel.ALL.displayName)
    }

    /**
     * Save the selected CEFR level
     */
    fun setCEFRLevel(level: CEFRLevel) {
        prefs.edit().putString(KEY_CEFR_LEVEL, level.displayName).apply()
    }
}
