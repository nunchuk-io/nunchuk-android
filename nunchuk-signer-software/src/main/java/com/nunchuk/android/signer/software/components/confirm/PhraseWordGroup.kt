package com.nunchuk.android.signer.software.components.confirm

import timber.log.Timber
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
    Timber.d("random3LastPhraseWords($this)")
    val usedIndexes = ArrayList<Int>()
    val confirmWordIndexes = mutableSetOf<Int>()
    val result = ArrayList<PhraseWordGroup>(NUMBER_WORD_TO_CONFIRM)
    // Random two first confirmation words
    do {
        confirmWordIndexes.add(Random.nextInt(0, size - 1))
    } while (confirmWordIndexes.size < 2)

    // Random #3 confirmation word (should be a word from words #22-24)
    do {
        confirmWordIndexes.add(Random.nextInt(size - 3, size))
    } while (confirmWordIndexes.size < 3)

    usedIndexes.addAll(confirmWordIndexes)
    confirmWordIndexes.mapTo(result) {
        randomPhraseWordGroup(it, usedIndexes)
    }

    result.sortBy(PhraseWordGroup::index)
    return result
}

internal fun List<String>.randomPhraseWordGroup(groupIndex: Int, usedIndexes: ArrayList<Int>): PhraseWordGroup {
    return when (Random.nextInt(0, NUMBER_WORD_TO_CONFIRM)) {
        0 -> PhraseWordGroup(
            index = groupIndex,
            firstWord = PhraseWord(this[groupIndex], true),
            secondWord = randomPhraseWord(usedIndexes),
            thirdWord = randomPhraseWord(usedIndexes)
        )
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

internal fun List<String>.randomNotDuplicatedNum(size: Int, usedIndexes: ArrayList<Int>): Int {
    Timber.d("used(${this.filterIndexed { index, _ -> index in usedIndexes }})")
    var randomNum: Int
    do {
        randomNum = Random.nextInt(0, size - 1)
    } while (usedIndexes.contains(randomNum))
    usedIndexes.add(randomNum)
    Timber.d("random(${this[randomNum]})")
    return randomNum
}

internal fun List<String>.randomPhraseWord(usedIndexes: ArrayList<Int>): PhraseWord {
    return PhraseWord(this[randomNotDuplicatedNum(size, usedIndexes)])
}

internal const val NUMBER_WORD_TO_CONFIRM = 3