package okano.dev.android.derdiedas.ui.exercise.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import okano.dev.android.derdiedas.core.exercise.AnswerState
import okano.dev.android.derdiedas.core.exercise.CategorizeStep
import okano.dev.android.derdiedas.core.exercise.ChoiceOption
import okano.dev.android.derdiedas.core.exercise.StepResult
import okano.dev.android.derdiedas.ui.exercise.ExerciseTestTags
import okano.dev.android.derdiedas.ui.theme.LocalFeedbackColors

/**
 * Drop each token into the bucket it belongs to.
 *
 * The buckets are both the target and the record: they are always on screen, and what has
 * been sorted so far sits inside them. That is what the matching screen had to be taught
 * the hard way -- nothing here disappears as it is answered, so the layout does not
 * shrink out from under the next tap, and there is no separate list of what has gone by.
 *
 * It fits because of the shape of the content: two to five buckets, a dozen tokens of
 * around twenty characters, and a median of four tokens per bucket once sorted.
 *
 * No local state at all. The current token is simply the first one not yet in a bucket,
 * so pulling a token back out makes it current again with nothing to keep in sync.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategorizeStepContent(
    step: CategorizeStep,
    answer: AnswerState.Placements,
    result: StepResult?,
    onAnswerChange: (AnswerState.Placements) -> Unit,
    modifier: Modifier = Modifier,
) {
    val graded = result != null
    val placed = answer.byRef
    val current = step.tokens.firstOrNull { placed[it.key].isNullOrEmpty() }
    val sorted = step.tokens.count { !placed[it.key].isNullOrEmpty() }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(
            text = "${if (current == null) sorted else sorted + 1} / ${step.tokens.size}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )

        // The token waiting to be sorted. Its slot stays occupied once everything is in a
        // bucket, so the buckets below do not slide up on the last tap.
        Text(
            text = current?.text ?: "",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth().testTag(
                if (current == null) ExerciseTestTags.CATEGORIZE_DONE else ExerciseTestTags.CATEGORIZE_TOKEN,
            ),
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            step.buckets.forEach { bucket ->
                Bucket(
                    label = bucket.text,
                    tokens = step.tokens.filter { placed[it.key] == bucket.key },
                    result = result,
                    enabled = !graded && current != null,
                    onDrop = {
                        if (current != null) {
                            onAnswerChange(AnswerState.Placements(placed + (current.key to bucket.key)))
                        }
                    },
                    onTakeBack = { token ->
                        if (!graded) onAnswerChange(AnswerState.Placements(placed - token.key))
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Bucket(
    label: String,
    tokens: List<ChoiceOption>,
    result: StepResult?,
    enabled: Boolean,
    onDrop: () -> Unit,
    onTakeBack: (ChoiceOption) -> Unit,
) {
    val feedback = LocalFeedbackColors.current

    OutlinedCard(
        onClick = onDrop,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder(enabled = enabled),
        modifier = Modifier.fillMaxWidth().testTag(ExerciseTestTags.CATEGORIZE_BUCKET),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (tokens.isEmpty()) {
                Text(
                    text = "—",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                )
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    tokens.forEach { token ->
                        val item = result?.items?.firstOrNull { it.ref == token.key }
                        val accent: Color? = when {
                            item == null -> null
                            item.correct -> feedback.correct
                            else -> feedback.wrong
                        }
                        Placed(
                            // A glyph as well as colour, so the state survives colour blindness.
                            text = when {
                                item == null -> token.text
                                item.correct -> "${token.text}  ✓"
                                else -> "${token.text}  ✗"
                            },
                            accent = accent,
                            enabled = result == null,
                            onClick = { onTakeBack(token) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Placed(
    text: String,
    accent: Color?,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = accent ?: MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                accent?.copy(alpha = 0.12f)
                    ?: MaterialTheme.colorScheme.surfaceVariant,
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag(ExerciseTestTags.CATEGORIZE_PLACED),
    )
}
