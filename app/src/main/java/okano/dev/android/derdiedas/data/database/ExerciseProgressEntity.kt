package okano.dev.android.derdiedas.data.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * One finished session: a part of a block of a set.
 *
 * Keyed on what identifies a session rather than on a generated id, so replaying a part
 * updates the row it already has instead of piling up history. The app only asks what has
 * been covered; a full attempt log would be a different feature with a different shape.
 */
@Entity(tableName = "exercise_progress", primaryKeys = ["setId", "block", "part"])
data class ExerciseProgressEntity(
    val setId: String,
    /** [okano.dev.android.derdiedas.data.exercise.model.Block] by name, so the column survives the enum gaining values. */
    val block: String,
    val part: Int,
    /** How many parts the block split into when this was played. See SessionRecord. */
    val partCount: Int,
    val totalSteps: Int,
    val masteredSteps: Int,
    val firstTryAccuracy: Int,
    val completedAt: Long,
)

@Dao
interface ExerciseProgressDao {

    @Query("SELECT * FROM exercise_progress")
    fun observeAll(): Flow<List<ExerciseProgressEntity>>

    /**
     * Replaces the row for this session if it exists.
     *
     * Keeping the better run rather than the latest is decided above this, in the
     * repository, where it can be reasoned about and tested.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: ExerciseProgressEntity)

    @Query("SELECT * FROM exercise_progress WHERE setId = :setId AND block = :block AND part = :part")
    suspend fun find(setId: String, block: String, part: Int): ExerciseProgressEntity?
}
