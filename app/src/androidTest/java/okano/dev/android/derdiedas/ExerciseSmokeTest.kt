package okano.dev.android.derdiedas

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import okano.dev.android.derdiedas.ui.exercise.ExerciseTestTags
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Drives the app on a real device.
 *
 * These exist for the failures the 99 unit tests structurally cannot reach. That suite is
 * pure JVM: no composable is ever composed and no Android runtime is involved, which is
 * what makes the grading engine testable, but it also means a regex Android's ICU engine
 * rejects, a widget that will not lay out, a broken nav route or an R8 keep rule that
 * strips a serializer all pass there and fail on a device. Two of those have already
 * happened.
 *
 * So these tests deliberately do not re-check grading, step splitting or session
 * bookkeeping -- that is all pinned on the JVM, where it is fast and precise. They check
 * that the screens compose, that the routes connect, and that a session can be played
 * from beginning to end.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class ExerciseSmokeTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    // ---- the happy path ----

    @Test
    fun canWalkFromHomeIntoASessionAndAnswerAStep() {
        openFirstSession()

        compose.onNodeWithTag(ExerciseTestTags.SESSION_PROGRESS).assertIsDisplayed()
        answerCurrentStep()
        compose.onNodeWithTag(ExerciseTestTags.CHECK_BUTTON).performClick()

        // Grading closes the loop, whether the answer was right or wrong.
        compose.waitUntilAtLeastOneExists(hasTestTag(ExerciseTestTags.CONTINUE_BUTTON), TIMEOUT_MS)
        compose.onNodeWithTag(ExerciseTestTags.RESULT_BANNER).assertIsDisplayed()

        compose.onNodeWithTag(ExerciseTestTags.CONTINUE_BUTTON).performClick()
        compose.waitForIdle()
    }

    /**
     * The one that earns its runtime: plays a whole session to the summary.
     *
     * It never needs to know the right answer. A step that is missed comes back once and
     * is then dropped, so answering everything terminates either way -- which also means
     * this exercises re-queueing and the attempt cap against real content, not fixtures.
     */
    @Test
    fun playsAWholeSessionThroughToTheSummary() {
        openFirstSession()

        var guard = 0
        while (guard++ < MAX_ANSWERS) {
            if (nodes(ExerciseTestTags.SESSION_SUMMARY).fetchSemanticsNodes().isNotEmpty()) break
            if (nodes(ExerciseTestTags.CHECK_BUTTON).fetchSemanticsNodes().isEmpty()) break

            answerCurrentStep()
            compose.onNodeWithTag(ExerciseTestTags.CHECK_BUTTON).performClick()
            compose.waitUntilAtLeastOneExists(hasTestTag(ExerciseTestTags.CONTINUE_BUTTON), TIMEOUT_MS)
            compose.onNodeWithTag(ExerciseTestTags.CONTINUE_BUTTON).performClick()
            compose.waitForIdle()
        }

        compose.waitUntilAtLeastOneExists(hasTestTag(ExerciseTestTags.SESSION_SUMMARY), TIMEOUT_MS)
        compose.onNodeWithTag(ExerciseTestTags.SESSION_SUMMARY).assertIsDisplayed()
    }

    @Test
    fun answeringAStepAlwaysLeadsSomewhere() {
        openFirstSession()

        // A typed gap gets a deliberately wrong answer, since nothing in the corpus
        // accepts it; a choice step gets its first option, right or wrong. Either way
        // the loop has to move on rather than stall on the graded step.
        val fields = nodes(ExerciseTestTags.GAP_FIELD)
        if (fields.fetchSemanticsNodes().isNotEmpty()) {
            fields.onFirst().performTextInput("qqqqqq")
        } else if (nodes(ExerciseTestTags.OPTION).fetchSemanticsNodes().isNotEmpty()) {
            nodes(ExerciseTestTags.OPTION).onFirst().performClick()
        } else if (nodes(ExerciseTestTags.ORDER_TILE).fetchSemanticsNodes().isNotEmpty()) {
            // A sentence-building step, left deliberately half-built: an incomplete
            // sequence is graded wrong, which is exactly the case under test.
            nodes(ExerciseTestTags.ORDER_TILE).onFirst().performClick()
        } else if (nodes(ExerciseTestTags.BANK_WORD).fetchSemanticsNodes().isNotEmpty()) {
            // A word-bank passage with one blank filled and the rest empty, which is
            // likewise wrong.
            nodes(ExerciseTestTags.BANK_WORD).onFirst().performClick()
        } else {
            // A matching board with one prompt paired and the rest empty.
            nodes(ExerciseTestTags.MATCH_POOL_ENTRY).onFirst().performClick()
        }

        compose.onNodeWithTag(ExerciseTestTags.CHECK_BUTTON).performClick()
        compose.waitUntilAtLeastOneExists(hasTestTag(ExerciseTestTags.CONTINUE_BUTTON), TIMEOUT_MS)
        compose.onNodeWithTag(ExerciseTestTags.RESULT_BANNER).assertIsDisplayed()

        compose.onNodeWithTag(ExerciseTestTags.CONTINUE_BUTTON).performClick()
        compose.waitForIdle()

        // Either another step or the summary. Getting neither means the session stalled.
        val movedOn = nodes(ExerciseTestTags.CHECK_BUTTON).fetchSemanticsNodes().isNotEmpty() ||
            nodes(ExerciseTestTags.SESSION_SUMMARY).fetchSemanticsNodes().isNotEmpty()
        assertTrue("the session stalled after continuing", movedOn)
    }

    // ---- navigation ----

    @Test
    fun everyCollectionOpensAndListsSets() {
        compose.onNodeWithTag(ExerciseTestTags.EXERCISES_ENTRY).performClick()
        compose.waitUntilAtLeastOneExists(hasTestTag(ExerciseTestTags.COLLECTION_CARD), TIMEOUT_MS)

        val count = nodes(ExerciseTestTags.COLLECTION_CARD).fetchSemanticsNodes().size
        // Hören is hidden because every exercise in it needs audio, so three are expected;
        // asserting "more than one" keeps this from breaking on a content sync.
        assertTrue("expected at least two collections, found $count", count >= 2)

        repeat(count) { index ->
            nodes(ExerciseTestTags.COLLECTION_CARD)[index].performClick()
            compose.waitUntilAtLeastOneExists(hasTestTag(ExerciseTestTags.SET_CARD), TIMEOUT_MS)
            compose.onAllNodesWithTag(ExerciseTestTags.LEVEL_TAB).onFirst().assertIsDisplayed()
            Espresso.pressBack()
            compose.waitUntilAtLeastOneExists(hasTestTag(ExerciseTestTags.COLLECTION_CARD), TIMEOUT_MS)
        }
    }

    @Test
    fun switchingLevelTabKeepsTheListUsable() {
        compose.onNodeWithTag(ExerciseTestTags.EXERCISES_ENTRY).performClick()
        compose.waitUntilAtLeastOneExists(hasTestTag(ExerciseTestTags.COLLECTION_CARD), TIMEOUT_MS)
        nodes(ExerciseTestTags.COLLECTION_CARD).onFirst().performClick()
        compose.waitUntilAtLeastOneExists(hasTestTag(ExerciseTestTags.SET_CARD), TIMEOUT_MS)

        // Every collection spans at least two levels, so the last tab is a real switch.
        nodes(ExerciseTestTags.LEVEL_TAB).onLast().performClick()
        compose.waitUntilAtLeastOneExists(hasTestTag(ExerciseTestTags.SET_CARD), TIMEOUT_MS)
        nodes(ExerciseTestTags.SET_CARD).onFirst().assertIsDisplayed()
    }

    @Test
    fun backNavigationUnwindsAllTheWayHome() {
        openFirstSession()

        Espresso.pressBack() // session -> set list
        compose.waitUntilAtLeastOneExists(hasTestTag(ExerciseTestTags.SET_CARD), TIMEOUT_MS)

        Espresso.pressBack() // set list -> collections
        compose.waitUntilAtLeastOneExists(hasTestTag(ExerciseTestTags.COLLECTION_CARD), TIMEOUT_MS)

        Espresso.pressBack() // collections -> home
        compose.waitUntilAtLeastOneExists(hasTestTag(ExerciseTestTags.EXERCISES_ENTRY), TIMEOUT_MS)
        compose.onNodeWithTag(ExerciseTestTags.EXERCISES_ENTRY).assertIsDisplayed()
    }

    // ---- the original feature ----

    @Test
    fun theFlashcardGameStillStarts() {
        // No coverage of its own, and the exercise work moved its dependencies into
        // AppContainer and rewrote the nav graph around it.
        compose.onNodeWithTag(ExerciseTestTags.NEW_GAME_ENTRY).performClick()
        compose.waitUntilAtLeastOneExists(hasTestTag(ExerciseTestTags.CARD_COUNT_OPTION), TIMEOUT_MS)

        nodes(ExerciseTestTags.CARD_COUNT_OPTION).onFirst().performClick()

        // Three article buttons mean a card was dealt from the repository.
        compose.waitUntilAtLeastOneExists(hasTestTag(ExerciseTestTags.ARTICLE_BUTTON), TIMEOUT_MS)
        nodes(ExerciseTestTags.ARTICLE_BUTTON).onFirst().performClick()
        compose.waitForIdle()
    }

    // ---- helpers ----

    private fun nodes(tag: String): SemanticsNodeInteractionCollection =
        compose.onAllNodesWithTag(tag)

    /** Home -> first collection -> first set -> first block, leaving a step on screen. */
    private fun openFirstSession() {
        compose.onNodeWithTag(ExerciseTestTags.EXERCISES_ENTRY).assertIsDisplayed().performClick()

        // Reading 226 assets off disk is not instant on a cold emulator.
        compose.waitUntilAtLeastOneExists(hasTestTag(ExerciseTestTags.COLLECTION_CARD), TIMEOUT_MS)
        nodes(ExerciseTestTags.COLLECTION_CARD).onFirst().performClick()

        compose.waitUntilAtLeastOneExists(hasTestTag(ExerciseTestTags.SET_CARD), TIMEOUT_MS)
        nodes(ExerciseTestTags.SET_CARD).onFirst().performClick()

        compose.waitUntilAtLeastOneExists(hasTestTag(ExerciseTestTags.PRACTISE_BUTTON), TIMEOUT_MS)
        nodes(ExerciseTestTags.PRACTISE_BUTTON).onFirst().performClick()

        compose.waitUntilAtLeastOneExists(hasTestTag(ExerciseTestTags.CHECK_BUTTON), TIMEOUT_MS)
    }

    /**
     * Answers whichever kind of step came up.
     *
     * Which one it is depends on the content, and pinning that down would make these fail
     * on a content sync rather than on a real regression.
     */
    private fun answerCurrentStep() {
        val options = nodes(ExerciseTestTags.OPTION)
        if (options.fetchSemanticsNodes().isNotEmpty()) {
            options.onFirst().performClick()
            return
        }
        // Sentence building: place every tile, or Check grades a half-built sentence and
        // the session never moves past it.
        val tiles = nodes(ExerciseTestTags.ORDER_TILE).fetchSemanticsNodes().size
        if (tiles > 0) {
            repeat(tiles) {
                nodes(ExerciseTestTags.ORDER_TILE).onFirst().performClick()
            }
            return
        }
        // Word bank: tapping a word fills the focused blank and focus moves on, so one
        // tap per blank fills the passage. Indexing rather than taking the first node
        // each time, because a word already used is dimmed and stops responding.
        val blanks = nodes(ExerciseTestTags.BANK_GAP).fetchSemanticsNodes().size
        if (blanks > 0) {
            val words = nodes(ExerciseTestTags.BANK_WORD).fetchSemanticsNodes().size
            repeat(minOf(blanks, words)) { index ->
                nodes(ExerciseTestTags.BANK_WORD)[index].performClick()
            }
            return
        }
        // Matching: same shape as the word bank -- one tap per prompt fills them in turn.
        val prompts = nodes(ExerciseTestTags.MATCH_PROMPT).fetchSemanticsNodes().size
        if (prompts > 0) {
            val entries = nodes(ExerciseTestTags.MATCH_POOL_ENTRY).fetchSemanticsNodes().size
            repeat(minOf(prompts, entries)) { index ->
                nodes(ExerciseTestTags.MATCH_POOL_ENTRY)[index].performClick()
            }
            return
        }
        val fields = nodes(ExerciseTestTags.GAP_FIELD)
        if (fields.fetchSemanticsNodes().isNotEmpty()) {
            fields.onFirst().performTextInput("test")
        }
    }

    private companion object {
        // Generous: a cold emulator parsing the bundled index is slow the first time.
        const val TIMEOUT_MS = 15_000L

        // A session holds at most 15 steps and each is offered at most twice, so any
        // session must end well inside this. The cap stops a regression hanging the run.
        const val MAX_ANSWERS = 40
    }
}
