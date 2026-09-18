package okano.dev.android.derdiedas.ui.flashcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okano.dev.android.derdiedas.data.database.GameSessionEntity
import okano.dev.android.derdiedas.data.model.Article
import okano.dev.android.derdiedas.data.model.CEFRLevel
import okano.dev.android.derdiedas.data.model.GermanNoun
import okano.dev.android.derdiedas.data.model.Language
import okano.dev.android.derdiedas.data.repository.GameSessionRepository
import okano.dev.android.derdiedas.data.repository.NounRepository

/**
 * UI state for the flashcard screen
 */
data class FlashcardUiState(
    val currentNoun: GermanNoun? = null,
    val isAnswered: Boolean = false,
    val isCorrect: Boolean = false,
    val selectedArticle: Article? = null,
    val isLoading: Boolean = true,
    val correctAnswers: Int = 0,
    val wrongAnswers: Int = 0,
    val currentCardIndex: Int = 0,
    val totalCards: Int = 0,
    val isGameEnded: Boolean = false,
    val language: Language = Language.ENGLISH,
    val elapsedTimeMillis: Long = 0L
) {
    val totalAnswers: Int get() = correctAnswers + wrongAnswers
    val accuracyPercentage: Int get() = if (totalAnswers > 0) {
        ((correctAnswers.toFloat() / totalAnswers) * 100).toInt()
    } else {
        0
    }
}

/**
 * ViewModel for managing flashcard logic and state with game sessions
 */
class FlashcardViewModel(
    private val nounRepository: NounRepository,
    private val gameSessionRepository: GameSessionRepository,
    private val totalCardCount: Int,
    private val cefrLevel: CEFRLevel,
    private val language: Language
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        FlashcardUiState(
            totalCards = totalCardCount,
            language = language
        )
    )
    val uiState: StateFlow<FlashcardUiState> = _uiState.asStateFlow()

    private val shuffledNouns: List<GermanNoun>
    private var gameStartTime: Long = 0L
    private var pausedTimeOffset: Long = 0L
    private var lastPauseTime: Long = 0L
    private var isPaused: Boolean = false
    private var onGameEndCallback: ((Int, Int, Long, Float) -> Unit)? = null
    private var isGameEndingInProgress = false
    private var timerJob: kotlinx.coroutines.Job? = null

    init {
        // Get allowed levels based on selected CEFR level
        val allowedLevels = CEFRLevel.getLevelsUpTo(cefrLevel)

        // Filter nouns by CEFR level, shuffle, and take the required number of cards
        shuffledNouns = nounRepository.getAllNouns()
            .filter { noun -> allowedLevels.contains(noun.level) }
            .shuffled()
            .take(totalCardCount)

        gameStartTime = System.currentTimeMillis()

        // Start timer
        startTimer()

        // Load first card synchronously to avoid timing issues
        loadNextCard()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                if (!isPaused) {
                    val elapsed = System.currentTimeMillis() - gameStartTime - pausedTimeOffset
                    _uiState.value = _uiState.value.copy(elapsedTimeMillis = elapsed)
                }
                delay(100) // Update every 100ms
            }
        }
    }

    fun pauseTimer() {
        if (!isPaused) {
            isPaused = true
            lastPauseTime = System.currentTimeMillis()
        }
    }

    fun resumeTimer() {
        if (isPaused) {
            isPaused = false
            pausedTimeOffset += System.currentTimeMillis() - lastPauseTime
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    fun setOnGameEndCallback(callback: (correct: Int, wrong: Int, duration: Long, cardsPerMinute: Float) -> Unit) {
        onGameEndCallback = callback
    }

    /**
     * Cancel the game without saving - for when user exits early
     */
    fun cancelGame() {
        timerJob?.cancel()
        isPaused = true
    }

    /**
     * Handle user's article selection
     */
    fun onArticleSelected(selectedArticle: Article) {
        val currentNoun = _uiState.value.currentNoun ?: return
        val isCorrect = currentNoun.article == selectedArticle

        _uiState.value = _uiState.value.copy(
            isAnswered = true,
            isCorrect = isCorrect,
            selectedArticle = selectedArticle,
            correctAnswers = if (isCorrect) _uiState.value.correctAnswers + 1 else _uiState.value.correctAnswers,
            wrongAnswers = if (!isCorrect) _uiState.value.wrongAnswers + 1 else _uiState.value.wrongAnswers
        )

        // Auto-advance to next card after showing result
        viewModelScope.launch {
            delay(1500) // Show result for 1.5 seconds

            // Check if game should end (after answering all cards)
            if (_uiState.value.totalAnswers >= totalCardCount) {
                endGame()
            } else {
                loadNextCard()
            }
        }
    }

    /**
     * Load the next flashcard
     */
    private fun loadNextCard() {
        val currentState = _uiState.value
        val nextIndex = currentState.currentCardIndex

        if (nextIndex >= shuffledNouns.size) {
            endGame()
            return
        }

        _uiState.value = currentState.copy(
            currentNoun = shuffledNouns[nextIndex],
            isAnswered = false,
            isCorrect = false,
            selectedArticle = null,
            isLoading = false,
            currentCardIndex = nextIndex + 1
        )
    }

    /**
     * End the game and save session to database
     */
    private fun endGame() {
        // Prevent multiple calls
        if (isGameEndingInProgress) return
        isGameEndingInProgress = true

        val state = _uiState.value
        val durationMillis = System.currentTimeMillis() - gameStartTime
        val durationMinutes = durationMillis / 60000.0
        val cardsPerMinute = if (durationMinutes > 0) {
            (state.totalAnswers / durationMinutes).toFloat()
        } else {
            0f
        }

        // Save game session to database
        viewModelScope.launch {
            val session = GameSessionEntity(
                totalCards = totalCardCount,
                correctAnswers = state.correctAnswers,
                wrongAnswers = state.wrongAnswers,
                accuracyPercentage = state.accuracyPercentage,
                durationMillis = durationMillis,
                cardsPerMinute = cardsPerMinute
            )
            gameSessionRepository.saveGameSession(session)

            // Trigger callback after saving (ensure UI thread)
            onGameEndCallback?.invoke(
                state.correctAnswers,
                state.wrongAnswers,
                durationMillis,
                cardsPerMinute
            )
        }

        _uiState.value = state.copy(isGameEnded = true)
    }
}