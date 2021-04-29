package com.nunchuk.android.signer.ss.confirm

import kotlin.random.Random

data class PhraseWordGroup(
    val index: Int,
    val firstWord: PhraseWord,
    val secondWord: PhraseWord,
    val thirdWord: PhraseWord
)

data class PhraseWord(
    val word: String,
    val correct: Boolean = false,
    val selected: Boolean = false
)

internal fun List<String>.randomPhraseWordGroups(): List<PhraseWordGroup> {
    val src = ArrayList(this)
    val result = ArrayList<PhraseWordGroup>(NUMBER_WORD_TO_CONFIRM)
    for (i in 0 until NUMBER_WORD_TO_CONFIRM) {
        val group = src.randomPhraseWordGroup()
        result.add(group)
    }
    result.sortBy(PhraseWordGroup::index)
    return result
}

internal fun List<String>.randomPhraseWordGroup(): PhraseWordGroup {
    val src = ArrayList(this)
    val randomGroupIndex = Random.nextInt(0, size - 1)
    return when (Random.nextInt(0, NUMBER_WORD_TO_CONFIRM - 1)) {
        0 -> {
            PhraseWordGroup(
                index = randomGroupIndex,
                firstWord = PhraseWord(src[randomGroupIndex], true),
                secondWord = src.randomPhraseWord(),
                thirdWord = src.randomPhraseWord()
            )
        }
        1 -> PhraseWordGroup(
            index = randomGroupIndex,
            firstWord = src.randomPhraseWord(),
            secondWord = PhraseWord(src[randomGroupIndex], true),
            thirdWord = src.randomPhraseWord()
        )
        else -> PhraseWordGroup(
            index = randomGroupIndex,
            firstWord = randomPhraseWord(),
            secondWord = randomPhraseWord(),
            thirdWord = PhraseWord(src[randomGroupIndex], true)
        )
    }
}

internal fun List<String>.randomPhraseWord() = PhraseWord(this[Random.nextInt(0, size - 1)])

internal const val NUMBER_WORD_TO_CONFIRM = 3