package okano.dev.android.derdiedas.core.exercise

import okano.dev.android.derdiedas.data.exercise.model.Block
import okano.dev.android.derdiedas.data.exercise.model.Exercise
import okano.dev.android.derdiedas.data.exercise.model.ExerciseSet
import okano.dev.android.derdiedas.data.exercise.model.GapTextBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun gapTextExercise(id: String, block: Block = Block.A, gaps: Int = 1) = Exercise(
    id = id,
    block = block,
    title = "Übung $id",
    body = GapTextBody(
        text = (1..gaps).joinToString("\n") { "$it. Satz {$it}" },
        answers = (1..gaps).associate { it.toString() to listOf("a$it") },
        listLayout = true,
    ),
)

private fun set(vararg exercises: Exercise) = ExerciseSet(
    formatVersion = 1,
    id = "test-set",
    lesson = "EX/test",
    title = "Test",
    exercises = exercises.toList(),
)

class SessionPlanTest {

    @Test
    fun `a short block is a single session`() {
        val plans = set(gapTextExercise("A1", gaps = 5)).sessionsFor(Block.A)

        assertEquals(1, plans.size)
        assertEquals(5, plans.single().steps.size)
        assertEquals(1, plans.single().partCount)
    }

    @Test
    fun `a long block is split into evenly sized parts`() {
        // 16 steps must become 8 + 8, not 15 + 1: no session should be a lone leftover.
        val plans = set(gapTextExercise("A1", gaps = 16)).sessionsFor(Block.A)

        assertEquals(2, plans.size)
        assertEquals(listOf(8, 8), plans.map { it.steps.size })
        assertEquals(listOf(0, 1), plans.map { it.part })
        assertTrue(plans.all { it.partCount == 2 })
    }

    @Test
    fun `no session exceeds the cap`() {
        val plans = set(gapTextExercise("A1", gaps = 99)).sessionsFor(Block.A)

        assertTrue(plans.all { it.steps.size <= MAX_SESSION_STEPS })
        assertEquals(99, plans.sumOf { it.steps.size })
    }

    @Test
    fun `parts cover every step exactly once and keep their order`() {
        val plans = set(gapTextExercise("A1", gaps = 17)).sessionsFor(Block.A)
        val ids = plans.flatMap { it.steps }.map { it.id.value }

        assertEquals(17, ids.size)
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `only the requested block is included`() {
        val exercises = set(
            gapTextExercise("A1", block = Block.A, gaps = 3),
            gapTextExercise("B1", block = Block.B, gaps = 4),
        )

        assertEquals(3, exercises.sessionsFor(Block.A).single().steps.size)
        assertEquals(4, exercises.sessionsFor(Block.B).single().steps.size)
    }

    @Test
    fun `a block with no playable steps yields no sessions`() {
        assertTrue(set(gapTextExercise("A1", block = Block.A)).sessionsFor(Block.D).isEmpty())
    }
}

class SessionEngineTest {

    private fun engineOf(gaps: Int): SessionEngine =
        SessionEngine(set(gapTextExercise("A1", gaps = gaps)).sessionsFor(Block.A).single())

    /** The correct answer for the current step, derived from the step itself. */
    private fun correctAnswer(engine: SessionEngine): AnswerState {
        val step = engine.state().step as GapTextStep
        return AnswerState.Texts(step.gapKeys.associateWith { step.answers.getValue(it).first() })
    }

    private val wrongAnswer = AnswerState.Texts(mapOf("1" to "definitiv falsch"))

    @Test
    fun `starts answering the first step`() {
        val state = engineOf(3).state()

        assertEquals(SessionPhase.Answering, state.phase)
        assertNotNull(state.step)
        assertNull(state.result)
        assertEquals(3, state.totalSteps)
        assertEquals(0f, state.progress, 0.001f)
    }

    @Test
    fun `a correct answer grades then advances and counts as mastered`() {
        val engine = engineOf(2)

        val graded = engine.check(correctAnswer(engine))
        assertEquals(SessionPhase.Graded, graded.phase)
        assertTrue(graded.result!!.correct)
        // Still on the same step: the banner must persist until the learner continues.
        assertEquals(0, graded.masteredCount)

        val next = engine.advance()
        assertEquals(SessionPhase.Answering, next.phase)
        assertEquals(1, next.masteredCount)
        assertNull(next.result)
    }

    @Test
    fun `a wrong step comes back later in the same session`() {
        val engine = engineOf(3)
        val firstId = engine.state().step!!.id

        engine.check(wrongAnswer)
        val afterAdvance = engine.advance()

        // Not immediately: it goes to the back of the queue.
        assertTrue(afterAdvance.step!!.id != firstId)

        // But it does come back before the session can finish.
        val seen = mutableListOf(afterAdvance.step!!.id)
        repeat(2) {
            engine.check(correctAnswer(engine))
            engine.advance().step?.let { seen += it.id }
        }
        assertTrue("the missed step should reappear", firstId in seen)
    }

    @Test
    fun `a step is re-queued only once and the session terminates`() {
        // Without a cap, a learner who keeps missing one gap could never finish.
        val engine = engineOf(1)

        engine.check(wrongAnswer)
        engine.advance()
        assertEquals(SessionPhase.Answering, engine.state().phase)

        engine.check(wrongAnswer)
        val finished = engine.advance()

        assertEquals(SessionPhase.Finished, finished.phase)
        assertEquals(0, finished.masteredCount)
        assertEquals(1, finished.unmastered.size)
    }

    @Test
    fun `a step missed once then answered correctly counts as mastered`() {
        val engine = engineOf(1)

        engine.check(wrongAnswer)
        engine.advance()
        engine.check(correctAnswer(engine))
        val finished = engine.advance()

        assertEquals(SessionPhase.Finished, finished.phase)
        assertEquals(1, finished.masteredCount)
        assertTrue(finished.unmastered.isEmpty())
        assertEquals(1f, finished.progress, 0.001f)
    }

    @Test
    fun `progress never decreases and never exceeds one`() {
        val engine = engineOf(6)
        var previous = 0f
        var guard = 0

        while (engine.state().phase != SessionPhase.Finished && guard++ < 100) {
            // Alternate right and wrong so the queue grows and shrinks.
            val answer = if (guard % 2 == 0) correctAnswer(engine) else wrongAnswer
            engine.check(answer)
            val state = engine.advance()
            assertTrue("progress went backwards", state.progress >= previous)
            assertTrue("progress exceeded 1.0", state.progress <= 1f)
            previous = state.progress
        }

        assertEquals(SessionPhase.Finished, engine.state().phase)
    }

    @Test
    fun `accuracy reflects first attempts not eventual mastery`() {
        val engine = engineOf(2)

        // Miss the first step, then get it right on the retry; get the second right away.
        engine.check(wrongAnswer)
        engine.advance()
        engine.check(correctAnswer(engine))
        engine.advance()
        engine.check(correctAnswer(engine))
        val finished = engine.advance()

        assertEquals(SessionPhase.Finished, finished.phase)
        assertEquals(2, finished.masteredCount)
        // One of two first attempts was right, so 50% even though everything was mastered.
        assertEquals(1, finished.firstTryCorrect)
        assertEquals(1, finished.firstTryWrong)
        assertEquals(50, finished.accuracyPercentage)
    }

    @Test
    fun `checking twice without advancing does not double count`() {
        val engine = engineOf(2)

        engine.check(wrongAnswer)
        val second = engine.check(correctAnswer(engine))

        // The second check is ignored while graded, so inputs stay frozen behind the banner.
        assertFalse(second.result!!.correct)
        assertEquals(1, second.firstTryWrong)
        assertEquals(0, second.firstTryCorrect)
    }

    @Test
    fun `advancing before checking does nothing`() {
        val engine = engineOf(2)
        val before = engine.state().step!!.id

        val after = engine.advance()

        assertEquals(before, after.step!!.id)
        assertEquals(SessionPhase.Answering, after.phase)
    }

    @Test
    fun `an empty plan is finished immediately`() {
        val engine = SessionEngine(
            SessionPlan("s", "t", Block.A, part = 0, partCount = 1, steps = emptyList()),
        )

        assertEquals(SessionPhase.Finished, engine.state().phase)
        assertEquals(0f, engine.state().progress, 0.001f)
        assertEquals(0, engine.state().accuracyPercentage)
    }
}
