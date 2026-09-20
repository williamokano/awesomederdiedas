package okano.dev.android.derdiedas.ui.exercise.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import okano.dev.android.derdiedas.ui.exercise.ExerciseTestTags
import okano.dev.android.derdiedas.core.exercise.AnswerState
import okano.dev.android.derdiedas.core.exercise.GapSegment
import okano.dev.android.derdiedas.core.exercise.GapTextStep
import okano.dev.android.derdiedas.core.exercise.ItemResult
import okano.dev.android.derdiedas.core.exercise.StepResult
import okano.dev.android.derdiedas.ui.theme.LocalFeedbackColors

/**
 * A gap-fill step: the sentence (or passage) with a field per blank.
 *
 * Compose has no way to put a text field inline in flowing text — InlineTextContent needs a
 * fixed-size placeholder, which variable-length input breaks. So the sentence is rendered
 * with the gaps shown as `____` and the fields sit beneath it.
 *
 * The widget never grades. It renders state and reports edits upward; the session engine
 * does the grading, which is what keeps that logic testable without a Compose runtime.
 */
@Composable
fun GapTextStepContent(
    step: GapTextStep,
    answer: AnswerState.Texts,
    result: StepResult?,
    onAnswerChange: (AnswerState.Texts) -> Unit,
    modifier: Modifier = Modifier,
) {
    val graded = result != null
    val resultsByRef = result?.items?.associateBy { it.ref }.orEmpty()
    // With one gap the banner already names the answer, so repeating it under the field
    // just says the same thing twice. With several, only a per-gap mark can show which
    // one was wrong.
    val single = step.gapKeys.size == 1

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(
            text = renderSentence(step),
            style = if (single) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            lineHeight = MaterialTheme.typography.headlineMedium.fontSize * 1.4f,
        )

        // A drill whose whole answer space is der/die/das is a choice, not a spelling
        // test, so it gets chips instead of a keyboard.
        if (step.options.isNotEmpty()) {
            val key = step.gapKeys.single()
            OptionChips(
                options = step.options,
                selected = answer.byRef[key],
                enabled = !graded,
                itemResult = resultsByRef[key],
                onSelect = { option -> onAnswerChange(AnswerState.Texts(answer.byRef + (key to option))) },
            )
            return@Column
        }

        step.gapKeys.forEach { key ->
            GapField(
                key = key,
                value = answer.byRef[key].orEmpty(),
                cue = step.cues[key],
                showLabel = !single,
                showInlineResult = !single,
                enabled = !graded,
                itemResult = resultsByRef[key],
                onValueChange = { text ->
                    onAnswerChange(AnswerState.Texts(answer.byRef + (key to text)))
                },
            )
        }
    }
}

/** The sentence with each gap shown as a blank, so the learner can read around it. */
private fun renderSentence(step: GapTextStep): String = buildString {
    val numbered = step.gapKeys.size > 1
    for (segment in step.segments) {
        when (segment) {
            is GapSegment.Literal -> append(segment.text)
            is GapSegment.Gap -> append(if (numbered) "(${segment.key}) ____" else "____")
        }
    }
}.trim()

@Composable
private fun GapField(
    key: String,
    value: String,
    cue: String?,
    showLabel: Boolean,
    showInlineResult: Boolean,
    enabled: Boolean,
    itemResult: ItemResult?,
    onValueChange: (String) -> Unit,
) {
    val feedback = LocalFeedbackColors.current

    val accent = when {
        itemResult == null -> null
        itemResult.correct && itemResult.note != null -> feedback.accepted
        itemResult.correct -> feedback.correct
        else -> feedback.wrong
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            // Disabled greys the text out, which makes a graded answer hard to read.
            // Read-only keeps it legible while still refusing edits.
            readOnly = !enabled,
            singleLine = true,
            textStyle = MaterialTheme.typography.titleLarge,
            label = if (showLabel) ({ Text("($key)") }) else null,
            placeholder = cue?.let { { Text(it) } },
            colors = if (accent == null) {
                OutlinedTextFieldDefaults.colors()
            } else {
                OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent,
                    unfocusedBorderColor = accent,
                    focusedTextColor = accent,
                    unfocusedTextColor = accent,
                )
            },
            keyboardOptions = KeyboardOptions(
                // German nouns are capitalised, but nothing in the corpus grades
                // case-sensitively, so don't fight the keyboard over it.
                capitalization = KeyboardCapitalization.None,
                imeAction = ImeAction.Done,
            ),
            modifier = Modifier.fillMaxWidth().testTag(ExerciseTestTags.GAP_FIELD),
        )

        if (showInlineResult && itemResult != null) {
            // Glyph as well as colour: green-vs-red alone is invisible to some readers,
            // and it is the only thing distinguishing "right" from "accepted variant".
            val (glyph, message) = when {
                itemResult.correct && itemResult.note != null -> "≈" to itemResult.note
                itemResult.correct -> "✓" to null
                else -> "✗" to itemResult.expected
            }
            Text(
                text = listOfNotNull(glyph, message).joinToString("  "),
                style = MaterialTheme.typography.labelLarge,
                color = accent ?: MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 4.dp, top = 6.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptionChips(
    options: List<String>,
    selected: String?,
    enabled: Boolean,
    itemResult: ItemResult?,
    onSelect: (String) -> Unit,
) {
    val feedback = LocalFeedbackColors.current

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        options.forEach { option ->
            val isSelected = option.equals(selected, ignoreCase = true)
            // After grading, mark the learner's pick and reveal the right one if they
            // differ, which is the whole feedback for this step.
            val isExpected = itemResult != null && option.equals(itemResult.expected, ignoreCase = true)
            val container = when {
                itemResult == null -> null
                isSelected && itemResult.correct -> feedback.correctContainer
                isSelected -> feedback.wrongContainer
                isExpected -> feedback.correctContainer
                else -> null
            }

            FilterChip(
                selected = isSelected,
                onClick = { if (enabled) onSelect(option) },
                enabled = enabled || isSelected || isExpected,
                label = {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 6.dp),
                    )
                },
                modifier = Modifier.testTag(ExerciseTestTags.OPTION),
                colors = if (container == null) {
                    FilterChipDefaults.filterChipColors()
                } else {
                    FilterChipDefaults.filterChipColors(
                        containerColor = container,
                        selectedContainerColor = container,
                        disabledContainerColor = container,
                        disabledSelectedContainerColor = container,
                    )
                },
            )
        }
    }
}
