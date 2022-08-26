package com.nunchuk.android.signer.software.components.primarykey.intro.replace

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.account.PrimaryKeySignerInfoHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class PKeyReplaceKeyIntroViewModel @Inject constructor(
    private val primaryKeySignerInfoHolder: PrimaryKeySignerInfoHolder
) : NunchukViewModel<Unit, PKeyReplaceKeyIntroEvent>() {
    override val initialState: Unit = Unit

    fun checkNeedPassphraseSent() {
        setEvent(PKeyReplaceKeyIntroEvent.LoadingEvent(true))
        viewModelScope.launch {
            val isNeeded = primaryKeySignerInfoHolder.isNeedPassphraseSent()
            setEvent(PKeyReplaceKeyIntroEvent.LoadingEvent(false))
            setEvent(PKeyReplaceKeyIntroEvent.CheckNeedPassphraseSent(isNeeded))
        }
    }
}