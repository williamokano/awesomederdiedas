package okano.dev.android.derdiedas.ui.resources

import okano.dev.android.derdiedas.data.model.Language

/**
 * Centralized string resources for UI translations
 */
object StringResources {

    // Home Screen
    fun newGame(language: Language) = when (language) {
        Language.ENGLISH -> "New Game"
        Language.PORTUGUESE -> "Novo Jogo"
    }

    fun history(language: Language) = when (language) {
        Language.ENGLISH -> "History"
        Language.PORTUGUESE -> "Histórico"
    }

    fun settings(language: Language) = when (language) {
        Language.ENGLISH -> "Settings"
        Language.PORTUGUESE -> "Configurações"
    }

    // Card Selection Screen
    fun selectCardCount(language: Language) = when (language) {
        Language.ENGLISH -> "Select number of cards"
        Language.PORTUGUESE -> "Selecione o número de cartas"
    }

    fun cards(language: Language) = when (language) {
        Language.ENGLISH -> "Cards"
        Language.PORTUGUESE -> "Cartas"
    }

    fun back(language: Language) = when (language) {
        Language.ENGLISH -> "Back"
        Language.PORTUGUESE -> "Voltar"
    }

    // Game Screen
    fun correct(language: Language) = when (language) {
        Language.ENGLISH -> "Correct"
        Language.PORTUGUESE -> "Correto"
    }

    fun wrong(language: Language) = when (language) {
        Language.ENGLISH -> "Wrong"
        Language.PORTUGUESE -> "Errado"
    }

    fun accuracy(language: Language) = when (language) {
        Language.ENGLISH -> "Accuracy"
        Language.PORTUGUESE -> "Precisão"
    }

    fun correctFeedback(language: Language) = when (language) {
        Language.ENGLISH -> "✓ Correct!"
        Language.PORTUGUESE -> "✓ Correto!"
    }

    fun wrongFeedback(language: Language) = when (language) {
        Language.ENGLISH -> "✗ Wrong!"
        Language.PORTUGUESE -> "✗ Errado!"
    }

    // Results Screen
    fun gameResults(language: Language) = when (language) {
        Language.ENGLISH -> "Game Results"
        Language.PORTUGUESE -> "Resultados do Jogo"
    }

    fun correctAnswers(language: Language) = when (language) {
        Language.ENGLISH -> "Correct Answers"
        Language.PORTUGUESE -> "Respostas Corretas"
    }

    fun wrongAnswers(language: Language) = when (language) {
        Language.ENGLISH -> "Wrong Answers"
        Language.PORTUGUESE -> "Respostas Erradas"
    }

    fun duration(language: Language) = when (language) {
        Language.ENGLISH -> "Duration"
        Language.PORTUGUESE -> "Duração"
    }

    fun cardsPerMinute(language: Language) = when (language) {
        Language.ENGLISH -> "Cards per Minute"
        Language.PORTUGUESE -> "Cartas por Minuto"
    }

    fun backToHome(language: Language) = when (language) {
        Language.ENGLISH -> "Back to Home"
        Language.PORTUGUESE -> "Voltar ao Início"
    }

    // History Screen
    fun gameHistory(language: Language) = when (language) {
        Language.ENGLISH -> "Game History"
        Language.PORTUGUESE -> "Histórico de Jogos"
    }

    fun noGamesYet(language: Language) = when (language) {
        Language.ENGLISH -> "No games played yet"
        Language.PORTUGUESE -> "Nenhum jogo jogado ainda"
    }

    fun clearAllHistory(language: Language) = when (language) {
        Language.ENGLISH -> "Clear All History"
        Language.PORTUGUESE -> "Limpar Todo Histórico"
    }

    fun confirmClearHistory(language: Language) = when (language) {
        Language.ENGLISH -> "Are you sure you want to delete all game history?"
        Language.PORTUGUESE -> "Tem certeza de que deseja excluir todo o histórico de jogos?"
    }

    fun cancel(language: Language) = when (language) {
        Language.ENGLISH -> "Cancel"
        Language.PORTUGUESE -> "Cancelar"
    }

    fun delete(language: Language) = when (language) {
        Language.ENGLISH -> "Delete"
        Language.PORTUGUESE -> "Excluir"
    }

    fun confirmDelete(language: Language) = when (language) {
        Language.ENGLISH -> "Confirm"
        Language.PORTUGUESE -> "Confirmar"
    }

    // Settings Screen
    fun appSettings(language: Language) = when (language) {
        Language.ENGLISH -> "App Settings"
        Language.PORTUGUESE -> "Configurações do App"
    }

    fun languageLabel(language: Language) = when (language) {
        Language.ENGLISH -> "Language"
        Language.PORTUGUESE -> "Idioma"
    }

    fun levelLabel(language: Language) = when (language) {
        Language.ENGLISH -> "CEFR Level"
        Language.PORTUGUESE -> "Nível CEFR"
    }

    fun levelDescription(language: Language) = when (language) {
        Language.ENGLISH -> "Select your proficiency level. The app will show words up to and including your selected level."
        Language.PORTUGUESE -> "Selecione seu nível de proficiência. O app mostrará palavras até e incluindo seu nível selecionado."
    }

    // Time formatting
    fun seconds(language: Language) = when (language) {
        Language.ENGLISH -> "seconds"
        Language.PORTUGUESE -> "segundos"
    }

    fun minutes(language: Language) = when (language) {
        Language.ENGLISH -> "minutes"
        Language.PORTUGUESE -> "minutos"
    }

    // Exercises
    fun exercises(language: Language) = when (language) {
        Language.ENGLISH -> "Exercises"
        Language.PORTUGUESE -> "Exercícios"
    }

    fun allLevels(language: Language) = when (language) {
        Language.ENGLISH -> "All"
        Language.PORTUGUESE -> "Todos"
    }

    fun practise(language: Language) = when (language) {
        Language.ENGLISH -> "Practise"
        Language.PORTUGUESE -> "Praticar"
    }

    fun exerciseCount(language: Language, count: Int) = when (language) {
        Language.ENGLISH -> "$count exercises"
        Language.PORTUGUESE -> "$count exercícios"
    }

    /**
     * Block names come from the content repo and stay German, the way a course book would
     * keep them, with a gloss in the reader's language.
     */
    fun blockName(language: Language, block: String): String {
        val german = when (block) {
            "H" -> "Hören"
            "A" -> "Basistraining"
            "B" -> "Vertiefung"
            "C" -> "Prüfungstraining"
            "D" -> "Wiederholung"
            else -> "Prüfung"
        }
        val gloss = when (block) {
            "H" -> when (language) {
                Language.ENGLISH -> "Listening"
                Language.PORTUGUESE -> "Compreensão oral"
            }
            "A" -> when (language) {
                Language.ENGLISH -> "Basics"
                Language.PORTUGUESE -> "Básico"
            }
            "B" -> when (language) {
                Language.ENGLISH -> "In depth"
                Language.PORTUGUESE -> "Aprofundamento"
            }
            "C" -> when (language) {
                Language.ENGLISH -> "Exam practice"
                Language.PORTUGUESE -> "Preparação para exame"
            }
            "D" -> when (language) {
                Language.ENGLISH -> "Review"
                Language.PORTUGUESE -> "Revisão"
            }
            else -> when (language) {
                Language.ENGLISH -> "Exam"
                Language.PORTUGUESE -> "Exame"
            }
        }
        return "$german · $gloss"
    }

    fun check(language: Language) = when (language) {
        Language.ENGLISH -> "Check"
        Language.PORTUGUESE -> "Verificar"
    }

    fun continueLabel(language: Language) = when (language) {
        Language.ENGLISH -> "Continue"
        Language.PORTUGUESE -> "Continuar"
    }

    fun gapsCorrect(language: Language, correct: Int, total: Int) = when (language) {
        Language.ENGLISH -> "$correct of $total gaps correct"
        Language.PORTUGUESE -> "$correct de $total lacunas corretas"
    }

    fun sessionComplete(language: Language) = when (language) {
        Language.ENGLISH -> "Session complete"
        Language.PORTUGUESE -> "Sessão concluída"
    }

    fun firstTryAccuracy(language: Language, percentage: Int) = when (language) {
        Language.ENGLISH -> "$percentage% first-try accuracy"
        Language.PORTUGUESE -> "$percentage% de acerto na primeira tentativa"
    }

    fun stepsMastered(language: Language, mastered: Int, total: Int) = when (language) {
        Language.ENGLISH -> "$mastered of $total answered correctly"
        Language.PORTUGUESE -> "$mastered de $total respondidos corretamente"
    }

    fun stillToReview(language: Language, count: Int) = when (language) {
        Language.ENGLISH -> "$count still to review"
        Language.PORTUGUESE -> "$count ainda para revisar"
    }

    fun done(language: Language) = when (language) {
        Language.ENGLISH -> "Done"
        Language.PORTUGUESE -> "Concluir"
    }

    fun tryAgain(language: Language) = when (language) {
        Language.ENGLISH -> "Try again"
        Language.PORTUGUESE -> "Tentar novamente"
    }
}
