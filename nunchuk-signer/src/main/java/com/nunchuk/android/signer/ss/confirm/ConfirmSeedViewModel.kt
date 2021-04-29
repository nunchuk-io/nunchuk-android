package com.nunchuk.android.signer.ss.confirm

import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.signer.ss.confirm.ConfirmSeedEvent.ConfirmSeedCompletedEvent
import com.nunchuk.android.signer.ss.confirm.ConfirmSeedEvent.SelectedIncorrectWordEvent
import com.nunchuk.android.signer.ss.create.PHRASE_SEPARATOR
import javax.inject.Inject

internal class ConfirmSeedViewModel @Inject constructor(

) : NunchukViewModel<ConfirmSeedState, ConfirmSeedEvent>() {

    private lateinit var mnemonic: String
    private lateinit var phrases: List<String>

    override val initialState = ConfirmSeedState()

    fun init(mnemonic: String) {
        this.mnemonic = mnemonic
        phrases = this.mnemonic.split(PHRASE_SEPARATOR)
        updateState { copy(groups = phrases.randomPhraseWordGroups()) }
    }

    fun handleContinueEvent() {
        val isAllSelectedCorrect = getState().groups.all { it.isCorrectSelected() }
        if (isAllSelectedCorrect) {
            event(ConfirmSeedCompletedEvent)
        } else {
            event(SelectedIncorrectWordEvent)
        }
    }

    fun updatePhraseWordGroup(phraseWordGroup: PhraseWordGroup) {
        val groups = ArrayList(getState().groups)
        groups.forEachIndexed { index, group ->
            if (group.index == phraseWordGroup.index) {
                groups[index] = phraseWordGroup
            }
        }
        updateState { copy(groups = groups) }
    }

    private fun PhraseWordGroup.isCorrectSelected() = (firstWord.selected && firstWord.correct) ||
            (secondWord.selected && secondWord.correct) ||
            (thirdWord.selected && thirdWord.correct)

}