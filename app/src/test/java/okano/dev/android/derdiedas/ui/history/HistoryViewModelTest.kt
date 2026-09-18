package okano.dev.android.derdiedas.ui.history

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okano.dev.android.derdiedas.data.database.GameSessionEntity
import okano.dev.android.derdiedas.data.repository.GameSessionRepository
import okano.dev.android.derdiedas.testutil.FakeGameSessionDao
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var dao: FakeGameSessionDao
    private lateinit var repository: GameSessionRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        dao = FakeGameSessionDao()
        repository = GameSessionRepository(dao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun session(timestamp: Long) = GameSessionEntity(
        totalCards = 5,
        correctAnswers = 4,
        wrongAnswers = 1,
        accuracyPercentage = 80,
        durationMillis = 30_000,
        cardsPerMinute = 10f,
        timestamp = timestamp
    )

    @Test
    fun `factory creates a HistoryViewModel`() {
        val viewModel = HistoryViewModelFactory(repository).create(HistoryViewModel::class.java)
        assertNotNull(viewModel)
    }

    @Test
    fun `deleteSession removes only that session`() = runTest(dispatcher) {
        val firstId = repository.saveGameSession(session(timestamp = 1))
        val secondId = repository.saveGameSession(session(timestamp = 2))
        val viewModel = HistoryViewModel(repository)

        viewModel.deleteSession(firstId)
        advanceUntilIdle()

        assertEquals(listOf(secondId), dao.sessions.map { it.id })
    }

    @Test
    fun `clearAllHistory removes every session`() = runTest(dispatcher) {
        repository.saveGameSession(session(timestamp = 1))
        repository.saveGameSession(session(timestamp = 2))
        val viewModel = HistoryViewModel(repository)

        viewModel.clearAllHistory()
        advanceUntilIdle()

        assertTrue(dao.sessions.isEmpty())
    }
}
