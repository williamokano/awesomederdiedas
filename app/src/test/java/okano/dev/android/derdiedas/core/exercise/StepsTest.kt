package okano.dev.android.derdiedas.core.exercise

import okano.dev.android.derdiedas.data.exercise.model.Block
import okano.dev.android.derdiedas.data.exercise.model.Exercise
import okano.dev.android.derdiedas.data.exercise.model.GapBankBody
import okano.dev.android.derdiedas.data.exercise.model.GapTextBody
import okano.dev.android.derdiedas.data.exercise.model.OddOneOutBody
import okano.dev.android.derdiedas.data.exercise.model.OddOneOutGroup
import okano.dev.android.derdiedas.data.exercise.model.Option
import okano.dev.android.derdiedas.data.exercise.model.SingleChoiceBody
import okano.dev.android.derdiedas.data.exercise.model.SingleChoiceItem
import okano.dev.android.derdiedas.data.exercise.model.TrueFalseBody
import okano.dev.android.derdiedas.data.exercise.model.TrueFalseItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GapSegmentsTest {

    private fun render(text: String) = parseSegments(text).joinToString("") {
        when (it) {
            is GapSegment.Literal -> it.text
            is GapSegment.Gap -> "<${it.key}>"
        }
    }

    @Test
    fun `splits literal and gap segments in order`() {
        assertEquals("Ich <1> gern", render("Ich {1} gern"))
    }

    @Test
    fun `handles a gap at the start`() {
        assertEquals("<2> ich", render("{2} ich"))
    }

    @Test
    fun `handles a gap at the end`() {
        assertEquals("ich <3>", render("ich {3}"))
    }

    @Test
    fun `handles adjacent gaps`() {
        assertEquals("<1><2>", render("{1}{2}"))
    }

    @Test
    fun `handles multi-digit gap keys`() {
        assertEquals("a <10> b", render("a {10} b"))
    }

    @Test
    fun `leaves a brace that is not a placeholder alone`() {
        assertEquals("a { b", render("a { b"))
        assertEquals("a {x} b", render("a {x} b"))
    }

    @Test
    fun `returns nothing for empty text`() {
        assertTrue(parseSegments("").isEmpty())
    }
}

class StepsTest {

    private fun exercise(body: okano.dev.android.derdiedas.data.exercise.model.ExerciseBody) = Exercise(
        id = "A3",
        block = Block.A,
        title = "Den Kauf planen",
        instructions = "Ergänze das Modalverb.",
        body = body,
    )

    @Test
    fun `numbered lines become one step each`() {
        val body = GapTextBody(
            text = """
                1. Ich {1} vor dem Kauf eine Probefahrt machen. (möchten, ich-Form)
                2. {2} ich mir den TÜV-Bericht ansehen? (dürfen, ich-Form)
                3. Ich {3} schon eine Probefahrt gemacht. (haben, Perfekt)
            """.trimIndent(),
            answers = mapOf("1" to listOf("möchte"), "2" to listOf("Darf"), "3" to listOf("habe")),
            listLayout = true,
        )

        val steps = exercise(body).toSteps("sit-78").filterIsInstance<GapTextStep>()

        assertEquals(3, steps.size)
        assertEquals(listOf(listOf("1"), listOf("2"), listOf("3")), steps.map { it.gapKeys })
        // Each step carries only its own answer, so grading one cannot see the others.
        assertEquals(mapOf("1" to listOf("möchte")), steps[0].answers)
        // The authored numbering is stripped; the session shows its own position instead.
        val first = steps[0].segments.first() as GapSegment.Literal
        assertTrue("should not keep the authored '1.' prefix: '${first.text}'", first.text.startsWith("Ich "))
        // The inline hint stays with its sentence.
        assertTrue(steps[0].segments.last().let { it is GapSegment.Literal && it.text.contains("möchten, ich-Form") })
    }

    @Test
    fun `a prose passage stays a single step`() {
        val body = GapTextBody(
            text = "Guten Tag, wir würden uns gerne den {1} ansehen, den Sie inseriert haben. " +
                "Wie hoch ist der {2}? Und wann war der letzte {3}?",
            answers = mapOf("1" to listOf("Gebrauchtwagen"), "2" to listOf("Kilometerstand"), "3" to listOf("TÜV")),
            listLayout = false,
        )

        val steps = exercise(body).toSteps("sit-78").filterIsInstance<GapTextStep>()

        assertEquals(1, steps.size)
        assertEquals(listOf("1", "2", "3"), steps.single().gapKeys)
    }

    @Test
    fun `lines without a gap are skipped`() {
        // Authored list intros and blank lines carry no gap and must not become steps.
        val body = GapTextBody(
            text = "Beispiel: Ich wohne in Berlin.\n\n1. Ich {1} in Hamburg.",
            answers = mapOf("1" to listOf("wohne")),
            listLayout = true,
        )

        val steps = exercise(body).toSteps("sit-78")

        assertEquals(1, steps.size)
    }

    @Test
    fun `step ids are unique within a set`() {
        val body = GapTextBody(
            text = "1. a {1}\n2. b {2}\n3. c {3}",
            answers = mapOf("1" to listOf("x"), "2" to listOf("y"), "3" to listOf("z")),
            listLayout = true,
        )

        val ids = exercise(body).toSteps("sit-78").map { it.id }

        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `a closed answer set becomes tappable options`() {
        // The whole exercise only ever answers der, die or das, so typing is the wrong
        // input. Options come from the exercise, not the gap, so nothing is given away.
        val body = GapTextBody(
            text = "1. ___ Kaffee {1}\n2. ___ Milch {2}\n3. ___ Wasser {3}",
            answers = mapOf("1" to listOf("der"), "2" to listOf("die"), "3" to listOf("das")),
            listLayout = true,
        )

        val steps = exercise(body).toSteps("a1-01").filterIsInstance<GapTextStep>()

        assertEquals(3, steps.size)
        // Every step offers all three, in a stable order, whatever its own answer is.
        steps.forEach { assertEquals(listOf("das", "der", "die"), it.options) }
    }

    @Test
    fun `a wide answer set stays free text`() {
        val body = GapTextBody(
            text = "1. a {1}\n2. b {2}\n3. c {3}\n4. d {4}\n5. e {5}",
            answers = mapOf(
                "1" to listOf("eins"), "2" to listOf("zwei"), "3" to listOf("drei"),
                "4" to listOf("vier"), "5" to listOf("fuenf"),
            ),
            listLayout = true,
        )

        exercise(body).toSteps("x").filterIsInstance<GapTextStep>()
            .forEach { assertTrue("should be free text", it.options.isEmpty()) }
    }

    @Test
    fun `a single distinct answer stays free text`() {
        // Offering one chip would simply hand over the answer.
        val body = GapTextBody(
            text = "1. a {1}\n2. b {2}",
            answers = mapOf("1" to listOf("ist"), "2" to listOf("ist")),
            listLayout = true,
        )

        exercise(body).toSteps("x").filterIsInstance<GapTextStep>()
            .forEach { assertTrue(it.options.isEmpty()) }
    }

    @Test
    fun `multi-word answers stay free text`() {
        val body = GapTextBody(
            text = "1. a {1}\n2. b {2}",
            answers = mapOf("1" to listOf("guten Tag"), "2" to listOf("gute Nacht")),
            listLayout = true,
        )

        exercise(body).toSteps("x").filterIsInstance<GapTextStep>()
            .forEach { assertTrue(it.options.isEmpty()) }
    }

    @Test
    fun `case-sensitive exercises stay free text`() {
        // A single lowercase chip could not answer a gap whose expected form is
        // capitalised, so chips are withheld rather than made unanswerable.
        val body = GapTextBody(
            text = "1. a {1}\n2. b {2}",
            answers = mapOf("1" to listOf("Der"), "2" to listOf("die")),
            listLayout = true,
        )
        val caseSensitive = exercise(body).copy(flags = GradingFlags(caseSensitive = true))

        caseSensitive.toSteps("x").filterIsInstance<GapTextStep>()
            .forEach { assertTrue(it.options.isEmpty()) }
    }

    @Test
    fun `options are withheld from a multi-gap step`() {
        // Several gaps sharing one set of chips needs a per-gap selection UI that does
        // not exist yet, so those keep their text fields.
        val body = GapTextBody(
            text = "Ich nehme {1} Kaffee und {2} Milch.",
            answers = mapOf("1" to listOf("den"), "2" to listOf("die")),
            listLayout = false,
        )

        val step = exercise(body).toSteps("x").filterIsInstance<GapTextStep>().single()

        assertEquals(2, step.gapKeys.size)
        assertTrue(step.options.isEmpty())
    }

    @Test
    fun `types not implemented yet produce no steps`() {
        // This is what will fail loudly in PR4 and remind us to update the splitter.
        val body = GapBankBody(
            text = "Ich {1} hier.",
            bank = listOf("WOHNE", "LEBE"),
            answers = mapOf("1" to listOf("WOHNE")),
        )

        assertTrue(exercise(body).toSteps("sit-78").isEmpty())
    }
}

class StepGraderTest {

    private fun step(
        answers: Map<String, List<String>>,
        alts: Map<String, List<AltAnswer>> = emptyMap(),
        flags: GradingFlags = GradingFlags(),
    ) = GapTextStep(
        id = StepId("s#A1#0"),
        exerciseId = "A1",
        title = "t",
        instructions = null,
        instructionsEn = null,
        flags = flags,
        segments = parseSegments(answers.keys.joinToString(" ") { "{$it}" }),
        gapKeys = answers.keys.toList(),
        answers = answers,
        alts = alts,
        cues = emptyMap(),
        listLayout = true,
    )

    @Test
    fun `a fully correct step is correct`() {
        val result = gradeStep(
            step(mapOf("1" to listOf("möchte"))),
            AnswerState.Texts(mapOf("1" to "moechte")),
        )
        assertTrue(result.correct)
        assertEquals(1, result.correctCount)
    }

    @Test
    fun `one wrong gap makes the whole step wrong`() {
        val result = gradeStep(
            step(mapOf("1" to listOf("a"), "2" to listOf("b"))),
            AnswerState.Texts(mapOf("1" to "a", "2" to "zzz")),
        )
        assertTrue(!result.correct)
        assertEquals(1, result.correctCount)
        assertEquals(2, result.items.size)
    }

    @Test
    fun `an unanswered gap is wrong and reports the expected answer`() {
        val result = gradeStep(step(mapOf("1" to listOf("möchte"))), AnswerState.Texts())
        assertTrue(!result.correct)
        assertEquals("", result.items.single().given)
        assertEquals("möchte", result.items.single().expected)
    }

    @Test
    fun `an accepted alternative is correct and carries its note`() {
        val result = gradeStep(
            step(mapOf("1" to listOf("etwas")), alts = mapOf("1" to listOf(AltAnswer("irgendetwas", "umgangssprachlich")))),
            AnswerState.Texts(mapOf("1" to "irgendetwas")),
        )
        assertTrue(result.correct)
        assertEquals("umgangssprachlich", result.items.single().note)
    }

    @Test
    fun `the first listed answer is the one shown back`() {
        val result = gradeStep(
            step(mapOf("1" to listOf("heißt", "ist"))),
            AnswerState.Texts(mapOf("1" to "falsch")),
        )
        assertEquals("heißt", result.items.single().expected)
    }

    @Test
    fun `exercise flags reach the comparison`() {
        val strict = step(mapOf("1" to listOf("heiße")), flags = GradingFlags(strictUmlaut = true))
        assertTrue(!gradeStep(strict, AnswerState.Texts(mapOf("1" to "heisse"))).correct)
        assertTrue(gradeStep(strict, AnswerState.Texts(mapOf("1" to "heiße"))).correct)
    }
}

class ChoiceStepsTest {

    private fun exercise(body: okano.dev.android.derdiedas.data.exercise.model.ExerciseBody) = Exercise(
        id = "B2",
        block = Block.B,
        title = "Wähle",
        instructions = "Wähle die passende Variante.",
        body = body,
    )

    @Test
    fun `each single-choice question becomes its own step`() {
        val body = SingleChoiceBody(
            items = listOf(
                SingleChoiceItem(
                    q = "Frage eins?",
                    options = listOf(Option("a", "falsch"), Option("b", "richtig")),
                    answer = "b",
                    why = "weil b",
                ),
                SingleChoiceItem(
                    q = "Frage zwei?",
                    options = listOf(Option("a", "ja"), Option("b", "nein")),
                    answer = "a",
                ),
            ),
        )

        val steps = exercise(body).toSteps("s").filterIsInstance<ChoiceStep>()

        assertEquals(2, steps.size)
        assertEquals("Frage eins?", steps[0].prompt)
        assertEquals("b", steps[0].answerKey)
        assertEquals("weil b", steps[0].why)
        assertEquals(listOf("falsch", "richtig"), steps[0].options.map { it.text })
    }

    @Test
    fun `true-false becomes a two-option choice using the exercise's own labels`() {
        val body = TrueFalseBody(
            positiveLabel = "Stimmt",
            negativeLabel = "Stimmt nicht",
            items = listOf(TrueFalseItem(q = "Berlin liegt in Deutschland.", answer = true, why = "Hauptstadt")),
        )

        val step = exercise(body).toSteps("s").filterIsInstance<ChoiceStep>().single()

        assertEquals(listOf("Stimmt", "Stimmt nicht"), step.options.map { it.text })
        assertEquals(step.options.first().key, step.answerKey)
    }

    @Test
    fun `a false answer points at the negative option`() {
        val body = TrueFalseBody(items = listOf(TrueFalseItem(q = "Falsch.", answer = false)))

        val step = exercise(body).toSteps("s").filterIsInstance<ChoiceStep>().single()

        assertEquals(step.options[1].key, step.answerKey)
    }

    @Test
    fun `odd-one-out offers the group and has no prompt of its own`() {
        val body = OddOneOutBody(
            groups = listOf(
                OddOneOutGroup(items = listOf("Hallo", "Guten Tag", "Tschüss"), odd = 2, why = "Abschied"),
            ),
        )

        val step = exercise(body).toSteps("s").filterIsInstance<ChoiceStep>().single()

        // The instruction already asks which one does not belong.
        assertNull(step.prompt)
        assertEquals(listOf("Hallo", "Guten Tag", "Tschüss"), step.options.map { it.text })
        assertEquals("2", step.answerKey)
    }

    @Test
    fun `an answer naming no option is dropped rather than shown unanswerable`() {
        val body = SingleChoiceBody(
            items = listOf(
                SingleChoiceItem(q = "Kaputt", options = listOf(Option("a", "eins")), answer = "zzz"),
                SingleChoiceItem(q = "Gut", options = listOf(Option("a", "eins")), answer = "a"),
            ),
        )

        val steps = exercise(body).toSteps("s").filterIsInstance<ChoiceStep>()

        assertEquals(1, steps.size)
        assertEquals("Gut", steps.single().prompt)
    }

    @Test
    fun `an out-of-range odd index is dropped`() {
        val body = OddOneOutBody(groups = listOf(OddOneOutGroup(items = listOf("a", "b"), odd = 9)))

        assertTrue(exercise(body).toSteps("s").isEmpty())
    }
}

class ChoiceGraderTest {

    private fun step(answerKey: String = "b", why: String? = "weil") = ChoiceStep(
        id = StepId("s#B2#0"),
        exerciseId = "B2",
        title = "t",
        instructions = null,
        instructionsEn = null,
        flags = GradingFlags(),
        prompt = "Frage?",
        options = listOf(ChoiceOption("a", "erste"), ChoiceOption("b", "zweite")),
        answerKey = answerKey,
        why = why,
    )

    @Test
    fun `the right option is correct`() {
        val result = gradeStep(step(), AnswerState.Choice("b"))
        assertTrue(result.correct)
    }

    @Test
    fun `the wrong option is wrong and reports both texts, not keys`() {
        val result = gradeStep(step(), AnswerState.Choice("a"))

        assertFalse(result.correct)
        // "a" and "b" mean nothing to a learner; the option text does.
        assertEquals("erste", result.items.single().given)
        assertEquals("zweite", result.items.single().expected)
    }

    @Test
    fun `no selection is wrong rather than crashing`() {
        val result = gradeStep(step(), AnswerState.Choice(null))

        assertFalse(result.correct)
        assertEquals("", result.items.single().given)
    }

    @Test
    fun `the explanation rides along for the banner`() {
        assertEquals("weil", gradeStep(step(), AnswerState.Choice("a")).items.single().note)
    }
}
