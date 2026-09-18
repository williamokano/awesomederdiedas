package okano.dev.android.derdiedas.ui.flashcard

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okano.dev.android.derdiedas.data.model.Article
import okano.dev.android.derdiedas.data.model.CEFRLevel
import okano.dev.android.derdiedas.data.model.GermanNoun
import okano.dev.android.derdiedas.data.model.Language
import okano.dev.android.derdiedas.data.repository.GameSessionRepository
import okano.dev.android.derdiedas.testutil.FakeGameSessionDao
import okano.dev.android.derdiedas.testutil.FakeNounRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FlashcardViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var dao: FakeGameSessionDao

    // All nouns are "der", so Article.DER is always the correct answer
    private val nouns = listOf("Tisch", "Stuhl", "Hund").map {
        GermanNoun(it, Article.DER, mapOf("en" to it, "pt" to it), CEFRLevel.A1)
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        dao = FakeGameSessionDao()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(cardCount: Int) = FlashcardViewModel(
        nounRepository = FakeNounRepository(nouns),
        gameSessionRepository = GameSessionRepository(dao),
        totalCardCount = cardCount,
        cefrLevel = CEFRLevel.ALL,
        language = Language.ENGLISH,
        // Virtual time of the test scheduler, so delays and the clock agree
        clock = { dispatcher.scheduler.currentTime }
    )

    @Test
    fun `saved duration excludes time spent paused`() = runTest(dispatcher) {
        val viewModel = createViewModel(cardCount = 1)

        advanceTimeBy(10_000) // play 10s
        viewModel.pauseTimer()
        advanceTimeBy(60_000) // app in background for 60s
        viewModel.resumeTimer()
        advanceTimeBy(5_000) // play 5s more
        viewModel.onArticleSelected(Article.DER)
        advanceTimeBy(1_500) // result is shown for 1.5s, then the game ends
        runCurrent()

        val expectedActiveMillis = 16_500L
        val result = viewModel.uiState.value.gameResult
        assertNotNull(result)
        assertEquals(expectedActiveMillis, result!!.durationMillis)
        assertEquals(expectedActiveMillis, dao.sessions.single().durationMillis)
    }

    @Test
    fun `cards per minute is based on active play time`() = runTest(dispatcher) {
        val viewModel = createViewModel(cardCount = 1)

        viewModel.pauseTimer()
        advanceTimeBy(120_000) // 2 minutes in background
        viewModel.resumeTimer()
        advanceTimeBy(4_500)
        viewModel.onArticleSelected(Article.DER)
        advanceTimeBy(1_500)
        runCurrent()

        // 1 card in 6 active seconds = 10 cards per minute
        assertEquals(10f, viewModel.uiState.value.gameResult!!.cardsPerMinute, 0.001f)
        assertEquals(10f, dao.sessions.single().cardsPerMinute, 0.001f)
    }

    @Test
    fun `game result is published after the session is saved`() = runTest(dispatcher) {
        val viewModel = createViewModel(cardCount = 1)

        viewModel.onArticleSelected(Article.DIE) // wrong answer
        advanceTimeBy(1_500)
        runCurrent()

        val result = viewModel.uiState.value.gameResult!!
        val saved = dao.sessions.single()
        assertEquals(0, result.correctAnswers)
        assertEquals(1, result.wrongAnswers)
        assertEquals(saved.correctAnswers, result.correctAnswers)
        assertEquals(saved.wrongAnswers, result.wrongAnswers)
        assertTrue(viewModel.uiState.value.isGameEnded)
    }

    @Test
    fun `second tap on the same card is ignored`() = runTest(dispatcher) {
        val viewModel = createViewModel(cardCount = 2)

        viewModel.onArticleSelected(Article.DER)
        viewModel.onArticleSelected(Article.DIE) // double tap before the card advances

        val state = viewModel.uiState.value
        assertEquals(1, state.correctAnswers)
        assertEquals(0, state.wrongAnswers)
        assertEquals(Article.DER, state.selectedArticle)

        advanceTimeBy(1_500)
        runCurrent()

        // The game must continue to the second card instead of ending early
        assertFalse(viewModel.uiState.value.isGameEnded)
        assertEquals(2, viewModel.uiState.value.currentCardIndex)
        assertTrue(dao.sessions.isEmpty())

        viewModel.cancelGame()
    }

    @Test
    fun `exiting while the last answer is shown does not save the game`() = runTest(dispatcher) {
        val viewModel = createViewModel(cardCount = 1)

        viewModel.onArticleSelected(Article.DER)
        viewModel.cancelGame() // user taps back during the 1.5s result display
        advanceTimeBy(5_000)
        runCurrent()

        assertTrue(dao.sessions.isEmpty())
        assertNull(viewModel.uiState.value.gameResult)
    }

    @Test
    fun `answers after cancelling are ignored`() = runTest(dispatcher) {
        val viewModel = createViewModel(cardCount = 2)

        viewModel.cancelGame()
        viewModel.onArticleSelected(Article.DER)

        assertEquals(0, viewModel.uiState.value.totalAnswers)
    }
}
