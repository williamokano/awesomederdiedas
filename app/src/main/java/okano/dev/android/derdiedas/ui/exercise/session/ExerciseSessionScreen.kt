package okano.dev.android.derdiedas.ui.exercise.session

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import okano.dev.android.derdiedas.ui.exercise.ExerciseTestTags
import okano.dev.android.derdiedas.core.exercise.AnswerState
import okano.dev.android.derdiedas.core.exercise.ChoiceStep
import okano.dev.android.derdiedas.core.exercise.GapBankStep
import okano.dev.android.derdiedas.core.exercise.GapTextStep
import okano.dev.android.derdiedas.core.exercise.OrderStep
import okano.dev.android.derdiedas.core.exercise.SessionPhase
import okano.dev.android.derdiedas.core.exercise.SessionState
import okano.dev.android.derdiedas.core.exercise.SessionStep
import okano.dev.android.derdiedas.core.exercise.StepResult
import okano.dev.android.derdiedas.data.model.Language
import okano.dev.android.derdiedas.ui.exercise.steps.ChoiceStepContent
import okano.dev.android.derdiedas.ui.exercise.steps.GapBankStepContent
import okano.dev.android.derdiedas.ui.exercise.steps.GapTextStepContent
import okano.dev.android.derdiedas.ui.exercise.steps.OrderStepContent
import okano.dev.android.derdiedas.ui.resources.StringResources
import okano.dev.android.derdiedas.ui.theme.LocalFeedbackColors

/**
 * One exercise session: a progress bar, one step at a time, check, feedback, continue.
 */
@Composable
fun ExerciseSessionScreen(
    viewModel: ExerciseSessionViewModel,
    language: Language,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val answer by viewModel.answer.collectAsState()

    when (val state = uiState) {
        is SessionUiState.Loading -> Box(modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }

        is SessionUiState.Error -> Box(modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state.message, style = MaterialTheme.typography.bodyLarge)
                Button(onClick = viewModel::onRetry, modifier = Modifier.padding(top = 16.dp)) {
                    Text(StringResources.tryAgain(language))
                }
                TextButton(onClick = onExit) { Text(StringResources.back(language)) }
            }
        }

        is SessionUiState.Active -> {
            if (state.session.phase == SessionPhase.Finished) {
                SessionSummary(state, language, onExit, modifier)
            } else {
                ActiveSession(state.session, answer, language, viewModel, onExit, modifier)
            }
        }
    }
}

@Composable
private fun ActiveSession(
    session: SessionState,
    answer: AnswerState,
    language: Language,
    viewModel: ExerciseSessionViewModel,
    onExit: () -> Unit,
    modifier: Modifier,
) {
    val step = session.step ?: return
    val graded = session.phase == SessionPhase.Graded

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(onClick = onExit) { Text("✕") }
            LinearProgressIndicator(
                progress = { session.progress },
                modifier = Modifier.weight(1f).height(10.dp).testTag(ExerciseTestTags.SESSION_PROGRESS),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
        }

        // heightIn(min = maxHeight) is what makes the centring work: inside a
        // verticalScroll children are measured with unbounded height, so without a
        // minimum there is no slack for an arrangement to centre within. With it, a
        // short step sits in the middle and a long passage still scrolls.
        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val viewportHeight = maxHeight
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = viewportHeight)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
            ) {
                step.instructions?.let {
                    Text(it, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                step.instructionsEn
                    ?.takeIf { language == Language.ENGLISH }
                    ?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }

                StepContent(step, answer, session.result, viewModel::onAnswerChange)
            }
        }

        // One slot at the bottom, with the banner drawn over the check button rather
        // than stacked beside it. While the banner slides out its layout height is
        // already zero but it is still drawn, so in a column the check button was laid
        // into the space the banner was still painting over. Sharing the slot turns that
        // collision into the banner uncovering the button as it leaves.
        Box(modifier = Modifier.fillMaxWidth()) {
            if (!graded) {
                Button(
                    onClick = viewModel::onCheck,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .height(56.dp)
                        .testTag(ExerciseTestTags.CHECK_BUTTON),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(StringResources.check(language), fontWeight = FontWeight.Bold)
                }
            }

            ResultBanner(
                result = session.result,
                visible = graded,
                language = language,
                onContinue = viewModel::onContinue,
            )
        }
    }
}

/**
 * Dispatches a step to its widget. Exhaustive on purpose: adding an exercise type in a
 * later phase will not compile until its widget exists.
 */
@Composable
private fun StepContent(
    step: SessionStep,
    answer: AnswerState,
    result: StepResult?,
    onAnswerChange: (AnswerState) -> Unit,
) {
    when (step) {
        is GapTextStep -> GapTextStepContent(
            step = step,
            answer = answer as? AnswerState.Texts ?: AnswerState.Texts(),
            result = result,
            onAnswerChange = onAnswerChange,
        )

        is ChoiceStep -> ChoiceStepContent(
            step = step,
            answer = answer as? AnswerState.Choice ?: AnswerState.Choice(),
            result = result,
            onAnswerChange = onAnswerChange,
        )

        is GapBankStep -> GapBankStepContent(
            step = step,
            answer = answer as? AnswerState.Placements ?: AnswerState.Placements(),
            result = result,
            onAnswerChange = onAnswerChange,
        )

        is OrderStep -> OrderStepContent(
            step = step,
            answer = answer as? AnswerState.Sequence ?: AnswerState.Sequence(),
            result = result,
            onAnswerChange = onAnswerChange,
        )
    }
}

@Composable
private fun ResultBanner(
    result: StepResult?,
    visible: Boolean,
    language: Language,
    onContinue: () -> Unit,
) {
    val feedback = LocalFeedbackColors.current

    // AnimatedVisibility keeps its content composed while it slides away, but by then
    // the session has already cleared the result for the next step. Reading the live
    // value repainted a correct answer's banner in the wrong colours halfway through
    // the exit, so the last result shown is held until a new one replaces it.
    var lastShown by remember { mutableStateOf<StepResult?>(null) }
    if (result != null && result != lastShown) lastShown = result
    val shown = result ?: lastShown

    AnimatedVisibility(
        visible = visible && result != null,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
    ) {
        val correct = shown?.correct == true
        Surface(
            color = if (correct) feedback.correctContainer else feedback.wrongContainer,
            contentColor = if (correct) feedback.onCorrectContainer else feedback.onWrongContainer,
            modifier = Modifier.fillMaxWidth().testTag(ExerciseTestTags.RESULT_BANNER),
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (correct) {
                        StringResources.correctFeedback(language)
                    } else {
                        StringResources.wrongFeedback(language)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (!correct) {
                    val expected = shown?.items.orEmpty()
                        .filterNot { it.correct }
                        .map { it.expected }
                        .filter { it.isNotBlank() }
                    if (expected.isNotEmpty()) {
                        Text(
                            text = StringResources.theAnswer(language, expected.joinToString("  ·  ")),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                shown?.items?.firstNotNullOfOrNull { it.note }?.let { why ->
                    Text(
                        text = why,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                // Only worth saying for multi-gap steps; "1 of 1" is noise.
                shown?.takeIf { it.items.size > 1 }?.let {
                    Text(
                        StringResources.gapsCorrect(language, it.correctCount, it.items.size),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag(ExerciseTestTags.CONTINUE_BUTTON),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (correct) feedback.correct else feedback.wrong,
                        contentColor = if (correct) feedback.onCorrect else feedback.onWrong,
                    ),
                ) {
                    Text(StringResources.continueLabel(language), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SessionSummary(
    state: SessionUiState.Active,
    language: Language,
    onExit: () -> Unit,
    modifier: Modifier,
) {
    val session = state.session

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp)
            .testTag(ExerciseTestTags.SESSION_SUMMARY),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🎉", style = MaterialTheme.typography.displayLarge)
        Text(
            StringResources.sessionComplete(language),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            StringResources.firstTryAccuracy(language, session.accuracyPercentage),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            StringResources.stepsMastered(language, session.masteredCount, session.totalSteps),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 4.dp),
        )
        if (session.unmastered.isNotEmpty()) {
            Text(
                StringResources.stillToReview(language, session.unmastered.size),
                style = MaterialTheme.typography.bodyMedium,
                color = LocalFeedbackColors.current.accepted,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        Button(
            onClick = onExit,
            modifier = Modifier.fillMaxWidth().padding(top = 32.dp).height(56.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(StringResources.done(language), fontWeight = FontWeight.Bold)
        }
    }
}
