package okano.dev.android.derdiedas.data.exercise.model

/**
 * The four bodies of content the sets come from, told apart by the prefix of their
 * upstream id. Browsing 226 sets as one list mixes a graded course with standalone drills
 * and everyday situations, which are three different things to be in the mood for.
 *
 * Derived rather than stored: the prefix is already part of `lesson`, so this needs no
 * change to the generated assets.
 */
enum class ExerciseCollection {
    /** The graded A1-C1 course, one set per lesson. */
    LEKTIONEN,

    /** Standalone grammar-point drills, from THEMEN/. */
    THEMEN,

    /** Everyday situations, from SITUATIONEN/. */
    ALLTAG,

    /** Listening sets, from HOEREN/. Empty today: every exercise in them needs audio. */
    HOEREN,
    ;

    companion object {
        fun of(lesson: String): ExerciseCollection = when {
            lesson.startsWith("EX/") -> THEMEN
            lesson.startsWith("SIT/") -> ALLTAG
            lesson.startsWith("HV/") -> HOEREN
            else -> LEKTIONEN
        }
    }
}

val ExerciseSetSummary.collection: ExerciseCollection
    get() = ExerciseCollection.of(lesson)
