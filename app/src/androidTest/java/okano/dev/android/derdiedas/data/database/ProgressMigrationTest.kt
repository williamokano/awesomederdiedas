package okano.dev.android.derdiedas.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Opens a version 1 database and checks that version 2 arrives without taking anything with it.
 *
 * This is the one thing the JVM suite cannot reach and a fresh install does not exercise:
 * installing the app on a clean device creates version 2 directly, so [MIGRATION_1_2]
 * never runs there. Only an upgrade over an existing install does -- which is every
 * learner who already has the app -- and a hand-written CREATE TABLE that does not match
 * the entity fails that upgrade with an IllegalStateException at launch.
 *
 * The version 1 database is built here by hand rather than with MigrationTestHelper,
 * because that needs the exported schema for version 1 and it is not in the repository:
 * schema export was only turned on later, and nothing has built version 1 since.
 *
 * The assertion is mostly implicit. Room validates the whole schema against the entities
 * immediately after running a migration, so simply opening the database is the test; the
 * reads below then confirm the new table works and the old rows are still there.
 */
@RunWith(AndroidJUnit4::class)
class ProgressMigrationTest {

    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

    /** Not the app's own database name: this test creates and deletes the file it uses. */
    private val name = "migration_test_derdiedas"

    @Before
    fun setUp() {
        context.deleteDatabase(name)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(name)
    }

    @Test
    fun migratesFromVersionOneAndKeepsTheFlashcardHistory() {
        createVersionOneWithOneSession()

        val database = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(MIGRATION_1_2)
            // A throwaway database read from the instrumentation thread; the thread policy
            // is not what is under test here.
            .allowMainThreadQueries()
            .build()

        try {
            val dao = database.exerciseProgressDao()
            val progress = ExerciseProgressEntity(
                setId = "lektion-1",
                block = "A",
                part = 0,
                partCount = 2,
                totalSteps = 12,
                masteredSteps = 12,
                firstTryAccuracy = 75,
                completedAt = 1_700_000_000_000L,
            )

            runBlocking {
                dao.upsert(progress)
                assertEquals(progress, dao.find("lektion-1", "A", 0))
            }

            // The reason destructive migration was not an option.
            database.query("SELECT COUNT(*) FROM game_sessions", null).use {
                assertTrue(it.moveToFirst())
                assertEquals(1, it.getInt(0))
            }
        } finally {
            database.close()
        }
    }

    /**
     * The version 1 schema, copied from what Room generates for GameSessionEntity.
     *
     * Written out in full rather than built through Room, so that this test fails if the
     * migration stops matching the shape it actually has to upgrade from.
     */
    private fun createVersionOneWithOneSession() {
        val db = context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null)
        db.use {
            it.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `game_sessions` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `totalCards` INTEGER NOT NULL,
                    `correctAnswers` INTEGER NOT NULL,
                    `wrongAnswers` INTEGER NOT NULL,
                    `accuracyPercentage` INTEGER NOT NULL,
                    `durationMillis` INTEGER NOT NULL,
                    `cardsPerMinute` REAL NOT NULL,
                    `timestamp` INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            it.execSQL(
                """
                INSERT INTO `game_sessions`
                    (`totalCards`, `correctAnswers`, `wrongAnswers`, `accuracyPercentage`,
                     `durationMillis`, `cardsPerMinute`, `timestamp`)
                VALUES (10, 8, 2, 80, 60000, 10.0, 1699000000000)
                """.trimIndent(),
            )
            it.version = 1
        }
    }
}
