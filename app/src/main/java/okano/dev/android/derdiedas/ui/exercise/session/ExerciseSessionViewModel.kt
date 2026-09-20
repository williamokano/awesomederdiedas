package okano.dev.android.derdiedas.ui.exercise.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okano.dev.android.derdiedas.core.exercise.AnswerState
import okano.dev.android.derdiedas.core.exercise.SessionEngine
import okano.dev.android.derdiedas.core.exercise.SessionPhase
import okano.dev.android.derdiedas.core.exercise.SessionPlan
import okano.dev.android.derdiedas.core.exercise.SessionState
import okano.dev.android.derdiedas.core.exercise.sessionsFor
import okano.dev.android.derdiedas.data.exercise.ExerciseRepository
import okano.dev.android.derdiedas.data.exercise.model.Block

/**
 * A thin adapter over [SessionEngine]. The loop itself, including re-queueing and scoring,
 * lives in the engine so it can be tested without Android.
 */

sealed interface SessionUiState {
    data object Loading : SessionUiState
    data class Error(val message: String) : SessionUiState
    data class Active(
        val setTitle: String,
        val block: Block,
        val part: Int,
        val partCount: Int,
        val session: SessionState,
    ) : SessionUiState
}

class ExerciseSessionViewModel(
    private val repository: ExerciseRepository,
    private val setId: String,
    private val block: Block,
    private val part: Int,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SessionUiState>(SessionUiState.Loading)
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    // Kept apart from uiState on purpose: the answer changes on every keystroke, and
    // folding it into the screen state would recompose the progress bar and banner too.
    private val _answer = MutableStateFlow<AnswerState>(AnswerState.Texts())
    val answer: StateFlow<AnswerState> = _answer.asStateFlow()

    private var engine: SessionEngine? = null
    private var plan: SessionPlan? = null

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = SessionUiState.Loading
            try {
                val set = repository.getSet(setId)
                val plans = set.sessionsFor(block)
                val chosen = plans.getOrNull(part)
                if (chosen == null) {
                    _uiState.value = SessionUiState.Error("This part is no longer available.")
                    return@launch
                }
                plan = chosen
                engine = SessionEngine(chosen)
                resetAnswerForCurrentStep()
                publish()
            } catch (error: Exception) {
                _uiState.value = SessionUiState.Error(error.message ?: "Could not load this exercise.")
            }
        }
    }

    fun onAnswerChange(answer: AnswerState) {
        // Ignored once graded, which is what freezes the inputs behind the result banner.
        if (currentPhase() != SessionPhase.Answering) return
        _answer.value = answer
    }

    fun onCheck() {
        engine?.check(_answer.value)
        publish()
    }

    fun onContinue() {
        engine?.advance()
        resetAnswerForCurrentStep()
        publish()
    }

    fun onRetry() = load()

    private fun currentPhase(): SessionPhase? = engine?.state()?.phase

    private fun resetAnswerForCurrentStep() {
        _answer.value = engine?.state()?.step?.emptyAnswer() ?: AnswerState.Texts()
    }

    private fun publish() {
        val engine = engine ?: return
        val plan = plan ?: return
        _uiState.value = SessionUiState.Active(
            setTitle = plan.setTitle,
            block = plan.block,
            part = plan.part,
            partCount = plan.partCount,
            session = engine.state(),
        )
    }
}

class ExerciseSessionViewModelFactory(
    private val repository: ExerciseRepository,
    private val setId: String,
    private val block: Block,
    private val part: Int,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExerciseSessionViewModel::class.java)) {
            return ExerciseSessionViewModel(repository, setId, block, part) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
