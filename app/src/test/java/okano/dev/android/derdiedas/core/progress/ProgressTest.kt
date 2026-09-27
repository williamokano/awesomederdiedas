package okano.dev.android.derdiedas.core.progress

import okano.dev.android.derdiedas.data.exercise.model.Block
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressTest {

    @Test
    fun `a block with every part finished reads as complete`() {
        val progress = blockProgress(listOf(record(part = 0), record(part = 1))).getValue(key)

        assertEquals(2, progress.partsDone)
        assertTrue(progress.complete)
    }

    @Test
    fun `a block with parts left is not complete`() {
        val progress = blockProgress(listOf(record(part = 0))).getValue(key)

        assertEquals(1, progress.partsDone)
        assertEquals(2, progress.partCount)
        assertFalse(progress.complete)
    }

    @Test
    fun `replaying the same part does not push a block past its own length`() {
        // Coverage is the point, not repetition: practising part 1 three times must not
        // make a two-part block read as done.
        val records = listOf(
            record(part = 0, at = 1),
            record(part = 0, at = 2),
            record(part = 0, at = 3),
        )

        val progress = blockProgress(records).getValue(key)

        assertEquals(1, progress.partsDone)
        assertFalse(progress.complete)
    }

    @Test
    fun `the part count follows the most recent record`() {
        // A content sync can change how a block splits. Progress should follow the new
        // shape rather than stay pinned to the length it had when first played.
        val records = listOf(
            record(part = 0, partCount = 2, at = 1),
            record(part = 0, partCount = 3, at = 2),
        )

        assertEquals(3, blockProgress(records).getValue(key).partCount)
    }

    @Test
    fun `the best accuracy across parts is kept, not the latest`() {
        val records = listOf(
            record(part = 0, accuracy = 90, at = 1),
            record(part = 1, accuracy = 40, at = 2),
        )

        assertEquals(90, blockProgress(records).getValue(key).bestAccuracy)
    }

    @Test
    fun `blocks and sets are kept apart`() {
        val records = listOf(
            record(part = 0),
            record(part = 0, block = Block.B),
            record(part = 0, setId = "other"),
        )

        val progress = blockProgress(records)

        assertEquals(3, progress.size)
        assertEquals(setOf("a1-01", "other"), setsTouched(records))
    }

    @Test
    fun `no records means no progress rather than zeroes for everything`() {
        assertTrue(blockProgress(emptyList()).isEmpty())
        assertTrue(setsTouched(emptyList()).isEmpty())
    }

    @Test
    fun `a block that reports no parts is never complete`() {
        // Defensive: a zero would otherwise satisfy "done >= total" and mark it finished.
        val progress = blockProgress(listOf(record(part = 0, partCount = 0))).getValue(key)

        assertFalse(progress.complete)
    }

    @Test
    fun `the next part is the first one not finished`() {
        val progress = blockProgress(listOf(record(part = 0, partCount = 3))).getValue(key)

        assertEquals(1, progress.nextPart)
    }

    @Test
    fun `the next part skips over a gap rather than counting finished parts`() {
        // Parts finished out of order: two are done, but the one still missing is part 1,
        // and offering part 2 would leave a hole the learner cannot get back to.
        val records = listOf(record(part = 0, partCount = 3), record(part = 2, partCount = 3))

        assertEquals(1, blockProgress(records).getValue(key).nextPart)
    }

    @Test
    fun `a finished block replays from the top`() {
        val records = listOf(record(part = 0), record(part = 1))

        assertEquals(0, blockProgress(records).getValue(key).nextPart)
    }

    @Test
    fun `a replay that went worse does not erase the better run`() {
        val best = record(part = 0, accuracy = 90, at = 1)
        val worse = record(part = 0, accuracy = 40, at = 2)

        assertEquals(best, preferred(best, worse))
    }

    @Test
    fun `a replay that went better replaces the old run`() {
        val old = record(part = 0, accuracy = 40, at = 1)
        val better = record(part = 0, accuracy = 90, at = 2)

        assertEquals(better, preferred(old, better))
    }

    @Test
    fun `an equal replay is kept so the completion time moves forward`() {
        val old = record(part = 0, accuracy = 90, at = 1)
        val again = record(part = 0, accuracy = 90, at = 2)

        assertEquals(2L, preferred(old, again).completedAt)
    }

    @Test
    fun `the first run of a session is always kept`() {
        val first = record(part = 0, accuracy = 0)

        assertEquals(first, preferred(null, first))
    }

    private val key = BlockKey("a1-01", Block.A)

    private fun record(
        part: Int,
        setId: String = "a1-01",
        block: Block = Block.A,
        partCount: Int = 2,
        accuracy: Int = 100,
        at: Long = 0,
    ) = SessionRecord(
        setId = setId,
        block = block,
        part = part,
        partCount = partCount,
        totalSteps = 12,
        masteredSteps = 12,
        firstTryAccuracy = accuracy,
        completedAt = at,
    )
}
