package okano.dev.android.derdiedas.core.exercise

import okano.dev.android.derdiedas.data.exercise.model.Exercise
import okano.dev.android.derdiedas.data.exercise.model.ExerciseBody
import okano.dev.android.derdiedas.data.exercise.model.GapTextBody

/**
 * Turns authored exercises into the units the session runner presents, one per screen.
 *
 * An exercise is not a screen: the 3,243 bundled exercises hold 16,977 individual items.
 * Nor is an item always a screen: a word bank, a pairing board or a prose passage is one
 * task whose parts make no sense in isolation. So each type decides for itself.
 *
 * Pure Kotlin, no Android imports.
 */

/** Identifies a step within a session. Steps of the same exercise share [exerciseId]. */
@JvmInline
value class StepId(val value: String)

sealed interface SessionStep {
    val id: StepId
    val exerciseId: String
    val title: String
    val instructions: String?
    val instructionsEn: String?
    val flags: GradingFlags

    /** The blank answer this step starts from, and the one it resets to when re-queued. */
    fun emptyAnswer(): AnswerState
}

/** A run of literal text, or a gap the learner fills. */
sealed interface GapSegment {
    data class Literal(val text: String) : GapSegment
    data class Gap(val key: String) : GapSegment
}

data class GapTextStep(
    override val id: StepId,
    override val exerciseId: String,
    override val title: String,
    override val instructions: String?,
    override val instructionsEn: String?,
    override val flags: GradingFlags,
    val segments: List<GapSegment>,
    /** The gaps this step asks for, in reading order. A subset of the exercise's gaps. */
    val gapKeys: List<String>,
    val answers: Map<String, List<String>>,
    val alts: Map<String, List<AltAnswer>>,
    val cues: Map<String, String>,
    /** True when this step is one numbered line; false when it is a whole passage. */
    val listLayout: Boolean,
    /**
     * A closed set of choices to tap instead of typing, or empty for a free-text gap.
     *
     * Derived from the whole exercise, never a single gap, so it does not give the answer
     * away: an article drill whose every answer is der/die/das offers those three on each
     * of its sentences, which is how the drill is meant to be done.
     */
    val options: List<String> = emptyList(),
) : SessionStep {
    override fun emptyAnswer() = AnswerState.Texts()
}

/**
 * Every exercise type's answer reduces to one of four shapes, which is what lets the
 * session runner stay type-agnostic. Only [Texts] is used until PR3; the rest are
 * declared now because this interface is the shape the whole design rests on.
 */
sealed interface AnswerState {
    /** gap-text, table-fill — keyed by gap. */
    data class Texts(val byRef: Map<String, String> = emptyMap()) : AnswerState

    /** single-choice, true-false, odd-one-out — one selection. */
    data class Choice(val key: String? = null) : AnswerState

    /** gap-bank, matching, categorize — tokens placed into slots. */
    data class Placements(val byRef: Map<String, String?> = emptyMap()) : AnswerState

    /** order — a permutation of tile indices. */
    data class Sequence(val order: List<Int> = emptyList()) : AnswerState
}

fun AnswerState.asTexts(): AnswerState.Texts = this as? AnswerState.Texts
    ?: error("expected AnswerState.Texts for this step but was ${this::class.simpleName}")

// Both braces are escaped on purpose. Java's regex engine tolerates a bare closing "}",
// but Android's ICU-backed engine rejects the pattern outright, and the failure is a
// crash in the static initialiser rather than anything a JVM unit test can reach.
private val PLACEHOLDER = Regex("""\{(\d+)\}""")
private val NUMBERED_LINE = Regex("""^\s*\d+[.)]\s?""")

/** Splits `"Ich {1} gern"` into literal and gap segments, preserving order. */
internal fun parseSegments(text: String): List<GapSegment> {
    val segments = mutableListOf<GapSegment>()
    var cursor = 0
    for (match in PLACEHOLDER.findAll(text)) {
        if (match.range.first > cursor) {
            segments += GapSegment.Literal(text.substring(cursor, match.range.first))
        }
        segments += GapSegment.Gap(match.groupValues[1])
        cursor = match.range.last + 1
    }
    if (cursor < text.length) segments += GapSegment.Literal(text.substring(cursor))
    return segments
}

private fun gapKeysOf(segments: List<GapSegment>): List<String> =
    segments.filterIsInstance<GapSegment.Gap>().map { it.key }

/**
 * Expands an exercise into its steps.
 *
 * Every branch is declared even though eight of them are not implemented yet, so that the
 * full asset tree could ship in one go rather than being re-synced once per phase. Adding
 * a type is then a change here and in the grader, both of which the compiler checks.
 */
fun Exercise.toSteps(setId: String): List<SessionStep> = when (val body = body) {
    is GapTextBody -> gapTextSteps(setId, body)

    // PR2: table-fill. PR3: single-choice, true-false, odd-one-out.
    // PR4: gap-bank. PR5: matching, categorize. PR6: order.
    else -> emptyList()
}

/**
 * The exercise's answers as a small closed set, when it has one.
 *
 * Typing is the wrong input for a drill whose entire answer space is three articles. The
 * bar is deliberately narrow: a handful of short single words, or it is free text.
 */
private fun GapTextBody.closedOptions(flags: GradingFlags): List<String> {
    // Case-insensitive grading is what lets one "der" chip answer both "der" and
    // sentence-initial "Der". Without it the chips could be unanswerable.
    if (flags.caseSensitive) return emptyList()

    val byNormalised = LinkedHashMap<String, String>()
    for (alternatives in answers.values) {
        for (answer in alternatives) {
            if (answer.length > MAX_OPTION_LENGTH || answer.any { it.isWhitespace() }) return emptyList()
            byNormalised.putIfAbsent(answer.lowercase(), answer.lowercase())
        }
    }
    // One option gives the answer away; too many is a list, not a choice.
    return if (byNormalised.size in MIN_OPTIONS..MAX_OPTIONS) byNormalised.values.sorted() else emptyList()
}

private const val MIN_OPTIONS = 2
private const val MAX_OPTIONS = 4
private const val MAX_OPTION_LENGTH = 14

private fun Exercise.gapTextSteps(setId: String, body: GapTextBody): List<SessionStep> {
    val options = body.closedOptions(flags)

    fun step(suffix: String, segments: List<GapSegment>, listLayout: Boolean): GapTextStep? {
        val keys = gapKeysOf(segments)
        if (keys.isEmpty()) return null
        return GapTextStep(
            id = StepId("$setId#$id#$suffix"),
            exerciseId = id,
            title = title,
            instructions = instructions,
            instructionsEn = instructionsEn,
            flags = flags,
            segments = segments,
            gapKeys = keys,
            answers = body.answers.filterKeys { it in keys },
            alts = body.alts.filterKeys { it in keys },
            cues = body.cues.filterKeys { it in keys },
            listLayout = listLayout,
            // Only offer choices for a single gap: several gaps sharing one set of chips
            // needs a per-gap selection UI that does not exist yet.
            options = if (keys.size == 1) options else emptyList(),
        )
    }

    // A passage's gaps only make sense alongside the sentences around them, so it stays
    // one screen. Numbered lines are independent drills, so each becomes its own.
    if (!body.listLayout) {
        return listOfNotNull(step(suffix = "0", segments = parseSegments(body.text), listLayout = false))
    }

    return body.text.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        // Strip the authored "1." numbering: the session shows its own position instead.
        .map { it.replace(NUMBERED_LINE, "") }
        .mapIndexedNotNull { index, line -> step(suffix = index.toString(), segments = parseSegments(line), listLayout = true) }
        .toList()
}
