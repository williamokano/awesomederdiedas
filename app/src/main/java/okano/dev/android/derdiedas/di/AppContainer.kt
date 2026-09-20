package okano.dev.android.derdiedas.di

import android.content.Context
import okano.dev.android.derdiedas.data.database.AppDatabase
import okano.dev.android.derdiedas.data.exercise.AssetExerciseRepository
import okano.dev.android.derdiedas.data.exercise.ExerciseRepository
import okano.dev.android.derdiedas.data.preferences.AppPreferences
import okano.dev.android.derdiedas.data.repository.GameSessionRepository
import okano.dev.android.derdiedas.data.repository.LocalNounRepository
import okano.dev.android.derdiedas.data.repository.NounRepository

/**
 * The app's dependencies, built once.
 *
 * These used to be constructed inside `setContent { }`, whose body re-runs on every
 * recomposition — so the noun list and the preferences wrapper were being reallocated
 * each time. That was survivable for a hardcoded list; it is not for a repository that
 * parses JSON and caches it, which would drop its cache on every recomposition.
 *
 * Constructing them here, as an Activity field before setContent, fixes that and keeps
 * the parameter list of AppNavGraph from growing with every new collaborator.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val appPreferences = AppPreferences(appContext)
    val nounRepository: NounRepository = LocalNounRepository()
    val exerciseRepository: ExerciseRepository = AssetExerciseRepository(appContext.assets)

    private val database = AppDatabase.getDatabase(appContext)
    val gameSessionRepository = GameSessionRepository(database.gameSessionDao())
}
