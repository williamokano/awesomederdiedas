package okano.dev.android.derdiedas.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import okano.dev.android.derdiedas.data.repository.GameSessionRepository

/**
 * Factory for creating HistoryViewModel with dependencies
 */
class HistoryViewModelFactory(
    private val gameSessionRepository: GameSessionRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            return HistoryViewModel(gameSessionRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
