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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import okano.dev.android.derdiedas.core.progress.BlockKey
import okano.dev.android.derdiedas.core.progress.BlockProgress
import okano.dev.android.derdiedas.ui.exercise.ExerciseTestTags
import okano.dev.android.derdiedas.data.exercise.model.Block
import okano.dev.android.derdiedas.data.exercise.model.BlockSummary
import okano.dev.android.derdiedas.data.exercise.model.ExerciseCollection
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
    collection: ExerciseCollection,
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
                title = { Text(StringResources.collectionName(language, collection.name)) },
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

            is CatalogUiState.Ready -> Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                val selectedIndex = state.levels.indexOf(state.selectedLevel).coerceAtLeast(0)
                if (state.levels.isNotEmpty()) {
                    TabRow(selectedTabIndex = selectedIndex) {
                        state.levels.forEachIndexed { index, level ->
                            Tab(
                                selected = index == selectedIndex,
                                onClick = { viewModel.onLevelSelected(level) },
                                text = { Text(level.name) },
                                modifier = Modifier.testTag(ExerciseTestTags.LEVEL_TAB),
                            )
                        }
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.visible, key = { it.id }) { summary ->
                        ExerciseSetCard(
                            summary = summary,
                            progress = state.progress,
                            started = summary.id in state.started,
                            language = language,
                            onStartSession = onStartSession,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseSetCard(
    summary: ExerciseSetSummary,
    progress: Map<BlockKey, BlockProgress>,
    started: Boolean,
    language: Language,
    onStartSession: (String, Block, Int) -> Unit,
) {
    var expanded by remember(summary.id) { mutableStateOf(false) }

    // Counted from the index's own block list rather than from the records, so a set only
    // reads as finished when every block it actually has is finished.
    val blocksDone = summary.blocks.count { progress[BlockKey(summary.id, it.block)]?.complete == true }
    val setDone = summary.blocks.isNotEmpty() && blocksDone == summary.blocks.size

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(ExerciseTestTags.SET_CARD)
            .clickable { expanded = !expanded },
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
                if (setDone) {
                    // A glyph as well as the colour: green alone is invisible to a good
                    // number of people, and this is the only signal on a collapsed card.
                    Text(
                        text = "\u2713",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
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
                    BlockRow(
                        setId = summary.id,
                        block = block,
                        progress = progress[BlockKey(summary.id, block.block)],
                        language = language,
                        onStartSession = onStartSession,
                    )
                }
            } else {
                Text(
                    text = run {
                        val exercises = summary.blocks.sumOf { it.exerciseCount }
                        when {
                            // The tick already says it; "started" underneath would contradict it.
                            setDone -> StringResources.exerciseCount(language, exercises)
                            blocksDone in 1 until summary.blocks.size ->
                                StringResources.blocksDone(language, blocksDone, summary.blocks.size)
                            started -> StringResources.exerciseCountStarted(language, exercises)
                            else -> StringResources.exerciseCount(language, exercises)
                        }
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

/**
 * One block of a set, with what the learner has already done to it.
 *
 * An untouched block says nothing about parts, deliberately: the index does not carry how
 * many parts a block splits into, so before the first session is finished there is no
 * denominator to show. Inventing one would be worse than showing none.
 */
@Composable
private fun BlockRow(
    setId: String,
    block: BlockSummary,
    progress: BlockProgress?,
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
            if (progress != null) {
                Text(
                    text = if (progress.complete) {
                        StringResources.blockDone(language, progress.bestAccuracy)
                    } else {
                        StringResources.blockPartsDone(
                            language,
                            progress.partsDone,
                            progress.partCount,
                            progress.bestAccuracy,
                        )
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (progress.complete) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    },
                )
            }
        }
        Button(
            // The first part not yet finished, so a block that splits into several is worked
            // through instead of replaying its opening part every time.
            onClick = { onStartSession(setId, block.block, progress?.nextPart ?: 0) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag(ExerciseTestTags.PRACTISE_BUTTON),
        ) {
            Text(StringResources.practise(language))
        }
    }
}
