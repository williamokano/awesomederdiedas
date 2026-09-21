package okano.dev.android.derdiedas.core.exercise

import okano.dev.android.derdiedas.data.exercise.model.Block
import okano.dev.android.derdiedas.data.exercise.model.Exercise
import okano.dev.android.derdiedas.data.exercise.model.GapBankBody
import okano.dev.android.derdiedas.data.exercise.model.GapTextBody
import okano.dev.android.derdiedas.data.exercise.model.OddOneOutBody
import okano.dev.android.derdiedas.data.exercise.model.OddOneOutGroup
import okano.dev.android.derdiedas.data.exercise.model.OrderBody
import okano.dev.android.derdiedas.data.exercise.model.OrderItem
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
        // matching, categorize and table-fill are still to come; this is what will fail
        // loudly when one of them lands and the splitter has not been updated.
        val body = okano.dev.android.derdiedas.data.exercise.model.CategorizeBody(
            buckets = listOf(okano.dev.android.derdiedas.data.exercise.model.Bucket("a", "der")),
            tokens = listOf(okano.dev.android.derdiedas.data.exercise.model.Token("Tisch", "a")),
        )

        assertTrue(exercise(body).toSteps("sit-78").isEmpty())
    }
}

class GapBankStepsTest {

    @Test
    fun `a passage becomes one step holding every gap`() {
        val step = step(
            text = "Ich {1} hier.\nDu {2} dort.",
            bank = listOf("wohne", "lebst", "extra"),
            answers = mapOf("1" to listOf("wohne"), "2" to listOf("lebst")),
        )

        assertEquals(listOf("1", "2"), step.gapKeys)
        assertEquals(2, step.lines.size)
    }

    @Test
    fun `the bank is shuffled because as authored it spells out the answers`() {
        // 214 of the corpus's 327 banks begin with the answers in gap order, so a bank
        // shown as authored would be solvable left to right without reading the passage.
        val step = step(
            text = "{1} {2} {3} {4} {5}",
            bank = listOf("a", "b", "c", "d", "e"),
            answers = (1..5).associate { it.toString() to listOf("abcde"[it - 1].toString()) },
        )

        assertTrue("the bank was left in answer order", step.bank != listOf("a", "b", "c", "d", "e"))
        assertEquals(listOf("a", "b", "c", "d", "e"), step.bank.sorted())
    }

    @Test
    fun `the shuffle is stable for the same exercise`() {
        // A bank that rearranged itself when a missed step came back would be unfair.
        val body = body("Ich {1} hier.", listOf("a", "b", "c", "d"), mapOf("1" to listOf("a")))

        assertEquals(
            exercise(body).toSteps("s").filterIsInstance<GapBankStep>().single().bank,
            exercise(body).toSteps("s").filterIsInstance<GapBankStep>().single().bank,
        )
    }

    @Test
    fun `capacity is the number of copies the bank holds`() {
        val step = step(
            text = "{1} {2} {3}",
            bank = listOf("ja", "ja", "nein"),
            answers = mapOf("1" to listOf("ja"), "2" to listOf("ja"), "3" to listOf("nein")),
        )

        assertEquals(2, step.capacity["ja"])
        assertEquals(1, step.capacity["nein"])
    }

    @Test
    fun `capacity rises to meet an answer the bank lists too few times`() {
        // Three corpus exercises are classification drills: a couple of labels, reused
        // across every gap. Consuming the single listed copy would strand the learner.
        val step = step(
            text = "{1} {2} {3}",
            bank = listOf("-s-", "-es-"),
            answers = mapOf("1" to listOf("-s-"), "2" to listOf("-s-"), "3" to listOf("-es-")),
        )

        assertEquals(2, step.capacity["-s-"])
        assertEquals(1, step.capacity["-es-"])
    }

    @Test
    fun `a gap with no answer drops the exercise rather than shipping it unanswerable`() {
        val body = body("Ich {1} {2} hier.", listOf("wohne"), mapOf("1" to listOf("wohne")))

        assertTrue(exercise(body).toSteps("s").isEmpty())
    }

    @Test
    fun `an answer that is not in the bank drops the exercise`() {
        val body = body("Ich {1} hier.", listOf("lebe"), mapOf("1" to listOf("wohne")))

        assertTrue(exercise(body).toSteps("s").isEmpty())
    }

    @Test
    fun `an answer naming a gap the text does not have drops the exercise`() {
        val body = body("Ich {1} hier.", listOf("wohne", "lebe"), mapOf("1" to listOf("wohne"), "9" to listOf("lebe")))

        assertTrue(exercise(body).toSteps("s").isEmpty())
    }

    @Test
    fun `a passage with no gaps drops the exercise`() {
        assertTrue(exercise(body("Ich wohne hier.", listOf("a"), emptyMap())).toSteps("s").isEmpty())
    }

    @Test
    fun `blank lines are dropped so the passage does not render gaps of empty rows`() {
        val step = step(
            text = "Ich {1} hier.\n\n\nDu {2} dort.",
            bank = listOf("wohne", "lebst"),
            answers = mapOf("1" to listOf("wohne"), "2" to listOf("lebst")),
        )

        assertEquals(2, step.lines.size)
    }

    private fun step(text: String, bank: List<String>, answers: Map<String, List<String>>) =
        exercise(body(text, bank, answers)).toSteps("s").filterIsInstance<GapBankStep>().single()

    private fun body(text: String, bank: List<String>, answers: Map<String, List<String>>) =
        GapBankBody(text = text, bank = bank, answers = answers)

    private fun exercise(body: okano.dev.android.derdiedas.data.exercise.model.ExerciseBody) = Exercise(
        id = "C1",
        block = Block.C,
        title = "Lückentext mit Wortbank",
        instructions = "Ergänze mit den Wörtern aus der Bank.",
        body = body,
    )
}

class GapBankGraderTest {

    @Test
    fun `every gap filled correctly is a correct step`() {
        val step = step()
        val result = gradeStep(step, AnswerState.Placements(mapOf("1" to "wohne", "2" to "lebst")))

        assertTrue(result.correct)
        assertEquals(2, result.correctCount)
    }

    @Test
    fun `one wrong gap makes the whole step wrong but reports the rest`() {
        val result = gradeStep(step(), AnswerState.Placements(mapOf("1" to "wohne", "2" to "extra")))

        assertFalse(result.correct)
        assertEquals(1, result.correctCount)
        assertEquals(2, result.items.size)
    }

    @Test
    fun `an unfilled gap is wrong rather than skipped`() {
        val result = gradeStep(step(), AnswerState.Placements(mapOf("1" to "wohne")))

        assertFalse(result.correct)
        assertEquals("", result.items.single { it.ref == "2" }.given)
    }

    @Test
    fun `bank answers are compared exactly with no normalisation`() {
        // checkText trims and folds case; checkBank deliberately does not, because the
        // learner picked the word from a fixed bank rather than typing it.
        assertFalse(gradeStep(step(), AnswerState.Placements(mapOf("1" to "Wohne", "2" to "lebst"))).correct)
        assertFalse(gradeStep(step(), AnswerState.Placements(mapOf("1" to " wohne", "2" to "lebst"))).correct)
    }

    @Test
    fun `any listed alternative is accepted`() {
        val step = exercise(
            GapBankBody(
                text = "Ich {1} hier.",
                bank = listOf("wohne", "lebe"),
                answers = mapOf("1" to listOf("wohne", "lebe")),
            ),
        ).toSteps("s").filterIsInstance<GapBankStep>().single()

        assertTrue(gradeStep(step, AnswerState.Placements(mapOf("1" to "lebe"))).correct)
    }

    @Test
    fun `the reported expected answer is the drilled form`() {
        val result = gradeStep(step(), AnswerState.Placements(emptyMap()))

        assertEquals("wohne", result.items.single { it.ref == "1" }.expected)
    }

    private fun step() = exercise(
        GapBankBody(
            text = "Ich {1} hier.\nDu {2} dort.",
            bank = listOf("wohne", "lebst", "extra"),
            answers = mapOf("1" to listOf("wohne"), "2" to listOf("lebst")),
        ),
    ).toSteps("s").filterIsInstance<GapBankStep>().single()

    private fun exercise(body: okano.dev.android.derdiedas.data.exercise.model.ExerciseBody) = Exercise(
        id = "C1",
        block = Block.C,
        title = "Lückentext mit Wortbank",
        instructions = "Ergänze mit den Wörtern aus der Bank.",
        body = body,
    )
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

class OrderStepsTest {

    private fun exercise(body: okano.dev.android.derdiedas.data.exercise.model.ExerciseBody) = Exercise(
        id = "B5",
        block = Block.B,
        title = "Satzbau",
        instructions = "Bring die Wörter in die richtige Reihenfolge.",
        body = body,
    )

    private fun orderBody(vararg items: OrderItem) = OrderBody(items = items.toList())

    @Test
    fun `each sentence becomes its own step`() {
        val body = orderBody(
            OrderItem(tiles = listOf("Ich", "heiße", "Anna"), answer = listOf(0, 1, 2)),
            OrderItem(tiles = listOf("Er", "wohnt", "hier"), answer = listOf(0, 1, 2)),
        )

        assertEquals(2, exercise(body).toSteps("s").filterIsInstance<OrderStep>().size)
    }

    @Test
    fun `tiles are shuffled so the answer is never already in order`() {
        // 85% of the corpus stores its tiles in the correct order. Presenting them as
        // authored would let the learner solve the exercise by tapping left to right.
        val body = orderBody(OrderItem(tiles = listOf("Ich", "heiße", "Anna"), answer = listOf(0, 1, 2)))

        val step = exercise(body).toSteps("s").filterIsInstance<OrderStep>().single()

        assertEquals(listOf("Ich", "heiße", "Anna").sorted(), step.tiles.sorted())
        assertTrue("tiles were left in answer order: ${step.tiles}", step.answer != listOf(0, 1, 2))
    }

    @Test
    fun `the remapped answer still assembles the original sentence`() {
        val body = orderBody(
            OrderItem(tiles = listOf("gibt", "einen", "es", "Balkon"), answer = listOf(2, 0, 1, 3)),
        )

        val step = exercise(body).toSteps("s").filterIsInstance<OrderStep>().single()

        // Answer indexes into the shuffled tiles, so playing it back must rebuild the sentence.
        assertEquals("es gibt einen Balkon", step.answer.joinToString(" ") { step.tiles[it] })
        assertEquals("es gibt einen Balkon", step.solution)
    }

    @Test
    fun `alternative orderings are remapped too`() {
        val body = orderBody(
            OrderItem(
                tiles = listOf("Ich", "wohne", "jetzt", "in Berlin"),
                answer = listOf(0, 1, 2, 3),
                alt = listOf(listOf(2, 1, 0, 3)),
            ),
        )

        val step = exercise(body).toSteps("s").filterIsInstance<OrderStep>().single()

        assertEquals(1, step.alternatives.size)
        assertEquals("jetzt wohne Ich in Berlin", step.alternatives.single().joinToString(" ") { step.tiles[it] })
    }

    @Test
    fun `the shuffle is stable for the same step`() {
        // A tile pool that rearranged itself when a missed step came back would be unfair.
        val body = orderBody(OrderItem(tiles = listOf("a", "b", "c", "d", "e"), answer = listOf(0, 1, 2, 3, 4)))

        val first = exercise(body).toSteps("s").filterIsInstance<OrderStep>().single()
        val second = exercise(body).toSteps("s").filterIsInstance<OrderStep>().single()

        assertEquals(first.tiles, second.tiles)
    }

    @Test
    fun `different steps get different shuffles`() {
        val body = orderBody(
            OrderItem(tiles = listOf("a", "b", "c", "d", "e", "f"), answer = listOf(0, 1, 2, 3, 4, 5)),
            OrderItem(tiles = listOf("a", "b", "c", "d", "e", "f"), answer = listOf(0, 1, 2, 3, 4, 5)),
        )

        val steps = exercise(body).toSteps("s").filterIsInstance<OrderStep>()

        assertTrue("both steps got the same tile order", steps[0].tiles != steps[1].tiles)
    }

    @Test
    fun `a note that just restates the sentence becomes the solution and is dropped`() {
        // The shape 291 corpus items actually have: punctuation is its own tile and the
        // first tile is lowercase, so the join reads "hast du Geschwister ?" while the
        // note is the same sentence written properly. The banner should show the note.
        val body = orderBody(
            OrderItem(
                tiles = listOf("hast", "du", "Geschwister", "?"),
                answer = listOf(0, 1, 2, 3),
                note = "Hast du Geschwister?",
            ),
        )

        val step = exercise(body).toSteps("s").filterIsInstance<OrderStep>().single()

        assertEquals("Hast du Geschwister?", step.solution)
        assertNull(step.note)
    }

    @Test
    fun `without a note the solution falls back to the joined tiles`() {
        val body = orderBody(
            OrderItem(tiles = listOf("hast", "du", "Geschwister", "?"), answer = listOf(0, 1, 2, 3)),
        )

        val step = exercise(body).toSteps("s").filterIsInstance<OrderStep>().single()

        assertEquals("hast du Geschwister ?", step.solution)
        assertNull(step.note)
    }

    @Test
    fun `a note about a different sentence is kept and does not become the solution`() {
        // The restatement check is on the letters, so it must not fire on a note that
        // merely shares some words with the sentence.
        val body = orderBody(
            OrderItem(
                tiles = listOf("Ich", "bin", "hier"),
                answer = listOf(0, 1, 2),
                note = "Ich bin hier gewesen",
            ),
        )

        val step = exercise(body).toSteps("s").filterIsInstance<OrderStep>().single()

        assertEquals("Ich bin hier", step.solution)
        assertEquals("Ich bin hier gewesen", step.note)
    }

    @Test
    fun `a note that explains something is kept`() {
        val body = orderBody(
            OrderItem(
                tiles = listOf("Ich", "bin", "hier"),
                answer = listOf(0, 1, 2),
                note = "verb stays in position 2",
            ),
        )

        assertEquals(
            "verb stays in position 2",
            exercise(body).toSteps("s").filterIsInstance<OrderStep>().single().note,
        )
    }

    @Test
    fun `an answer that does not use every tile is dropped`() {
        val body = orderBody(OrderItem(tiles = listOf("a", "b", "c"), answer = listOf(0, 1)))

        assertTrue(exercise(body).toSteps("s").isEmpty())
    }

    @Test
    fun `a single-tile item is dropped`() {
        assertTrue(exercise(orderBody(OrderItem(tiles = listOf("a"), answer = listOf(0)))).toSteps("s").isEmpty())
    }
}

class OrderGraderTest {

    private fun step(
        tiles: List<String> = listOf("heiße", "Ich", "Anna"),
        answer: List<Int> = listOf(1, 0, 2),
        alternatives: List<List<Int>> = emptyList(),
        note: String? = null,
    ) = OrderStep(
        id = StepId("s#B5#0"),
        exerciseId = "B5",
        title = "t",
        instructions = null,
        instructionsEn = null,
        flags = GradingFlags(),
        tiles = tiles,
        answer = answer,
        alternatives = alternatives,
        solution = "Ich heiße Anna",
        note = note,
    )

    @Test
    fun `the right order is correct`() {
        assertTrue(gradeStep(step(), AnswerState.Sequence(listOf(1, 0, 2))).correct)
    }

    @Test
    fun `a wrong order is wrong and shows the sentence`() {
        val result = gradeStep(step(), AnswerState.Sequence(listOf(0, 1, 2)))

        assertFalse(result.correct)
        assertEquals("heiße Ich Anna", result.items.single().given)
        assertEquals("Ich heiße Anna", result.items.single().expected)
    }

    @Test
    fun `a half-built sentence is wrong rather than partly right`() {
        assertFalse(gradeStep(step(), AnswerState.Sequence(listOf(1, 0))).correct)
    }

    @Test
    fun `nothing placed is wrong`() {
        assertFalse(gradeStep(step(), AnswerState.Sequence()).correct)
    }

    @Test
    fun `an accepted alternative ordering is correct`() {
        val graded = gradeStep(
            step(alternatives = listOf(listOf(2, 1, 0))),
            AnswerState.Sequence(listOf(2, 1, 0)),
        )
        assertTrue(graded.correct)
    }
}
