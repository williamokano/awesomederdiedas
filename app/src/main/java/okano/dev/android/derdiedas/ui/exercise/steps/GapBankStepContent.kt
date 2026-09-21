package okano.dev.android.derdiedas.ui.exercise.steps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import okano.dev.android.derdiedas.core.exercise.GapBankStep
import okano.dev.android.derdiedas.core.exercise.GapSegment
import okano.dev.android.derdiedas.core.exercise.StepResult
import okano.dev.android.derdiedas.ui.exercise.ExerciseTestTags
import okano.dev.android.derdiedas.ui.theme.LocalFeedbackColors

/**
 * Fill a passage's gaps from a shared bank.
 *
 * The passage is laid out a line at a time, each line a wrapping row of text and blanks.
 * That is what sidesteps Compose's lack of an inline field inside flowing text, and it
 * works here because the corpus's passages are dialogue: about ten lines of roughly
 * fifty characters, most carrying a single gap.
 *
 * Tapping a bank word fills the focused blank and moves focus to the next empty one, so
 * a ten-gap passage is ten taps in reading order. Tapping a blank focuses it; tapping a
 * filled blank empties it again.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GapBankStepContent(
    step: GapBankStep,
    answer: AnswerState.Placements,
    result: StepResult?,
    onAnswerChange: (AnswerState.Placements) -> Unit,
    modifier: Modifier = Modifier,
) {
    val feedback = LocalFeedbackColors.current
    val graded = result != null
    val placed = answer.byRef

    // Focus is presentation, not answer, so it stays here rather than in the runner.
    // Keyed on the step so a re-queued step starts at its first blank again.
    var focused by remember(step.id) { mutableStateOf(step.gapKeys.firstOrNull()) }
    // Takes the map as it will be after the placement rather than closing over the
    // current one, or filling the last empty blank would leave focus on that same blank.
    fun nextEmpty(after: String, updated: Map<String, String?>): String? {
        val order = step.gapKeys
        val start = order.indexOf(after)
        return (order.drop(start + 1) + order.take(maxOf(start, 0)))
            .firstOrNull { updated[it].isNullOrEmpty() }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            step.lines.forEach { line ->
                // No cross-axis alignment parameter here on purpose: FlowRow gained
                // itemVerticalAlignment in Foundation 1.8 and this project is on 1.7,
                // so each child aligns itself instead.
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    line.forEach { segment ->
                        when (segment) {
                            is GapSegment.Literal -> segment.text
                                .split(' ')
                                .filter { it.isNotBlank() }
                                .forEach { word ->
                                    Text(
                                        text = word,
                                        style = MaterialTheme.typography.bodyLarge,
                                        // Words are shorter than the blanks beside them,
                                        // so without this the line reads as a zigzag.
                                        modifier = Modifier.align(Alignment.CenterVertically),
                                    )
                                }

                            is GapSegment.Gap -> Blank(
                                modifier = Modifier.align(Alignment.CenterVertically),
                                word = placed[segment.key],
                                number = step.gapKeys.indexOf(segment.key) + 1,
                                focused = !graded && segment.key == focused,
                                accent = gapAccent(segment.key, result, feedback.correct, feedback.wrong),
                                onClick = {
                                    if (graded) return@Blank
                                    if (placed[segment.key].isNullOrEmpty()) {
                                        focused = segment.key
                                    } else {
                                        onAnswerChange(AnswerState.Placements(placed - segment.key))
                                        focused = segment.key
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            step.bank.forEachIndexed { index, word ->
                // A word may appear in the bank more than once, and a few exercises reuse
                // one word across several gaps, so availability is counted against
                // capacity rather than by striking out a word the moment it is used.
                val used = placed.values.count { it == word }
                val capacity = step.capacity[word] ?: 1
                // Which copy of this word this chip is: the first "Frist" in the bank is
                // ordinal 1, the second is 2. Dimming the first `used` copies means a
                // bank holding two of them still shows one live chip after one placement.
                val ordinal = step.bank.take(index + 1).count { it == word }
                BankWord(
                    text = word,
                    spent = ordinal <= used,
                    enabled = !graded && used < capacity,
                    onClick = {
                        val target = focused?.takeIf { placed[it].isNullOrEmpty() }
                            ?: step.gapKeys.firstOrNull { placed[it].isNullOrEmpty() }
                            ?: return@BankWord
                        val updated = placed + (target to word)
                        onAnswerChange(AnswerState.Placements(updated))
                        focused = nextEmpty(target, updated) ?: target
                    },
                )
            }
        }
    }
}

private fun gapAccent(key: String, result: StepResult?, correct: Color, wrong: Color): Color? {
    val item = result?.items?.firstOrNull { it.ref == key } ?: return null
    return if (item.correct) correct else wrong
}

@Composable
private fun Blank(
    word: String?,
    number: Int,
    focused: Boolean,
    accent: Color?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val border = accent ?: if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    OutlinedCard(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(if (focused || accent != null) 2.dp else 1.dp, border),
        colors = CardDefaults.outlinedCardColors(
            containerColor = accent?.copy(alpha = 0.10f)
                ?: CardDefaults.outlinedCardColors().containerColor,
        ),
        modifier = modifier.testTag(ExerciseTestTags.BANK_GAP),
    ) {
        Text(
            // An empty blank shows its number, which is what the instructions refer to.
            text = word?.takeIf { it.isNotEmpty() } ?: "$number",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (word.isNullOrEmpty()) FontWeight.Normal else FontWeight.Medium,
            color = accent ?: if (word.isNullOrEmpty()) {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            } else {
                Color.Unspecified
            },
            modifier = Modifier
                .widthIn(min = 56.dp)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun BankWord(
    text: String,
    spent: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
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
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag(ExerciseTestTags.BANK_WORD),
    )
}
