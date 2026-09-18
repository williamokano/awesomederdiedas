package okano.dev.android.derdiedas.ui.flashcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import okano.dev.android.derdiedas.data.model.CEFRLevel
import okano.dev.android.derdiedas.data.model.Language
import okano.dev.android.derdiedas.data.repository.GameSessionRepository
import okano.dev.android.derdiedas.data.repository.NounRepository

/**
 * Factory for creating FlashcardViewModel with dependencies
 */
class FlashcardViewModelFactory(
    private val nounRepository: NounRepository,
    private val gameSessionRepository: GameSessionRepository,
    private val totalCardCount: Int,
    private val cefrLevel: CEFRLevel,
    private val language: Language
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FlashcardViewModel::class.java)) {
            return FlashcardViewModel(
                nounRepository,
                gameSessionRepository,
                totalCardCount,
                cefrLevel,
                language
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}