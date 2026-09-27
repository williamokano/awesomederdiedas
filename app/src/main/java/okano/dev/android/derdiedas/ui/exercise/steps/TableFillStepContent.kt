package okano.dev.android.derdiedas.ui.exercise.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import okano.dev.android.derdiedas.core.exercise.AnswerState
import okano.dev.android.derdiedas.core.exercise.StepResult
import okano.dev.android.derdiedas.core.exercise.TableFillStep

/**
 * One row of a table: the row's stub, and a field per cell.
 *
 * Not a grid. The 55 authored tables run to 6 columns with answers up to 50 characters,
 * which is far past what a phone can lay out side by side -- the same measurement that
 * forced the matching board to stop being two columns. A row instead becomes a stub with
 * its fields stacked beneath it, which is the shape the gap-text lines already use, and
 * the column headings survive as the field labels.
 *
 * The widget holds no state and never grades. It renders what it is given and reports
 * edits upward.
 */
@Composable
fun TableFillStepContent(
    step: TableFillStep,
    answer: AnswerState.Texts,
    result: StepResult?,
    onAnswerChange: (AnswerState.Texts) -> Unit,
    modifier: Modifier = Modifier,
) {
    val graded = result != null
    val resultsByRef = result?.items?.associateBy { it.ref }.orEmpty()
    // With one field the banner already names the answer; repeating it under the field says
    // the same thing twice. With several, only a per-field mark shows which one was wrong.
    val single = step.fields.size == 1

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        step.example?.let { WorkedExample(it) }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            // The heading of the column the stub occupies, where the table gives it one.
            // "Konnektor: obwohl" says what a bare "obwohl" does not.
            step.promptLabel?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
            Text(
                text = step.prompt,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
            )
        }

        step.fields.forEach { field ->
            GapField(
                // The column heading. Always worth showing, unlike gap-text's bare number:
                // it is what says whether this cell wants the Dativ or the Akkusativ.
                label = field.header,
                value = answer.byRef[field.key].orEmpty(),
                cue = null,
                showInlineResult = !single,
                enabled = !graded,
                itemResult = resultsByRef[field.key],
                onValueChange = { text ->
                    onAnswerChange(AnswerState.Texts(answer.byRef + (field.key to text)))
                },
            )
        }
    }
}

/**
 * The row the table opens with already filled in.
 *
 * Four rows across two exercises are authored this way, and they carry real information:
 * in an ordinal table whose stubs are bare numbers, "1 -> erst- - erste" is the only thing
 * saying what shape the answers take.
 */
@Composable
private fun WorkedExample(example: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = example,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}
