package okano.dev.android.derdiedas.data.repository

import okano.dev.android.derdiedas.data.model.GermanNoun

/**
 * Repository interface for accessing German nouns.
 * This abstraction allows for different data sources (local, remote, database, etc.)
 */
interface NounRepository {
    /**
     * Get all available German nouns
     */
    fun getAllNouns(): List<GermanNoun>

    /**
     * Get a random German noun
     */
    fun getRandomNoun(): GermanNoun?
}