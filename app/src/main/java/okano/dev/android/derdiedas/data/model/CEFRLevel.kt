package okano.dev.android.derdiedas.data.model

/**
 * CEFR (Common European Framework of Reference) language proficiency levels
 */
enum class CEFRLevel(val displayName: String, val order: Int) {
    A1("A1", 1),
    A2("A2", 2),
    B1("B1", 3),
    B2("B2", 4),
    C1("C1", 5),
    C2("C2", 6),
    ALL("All", 999);

    companion object {
        fun fromDisplayName(name: String): CEFRLevel {
            return entries.find { it.displayName == name } ?: ALL
        }

        /**
         * Get all levels up to and including the specified level
         * For example, A2 returns [A1, A2], B1 returns [A1, A2, B1]
         */
        fun getLevelsUpTo(level: CEFRLevel): List<CEFRLevel> {
            return if (level == ALL) {
                entries.filter { it != ALL }
            } else {
                entries.filter { it.order <= level.order && it != ALL }
            }
        }
    }
}
