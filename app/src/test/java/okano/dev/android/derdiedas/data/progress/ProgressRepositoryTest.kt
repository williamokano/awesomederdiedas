package okano.dev.android.derdiedas.data.progress

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okano.dev.android.derdiedas.core.progress.SessionRecord
import okano.dev.android.derdiedas.data.database.ExerciseProgressEntity
import okano.dev.android.derdiedas.data.exercise.model.Block
import okano.dev.android.derdiedas.testutil.FakeExerciseProgressDao
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProgressRepositoryTest {

    private lateinit var dao: FakeExerciseProgressDao
    private lateinit var repository: ProgressRepository

    @Before
    fun setUp() {
        dao = FakeExerciseProgressDao()
        repository = RoomProgressRepository(dao)
    }

    private fun record(
        part: Int = 0,
        accuracy: Int = 80,
        completedAt: Long = 1_000L,
        block: Block = Block.A,
    ) = SessionRecord(
        setId = "lektion-1",
        block = block,
        part = part,
        partCount = 2,
        totalSteps = 10,
        masteredSteps = 10,
        firstTryAccuracy = accuracy,
        completedAt = completedAt,
    )

    @Test
    fun `records a finished session`() = runTest {
        repository.record(record())

        assertEquals(listOf(record()), repository.observeAll().first())
    }

    @Test
    fun `replaying a part keeps one row, not two`() = runTest {
        repository.record(record(completedAt = 1_000L))
        repository.record(record(completedAt = 2_000L))

        assertEquals(1, dao.rows.size)
    }

    @Test
    fun `a worse replay does not erase a better run`() = runTest {
        repository.record(record(accuracy = 90, completedAt = 1_000L))
        repository.record(record(accuracy = 40, completedAt = 2_000L))

        val stored = repository.observeAll().first().single()
        assertEquals(90, stored.firstTryAccuracy)
        // The better run is kept whole, timestamp included, rather than patched field by field.
        assertEquals(1_000L, stored.completedAt)
    }

    @Test
    fun `a better replay wins`() = runTest {
        repository.record(record(accuracy = 40, completedAt = 1_000L))
        repository.record(record(accuracy = 90, completedAt = 2_000L))

        val stored = repository.observeAll().first().single()
        assertEquals(90, stored.firstTryAccuracy)
        assertEquals(2_000L, stored.completedAt)
    }

    @Test
    fun `parts and blocks of the same set are separate rows`() = runTest {
        repository.record(record(part = 0))
        repository.record(record(part = 1))
        repository.record(record(part = 0, block = Block.B))

        assertEquals(3, repository.observeAll().first().size)
    }

    /**
     * A block name this build does not know must not take the whole query down with it,
     * which is what an eager valueOf would do after a downgrade or a content change.
     */
    @Test
    fun `a row with an unknown block name is dropped, not thrown on`() = runTest {
        dao.upsert(
            ExerciseProgressEntity(
                setId = "lektion-1",
                block = "Z",
                part = 0,
                partCount = 1,
                totalSteps = 4,
                masteredSteps = 4,
                firstTryAccuracy = 100,
                completedAt = 1_000L,
            ),
        )
        repository.record(record())

        val stored = repository.observeAll().first()
        assertEquals(1, stored.size)
        assertEquals(Block.A, stored.single().block)
        // The unreadable row is still on disk; it is only hidden from the app.
        assertTrue(dao.rows.any { it.block == "Z" })
    }
}
