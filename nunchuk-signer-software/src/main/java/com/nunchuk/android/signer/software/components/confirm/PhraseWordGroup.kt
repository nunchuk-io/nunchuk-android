/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

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