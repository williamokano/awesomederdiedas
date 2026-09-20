package okano.dev.android.derdiedas.ui.exercise.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import okano.dev.android.derdiedas.core.exercise.AnswerState
import okano.dev.android.derdiedas.core.exercise.ChoiceOption
import okano.dev.android.derdiedas.core.exercise.ChoiceStep
import okano.dev.android.derdiedas.core.exercise.StepResult
import okano.dev.android.derdiedas.ui.theme.LocalFeedbackColors

/**
 * Pick one of a few options.
 *
 * Serves single-choice, true-false and odd-one-out: they are one interaction wearing
 * different clothes, and the splitter has already reduced them to the same shape.
 */
@Composable
fun ChoiceStepContent(
    step: ChoiceStep,
    answer: AnswerState.Choice,
    result: StepResult?,
    onAnswerChange: (AnswerState.Choice) -> Unit,
    modifier: Modifier = Modifier,
) {
    val graded = result != null

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        // odd-one-out has no question of its own: the instruction asks which one does
        // not belong, and the options are the material.
        step.prompt?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            step.options.forEach { option ->
                ChoiceRow(
                    option = option,
                    selected = option.key == answer.key,
                    isAnswer = option.key == step.answerKey,
                    graded = graded,
                    onClick = { if (!graded) onAnswerChange(AnswerState.Choice(option.key)) },
                )
            }
        }
    }
}

@Composable
private fun ChoiceRow(
    option: ChoiceOption,
    selected: Boolean,
    isAnswer: Boolean,
    graded: Boolean,
    onClick: () -> Unit,
) {
    val feedback = LocalFeedbackColors.current

    // Before grading only the selection is marked. After it, the learner's pick is marked
    // right or wrong and the correct one is revealed if they missed it.
    val accent: Color? = when {
        !graded -> if (selected) MaterialTheme.colorScheme.primary else null
        selected && isAnswer -> feedback.correct
        selected -> feedback.wrong
        isAnswer -> feedback.correct
        else -> null
    }
    val container = when {
        !graded -> null
        selected && isAnswer -> feedback.correctContainer
        selected -> feedback.wrongContainer
        isAnswer -> feedback.correctContainer
        else -> null
    }
    val glyph = when {
        !graded -> null
        selected && isAnswer -> "✓"
        selected -> "✗"
        isAnswer -> "✓"
        else -> null
    }

    OutlinedCard(
        onClick = onClick,
        enabled = !graded,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = container ?: CardDefaults.outlinedCardColors().containerColor,
            disabledContainerColor = container ?: CardDefaults.outlinedCardColors().containerColor,
        ),
        border = CardDefaults.outlinedCardBorder(enabled = !graded).let { default ->
            if (accent == null) default else androidx.compose.foundation.BorderStroke(2.dp, accent)
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = option.text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            // A glyph as well as colour, so the state survives colour blindness.
            glyph?.let {
                Text(text = it, style = MaterialTheme.typography.titleMedium, color = accent ?: Color.Unspecified)
            }
        }
    }
}
