package okano.dev.android.derdiedas.ui.exercise.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okano.dev.android.derdiedas.data.exercise.ExerciseRepository
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
    private val collection: ExerciseCollection?,
    /** The learner's own level from settings, used to open on a sensible tab. */
    private val preferredLevel: CefrTag?,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CatalogUiState>(CatalogUiState.Loading)
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    private var selectedLevel: CefrTag? = null

    init {
        load()
    }

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
                val ready = CatalogUiState.Ready(all, collection, selectedLevel)
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
    private val collection: ExerciseCollection?,
    private val preferredLevel: CefrTag? = null,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExerciseCatalogViewModel::class.java)) {
            return ExerciseCatalogViewModel(repository, collection, preferredLevel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
