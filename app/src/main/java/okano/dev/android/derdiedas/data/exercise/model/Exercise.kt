package okano.dev.android.derdiedas.data.exercise.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import okano.dev.android.derdiedas.core.exercise.AltAnswer
import okano.dev.android.derdiedas.core.exercise.GradingFlags

/**
 * The bundled exercise content, mirroring what `tools/sync-exercises` emits.
 *
 * Shared fields live on [Exercise]; everything type-specific lives under [ExerciseBody].
 * Flattening the two would repeat nine base fields across nine variants and make adding
 * a base field a nine-file change.
 */

/** Bumped by the sync tool whenever the emitted shape changes; asserted by BundledContentTest. */
const val CONTENT_FORMAT_VERSION = 1

@Serializable
enum class CefrTag { A1, A2, B1, B2, C1 }

/** The content's own pedagogical grouping, and the unit a session is drawn from. */
@Serializable
enum class Block {
    @SerialName("H") H,
    @SerialName("A") A,
    @SerialName("B") B,
    @SerialName("C") C,
    @SerialName("D") D,
    @SerialName("exam") EXAM,
}

@Serializable
data class Exercise(
    val id: String,
    val block: Block,
    val title: String,
    val instructions: String? = null,
    val instructionsEn: String? = null,
    // Cross-lesson tagging axes upstream documents as extensible. Kept as String so a
    // new upstream value cannot throw while decoding a field the app never branches on.
    val skill: String? = null,
    val track: String? = null,
    val flags: GradingFlags = GradingFlags(),
    val body: ExerciseBody,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed interface ExerciseBody

@Serializable
@SerialName("gap-text")
data class GapTextBody(
    /** Prose or numbered lines with `{1}`, `{2}` placeholders marking the gaps. */
    val text: String,
    val answers: Map<String, List<String>>,
    val alts: Map<String, List<AltAnswer>> = emptyMap(),
    val cues: Map<String, String> = emptyMap(),
    /** True when [text] is independent numbered sentences rather than a passage. */
    val listLayout: Boolean = false,
) : ExerciseBody

@Serializable
@SerialName("gap-bank")
data class GapBankBody(
    val text: String,
    val bank: List<String>,
    val answers: Map<String, List<String>>,
) : ExerciseBody

@Serializable
@SerialName("single-choice")
data class SingleChoiceBody(val items: List<SingleChoiceItem>) : ExerciseBody

@Serializable
data class SingleChoiceItem(
    val q: String,
    val options: List<Option>,
    /** The [Option.key] of the correct option. */
    val answer: String,
    val why: String? = null,
)

@Serializable
data class Option(val key: String, val text: String)

@Serializable
@SerialName("true-false")
data class TrueFalseBody(
    val positiveLabel: String = "Richtig",
    val negativeLabel: String = "Falsch",
    val items: List<TrueFalseItem>,
) : ExerciseBody

@Serializable
data class TrueFalseItem(val q: String, val answer: Boolean, val why: String? = null)

@Serializable
@SerialName("matching")
data class MatchingBody(
    val left: List<KeyedText>,
    /** May hold more entries than [left]; the extras are distractors. */
    val right: List<KeyedText>,
    val answers: Map<String, String>,
) : ExerciseBody

@Serializable
data class KeyedText(val key: String, val text: String)

@Serializable
@SerialName("categorize")
data class CategorizeBody(
    val buckets: List<Bucket>,
    val tokens: List<Token>,
) : ExerciseBody

@Serializable
data class Bucket(val key: String, val label: String)

@Serializable
data class Token(val text: String, val bucket: String, val tag: String? = null)

@Serializable
@SerialName("odd-one-out")
data class OddOneOutBody(val groups: List<OddOneOutGroup>) : ExerciseBody

@Serializable
data class OddOneOutGroup(val items: List<String>, val odd: Int, val why: String? = null)

@Serializable
@SerialName("order")
data class OrderBody(val items: List<OrderItem>) : ExerciseBody

@Serializable
data class OrderItem(
    /** Display order of the words; [answer] indexes into this list. */
    val tiles: List<String>,
    val answer: List<Int>,
    val alt: List<List<Int>> = emptyList(),
    val note: String? = null,
)

@Serializable
@SerialName("table-fill")
data class TableFillBody(
    val columns: List<String>,
    val rows: List<TableRow>,
) : ExerciseBody

@Serializable
data class TableRow(val label: String, val cells: List<TableCell?>)

@Serializable
data class TableCell(val gap: Int, val answer: List<String>, val given: String? = null)

/** One exercise set: a curriculum lesson, a grammar theme, a situation or a listening set. */
@Serializable
data class ExerciseSet(
    val formatVersion: Int,
    /** Slugged id, used as the asset name and the navigation argument: "sit-78-auto-kaufen-b1". */
    val id: String,
    /** The content repo's own id, for display and cross-referencing: "SIT/78-auto-kaufen-b1". */
    val lesson: String,
    val title: String,
    val level: CefrTag? = null,
    val topic: String? = null,
    val category: String? = null,
    val summary: String? = null,
    val intro: String? = null,
    val exercises: List<Exercise>,
)

@Serializable
data class ExerciseSetSummary(
    val id: String,
    val lesson: String,
    val title: String,
    val level: CefrTag? = null,
    val topic: String? = null,
    val category: String? = null,
    val summary: String? = null,
    val blocks: List<BlockSummary>,
)

@Serializable
data class BlockSummary(
    val block: Block,
    val exerciseCount: Int,
    val types: List<String>,
)

@Serializable
data class ExerciseIndex(val formatVersion: Int, val sets: List<ExerciseSetSummary>)

@Serializable
data class ContentManifest(
    val formatVersion: Int,
    val sourceRepo: String,
    val sourceCommit: String,
    val setCount: Int,
    val exerciseCount: Int,
    /** Asset path to sha256, so a unit test can prove the tree was generated, not hand-edited. */
    val files: Map<String, String>,
)

/**
 * Decoding always names the serializer explicitly rather than using the reified
 * `decodeFromString<T>` form: that gives R8 a direct reference to the generated
 * serializer, which is what keeps minified release builds working.
 */
object ContentJson {
    val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun parseSet(text: String): ExerciseSet = json.decodeFromString(ExerciseSet.serializer(), text)
    fun parseIndex(text: String): ExerciseIndex = json.decodeFromString(ExerciseIndex.serializer(), text)
    fun parseManifest(text: String): ContentManifest = json.decodeFromString(ContentManifest.serializer(), text)
}
