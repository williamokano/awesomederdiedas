package okano.dev.android.derdiedas.core.exercise

/**
 * Grades a completed step. Pure, so the whole session state machine can be tested on the
 * JVM without a Compose runtime.
 *
 * The website's widgets grade themselves through an imperative `check()` called on a
 * component ref. Here the widget only renders and reports edits; the runner grades with
 * this function. That inversion is what makes the logic testable in isolation.
 */

data class StepResult(
    val items: List<ItemResult>,
    /**
     * Whole-step correctness. A gap-text with 6 of 8 gaps right is neither correct nor
     * wrong in the Duolingo model; treating it as wrong and re-queueing the whole step is
     * the simplest defensible rule, and matches how the website scores a block.
     */
    val correct: Boolean,
) {
    val correctCount: Int get() = items.count { it.correct }
    val scoreableCount: Int get() = items.count { it.scoreable }
}

fun gradeStep(step: SessionStep, answer: AnswerState): StepResult = when (step) {
    is GapTextStep -> gradeGapText(step, answer.asTexts())
    is ChoiceStep -> gradeChoice(step, answer.asChoice())
    is OrderStep -> gradeOrder(step, answer.asSequence())
    is GapBankStep -> gradeGapBank(step, answer.asPlacements())
}

/**
 * Each gap is graded on its own, then the step is right only if every gap is.
 *
 * checkBank compares the placed word exactly, with no normalisation: the learner picked
 * it from a fixed bank rather than typing it, so there is nothing to be lenient about.
 * A gap left empty is simply wrong.
 */
private fun gradeGapBank(step: GapBankStep, answer: AnswerState.Placements): StepResult {
    val items = step.gapKeys.map { key ->
        val expected = step.answers[key].orEmpty()
        val placed = answer.byRef[key]
        ItemResult(
            ref = key,
            correct = expected.any { checkBank(it, placed) },
            given = placed.orEmpty(),
            expected = expected.firstOrNull().orEmpty(),
        )
    }
    return StepResult(items = items, correct = items.all { it.correct })
}

private fun gradeOrder(step: OrderStep, answer: AnswerState.Sequence): StepResult {
    // A half-built sentence is wrong rather than partly right: the exercise is the whole
    // word order, and checkOrder compares the sequence as one thing.
    val correct = answer.order.size == step.tiles.size &&
        checkOrder(step.answer, answer.order, step.alternatives)

    val item = ItemResult(
        ref = step.id.value,
        correct = correct,
        given = answer.order.joinToString(" ") { step.tiles.getOrElse(it) { "" } },
        expected = step.solution,
        note = step.note,
    )
    return StepResult(items = listOf(item), correct = correct)
}

private fun gradeChoice(step: ChoiceStep, answer: AnswerState.Choice): StepResult {
    val chosen = answer.key
    val correct = checkEquality(step.answerKey, chosen)
    // Report the option's text rather than its key: "b" means nothing to a learner.
    fun textOf(key: String?) = step.options.firstOrNull { it.key == key }?.text.orEmpty()

    val item = ItemResult(
        ref = step.answerKey,
        correct = correct,
        given = textOf(chosen),
        expected = textOf(step.answerKey),
        note = step.why,
    )
    return StepResult(items = listOf(item), correct = correct)
}

private fun gradeGapText(step: GapTextStep, answer: AnswerState.Texts): StepResult {
    val items = step.gapKeys.map { key ->
        val expected = step.answers[key].orEmpty()
        val given = answer.byRef[key].orEmpty()
        val check = checkTextDetail(
            expected = expected,
            given = given,
            alts = step.alts[key].orEmpty(),
            flags = step.flags,
        )
        ItemResult(
            ref = key,
            correct = check.correct,
            given = given,
            // The first listed answer is the drilled form, and the one worth showing back.
            expected = expected.firstOrNull().orEmpty(),
            note = check.note,
        )
    }
    return StepResult(items = items, correct = items.all { it.correct })
}
