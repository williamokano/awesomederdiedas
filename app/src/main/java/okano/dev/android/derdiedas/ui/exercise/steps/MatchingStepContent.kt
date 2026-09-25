package okano.dev.android.derdiedas.ui.exercise.steps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import okano.dev.android.derdiedas.core.exercise.ChoiceOption
import okano.dev.android.derdiedas.core.exercise.MatchingStep
import okano.dev.android.derdiedas.core.exercise.StepResult
import okano.dev.android.derdiedas.ui.exercise.ExerciseTestTags
import okano.dev.android.derdiedas.ui.theme.LocalFeedbackColors

/**
 * Pair each prompt with its match, one prompt at a time.
 *
 * The obvious layout -- every prompt as a row, the pool underneath -- does not fit. Eight
 * prompts and eight replies of a median 38 characters run past the bottom of a phone, so
 * the pool sat off screen and answering meant scrolling down to tap and back up to see
 * what had been filled. Two columns fail for the same reason from the other direction.
 *
 * So while there is still something to answer, only the current prompt is on screen, with
 * the replies still unused as full-width options: the shape the choice steps already use,
 * and no scrolling. Elimination survives because an option leaves the list once spent.
 *
 * Once every prompt has a match the screen becomes the compact list of pairs to look over
 * before checking, and that is also what grading marks up. The dense list is therefore
 * only ever shown when it is being read rather than tapped.
 */
@Composable
fun MatchingStepContent(
    step: MatchingStep,
    answer: AnswerState.Placements,
    result: StepResult?,
    onAnswerChange: (AnswerState.Placements) -> Unit,
    modifier: Modifier = Modifier,
) {
    val graded = result != null
    val placed = answer.byRef
    val keys = step.prompts.map { it.key }

    // Which prompt is being answered. Null means "whichever is next", which is the normal
    // case; it only holds a key when a pair was undone out of order.
    var picked by remember(step.id) { mutableStateOf<String?>(null) }
    val nextUnanswered = keys.firstOrNull { placed[it].isNullOrEmpty() }
    val current = picked?.takeIf { placed[it].isNullOrEmpty() } ?: nextUnanswered

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        if (current == null || graded) {
            PairList(step = step, placed = placed, result = result, onUndo = { key ->
                if (!graded) {
                    onAnswerChange(AnswerState.Placements(placed - key))
                    picked = key
                }
            })
        } else {
            Answering(
                step = step,
                placed = placed,
                current = current,
                // Going back to a prompt that already has a match means changing it, so
                // the match is released first -- otherwise the pip would be tappable and
                // do nothing, because a filled prompt is never the current one.
                onPick = { key ->
                    if (!placed[key].isNullOrEmpty()) {
                        onAnswerChange(AnswerState.Placements(placed - key))
                    }
                    picked = key
                },
                onAnswer = { entry ->
                    onAnswerChange(AnswerState.Placements(placed + (current to entry.key)))
                    picked = null
                },
            )
        }
    }
}

@Composable
private fun Answering(
    step: MatchingStep,
    placed: Map<String, String?>,
    current: String,
    onPick: (String) -> Unit,
    onAnswer: (ChoiceOption) -> Unit,
) {
    val keys = step.prompts.map { it.key }
    val done = keys.count { !placed[it].isNullOrEmpty() }

    // What has been answered so far, so the exercise reads as it builds up instead of
    // being a run of unrelated questions. Numbered pips were here before and said only
    // how many were done, which told the learner nothing about what they had said.
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "${done + 1} / ${step.prompts.size}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        step.prompts
            .filter { !placed[it.key].isNullOrEmpty() }
            .forEach { prompt ->
                Answered(
                    prompt = prompt.text,
                    match = step.pool.firstOrNull { it.key == placed[prompt.key] }?.text.orEmpty(),
                    onClick = { onPick(prompt.key) },
                )
            }
    }

    Text(
        text = step.prompts.first { it.key == current }.text,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.testTag(ExerciseTestTags.MATCH_PROMPT),
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Spent entries are dropped rather than dimmed: with replies this long, a dimmed
        // row is still a row of clutter in a list the learner is reading through.
        step.pool
            .filter { placed.values.count { key -> key == it.key } < (step.capacity[it.key] ?: 1) }
            .forEach { entry ->
                OutlinedCard(
                    onClick = { onAnswer(entry) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag(ExerciseTestTags.MATCH_POOL_ENTRY),
                ) {
                    Text(
                        text = entry.text,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    )
                }
            }
    }
}

@Composable
private fun PairList(
    step: MatchingStep,
    placed: Map<String, String?>,
    result: StepResult?,
    onUndo: (String) -> Unit,
) {
    val feedback = LocalFeedbackColors.current
    val graded = result != null

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        step.prompts.forEach { prompt ->
            val item = result?.items?.firstOrNull { it.ref == prompt.key }
            val accent: Color? = when {
                !graded -> null
                item?.correct == true -> feedback.correct
                else -> feedback.wrong
            }
            val match = step.pool.firstOrNull { it.key == placed[prompt.key] }?.text

            OutlinedCard(
                onClick = { onUndo(prompt.key) },
                enabled = !graded,
                shape = RoundedCornerShape(12.dp),
                border = if (accent == null) {
                    CardDefaults.outlinedCardBorder(enabled = !graded)
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
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = prompt.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = match ?: "—",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = accent ?: Color.Unspecified,
                            modifier = Modifier.weight(1f),
                        )
                        // A glyph as well as colour, so the state survives colour blindness.
                        if (graded) {
                            Text(
                                text = if (item?.correct == true) "✓" else "✗",
                                style = MaterialTheme.typography.titleMedium,
                                color = accent ?: Color.Unspecified,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Answered(
    prompt: String,
    match: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag(ExerciseTestTags.MATCH_ANSWERED),
    ) {
        Text(
            text = prompt,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )
        Text(
            text = match,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
        )
    }
}
