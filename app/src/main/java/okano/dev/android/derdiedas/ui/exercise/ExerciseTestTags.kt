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
    const val SESSION_SUMMARY = "session_summary"
    const val ORDER_STRIP = "order_strip"
    const val ORDER_TILE = "order_tile"
    const val ORDER_PLACED_TILE = "order_placed_tile"
    const val BANK_GAP = "bank_gap"
    const val BANK_WORD = "bank_word"
    const val MATCH_PROMPT = "match_prompt"
    const val MATCH_POOL_ENTRY = "match_pool_entry"
    const val MATCH_ANSWERED = "match_answered"

    // The original flashcard flow, which has no coverage of its own and shares the
    // navigation and dependency wiring the exercise work refactored.
    const val NEW_GAME_ENTRY = "new_game_entry"
    const val CARD_COUNT_OPTION = "card_count_option"
    const val ARTICLE_BUTTON = "article_button"
}
