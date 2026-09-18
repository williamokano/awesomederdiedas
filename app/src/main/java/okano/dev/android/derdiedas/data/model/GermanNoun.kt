package okano.dev.android.derdiedas.data.model

/**
 * Represents a German noun with its grammatical article, translations, and CEFR level
 */
data class GermanNoun(
    val noun: String,
    val article: Article,
    val translations: Map<String, String>, // Map of language code to translation
    val level: CEFRLevel = CEFRLevel.A1
) {
    fun getTranslation(language: Language): String {
        return translations[language.code] ?: translations["en"] ?: noun
    }
}

/**
 * German grammatical articles (der, die, das)
 */
enum class Article {
    DER,  // masculine
    DIE,  // feminine
    DAS;  // neuter

    fun getDisplayName(): String {
        return this.name
    }
}
