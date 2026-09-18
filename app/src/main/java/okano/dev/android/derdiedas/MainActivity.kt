package okano.dev.android.derdiedas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import okano.dev.android.derdiedas.data.database.AppDatabase
import okano.dev.android.derdiedas.data.preferences.AppPreferences
import okano.dev.android.derdiedas.data.repository.GameSessionRepository
import okano.dev.android.derdiedas.data.repository.LocalNounRepository
import okano.dev.android.derdiedas.navigation.AppNavGraph
import okano.dev.android.derdiedas.ui.theme.DerDieDasTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DerDieDasTheme {
                val navController = rememberNavController()
                val nounRepository = LocalNounRepository()
                val database = AppDatabase.getDatabase(applicationContext)
                val gameSessionRepository = GameSessionRepository(database.gameSessionDao())
                val appPreferences = AppPreferences(applicationContext)

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavGraph(
                        navController = navController,
                        nounRepository = nounRepository,
                        gameSessionRepository = gameSessionRepository,
                        appPreferences = appPreferences,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}