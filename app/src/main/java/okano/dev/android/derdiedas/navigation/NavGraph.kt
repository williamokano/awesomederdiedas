package okano.dev.android.derdiedas.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import okano.dev.android.derdiedas.data.repository.GameSessionRepository
import okano.dev.android.derdiedas.data.repository.NounRepository
import okano.dev.android.derdiedas.data.preferences.AppPreferences
import okano.dev.android.derdiedas.ui.cardselection.CardSelectionScreen
import okano.dev.android.derdiedas.ui.flashcard.FlashcardScreen
import okano.dev.android.derdiedas.ui.flashcard.FlashcardViewModel
import okano.dev.android.derdiedas.ui.flashcard.FlashcardViewModelFactory
import okano.dev.android.derdiedas.ui.history.HistoryScreen
import okano.dev.android.derdiedas.ui.history.HistoryViewModel
import okano.dev.android.derdiedas.ui.history.HistoryViewModelFactory
import okano.dev.android.derdiedas.ui.easteregg.EasterEggScreen
import okano.dev.android.derdiedas.ui.home.HomeScreen
import okano.dev.android.derdiedas.ui.results.GameResultsScreen
import okano.dev.android.derdiedas.ui.settings.SettingsScreen

/**
 * Navigation routes for the app
 */
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object CardSelection : Screen("card_selection")
    object Game : Screen("game/{cardCount}") {
        fun createRoute(cardCount: Int) = "game/$cardCount"
    }
    object Results : Screen("results/{correct}/{wrong}/{duration}/{cardsPerMinute}") {
        fun createRoute(correct: Int, wrong: Int, duration: Long, cardsPerMinute: Float) =
            "results/$correct/$wrong/$duration/$cardsPerMinute"
    }
    object History : Screen("history")
    object Settings : Screen("settings")
    object EasterEgg : Screen("easter_egg")
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    nounRepository: NounRepository,
    gameSessionRepository: GameSessionRepository,
    appPreferences: AppPreferences,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        // Home Screen
        composable(
            route = Screen.Home.route
        ) {
            HomeScreen(
                language = appPreferences.getLanguage(),
                onNewGameClick = {
                    navController.navigate(Screen.CardSelection.route)
                },
                onHistoryClick = {
                    navController.navigate(Screen.History.route) {
                        // Save state to prevent flicker on back
                        restoreState = true
                    }
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                },
                onEasterEggClick = {
                    navController.navigate(Screen.EasterEgg.route)
                }
            )
        }

        // Card Selection Screen
        composable(Screen.CardSelection.route) {
            CardSelectionScreen(
                language = appPreferences.getLanguage(),
                onCardCountSelected = { count ->
                    navController.navigate(Screen.Game.createRoute(count))
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // Game Screen
        composable(
            route = Screen.Game.route,
            arguments = listOf(
                navArgument("cardCount") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val cardCount = backStackEntry.arguments?.getInt("cardCount") ?: 10
            val flashcardViewModel: FlashcardViewModel = viewModel(
                factory = FlashcardViewModelFactory(
                    nounRepository,
                    gameSessionRepository,
                    cardCount,
                    appPreferences.getCEFRLevel(),
                    appPreferences.getLanguage()
                )
            )

            FlashcardScreen(
                viewModel = flashcardViewModel,
                onGameEnd = { correct, wrong, duration, cardsPerMinute ->
                    navController.navigate(
                        Screen.Results.createRoute(correct, wrong, duration, cardsPerMinute)
                    ) {
                        popUpTo(Screen.Home.route)
                    }
                },
                onBackClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        // Results Screen
        composable(
            route = Screen.Results.route,
            arguments = listOf(
                navArgument("correct") { type = NavType.IntType },
                navArgument("wrong") { type = NavType.IntType },
                navArgument("duration") { type = NavType.LongType },
                navArgument("cardsPerMinute") { type = NavType.FloatType }
            )
        ) { backStackEntry ->
            val correct = backStackEntry.arguments?.getInt("correct") ?: 0
            val wrong = backStackEntry.arguments?.getInt("wrong") ?: 0
            val duration = backStackEntry.arguments?.getLong("duration") ?: 0L
            val cardsPerMinute = backStackEntry.arguments?.getFloat("cardsPerMinute") ?: 0f

            GameResultsScreen(
                correctAnswers = correct,
                wrongAnswers = wrong,
                durationMillis = duration,
                cardsPerMinute = cardsPerMinute,
                language = appPreferences.getLanguage(),
                onBackToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        // History Screen
        composable(Screen.History.route) {
            val viewModel: HistoryViewModel = viewModel(
                factory = HistoryViewModelFactory(gameSessionRepository)
            )
            HistoryScreen(
                viewModel = viewModel,
                language = appPreferences.getLanguage(),
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // Settings Screen
        composable(Screen.Settings.route) {
            SettingsScreen(
                appPreferences = appPreferences,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // Easter Egg Screen
        composable(Screen.EasterEgg.route) {
            EasterEggScreen()
        }
    }
}
