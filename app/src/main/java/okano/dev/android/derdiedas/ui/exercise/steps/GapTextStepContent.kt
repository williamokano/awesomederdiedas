package okano.dev.android.derdiedas.ui.exercise.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
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
 * with the gaps shown as `____` and the fields sit beneath it. For the 89% of gap-texts
 * that are single numbered sentences this reads naturally; for the passages it is the same
 * layout most language apps use.
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

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = renderSentence(step),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium,
        )

        step.gapKeys.forEach { key ->
            GapField(
                key = key,
                value = answer.byRef[key].orEmpty(),
                cue = step.cues[key],
                showNumber = step.gapKeys.size > 1,
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
    for (segment in step.segments) {
        when (segment) {
            is GapSegment.Literal -> append(segment.text)
            is GapSegment.Gap -> append(if (step.gapKeys.size > 1) "(${segment.key}) ____" else "____")
        }
    }
}.trim()

@Composable
private fun GapField(
    key: String,
    value: String,
    cue: String?,
    showNumber: Boolean,
    enabled: Boolean,
    itemResult: ItemResult?,
    onValueChange: (String) -> Unit,
) {
    val feedback = LocalFeedbackColors.current

    val accentColor = when {
        itemResult == null -> null
        itemResult.correct && itemResult.note != null -> feedback.accepted
        itemResult.correct -> feedback.correct
        else -> feedback.wrong
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            isError = itemResult?.correct == false,
            label = { Text(if (showNumber) "($key)" else "") },
            placeholder = cue?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(
                // German nouns are capitalised, but nothing in the corpus grades
                // case-sensitively, so don't fight the keyboard over it.
                capitalization = KeyboardCapitalization.None,
                imeAction = ImeAction.Done,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        if (itemResult != null) {
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
                color = accentColor ?: MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            )
        }
    }
}
