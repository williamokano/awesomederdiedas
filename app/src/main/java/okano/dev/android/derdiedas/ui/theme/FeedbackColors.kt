package okano.dev.android.derdiedas.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Right/wrong colours for exercise feedback.
 *
 * Deliberately fixed rather than derived from the colour scheme: dynamic colour would make
 * "correct" follow the user's wallpaper, and a purple correct-state reads as neither right
 * nor wrong. Every use is paired with a glyph (✓ ✗ ≈) so the state survives colour
 * blindness, which green-vs-red alone does not.
 */
@Immutable
data class FeedbackColors(
    val correct: Color,
    val onCorrect: Color,
    val correctContainer: Color,
    val onCorrectContainer: Color,
    val wrong: Color,
    val onWrong: Color,
    val wrongContainer: Color,
    val onWrongContainer: Color,
    /** Accepted, but not the form the exercise was drilling. */
    val accepted: Color,
    val acceptedContainer: Color,
    val onAcceptedContainer: Color,
)

val LightFeedbackColors = FeedbackColors(
    correct = Color(0xFF2E7D32),
    onCorrect = Color.White,
    correctContainer = Color(0xFFD7F5DB),
    onCorrectContainer = Color(0xFF16491A),
    wrong = Color(0xFFC62828),
    onWrong = Color.White,
    wrongContainer = Color(0xFFFFDAD6),
    onWrongContainer = Color(0xFF6E1414),
    accepted = Color(0xFF9A6700),
    acceptedContainer = Color(0xFFFFF2CC),
    onAcceptedContainer = Color(0xFF5C3D00),
)

val DarkFeedbackColors = FeedbackColors(
    correct = Color(0xFF7BD88F),
    onCorrect = Color(0xFF00390D),
    correctContainer = Color(0xFF1B4620),
    onCorrectContainer = Color(0xFFB9F2C1),
    wrong = Color(0xFFFF8A80),
    onWrong = Color(0xFF5C0A0A),
    wrongContainer = Color(0xFF5C1B1B),
    onWrongContainer = Color(0xFFFFDAD6),
    accepted = Color(0xFFF2C94C),
    acceptedContainer = Color(0xFF4A3A0B),
    onAcceptedContainer = Color(0xFFFFE9A8),
)

/** Static because the value only changes with the theme, never within a composition. */
val LocalFeedbackColors = staticCompositionLocalOf { LightFeedbackColors }
