package okano.dev.android.derdiedas.ui.exercise.catalog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import okano.dev.android.derdiedas.data.exercise.model.ExerciseCollection
import okano.dev.android.derdiedas.data.model.Language
import okano.dev.android.derdiedas.ui.resources.StringResources

/**
 * Picks which body of content to practise, before the set list.
 *
 * A graded course, standalone grammar drills and everyday situations are three different
 * things to be in the mood for, and 226 of them in one list is a wall.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseCollectionsScreen(
    viewModel: ExerciseCatalogViewModel,
    language: Language,
    onCollectionClick: (ExerciseCollection) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(StringResources.exercises(language)) },
                navigationIcon = {
                    TextButton(onClick = onBackClick) { Text(StringResources.back(language)) }
                },
            )
        },
    ) { padding ->
        when (val state = uiState) {
            is CatalogUiState.Loading -> Box(
                Modifier.fillMaxSize().padding(padding),
                Alignment.Center,
            ) { CircularProgressIndicator() }

            is CatalogUiState.Error -> Box(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.message, style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = viewModel::onRetry, modifier = Modifier.padding(top = 16.dp)) {
                        Text(StringResources.tryAgain(language))
                    }
                }
            }

            is CatalogUiState.Ready -> Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Only collections that actually have sets: HOEREN has none, because
                // every exercise in it needs audio and audio is not supported yet.
                ExerciseCollection.entries
                    .map { it to state.countIn(it) }
                    .filter { (_, count) -> count > 0 }
                    .forEach { (collection, count) ->
                        CollectionCard(collection, count, language) { onCollectionClick(collection) }
                    }
            }
        }
    }
}

@Composable
private fun CollectionCard(
    collection: ExerciseCollection,
    setCount: Int,
    language: Language,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = StringResources.collectionName(language, collection.name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = StringResources.collectionDescription(language, collection.name),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = StringResources.setCount(language, setCount),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
