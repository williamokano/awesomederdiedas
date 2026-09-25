package okano.dev.android.derdiedas.ui.exercise.steps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import okano.dev.android.derdiedas.core.exercise.AnswerState
import okano.dev.android.derdiedas.core.exercise.MatchingStep
import okano.dev.android.derdiedas.core.exercise.StepResult
import okano.dev.android.derdiedas.ui.exercise.ExerciseTestTags
import okano.dev.android.derdiedas.ui.theme.LocalFeedbackColors

/**
 * Pair each prompt with its match from a shared pool.
 *
 * Deliberately not the two facing columns the website uses. The pool entries run to a
 * median of 38 characters and up to 117, which side by side on a phone leaves both
 * columns too narrow to read. Instead the prompts are rows that each hold a slot, and the
 * pool sits underneath -- the same shape as the word bank, so the two types are learned
 * once rather than twice.
 *
 * Tapping a pool entry fills the focused prompt and focus moves to the next empty one, so
 * eight prompts are eight taps. Tapping a prompt focuses it; tapping a filled one clears
 * it and puts its entry back.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MatchingStepContent(
    step: MatchingStep,
    answer: AnswerState.Placements,
    result: StepResult?,
    onAnswerChange: (AnswerState.Placements) -> Unit,
    modifier: Modifier = Modifier,
) {
    val feedback = LocalFeedbackColors.current
    val graded = result != null
    val placed = answer.byRef

    var focused by remember(step.id) { mutableStateOf(step.prompts.firstOrNull()?.key) }

    fun nextEmpty(after: String, updated: Map<String, String?>): String? {
        val order = step.prompts.map { it.key }
        val start = order.indexOf(after)
        return (order.drop(start + 1) + order.take(maxOf(start, 0)))
            .firstOrNull { updated[it].isNullOrEmpty() }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            step.prompts.forEach { prompt ->
                val assigned = placed[prompt.key]
                val accent: Color? = when {
                    !graded -> if (prompt.key == focused) MaterialTheme.colorScheme.primary else null
                    result?.items?.firstOrNull { it.ref == prompt.key }?.correct == true -> feedback.correct
                    else -> feedback.wrong
                }
                PromptRow(
                    prompt = prompt.text,
                    // Before grading an empty slot invites a tap; after it, the pairing
                    // the learner actually made is what they need to see.
                    match = step.pool.firstOrNull { it.key == assigned }?.text,
                    accent = accent,
                    enabled = !graded,
                    onClick = {
                        if (assigned != null) onAnswerChange(AnswerState.Placements(placed - prompt.key))
                        focused = prompt.key
                    },
                )
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            step.pool.forEach { entry ->
                // Counted against capacity rather than struck out on first use, because a
                // few exercises answer several prompts with the same entry.
                val used = placed.values.count { it == entry.key }
                val spent = used >= (step.capacity[entry.key] ?: 1)
                PoolEntry(
                    text = entry.text,
                    spent = spent,
                    enabled = !graded && !spent,
                    onClick = {
                        val target = focused?.takeIf { placed[it].isNullOrEmpty() }
                            ?: step.prompts.map { it.key }.firstOrNull { placed[it].isNullOrEmpty() }
                            ?: return@PoolEntry
                        val updated = placed + (target to entry.key)
                        onAnswerChange(AnswerState.Placements(updated))
                        focused = nextEmpty(target, updated) ?: target
                    },
                )
            }
        }
    }
}

@Composable
private fun PromptRow(
    prompt: String,
    match: String?,
    accent: Color?,
    enabled: Boolean,
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
        colors = CardDefaults.outlinedCardColors(
            containerColor = accent?.copy(alpha = 0.08f)
                ?: CardDefaults.outlinedCardColors().containerColor,
            disabledContainerColor = accent?.copy(alpha = 0.08f)
                ?: CardDefaults.outlinedCardColors().containerColor,
        ),
        modifier = Modifier.fillMaxWidth().testTag(ExerciseTestTags.MATCH_PROMPT),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = prompt,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = match ?: "—",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (match == null) FontWeight.Normal else FontWeight.Medium,
                color = accent ?: if (match == null) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                } else {
                    Color.Unspecified
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PoolEntry(
    text: String,
    spent: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = if (spent) {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (spent) 0.4f else 1f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag(ExerciseTestTags.MATCH_POOL_ENTRY),
    )
}
