package okano.dev.android.derdiedas.ui.exercise.steps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import okano.dev.android.derdiedas.core.exercise.AnswerState
import okano.dev.android.derdiedas.core.exercise.OrderStep
import okano.dev.android.derdiedas.core.exercise.StepResult
import okano.dev.android.derdiedas.ui.exercise.ExerciseTestTags
import okano.dev.android.derdiedas.ui.theme.LocalFeedbackColors

/**
 * Build a sentence by tapping tiles into place.
 *
 * Two areas: the sentence being assembled, and the pool of tiles left. Tapping a pool tile
 * appends it; tapping a placed tile sends it back. That is the whole interaction, and it
 * is the one Duolingo is best known for.
 *
 * The tiles arrive already shuffled and the answer already remapped, so nothing here needs
 * to know the order they were authored in.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OrderStepContent(
    step: OrderStep,
    answer: AnswerState.Sequence,
    result: StepResult?,
    onAnswerChange: (AnswerState.Sequence) -> Unit,
    modifier: Modifier = Modifier,
) {
    val feedback = LocalFeedbackColors.current
    val graded = result != null
    val placed = answer.order
    val remaining = step.tiles.indices.filterNot { it in placed }

    val stripAccent: Color? = when {
        !graded -> null
        result?.correct == true -> feedback.correct
        else -> feedback.wrong
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        // The sentence so far. Keeps a minimum height so the pool below does not jump
        // up and down as tiles move between the two.
        Surface(
            color = stripAccent?.copy(alpha = 0.10f) ?: MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag(ExerciseTestTags.ORDER_STRIP),
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                placed.forEach { index ->
                    Tile(
                        text = step.tiles.getOrElse(index) { "" },
                        accent = stripAccent,
                        enabled = !graded,
                        tag = ExerciseTestTags.ORDER_PLACED_TILE,
                        onClick = { onAnswerChange(AnswerState.Sequence(placed - index)) },
                    )
                }
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            remaining.forEach { index ->
                Tile(
                    text = step.tiles[index],
                    accent = null,
                    enabled = !graded,
                    tag = ExerciseTestTags.ORDER_TILE,
                    onClick = { onAnswerChange(AnswerState.Sequence(placed + index)) },
                )
            }
        }
    }
}

@Composable
private fun Tile(
    text: String,
    accent: Color?,
    enabled: Boolean,
    tag: String,
    onClick: () -> Unit,
) {
    OutlinedCard(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = if (accent == null) {
            CardDefaults.outlinedCardBorder(enabled = enabled)
        } else {
            BorderStroke(2.dp, accent)
        },
        modifier = Modifier.testTag(tag),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = accent ?: Color.Unspecified,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}
