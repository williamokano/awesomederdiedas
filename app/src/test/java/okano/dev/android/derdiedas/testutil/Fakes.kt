package okano.dev.android.derdiedas.testutil

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import okano.dev.android.derdiedas.data.database.GameSessionDao
import okano.dev.android.derdiedas.data.database.GameSessionEntity
import okano.dev.android.derdiedas.data.model.GermanNoun
import okano.dev.android.derdiedas.data.repository.NounRepository

/**
 * In-memory GameSessionDao for unit tests
 */
class FakeGameSessionDao : GameSessionDao {
    private val state = MutableStateFlow<List<GameSessionEntity>>(emptyList())
    private var nextId = 1L

    val sessions: List<GameSessionEntity> get() = state.value

    override suspend fun insertGameSession(session: GameSessionEntity): Long {
        val id = nextId++
        state.value = state.value + session.copy(id = id)
        return id
    }

    override fun getAllGameSessions(): Flow<List<GameSessionEntity>> =
        state.map { sessions -> sessions.sortedByDescending { it.timestamp } }

    override fun getRecentGameSessions(limit: Int): Flow<List<GameSessionEntity>> =
        getAllGameSessions().map { it.take(limit) }

    override suspend fun deleteSession(sessionId: Long) {
        state.value = state.value.filterNot { it.id == sessionId }
    }

    override suspend fun deleteAllSessions() {
        state.value = emptyList()
    }
}

class FakeNounRepository(private val nouns: List<GermanNoun>) : NounRepository {
    override fun getAllNouns(): List<GermanNoun> = nouns
    override fun getRandomNoun(): GermanNoun? = nouns.randomOrNull()
}
