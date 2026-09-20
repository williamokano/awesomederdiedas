package okano.dev.android.derdiedas.core.exercise

import kotlinx.serialization.Serializable

/**
 * Port of the website's grading engine, `web/src/core/engine/grading.ts` in
 * williamokano/german-learning as of 2245fb18.
 *
 * Function names and semantics deliberately match the TypeScript so the two can be
 * diffed by eye. The same answer must grade the same way on the phone and on the
 * website; a divergence here is invisible until a learner is told they are wrong.
 *
 * Pure Kotlin, no Android imports, so it is unit-testable on the JVM.
 */

/** Per-exercise overrides for how leniently a typed answer is compared. */
@Serializable
data class GradingFlags(
    val caseSensitive: Boolean = false,
    val strictUmlaut: Boolean = false,
    val keepPunctuation: Boolean = false,
)

/** The outcome for one gap, item or group within a step. */
data class ItemResult(
    val ref: String,
    val correct: Boolean,
    val given: String,
    val expected: String,
    val scoreable: Boolean = true,
    /** Set when the answer was accepted as an alternative rather than the drilled form. */
    val note: String? = null,
)

data class TextCheck(val correct: Boolean, val note: String? = null)

/** An accepted alternative that is not the form the exercise is drilling. */
@Serializable
data class AltAnswer(val word: String, val note: String? = null)

// JavaScript's \s matches NBSP, U+1680, U+2000-200A, U+2028/29, U+202F, U+205F,
// U+3000 and U+FEFF. Java's \s is only [ \t\n\x0B\f\r]. Learner input carries these
// via paste and third-party keyboards, so the class is spelled out to keep parity.
private val WHITESPACE = Regex("[\\s\\u00A0\\u1680\\u2000-\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000\\uFEFF]+")

// \z rather than $, because Java's $ also matches before a final line terminator while
// JavaScript's (without /m) does not. The preceding trim() means the two behave the same
// on any input reaching this line today; \z just removes the question.
private val TRAILING_PUNCTUATION = Regex("[.?!,;:]+\\z")

internal fun foldUmlautAndEszett(s: String): String = buildString(s.length + 4) {
    for (ch in s) when (ch) {
        'ä', 'Ä' -> append("ae")
        'ö', 'Ö' -> append("oe")
        'ü', 'Ü' -> append("ue")
        'ß' -> append("ss")
        else -> append(ch)
    }
}

fun normalize(s: String, flags: GradingFlags = GradingFlags()): String {
    var t = s.trim { it.isWhitespace() || it == '﻿' }.replace(WHITESPACE, " ")
    if (!flags.keepPunctuation) t = t.replace(TRAILING_PUNCTUATION, "")
    // The no-argument overload is locale-independent on purpose: lowercase(Locale.getDefault())
    // maps I to a dotless i on a Turkish-locale device and would fail correct answers.
    if (!flags.caseSensitive) t = t.lowercase()
    if (!flags.strictUmlaut) t = foldUmlautAndEszett(t)
    return t
}

fun checkText(expected: List<String>, given: String, flags: GradingFlags = GradingFlags()): Boolean {
    val normalized = normalize(given, flags)
    return expected.any { normalize(it, flags) == normalized }
}

/**
 * Like [checkText], but also accepts [alts] and reports the matched alternative's note,
 * which the UI shows as an "accepted, but not the drilled form" state.
 */
fun checkTextDetail(
    expected: List<String>,
    given: String,
    alts: List<AltAnswer> = emptyList(),
    flags: GradingFlags = GradingFlags(),
): TextCheck {
    if (checkText(expected, given, flags)) return TextCheck(correct = true)
    val normalized = normalize(given, flags)
    for (alt in alts) {
        if (normalize(alt.word, flags) == normalized) return TextCheck(correct = true, note = alt.note)
    }
    return TextCheck(correct = false)
}

/**
 * Word-bank answers are compared exactly, with no normalisation. The learner picks a
 * token from a fixed bank rather than typing it, so there is nothing to be lenient
 * about. This asymmetry with [checkText] is deliberate and matches the website.
 */
fun checkBank(expectedWord: String, placedWord: String?): Boolean = placedWord == expectedWord

fun checkOrder(answer: List<Int>, given: List<Int>, alt: List<List<Int>> = emptyList()): Boolean =
    answer == given || alt.any { it == given }

fun <T> checkEquality(expected: T, given: T): Boolean = expected == given
