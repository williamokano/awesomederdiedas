package okano.dev.android.derdiedas.data.repository

import okano.dev.android.derdiedas.data.model.Article
import okano.dev.android.derdiedas.data.model.CEFRLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalNounRepositoryTest {

    private val nouns = LocalNounRepository().getAllNouns()

    @Test
    fun `no noun appears twice`() {
        val duplicates = nouns.groupBy { it.noun }.filterValues { it.size > 1 }.keys
        assertTrue("Duplicate nouns: $duplicates", duplicates.isEmpty())
    }

    @Test
    fun `every level has at least 60 nouns per article`() {
        val levels = CEFRLevel.entries.filter { it != CEFRLevel.ALL }
        for (level in levels) {
            for (article in Article.entries) {
                val count = nouns.count { it.level == level && it.article == article }
                assertTrue("$level $article has only $count nouns", count >= 60)
            }
        }
    }

    @Test
    fun `every noun is capitalized and has English and Portuguese translations`() {
        for (noun in nouns) {
            assertTrue("Not capitalized: ${noun.noun}", noun.noun.first().isUpperCase())
            assertFalse("Missing en: ${noun.noun}", noun.translations["en"].isNullOrBlank())
            assertFalse("Missing pt: ${noun.noun}", noun.translations["pt"].isNullOrBlank())
        }
    }

    @Test
    fun `known data errors stay fixed`() {
        val byNoun = nouns.associateBy { it.noun }
        assertFalse("Lement" in byNoun)
        assertFalse("Surplus" in byNoun)
        assertEquals(Article.DIE, byNoun.getValue("Zurückhaltung").article)
        // das Schild is a sign; the shield is der Schild
        assertEquals("sign", byNoun.getValue("Schild").translations["en"])
        // das Gehalt is a salary; content is der Gehalt
        assertEquals("salary", byNoun.getValue("Gehalt").translations["en"])
    }
}
