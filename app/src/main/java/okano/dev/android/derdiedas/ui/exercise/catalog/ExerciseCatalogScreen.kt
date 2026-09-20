package okano.dev.android.derdiedas.ui.exercise.catalog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import okano.dev.android.derdiedas.data.exercise.model.Block
import okano.dev.android.derdiedas.data.exercise.model.BlockSummary
import okano.dev.android.derdiedas.data.exercise.model.ExerciseSetSummary
import okano.dev.android.derdiedas.data.model.Language
import okano.dev.android.derdiedas.ui.resources.StringResources

/**
 * Browse the bundled exercise sets, filtered by level, and pick a block to practise.
 *
 * Blocks expand inline rather than getting their own route: a set has at most five, and a
 * third screen between "pick a topic" and "start" is a tap nobody wants.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseCatalogScreen(
    viewModel: ExerciseCatalogViewModel,
    language: Language,
    onStartSession: (setId: String, block: Block, part: Int) -> Unit,
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
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            is CatalogUiState.Error -> Box(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.message, style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = viewModel::onRetry, modifier = Modifier.padding(top = 16.dp)) {
                        Text(StringResources.tryAgain(language))
                    }
                }
            }

            is CatalogUiState.Ready -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    LevelFilter(
                        levels = state.levels.map { it.name },
                        selected = state.selectedLevel?.name,
                        allLabel = StringResources.allLevels(language),
                        onSelect = { name ->
                            viewModel.onLevelSelected(state.levels.firstOrNull { it.name == name })
                        },
                    )
                }
                items(state.visible, key = { it.id }) { summary ->
                    ExerciseSetCard(summary, language, onStartSession)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LevelFilter(
    levels: List<String>,
    selected: String?,
    allLabel: String,
    onSelect: (String?) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text(allLabel) })
        levels.forEach { level ->
            FilterChip(
                selected = selected == level,
                onClick = { onSelect(level) },
                label = { Text(level) },
            )
        }
    }
}

@Composable
private fun ExerciseSetCard(
    summary: ExerciseSetSummary,
    language: Language,
    onStartSession: (String, Block, Int) -> Unit,
) {
    var expanded by remember(summary.id) { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = summary.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                summary.level?.let {
                    Text(it.name, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }

            summary.summary?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            if (expanded) {
                summary.blocks.forEach { block ->
                    BlockRow(summary.id, block, language, onStartSession)
                }
            } else {
                Text(
                    text = StringResources.exerciseCount(language, summary.blocks.sumOf { it.exerciseCount }),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun BlockRow(
    setId: String,
    block: BlockSummary,
    language: Language,
    onStartSession: (String, Block, Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(StringResources.blockName(language, block.block.name), style = MaterialTheme.typography.bodyMedium)
            Text(
                text = StringResources.exerciseCount(language, block.exerciseCount),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
        Button(onClick = { onStartSession(setId, block.block, 0) }, shape = RoundedCornerShape(12.dp)) {
            Text(StringResources.practise(language))
        }
    }
}
