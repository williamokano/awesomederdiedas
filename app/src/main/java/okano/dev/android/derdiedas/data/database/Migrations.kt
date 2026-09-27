package okano.dev.android.derdiedas.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds the exercise progress table.
 *
 * Written by hand rather than generated: the exported schema for version 1 is not in the
 * repository, so there is nothing for Room to diff against and no MigrationTestHelper
 * test to be had. It is a plain CREATE TABLE with no change to what already exists, which
 * is the one shape safe to write this way.
 *
 * Destructive migration was the alternative and was rejected: the flashcard game's own
 * history lives in this database, and dropping a learner's records to add a feature they
 * did not ask for is not a trade to make.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `exercise_progress` (
                `setId` TEXT NOT NULL,
                `block` TEXT NOT NULL,
                `part` INTEGER NOT NULL,
                `partCount` INTEGER NOT NULL,
                `totalSteps` INTEGER NOT NULL,
                `masteredSteps` INTEGER NOT NULL,
                `firstTryAccuracy` INTEGER NOT NULL,
                `completedAt` INTEGER NOT NULL,
                PRIMARY KEY(`setId`, `block`, `part`)
            )
            """.trimIndent(),
        )
    }
}
