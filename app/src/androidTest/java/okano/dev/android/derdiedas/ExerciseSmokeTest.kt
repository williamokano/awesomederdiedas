package okano.dev.android.derdiedas

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import okano.dev.android.derdiedas.ui.exercise.ExerciseTestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Walks the exercise flow on a real device: home, collection, level, set, block, answer.
 *
 * This exists because the unit tests structurally cannot catch two whole classes of bug
 * that have already reached a device. The grading and session logic are pure Kotlin and
 * run on the JVM, where a regex Android's ICU engine rejects still compiles, and where no
 * composable is ever composed. Anything that only breaks once Android is involved -- a
 * bad pattern, a missing keep rule, a widget that will not lay out, a broken nav route --
 * only shows up here.
 *
 * Deliberately one happy path rather than broad coverage: it is a smoke test, and its job
 * is to fail loudly when the app stops starting or the flow stops working.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class ExerciseSmokeTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun canWalkFromHomeIntoASessionAndAnswerAStep() {
        // Home offers exercises.
        compose.onNodeWithTag(ExerciseTestTags.EXERCISES_ENTRY).assertIsDisplayed().performClick()

        // The index parses and yields collections. Reading 226 assets off disk is not
        // instant on a cold emulator, so wait rather than assume.
        compose.waitUntilAtLeastOneExists(hasTestTag(ExerciseTestTags.COLLECTION_CARD), TIMEOUT_MS)
        compose.onAllNodesWithTag(ExerciseTestTags.COLLECTION_CARD).onFirst().performClick()

        // A collection lists sets, with its levels as tabs.
        compose.waitUntilAtLeastOneExists(hasTestTag(ExerciseTestTags.SET_CARD), TIMEOUT_MS)
        compose.onAllNodesWithTag(ExerciseTestTags.LEVEL_TAB).onFirst().assertIsDisplayed()

        // Expanding a set reveals its blocks; the first one starts a session.
        compose.onAllNodesWithTag(ExerciseTestTags.SET_CARD).onFirst().performClick()
        compose.waitUntilAtLeastOneExists(hasTestTag(ExerciseTestTags.PRACTISE_BUTTON), TIMEOUT_MS)
        compose.onAllNodesWithTag(ExerciseTestTags.PRACTISE_BUTTON).onFirst().performClick()

        // The session loads a step. This is where the ICU regex crash used to happen:
        // the splitter's pattern failed to compile the moment a session was built.
        compose.waitUntilAtLeastOneExists(hasTestTag(ExerciseTestTags.CHECK_BUTTON), TIMEOUT_MS)
        compose.onNodeWithTag(ExerciseTestTags.SESSION_PROGRESS).assertIsDisplayed()

        answerCurrentStep()

        compose.onNodeWithTag(ExerciseTestTags.CHECK_BUTTON).performClick()

        // Grading produces a banner offering to continue, whether the answer was right
        // or wrong -- the smoke test cares that the loop closes, not that it scored.
        compose.waitUntilAtLeastOneExists(hasTestTag(ExerciseTestTags.CONTINUE_BUTTON), TIMEOUT_MS)
        compose.onNodeWithTag(ExerciseTestTags.RESULT_BANNER).assertIsDisplayed()

        // And continuing moves on rather than getting stuck.
        compose.onNodeWithTag(ExerciseTestTags.CONTINUE_BUTTON).performClick()
        compose.waitForIdle()
    }

    /**
     * Answers whatever kind of step came up.
     *
     * Which one it is depends on the content, and the first block of the first set is not
     * something the test should pin down -- that would make it fail on a content sync
     * rather than on a real regression.
     */
    private fun answerCurrentStep() {
        val options = compose.onAllNodesWithTag(ExerciseTestTags.OPTION)
        if (options.fetchSemanticsNodes().isNotEmpty()) {
            options.onFirst().performClick()
            return
        }
        val fields = compose.onAllNodesWithTag(ExerciseTestTags.GAP_FIELD)
        if (fields.fetchSemanticsNodes().isNotEmpty()) {
            // Any text will do: a wrong answer exercises the same loop as a right one.
            fields.onFirst().performTextInput("test")
        }
    }

    private companion object {
        // Generous: a cold emulator parsing the bundled index is slow the first time.
        const val TIMEOUT_MS = 15_000L
    }
}
