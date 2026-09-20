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
import okano.dev.android.derdiedas.data.exercise.model.ExerciseSetSummary

sealed interface CatalogUiState {
    data object Loading : CatalogUiState
    data class Error(val message: String) : CatalogUiState
    data class Ready(
        val all: List<ExerciseSetSummary>,
        val selectedLevel: CefrTag?,
    ) : CatalogUiState {
        val levels: List<CefrTag> = all.mapNotNull { it.level }.distinct().sorted()
        val visible: List<ExerciseSetSummary> =
            if (selectedLevel == null) all else all.filter { it.level == selectedLevel }
    }
}

class ExerciseCatalogViewModel(
    private val repository: ExerciseRepository,
    initialLevel: CefrTag?,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CatalogUiState>(CatalogUiState.Loading)
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    private var level: CefrTag? = initialLevel

    init {
        load()
    }

    fun onLevelSelected(level: CefrTag?) {
        this.level = level
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
                _uiState.value = CatalogUiState.Ready(repository.getIndex(), level)
            } catch (error: Exception) {
                _uiState.value = CatalogUiState.Error(error.message ?: "Could not load exercises.")
            }
        }
    }
}

class ExerciseCatalogViewModelFactory(
    private val repository: ExerciseRepository,
    private val initialLevel: CefrTag?,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExerciseCatalogViewModel::class.java)) {
            return ExerciseCatalogViewModel(repository, initialLevel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
