package okano.dev.android.derdiedas.data.progress

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okano.dev.android.derdiedas.core.progress.SessionRecord
import okano.dev.android.derdiedas.core.progress.preferred
import okano.dev.android.derdiedas.data.database.ExerciseProgressDao
import okano.dev.android.derdiedas.data.database.ExerciseProgressEntity
import okano.dev.android.derdiedas.data.exercise.model.Block

/**
 * Stores what has been finished, and nothing else.
 *
 * Deliberately thin: every decision worth arguing about -- which of two runs to keep, what
 * counts as a finished block -- lives in core/progress as pure functions, and this only
 * moves rows and maps types.
 */
interface ProgressRepository {
    fun observeAll(): Flow<List<SessionRecord>>
    suspend fun record(record: SessionRecord)
}

class RoomProgressRepository(private val dao: ExerciseProgressDao) : ProgressRepository {

    override fun observeAll(): Flow<List<SessionRecord>> =
        dao.observeAll().map { rows -> rows.mapNotNull { it.toRecord() } }

    override suspend fun record(record: SessionRecord) {
        val existing = dao.find(record.setId, record.block.name, record.part)?.toRecord()
        dao.upsert(preferred(existing, record).toEntity())
    }
}

private fun SessionRecord.toEntity() = ExerciseProgressEntity(
    setId = setId,
    block = block.name,
    part = part,
    partCount = partCount,
    totalSteps = totalSteps,
    masteredSteps = masteredSteps,
    firstTryAccuracy = firstTryAccuracy,
    completedAt = completedAt,
)

/**
 * Null when the stored block name is not one this build knows.
 *
 * Dropping the row is the right failure: it means the app was downgraded, or the content
 * gained a block since, and a progress row for a block that cannot be shown is better
 * ignored than crashed on.
 */
private fun ExerciseProgressEntity.toRecord(): SessionRecord? {
    val parsed = Block.entries.firstOrNull { it.name == block } ?: return null
    return SessionRecord(
        setId = setId,
        block = parsed,
        part = part,
        partCount = partCount,
        totalSteps = totalSteps,
        masteredSteps = masteredSteps,
        firstTryAccuracy = firstTryAccuracy,
        completedAt = completedAt,
    )
}
