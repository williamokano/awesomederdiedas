package okano.dev.android.derdiedas.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for game sessions
 */
@Dao
interface GameSessionDao {

    @Insert
    suspend fun insertGameSession(session: GameSessionEntity): Long

    @Query("SELECT * FROM game_sessions ORDER BY timestamp DESC")
    fun getAllGameSessions(): Flow<List<GameSessionEntity>>

    @Query("SELECT * FROM game_sessions ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentGameSessions(limit: Int): Flow<List<GameSessionEntity>>

    @Query("DELETE FROM game_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    @Query("DELETE FROM game_sessions")
    suspend fun deleteAllSessions()
}
