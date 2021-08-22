package com.nunchuk.android.signer.software.components.confirm

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

internal fun List<String>.random3LastPhraseWords(): List<PhraseWordGroup> {
    val usedIndexes = ArrayList<Int>()
    val result = ArrayList<PhraseWordGroup>(NUMBER_WORD_TO_CONFIRM)
    (0 until (NUMBER_WORD_TO_CONFIRM))
        .map { size - NUMBER_WORD_TO_CONFIRM + it }
        .mapTo(result) { randomPhraseWordGroup(it, usedIndexes) }
    result.sortBy(PhraseWordGroup::index)
    return result
}

internal fun List<String>.randomPhraseWordGroup(groupIndex: Int, usedIndexes: ArrayList<Int>): PhraseWordGroup {
    return when (Random.nextInt(0, NUMBER_WORD_TO_CONFIRM)) {
        0 -> {
            PhraseWordGroup(
                index = groupIndex,
                firstWord = PhraseWord(this[groupIndex], true),
                secondWord = randomPhraseWord(usedIndexes),
                thirdWord = randomPhraseWord(usedIndexes)
            )
        }
        1 -> PhraseWordGroup(
            index = groupIndex,
            firstWord = randomPhraseWord(usedIndexes),
            secondWord = PhraseWord(this[groupIndex], true),
            thirdWord = randomPhraseWord(usedIndexes)
        )
        else -> PhraseWordGroup(
            index = groupIndex,
            firstWord = randomPhraseWord(usedIndexes),
            secondWord = randomPhraseWord(usedIndexes),
            thirdWord = PhraseWord(this[groupIndex], true)
        )
    }
}

internal fun randomNotDuplicatedNum(size: Int, usedIndexes: ArrayList<Int>): Int {
    var randomNum = Random.nextInt(0, size - 1)
    while (usedIndexes.contains(randomNum)) {
        randomNum = Random.nextInt(0, size - 1)
    }
    usedIndexes.add(randomNum)
    return randomNum
}

internal fun List<String>.randomPhraseWord(usedIndexes: ArrayList<Int>) = PhraseWord(this[randomNotDuplicatedNum(size, usedIndexes)])

internal const val NUMBER_WORD_TO_CONFIRM = 3