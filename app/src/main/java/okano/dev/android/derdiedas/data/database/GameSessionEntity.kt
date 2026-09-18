package okano.dev.android.derdiedas.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Database entity representing a completed game session
 */
@Entity(tableName = "game_sessions")
data class GameSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val totalCards: Int,
    val correctAnswers: Int,
    val wrongAnswers: Int,
    val accuracyPercentage: Int,
    val durationMillis: Long,
    val cardsPerMinute: Float,
    val timestamp: Long = System.currentTimeMillis()
)
