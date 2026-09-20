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
import okano.dev.android.derdiedas.di.AppContainer
import okano.dev.android.derdiedas.navigation.AppNavGraph
import okano.dev.android.derdiedas.ui.theme.DerDieDasTheme

class MainActivity : ComponentActivity() {

    // Built before setContent: that lambda re-runs on recomposition, so anything
    // constructed inside it is rebuilt each time, discarding any cache it holds.
    private lateinit var container: AppContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        container = AppContainer(this)
        enableEdgeToEdge()
        setContent {
            DerDieDasTheme {
                val navController = rememberNavController()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavGraph(
                        navController = navController,
                        container = container,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}