package okano.dev.android.derdiedas.core.progress

import okano.dev.android.derdiedas.data.exercise.model.Block

/**
 * What the learner has finished, and what that adds up to.
 *
 * Pure Kotlin with no Android and no Room, so the arithmetic that decides whether a block
 * reads as done is testable on the JVM. The database layer maps its rows onto
 * [SessionRecord] and never does any counting of its own.
 */

/** One completed session: a block of a set, split into [partCount] parts, of which this is [part]. */
data class SessionRecord(
    val setId: String,
    val block: Block,
    val part: Int,
    /**
     * How many parts that block was split into when this was played.
     *
     * Stored per record rather than looked up, because the split depends on the content
     * and the index does not carry it. A re-sync that changes a block's length would
     * otherwise silently rewrite history.
     */
    val partCount: Int,
    val totalSteps: Int,
    val masteredSteps: Int,
    /** Percentage of steps answered correctly on the first attempt. */
    val firstTryAccuracy: Int,
    val completedAt: Long,
)

/** How far through one block of one set the learner has got. */
data class BlockProgress(
    /** Which parts are finished, by index. Not a count: see [nextPart]. */
    val doneParts: Set<Int>,
    val partCount: Int,
    /** The best first-try accuracy seen across the parts finished so far. */
    val bestAccuracy: Int,
) {
    val partsDone: Int get() = doneParts.size

    val complete: Boolean get() = partCount > 0 && partsDone >= partCount

    /**
     * Which part to open next.
     *
     * The first one not finished, so a block of three parts is worked through rather than
     * replaying part 1 forever -- which is what a fixed 0 did, leaving every multi-part
     * block permanently incomplete. Zero once the block is done, so the button then
     * replays it from the top.
     *
     * Not simply [partsDone]: parts can be finished out of order once they are reachable
     * individually, and a learner who did part 3 first should still be offered part 1.
     */
    val nextPart: Int
        get() = if (complete) 0 else (0 until partCount).firstOrNull { it !in doneParts } ?: 0
}

/**
 * Folds completed sessions into per-block progress.
 *
 * Playing the same part twice counts once -- the set of part indexes handles that -- since
 * the point is coverage, not repetition, and a part replayed for practice should not push a
 * block past its own length.
 *
 * [partCount] is taken from the most recent record, so if a content sync changes how a
 * block splits, progress follows the new shape rather than being stuck on the old one.
 */
fun blockProgress(records: List<SessionRecord>): Map<BlockKey, BlockProgress> =
    records
        .groupBy { BlockKey(it.setId, it.block) }
        .mapValues { (_, forBlock) ->
            BlockProgress(
                doneParts = forBlock.mapTo(mutableSetOf()) { it.part },
                partCount = forBlock.maxBy { it.completedAt }.partCount,
                bestAccuracy = forBlock.maxOf { it.firstTryAccuracy },
            )
        }

data class BlockKey(val setId: String, val block: Block)

/** The sets the learner has touched at all, for marking them in the catalogue. */
fun setsTouched(records: List<SessionRecord>): Set<String> = records.mapTo(mutableSetOf()) { it.setId }

/**
 * Which of two runs of the same session to keep.
 *
 * The better one. Practising a part again and doing worse should not erase what was
 * already achieved, which is what storing the latest would do. A tie goes to the fresh
 * record so that [SessionRecord.completedAt] moves forward on a genuine replay.
 */
fun preferred(existing: SessionRecord?, fresh: SessionRecord): SessionRecord =
    if (existing != null && existing.firstTryAccuracy > fresh.firstTryAccuracy) existing else fresh
