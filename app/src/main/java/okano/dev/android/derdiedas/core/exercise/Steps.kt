package okano.dev.android.derdiedas.core.exercise

import okano.dev.android.derdiedas.data.exercise.model.Exercise
import okano.dev.android.derdiedas.data.exercise.model.ExerciseBody
import okano.dev.android.derdiedas.data.exercise.model.GapBankBody
import okano.dev.android.derdiedas.data.exercise.model.GapTextBody
import okano.dev.android.derdiedas.data.exercise.model.OddOneOutBody
import okano.dev.android.derdiedas.data.exercise.model.OrderBody
import okano.dev.android.derdiedas.data.exercise.model.SingleChoiceBody
import okano.dev.android.derdiedas.data.exercise.model.TrueFalseBody

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
 * Fill a passage's gaps from a shared bank of words.
 *
 * Unlike gap-text this is never split: the bank is shared across every gap, so a single
 * sentence taken out of the passage would carry a bank that mostly does not belong to it.
 * Measured on the corpus, a passage runs to about ten short lines with roughly one gap
 * each, which is what makes a whole passage workable on one screen.
 */
data class GapBankStep(
    override val id: StepId,
    override val exerciseId: String,
    override val title: String,
    override val instructions: String?,
    override val instructionsEn: String?,
    override val flags: GradingFlags,
    /** The passage split into lines, each already parsed into literals and gaps. */
    val lines: List<List<GapSegment>>,
    /** Every gap in the passage, in reading order. */
    val gapKeys: List<String>,
    /** The bank as shown: shuffled, since as authored it usually spells out the answers. */
    val bank: List<String>,
    val answers: Map<String, List<String>>,
    /**
     * How many times each bank word may be placed.
     *
     * Normally the number of copies the bank holds, so placing a word uses it up and the
     * distractors left over are the ones that do not belong. Three exercises in the corpus
     * ask for a word more often than the bank lists it -- they are classification drills
     * where a handful of labels are meant to be reused -- so capacity is raised to what
     * the answers actually demand rather than leaving those unsolvable.
     */
    val capacity: Map<String, Int>,
) : SessionStep {
    override fun emptyAnswer() = AnswerState.Placements()
}

/** One option to pick. [key] is what grading compares; [text] is what the learner reads. */
data class ChoiceOption(val key: String, val text: String)

/**
 * Pick one of a few options.
 *
 * single-choice, true-false and odd-one-out are the same interaction wearing different
 * clothes, so they share a step and a widget rather than three near-identical ones. Only
 * the shape of the options differs: named options, two labels, or the members of a group.
 */
data class ChoiceStep(
    override val id: StepId,
    override val exerciseId: String,
    override val title: String,
    override val instructions: String?,
    override val instructionsEn: String?,
    override val flags: GradingFlags,
    /** The question. Null for odd-one-out, where the options themselves are the question. */
    val prompt: String?,
    val options: List<ChoiceOption>,
    val answerKey: String,
    /** Why that answer is right. Every item in the corpus has one, so it always shows. */
    val why: String?,
) : SessionStep {
    override fun emptyAnswer() = AnswerState.Choice()
}

/**
 * Build a sentence by placing tiles in order.
 *
 * [tiles] is already in display order and [answer] indexes into it, so the widget and the
 * grader never have to think about the authored order.
 */
data class OrderStep(
    override val id: StepId,
    override val exerciseId: String,
    override val title: String,
    override val instructions: String?,
    override val instructionsEn: String?,
    override val flags: GradingFlags,
    val tiles: List<String>,
    val answer: List<Int>,
    val alternatives: List<List<Int>>,
    /** The finished sentence, for the result banner. */
    val solution: String,
    /** A grammar note, when the author wrote one that is not just the sentence again. */
    val note: String?,
) : SessionStep {
    override fun emptyAnswer() = AnswerState.Sequence()
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

fun AnswerState.asChoice(): AnswerState.Choice = this as? AnswerState.Choice
    ?: error("expected AnswerState.Choice for this step but was ${this::class.simpleName}")

fun AnswerState.asSequence(): AnswerState.Sequence = this as? AnswerState.Sequence
    ?: error("expected AnswerState.Sequence for this step but was ${this::class.simpleName}")

fun AnswerState.asPlacements(): AnswerState.Placements = this as? AnswerState.Placements
    ?: error("expected AnswerState.Placements for this step but was ${this::class.simpleName}")

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
    is SingleChoiceBody -> singleChoiceSteps(setId, body)
    is TrueFalseBody -> trueFalseSteps(setId, body)
    is OddOneOutBody -> oddOneOutSteps(setId, body)
    is OrderBody -> orderSteps(setId, body)
    is GapBankBody -> gapBankSteps(setId, body)

    // Still to come: matching, categorize, table-fill.
    else -> emptyList()
}

private fun Exercise.choiceStep(
    setId: String,
    index: Int,
    prompt: String?,
    options: List<ChoiceOption>,
    answerKey: String,
    why: String?,
): ChoiceStep? {
    // An answer that names no option would be unanswerable, so drop the step rather than
    // show the learner something they cannot get right.
    if (options.none { it.key == answerKey }) return null
    return ChoiceStep(
        id = StepId("$setId#$id#$index"),
        exerciseId = id,
        title = title,
        instructions = instructions,
        instructionsEn = instructionsEn,
        flags = flags,
        prompt = prompt,
        options = options,
        answerKey = answerKey,
        why = why,
    )
}

/** Each question is independent, so each becomes its own screen. */
private fun Exercise.singleChoiceSteps(setId: String, body: SingleChoiceBody): List<SessionStep> =
    body.items.mapIndexedNotNull { index, item ->
        choiceStep(
            setId = setId,
            index = index,
            prompt = item.q,
            options = item.options.map { ChoiceOption(it.key, it.text) },
            answerKey = item.answer,
            why = item.why,
        )
    }

/** True/false is a two-option choice; the labels are the exercise's own wording. */
private fun Exercise.trueFalseSteps(setId: String, body: TrueFalseBody): List<SessionStep> =
    body.items.mapIndexedNotNull { index, item ->
        choiceStep(
            setId = setId,
            index = index,
            prompt = item.q,
            options = listOf(
                ChoiceOption(TRUE_KEY, body.positiveLabel),
                ChoiceOption(FALSE_KEY, body.negativeLabel),
            ),
            answerKey = if (item.answer) TRUE_KEY else FALSE_KEY,
            why = item.why,
        )
    }

/** The group's members are the options, and the odd one out is the answer. */
private fun Exercise.oddOneOutSteps(setId: String, body: OddOneOutBody): List<SessionStep> =
    body.groups.mapIndexedNotNull { index, group ->
        choiceStep(
            setId = setId,
            index = index,
            // The instruction already asks which one does not belong.
            prompt = null,
            options = group.items.mapIndexed { position, text -> ChoiceOption(position.toString(), text) },
            answerKey = group.odd.toString(),
            why = group.why,
        )
    }

private const val TRUE_KEY = "true"
private const val FALSE_KEY = "false"

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

/**
 * Sentence-building steps, with the tiles shuffled for display.
 *
 * Shuffling is not cosmetic. In 957 of the corpus's 1,124 order items the tiles are stored
 * already in the right order, so presenting them as authored would let the learner solve
 * the exercise by tapping left to right. The shuffle is seeded on the step's identity so
 * the same step always looks the same -- a tile pool that rearranged itself when the step
 * came back after a wrong answer would be its own kind of unfair.
 */
private fun Exercise.orderSteps(setId: String, body: OrderBody): List<SessionStep> =
    body.items.mapIndexedNotNull { index, item ->
        if (item.tiles.size < 2) return@mapIndexedNotNull null
        // An answer that does not name every tile exactly once cannot be built.
        if (item.answer.sorted() != item.tiles.indices.toList()) return@mapIndexedNotNull null

        val display = shuffleForDisplay(item.tiles.size, seed = "$setId#$id#$index", answer = item.answer)
        val positionOf = IntArray(display.size).also { display.forEachIndexed { at, original -> it[original] = at } }

        // Joining the tiles reproduces the sentence, but not the way anyone writes it:
        // tiles are lowercase and punctuation is its own tile, so the join reads
        // "hast du Geschwister ?". The author's note is almost always that same sentence
        // written properly, so when it matches, show it instead and drop the note -- it
        // would otherwise sit under the banner repeating what the banner just said.
        val tileJoin = item.answer.joinToString(" ") { item.tiles[it] }
        val restatement = item.note?.takeIf { it.saysTheSameAs(tileJoin) }
        OrderStep(
            id = StepId("$setId#$id#$index"),
            exerciseId = id,
            title = title,
            instructions = instructions,
            instructionsEn = instructionsEn,
            flags = flags,
            tiles = display.map { item.tiles[it] },
            answer = item.answer.map { positionOf[it] },
            alternatives = item.alt
                .filter { it.sorted() == item.tiles.indices.toList() }
                .map { alternative -> alternative.map { positionOf[it] } },
            solution = restatement ?: tileJoin,
            note = if (restatement != null) null else item.note,
        )
    }

/**
 * Whether two renderings of the same sentence differ only in the ways the tile join
 * inevitably differs: capitalisation, spacing, and where the punctuation sits.
 */
private fun String.saysTheSameAs(other: String): Boolean = lettersAndDigits() == other.lettersAndDigits()

private fun String.lettersAndDigits(): String = filter { it.isLetterOrDigit() }.lowercase()

/**
 * A deterministic permutation of `0 until size`, derived from [seed].
 *
 * Retries with a varied seed while the shuffle would leave [answer] already in order,
 * which matters most for the short items: with three tiles a plain shuffle hands the
 * learner the answer one time in six.
 */
internal fun shuffleForDisplay(size: Int, seed: String, answer: List<Int>): List<Int> {
    repeat(SHUFFLE_ATTEMPTS) { attempt ->
        val display = seededShuffle(size, "$seed#$attempt")
        val positionOf = IntArray(size).also { display.forEachIndexed { at, original -> it[original] = at } }
        val remapped = answer.map { positionOf[it] }
        if (remapped != remapped.indices.toList()) return display
    }
    // Every attempt landed on the identity, which needs a size of one to happen.
    return (0 until size).toList()
}

private const val SHUFFLE_ATTEMPTS = 8

/** Fisher-Yates over a cheap string hash, so the order is stable for a given seed. */
private fun seededShuffle(size: Int, seed: String): List<Int> {
    var state = seed.fold(HASH_SEED) { acc, ch -> acc * HASH_MULTIPLIER + ch.code }
    fun next(bound: Int): Int {
        state = state * HASH_MULTIPLIER + HASH_INCREMENT
        return ((state ushr 16) % bound).toInt().let { if (it < 0) it + bound else it }
    }

    val order = (0 until size).toMutableList()
    for (i in size - 1 downTo 1) {
        val j = next(i + 1)
        order[i] = order[j].also { order[j] = order[i] }
    }
    return order
}

private const val HASH_SEED = 1469598103934665603L
private const val HASH_MULTIPLIER = 31L
private const val HASH_INCREMENT = 1013904223L

/**
 * One step per exercise: the whole passage, with its bank.
 *
 * The bank is shuffled for the same reason the order tiles are. As authored, 214 of the
 * corpus's 327 banks begin with the answers in gap order, so taking the words left to
 * right would solve two thirds of them without reading the passage at all.
 */
private fun Exercise.gapBankSteps(setId: String, body: GapBankBody): List<SessionStep> {
    val lines = body.text.split('\n')
        .filter { it.isNotBlank() }
        .map { parseSegments(it) }
    val gapKeys = lines.flatMap { gapKeysOf(it) }

    // A gap with no answer could not be got right, and an answer with no gap could not be
    // placed; either way the exercise is broken rather than hard.
    if (gapKeys.isEmpty()) return emptyList()
    if (gapKeys.any { body.answers[it].isNullOrEmpty() }) return emptyList()
    if (body.answers.keys.any { it !in gapKeys }) return emptyList()

    val demand = gapKeys.mapNotNull { body.answers[it]?.firstOrNull() }
        .groupingBy { it }
        .eachCount()
    val supply = body.bank.groupingBy { it }.eachCount()
    // Every word the answers ask for has to be in the bank, or the passage is unsolvable.
    if (demand.keys.any { it !in supply }) return emptyList()

    return listOf(
        GapBankStep(
            id = StepId("$setId#$id#0"),
            exerciseId = id,
            title = title,
            instructions = instructions,
            instructionsEn = instructionsEn,
            flags = flags,
            lines = lines,
            gapKeys = gapKeys,
            bank = seededShuffle(body.bank.size, "$setId#$id#bank").map { body.bank[it] },
            answers = body.answers,
            capacity = supply.mapValues { (word, copies) -> maxOf(copies, demand[word] ?: 0) },
        ),
    )
}
