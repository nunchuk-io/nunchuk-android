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
    val usedIndexes = ArrayList<Int>()
    val result = ArrayList<PhraseWordGroup>(NUMBER_WORD_TO_CONFIRM)
    for (i in 0 until NUMBER_WORD_TO_CONFIRM) {
        val group = randomPhraseWordGroup(usedIndexes)
        result.add(group)
    }
    result.sortBy(PhraseWordGroup::index)
    return result
}

internal fun List<String>.randomPhraseWordGroup(usedIndexes: ArrayList<Int>): PhraseWordGroup {
    val randomGroupIndex = randomNum(size, usedIndexes)
    return when (Random.nextInt(0, NUMBER_WORD_TO_CONFIRM)) {
        0 -> {
            PhraseWordGroup(
                index = randomGroupIndex,
                firstWord = PhraseWord(this[randomGroupIndex], true),
                secondWord = randomPhraseWord(usedIndexes),
                thirdWord = randomPhraseWord(usedIndexes)
            )
        }
        1 -> PhraseWordGroup(
            index = randomGroupIndex,
            firstWord = randomPhraseWord(usedIndexes),
            secondWord = PhraseWord(this[randomGroupIndex], true),
            thirdWord = randomPhraseWord(usedIndexes)
        )
        else -> PhraseWordGroup(
            index = randomGroupIndex,
            firstWord = randomPhraseWord(usedIndexes),
            secondWord = randomPhraseWord(usedIndexes),
            thirdWord = PhraseWord(this[randomGroupIndex], true)
        )
    }
}

internal fun randomNum(size: Int, usedIndexes: ArrayList<Int>): Int {
    var randomNum = Random.nextInt(0, size - 1)
    while (usedIndexes.contains(randomNum)) {
        randomNum = Random.nextInt(0, size - 1)
    }
    usedIndexes.add(randomNum)
    return randomNum
}

internal fun List<String>.randomPhraseWord(usedIndexes: ArrayList<Int>) = PhraseWord(this[randomNum(size, usedIndexes)])

internal const val NUMBER_WORD_TO_CONFIRM = 3