package okano.dev.android.derdiedas.ui.exercise

/**
 * Handles for the instrumented smoke test.
 *
 * Tags rather than visible text: the UI is translated and the content is German, so a
 * test that looked for "Practise" would pass or fail depending on the device locale.
 * These are the few nodes a walk through the app has to find.
 */
object ExerciseTestTags {
    const val EXERCISES_ENTRY = "exercises_entry"
    const val COLLECTION_CARD = "collection_card"
    const val LEVEL_TAB = "level_tab"
    const val SET_CARD = "set_card"
    const val PRACTISE_BUTTON = "practise_button"

    const val SESSION_PROGRESS = "session_progress"
    const val CHECK_BUTTON = "check_button"
    const val CONTINUE_BUTTON = "continue_button"
    const val RESULT_BANNER = "result_banner"

    const val GAP_FIELD = "gap_field"
    const val OPTION = "option"
}
