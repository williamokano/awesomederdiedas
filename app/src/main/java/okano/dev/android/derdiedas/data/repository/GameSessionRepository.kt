package okano.dev.android.derdiedas.data.repository

import kotlinx.coroutines.flow.Flow
import okano.dev.android.derdiedas.data.database.GameSessionDao
import okano.dev.android.derdiedas.data.database.GameSessionEntity

/**
 * Repository for managing game session data
 */
class GameSessionRepository(private val dao: GameSessionDao) {

    fun getAllGameSessions(): Flow<List<GameSessionEntity>> {
        return dao.getAllGameSessions()
    }

    fun getRecentGameSessions(limit: Int): Flow<List<GameSessionEntity>> {
        return dao.getRecentGameSessions(limit)
    }

    suspend fun saveGameSession(session: GameSessionEntity): Long {
        return dao.insertGameSession(session)
    }

    suspend fun deleteSession(sessionId: Long) {
        dao.deleteSession(sessionId)
    }

    suspend fun clearAllSessions() {
        dao.deleteAllSessions()
    }
}
