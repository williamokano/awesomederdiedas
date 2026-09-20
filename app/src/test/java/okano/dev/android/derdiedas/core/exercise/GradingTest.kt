package okano.dev.android.derdiedas.core.exercise

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * The first four groups are a verbatim port of the website's own suite,
 * `web/src/core/__tests__/grading.test.ts` in williamokano/german-learning at 2245fb18.
 * Keep them in step: the same answer must grade the same way in both places.
 *
 * The groups after them cover what upstream does not test at all.
 */
class GradingTest {

    // ---- normalize ----

    @Test
    fun `lowercases by default`() {
        assertEquals("hallo", normalize("Hallo"))
    }

    @Test
    fun `folds umlaut by default`() {
        assertEquals("heisse", normalize("heiße"))
        assertEquals("sprichst", normalize("sprichst"))
        assertEquals("oesterreich", normalize("Österreich"))
    }

    @Test
    fun `strips trailing punctuation by default`() {
        assertEquals("hallo", normalize("Hallo!"))
        assertEquals("woher kommst du", normalize("Woher kommst du?"))
    }

    @Test
    fun `trims and collapses internal whitespace`() {
        assertEquals("ich bin", normalize("  ich  bin  "))
    }

    @Test
    fun `preserves punctuation with keepPunctuation flag`() {
        assertEquals("guten morgen!", normalize("Guten Morgen!", GradingFlags(keepPunctuation = true)))
    }

    @Test
    fun `preserves case with caseSensitive flag`() {
        assertEquals("Deutsch", normalize("Deutsch", GradingFlags(caseSensitive = true)))
        assertEquals("Berlin", normalize("Berlin", GradingFlags(caseSensitive = true)))
    }

    @Test
    fun `preserves umlauts with strictUmlaut flag`() {
        assertEquals("heiße", normalize("heiße", GradingFlags(strictUmlaut = true)))
        // still lowercased
        assertEquals("österreich", normalize("Österreich", GradingFlags(strictUmlaut = true)))
    }

    // ---- checkText ----

    @Test
    fun `accepts exact match`() {
        assertTrue(checkText(listOf("heiße"), "heiße"))
    }

    @Test
    fun `accepts umlaut-folded match`() {
        assertTrue(checkText(listOf("heiße"), "heisse"))
    }

    @Test
    fun `accepts case-insensitive match`() {
        assertTrue(checkText(listOf("heiße"), "HEISSE"))
        assertTrue(checkText(listOf("komme"), "Komme"))
    }

    @Test
    fun `accepts any alternative in an answer array`() {
        assertTrue(checkText(listOf("heißt", "ist"), "heißt"))
        assertTrue(checkText(listOf("heißt", "ist"), "ist"))
        assertTrue(checkText(listOf("heißt", "ist"), "heisst"))
    }

    @Test
    fun `rejects wrong answer`() {
        assertFalse(checkText(listOf("heiße"), "bin"))
        assertFalse(checkText(listOf("komme"), "kommst"))
    }

    @Test
    fun `rejects empty string`() {
        assertFalse(checkText(listOf("heiße"), ""))
    }

    @Test
    fun `accepts with trimmed whitespace`() {
        assertTrue(checkText(listOf("heiße"), "  heiße  "))
    }

    // ---- checkBank ----

    @Test
    fun `bank matches exact token`() {
        assertTrue(checkBank("heiße", "heiße"))
        assertTrue(checkBank("komme", "komme"))
    }

    @Test
    fun `bank rejects null for an empty slot`() {
        assertFalse(checkBank("heiße", null))
    }

    @Test
    fun `bank rejects wrong token`() {
        assertFalse(checkBank("heiße", "komme"))
        assertFalse(checkBank("lerne", "spreche"))
    }

    @Test
    fun `bank is case-sensitive`() {
        assertFalse(checkBank("BIN", "bin"))
        assertTrue(checkBank("BIN", "BIN"))
    }

    // ---- checkOrder ----

    @Test
    fun `order matches the correct order`() {
        assertTrue(checkOrder(listOf(0, 1, 2), listOf(0, 1, 2)))
    }

    @Test
    fun `order rejects wrong order`() {
        assertFalse(checkOrder(listOf(0, 1, 2), listOf(1, 0, 2)))
    }

    @Test
    fun `order accepts alternative orderings`() {
        assertTrue(checkOrder(listOf(0, 1, 2, 3), listOf(2, 0, 1, 3), listOf(listOf(2, 0, 1, 3))))
        assertTrue(checkOrder(listOf(0, 1, 2), listOf(2, 1, 0), listOf(listOf(2, 1, 0))))
    }

    @Test
    fun `order rejects when neither main nor alt matches`() {
        assertFalse(checkOrder(listOf(0, 1, 2), listOf(1, 2, 0), listOf(listOf(2, 1, 0))))
    }

    // ---- checkTextDetail: untested upstream, and the likeliest place a port diverges ----

    @Test
    fun `detail reports a plain match with no note`() {
        val check = checkTextDetail(listOf("etwas"), "etwas")
        assertTrue(check.correct)
        assertNull(check.note)
    }

    @Test
    fun `detail accepts an alternative and returns its note`() {
        val check = checkTextDetail(
            expected = listOf("etwas"),
            given = "irgendetwas",
            alts = listOf(AltAnswer(word = "irgendetwas", note = "umgangssprachlich")),
        )
        assertTrue(check.correct)
        assertEquals("umgangssprachlich", check.note)
    }

    @Test
    fun `detail prefers the drilled answer over an alternative`() {
        val check = checkTextDetail(
            expected = listOf("etwas"),
            given = "etwas",
            alts = listOf(AltAnswer(word = "etwas", note = "should not be reported")),
        )
        assertTrue(check.correct)
        assertNull(check.note)
    }

    @Test
    fun `detail normalises alternatives too`() {
        val check = checkTextDetail(
            expected = listOf("etwas"),
            given = "IRGENDETWAS",
            alts = listOf(AltAnswer(word = "irgendetwas")),
        )
        assertTrue(check.correct)
    }

    @Test
    fun `detail rejects when neither answer nor alternative matches`() {
        val check = checkTextDetail(listOf("etwas"), "nichts", listOf(AltAnswer("irgendetwas")))
        assertFalse(check.correct)
    }

    // ---- JVM/JS parity traps: each of these passes in JS and fails a naive Kotlin port ----

    @Test
    fun `collapses the non-breaking spaces that JavaScript's whitespace class covers`() {
        // A German keyboard or a paste can produce U+00A0; JS \s matches it, Java's does not.
        assertEquals("ich bin", normalize("ich bin"))
        assertEquals("ich bin", normalize("ich bin"))
        assertTrue(checkText(listOf("ich bin"), "ich bin"))
    }

    @Test
    fun `strips a byte order mark`() {
        assertEquals("hallo", normalize("﻿Hallo"))
    }

    @Test
    fun `strips punctuation followed by a newline`() {
        // Documents the behaviour rather than catching a divergence: trim() removes the
        // newline first, so the engine's \z and JavaScript's $ agree here either way.
        assertEquals("hallo", normalize("Hallo!\n"))
    }

    @Test
    fun `lowercases independently of the device locale`() {
        // lowercase(Locale.getDefault()) maps I to a dotless i in Turkish and would
        // fail an otherwise correct answer for anyone with a Turkish device.
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr"))
            assertEquals("ist", normalize("IST"))
            assertTrue(checkText(listOf("ist"), "IST"))
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun `folds every umlaut and eszett in both cases`() {
        assertEquals("aeoeuess", normalize("äöüß", GradingFlags(caseSensitive = true)))
        assertEquals("aeoeue", normalize("ÄÖÜ", GradingFlags(caseSensitive = true)))
    }
}
