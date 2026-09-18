package okano.dev.android.derdiedas.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okano.dev.android.derdiedas.data.database.GameSessionEntity
import okano.dev.android.derdiedas.data.repository.GameSessionRepository

/**
 * ViewModel for the history screen
 */
class HistoryViewModel(
    private val repository: GameSessionRepository
) : ViewModel() {

    val gameSessions: StateFlow<List<GameSessionEntity>> = repository.getAllGameSessions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllSessions()
        }
    }
}
