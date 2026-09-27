package okano.dev.android.derdiedas.ui.exercise.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import okano.dev.android.derdiedas.core.progress.BlockKey
import okano.dev.android.derdiedas.core.progress.BlockProgress
import okano.dev.android.derdiedas.core.progress.SessionRecord
import okano.dev.android.derdiedas.core.progress.blockProgress
import okano.dev.android.derdiedas.core.progress.setsTouched
import okano.dev.android.derdiedas.data.exercise.ExerciseRepository
import okano.dev.android.derdiedas.data.progress.ProgressRepository
import okano.dev.android.derdiedas.data.exercise.model.CefrTag
import okano.dev.android.derdiedas.data.exercise.model.ExerciseCollection
import okano.dev.android.derdiedas.data.exercise.model.ExerciseSetSummary
import okano.dev.android.derdiedas.data.exercise.model.collection

sealed interface CatalogUiState {
    data object Loading : CatalogUiState
    data class Error(val message: String) : CatalogUiState

    data class Ready(
        val all: List<ExerciseSetSummary>,
        /** Null on the collections screen, which spans all of them. */
        val collection: ExerciseCollection?,
        val selectedLevel: CefrTag?,
        /** Empty until the progress query arrives, so the list never waits on it. */
        val progress: Map<BlockKey, BlockProgress> = emptyMap(),
        val started: Set<String> = emptySet(),
    ) : CatalogUiState {
        private val inCollection: List<ExerciseSetSummary> =
            if (collection == null) all else all.filter { it.collection == collection }

        /** Only the levels this collection actually has: Alltag has no C1, for instance. */
        val levels: List<CefrTag> = inCollection.mapNotNull { it.level }.distinct().sorted()

        /** Exactly one level at a time, so the list is never a mix of everything. */
        val visible: List<ExerciseSetSummary> =
            inCollection.filter { selectedLevel == null || it.level == selectedLevel }

        fun countIn(collection: ExerciseCollection): Int = all.count { it.collection == collection }
    }
}

class ExerciseCatalogViewModel(
    private val repository: ExerciseRepository,
    private val progress: ProgressRepository,
    private val collection: ExerciseCollection?,
    /** The learner's own level from settings, used to open on a sensible tab. */
    private val preferredLevel: CefrTag?,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CatalogUiState>(CatalogUiState.Loading)
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    private var selectedLevel: CefrTag? = null

    /**
     * The last thing the progress query said, kept because the two loads race.
     *
     * Room answers from disk and the catalogue parses 226 assets, so on a cold start the
     * records usually arrive first, while the state is still [CatalogUiState.Loading].
     * Dropping them there would leave the ticks off until something wrote to the database
     * again — which, for a learner who only opens the list, is never. Holding them lets
     * [load] fold them in when the catalogue lands.
     */
    private var records: List<SessionRecord> = emptyList()

    init {
        load()
        observeProgress()
    }

    /**
     * Folded in separately from the catalogue itself, so a slow or failing progress query
     * never keeps the list of sets off the screen. The list is the point; the ticks are a
     * decoration on it.
     */
    private fun observeProgress() {
        viewModelScope.launch {
            progress.observeAll()
                .catch { /* No progress shown, rather than no catalogue. */ }
                .collect { fresh ->
                    records = fresh
                    val current = _uiState.value
                    if (current is CatalogUiState.Ready) {
                        _uiState.value = current.withProgress(fresh)
                    }
                }
        }
    }

    private fun CatalogUiState.Ready.withProgress(records: List<SessionRecord>) = copy(
        progress = blockProgress(records),
        started = setsTouched(records),
    )

    fun onLevelSelected(level: CefrTag) {
        selectedLevel = level
        val current = _uiState.value
        if (current is CatalogUiState.Ready) {
            _uiState.value = current.copy(selectedLevel = level)
        }
    }

    fun onRetry() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.value = CatalogUiState.Loading
            try {
                val all = repository.getIndex()
                val ready = CatalogUiState.Ready(all, collection, selectedLevel).withProgress(records)
                _uiState.value = if (collection == null || selectedLevel != null) {
                    ready
                } else {
                    // Open on the learner's own level when this collection has it, so the
                    // first thing they see is the work that fits them.
                    val initial = preferredLevel?.takeIf { it in ready.levels } ?: ready.levels.firstOrNull()
                    selectedLevel = initial
                    ready.copy(selectedLevel = initial)
                }
            } catch (error: Exception) {
                _uiState.value = CatalogUiState.Error(error.message ?: "Could not load exercises.")
            }
        }
    }
}

class ExerciseCatalogViewModelFactory(
    private val repository: ExerciseRepository,
    private val progress: ProgressRepository,
    private val collection: ExerciseCollection?,
    private val preferredLevel: CefrTag? = null,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExerciseCatalogViewModel::class.java)) {
            return ExerciseCatalogViewModel(repository, progress, collection, preferredLevel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
