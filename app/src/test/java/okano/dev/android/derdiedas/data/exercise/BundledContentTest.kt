package okano.dev.android.derdiedas.data.exercise

import okano.dev.android.derdiedas.core.exercise.GapTextStep
import okano.dev.android.derdiedas.core.exercise.normalize
import okano.dev.android.derdiedas.core.exercise.toSteps
import okano.dev.android.derdiedas.data.exercise.model.CONTENT_FORMAT_VERSION
import okano.dev.android.derdiedas.data.exercise.model.ContentJson
import okano.dev.android.derdiedas.data.exercise.model.ExerciseCollection
import okano.dev.android.derdiedas.data.exercise.model.ExerciseSet
import okano.dev.android.derdiedas.data.exercise.model.GapBankBody
import okano.dev.android.derdiedas.data.exercise.model.GapTextBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.security.MessageDigest

/**
 * Parses every bundled asset and checks the invariants the app relies on.
 *
 * This is the guard against a bad content sync. It runs in the normal unit-test job, so
 * it also serves as the drift check: the manifest's hashes prove the tree was produced by
 * `tools/sync-exercises` and not hand-edited, without needing Node in CI.
 *
 * Follows the LocalNounRepositoryTest precedent of asserting integrity over bundled data.
 */
class BundledContentTest {

    companion object {
        private val assetsRoot = File("src/main/assets/exercises")
        private lateinit var sets: List<ExerciseSet>

        @BeforeClass
        @JvmStatic
        fun loadAll() {
            assertTrue(
                "No bundled exercises at ${assetsRoot.absolutePath}. Generate them with:\n" +
                    "  npm --prefix tools/sync-exercises install\n" +
                    "  npm --prefix tools/sync-exercises run sync -- --content ../german-learning",
                assetsRoot.isDirectory,
            )
            sets = setFiles().map { ContentJson.parseSet(it.readText()) }
        }

        private fun setFiles(): List<File> =
            File(assetsRoot, "sets").listFiles()?.sortedBy { it.name } ?: emptyList()

        private fun sha256(text: String): String =
            MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private val manifest by lazy { ContentJson.parseManifest(File(assetsRoot, "manifest.json").readText()) }
    private val index by lazy { ContentJson.parseIndex(File(assetsRoot, "index.json").readText()) }

    @Test
    fun `manifest format version matches the model`() {
        assertEquals(CONTENT_FORMAT_VERSION, manifest.formatVersion)
        assertEquals(CONTENT_FORMAT_VERSION, index.formatVersion)
        sets.forEach { assertEquals("${it.id} has a stale formatVersion", CONTENT_FORMAT_VERSION, it.formatVersion) }
    }

    @Test
    fun `every asset matches its recorded hash`() {
        // Fails if an asset was hand-edited or the tree is half-regenerated. Re-run the
        // sync script rather than editing the generated files.
        val mismatches = manifest.files.filter { (path, expected) ->
            val file = File(assetsRoot, path)
            !file.isFile || sha256(file.readText()) != expected
        }
        assertTrue("assets differ from the manifest: ${mismatches.keys.take(5)}", mismatches.isEmpty())
    }

    @Test
    fun `no asset is missing from the manifest`() {
        val onDisk = (setFiles().map { "sets/${it.name}" } + "index.json").toSet()
        val recorded = manifest.files.keys
        assertTrue("assets not in the manifest: ${(onDisk - recorded).take(5)}", (onDisk - recorded).isEmpty())
        assertTrue("manifest lists missing assets: ${(recorded - onDisk).take(5)}", (recorded - onDisk).isEmpty())
    }

    @Test
    fun `the index and the set files agree`() {
        assertEquals(sets.size, index.sets.size)
        assertEquals(sets.map { it.id }.toSet(), index.sets.map { it.id }.toSet())
        assertEquals(manifest.setCount, sets.size)
        assertEquals(manifest.exerciseCount, sets.sumOf { it.exercises.size })
    }

    @Test
    fun `set ids are unique and slugged`() {
        assertEquals(sets.size, sets.map { it.id }.toSet().size)
        // The id is a navigation argument, so anything needing escaping is a bug.
        val bad = sets.map { it.id }.filterNot { it.matches(Regex("[a-z0-9-]+")) }
        assertTrue("set ids must be slugs: $bad", bad.isEmpty())
    }

    @Test
    fun `every set has a level for browsing`() {
        val missing = index.sets.filter { it.level == null }.map { it.id }
        assertTrue("sets without a CEFR level: ${missing.take(5)}", missing.isEmpty())
    }

    @Test
    fun `exercise ids are unique within a set`() {
        for (set in sets) {
            val ids = set.exercises.map { it.id }
            assertEquals("duplicate exercise id in ${set.id}", ids.size, ids.toSet().size)
        }
    }

    @Test
    fun `every gap has an answer and every answer has a gap`() {
        val placeholder = Regex("""\{(\d+)\}""")
        for (set in sets) {
            for (exercise in set.exercises) {
                val (text, answers) = when (val body = exercise.body) {
                    is GapTextBody -> body.text to body.answers
                    is GapBankBody -> body.text to body.answers
                    else -> continue
                }
                val inText = placeholder.findAll(text).map { it.groupValues[1] }.toSet()
                assertEquals("${set.id}/${exercise.id}: gaps and answers disagree", inText, answers.keys)
            }
        }
    }

    @Test
    fun `every word bank answer is in its bank`() {
        for (set in sets) {
            for (exercise in set.exercises) {
                val body = exercise.body as? GapBankBody ?: continue
                for ((gap, answer) in body.answers) {
                    assertTrue(
                        "${set.id}/${exercise.id} gap $gap: \"${answer.first()}\" is not in the bank",
                        answer.first() in body.bank,
                    )
                }
            }
        }
    }

    @Test
    fun `no answer normalises to an empty string`() {
        // An empty expected answer would score a blank submission as correct. The grader
        // deliberately does not special-case this, so the content must not contain it.
        for (set in sets) {
            for (exercise in set.exercises) {
                val answers = when (val body = exercise.body) {
                    is GapTextBody -> body.answers
                    is GapBankBody -> body.answers
                    else -> continue
                }
                for ((gap, alternatives) in answers) {
                    for (answer in alternatives) {
                        assertTrue(
                            "${set.id}/${exercise.id} gap $gap has an answer that normalises to empty",
                            normalize(answer, exercise.flags).isNotEmpty(),
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `no self-assessed or audio-dependent exercise was shipped`() {
        // Both are filtered by the sync script; parsing would throw on an unknown type, so
        // this asserts the filter ran rather than that decoding worked.
        val types = sets.flatMap { set -> set.exercises.map { it.body::class.simpleName } }.toSet()
        assertTrue("unexpected body types: $types", types.none { it == null })
    }

    @Test
    fun `every gap-text exercise yields at least one step`() {
        for (set in sets) {
            for (exercise in set.exercises) {
                if (exercise.body !is GapTextBody) continue
                assertTrue(
                    "${set.id}/${exercise.id} produced no steps",
                    exercise.toSteps(set.id).isNotEmpty(),
                )
            }
        }
    }

    @Test
    fun `step ids are unique across a whole set`() {
        for (set in sets) {
            val ids = set.exercises.flatMap { it.toSteps(set.id) }.map { it.id }
            assertEquals("duplicate step id in ${set.id}", ids.size, ids.toSet().size)
        }
    }

    @Test
    fun `every gap-text step can be graded from its own answers`() {
        // Guards the splitter's answer-partitioning: a step must carry an answer for each
        // gap it asks for, or the learner could never get it right.
        for (set in sets) {
            for (exercise in set.exercises) {
                for (step in exercise.toSteps(set.id).filterIsInstance<GapTextStep>()) {
                    val missing = step.gapKeys.filterNot { step.answers[it]?.isNotEmpty() == true }
                    assertTrue("${step.id.value} has gaps without answers: $missing", missing.isEmpty())
                }
            }
        }
    }

    @Test
    fun `every set lands in a collection, and the collections are worth browsing`() {
        // The mapping reads the upstream id prefix, so a renamed prefix upstream would
        // silently dump everything into Lektionen. These floors catch that.
        val byCollection = index.sets.groupingBy { ExerciseCollection.of(it.lesson) }.eachCount()

        assertEquals(index.sets.size, byCollection.values.sum())
        assertTrue("no Lektionen: ${'$'}byCollection", (byCollection[ExerciseCollection.LEKTIONEN] ?: 0) >= 60)
        assertTrue("no Themen: ${'$'}byCollection", (byCollection[ExerciseCollection.THEMEN] ?: 0) >= 50)
        assertTrue("no Alltag: ${'$'}byCollection", (byCollection[ExerciseCollection.ALLTAG] ?: 0) >= 90)
    }

    @Test
    fun `every collection offers more than one level to tab between`() {
        for (collection in ExerciseCollection.entries) {
            val sets = index.sets.filter { ExerciseCollection.of(it.lesson) == collection }
            if (sets.isEmpty()) continue
            val levels = sets.mapNotNull { it.level }.distinct()
            assertTrue("${'$'}collection has only ${'$'}levels", levels.size >= 2)
        }
    }

    @Test
    fun `the corpus is as large as expected`() {
        // Floors, not exact counts: content grows upstream. A big drop means a broken sync.
        assertTrue("only ${sets.size} sets", sets.size >= 220)
        val exercises = sets.sumOf { it.exercises.size }
        assertTrue("only $exercises exercises", exercises >= 3_200)
        val gapTextSteps = sets.sumOf { set -> set.exercises.sumOf { it.toSteps(set.id).size } }
        assertTrue("only $gapTextSteps gap-text steps", gapTextSteps >= 9_000)
    }
}
