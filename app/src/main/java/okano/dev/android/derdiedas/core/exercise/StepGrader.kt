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
