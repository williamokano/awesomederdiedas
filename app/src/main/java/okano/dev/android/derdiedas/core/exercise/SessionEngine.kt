package okano.dev.android.derdiedas.core.exercise

import okano.dev.android.derdiedas.data.exercise.model.Block
import okano.dev.android.derdiedas.data.exercise.model.ExerciseSet

/**
 * The session state machine: answer, check, see the result, continue, with wrong steps
 * coming back before the session ends.
 *
 * Deliberately not a ViewModel. Keeping it a plain class means the whole loop — including
 * re-queueing, progress and scoring — is testable on the JVM, and the ViewModel is a thin
 * adapter over it.
 */

/**
 * Steps per session. A block's steps run to a median of 14 but a p90 of 46 and a max of
 * 99, which is far past a sitting, so long blocks are split into evenly sized parts.
 * Fifteen puts the median session at 12 steps.
 */
const val MAX_SESSION_STEPS = 15

/** A step is offered once more after a wrong answer; a second miss lets the queue drain. */
const val MAX_ATTEMPTS = 2

enum class SessionPhase { Answering, Graded, Finished }

data class SessionState(
    val phase: SessionPhase,
    val step: SessionStep?,
    val result: StepResult?,
    /** Distinct steps in the session, fixed at the start so the progress bar cannot jump. */
    val totalSteps: Int,
    /** Distinct steps answered correctly at least once. */
    val masteredCount: Int,
    val firstTryCorrect: Int,
    val firstTryWrong: Int,
    /** Steps that ran out of attempts, surfaced in the summary as still to review. */
    val unmastered: List<SessionStep>,
) {
    val progress: Float get() = if (totalSteps == 0) 0f else masteredCount.toFloat() / totalSteps

    /** Accuracy over first attempts only; counting retries would make it always 100%. */
    val accuracyPercentage: Int
        get() {
            val attempted = firstTryCorrect + firstTryWrong
            return if (attempted == 0) 0 else (firstTryCorrect * 100) / attempted
        }
}

/**
 * One playable session: the steps of one block, or one part of a long block.
 *
 * [part] is 0-based and [partCount] is how many parts the block was split into, so the UI
 * can say "Teil 2 von 3" without recomputing the split.
 */
data class SessionPlan(
    val setId: String,
    val setTitle: String,
    val block: Block,
    val part: Int,
    val partCount: Int,
    val steps: List<SessionStep>,
)

/**
 * Splits a block's steps into evenly sized parts of at most [MAX_SESSION_STEPS].
 *
 * Even splitting rather than fixed-size chunking: 16 steps become 8+8, not 15+1, so no
 * session is a single leftover question.
 */
fun ExerciseSet.sessionsFor(block: Block): List<SessionPlan> {
    val steps = exercises.filter { it.block == block }.flatMap { it.toSteps(id) }
    if (steps.isEmpty()) return emptyList()

    val partCount = (steps.size + MAX_SESSION_STEPS - 1) / MAX_SESSION_STEPS
    val base = steps.size / partCount
    val remainder = steps.size % partCount

    var cursor = 0
    return (0 until partCount).map { part ->
        val size = base + if (part < remainder) 1 else 0
        val slice = steps.subList(cursor, cursor + size)
        cursor += size
        SessionPlan(
            setId = id,
            setTitle = title,
            block = block,
            part = part,
            partCount = partCount,
            steps = slice,
        )
    }
}

/** All playable sessions in a set, in block order. */
fun ExerciseSet.allSessions(): List<SessionPlan> =
    exercises.map { it.block }.distinct().sorted().flatMap { sessionsFor(it) }

class SessionEngine(private val plan: SessionPlan) {

    private val queue = ArrayDeque(plan.steps)
    private val attempts = mutableMapOf<StepId, Int>()
    private val mastered = mutableSetOf<StepId>()
    private val unmastered = mutableListOf<SessionStep>()

    private var phase = if (plan.steps.isEmpty()) SessionPhase.Finished else SessionPhase.Answering
    private var result: StepResult? = null
    private var firstTryCorrect = 0
    private var firstTryWrong = 0

    val totalSteps: Int = plan.steps.size

    fun state(): SessionState = SessionState(
        phase = phase,
        step = queue.firstOrNull(),
        result = result,
        totalSteps = totalSteps,
        masteredCount = mastered.size,
        firstTryCorrect = firstTryCorrect,
        firstTryWrong = firstTryWrong,
        unmastered = unmastered.toList(),
    )

    /** Grades the current step. A no-op unless a step is waiting to be answered. */
    fun check(answer: AnswerState): SessionState {
        val step = queue.firstOrNull()
        if (phase != SessionPhase.Answering || step == null) return state()

        val graded = gradeStep(step, answer)
        val attempt = (attempts[step.id] ?: 0) + 1
        attempts[step.id] = attempt

        if (attempt == 1) {
            if (graded.correct) firstTryCorrect++ else firstTryWrong++
        }

        result = graded
        phase = SessionPhase.Graded
        return state()
    }

    /** Advances past the graded step, re-queueing it if it was wrong and has attempts left. */
    fun advance(): SessionState {
        if (phase != SessionPhase.Graded) return state()
        val step = queue.removeFirstOrNull() ?: return state()
        val graded = result

        if (graded?.correct == true) {
            mastered += step.id
        } else if ((attempts[step.id] ?: 0) < MAX_ATTEMPTS) {
            // Back to the end of the queue: seeing it again before the session ends is the
            // point, but it must not be the very next card.
            queue.addLast(step)
        } else {
            unmastered += step
        }

        result = null
        phase = if (queue.isEmpty()) SessionPhase.Finished else SessionPhase.Answering
        return state()
    }
}
