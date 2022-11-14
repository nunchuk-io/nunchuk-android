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

package com.nunchuk.android.signer.software.components.primarykey.passphrase

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.domain.CheckPassphrasePrimaryKeyUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch

class PKeyEnterPassphraseViewModel @AssistedInject constructor(
    @Assisted private val args: PKeyEnterPassphraseArgs,
    private val checkPassphrasePrimaryKeyUseCase: CheckPassphrasePrimaryKeyUseCase
) : NunchukViewModel<PKeyEnterPassphraseState, PKeyEnterPassphraseEvent>() {

    override val initialState = PKeyEnterPassphraseState(args = args)

    fun updatePassphrase(passphrase: String) {
        updateState { copy(passphrase = passphrase) }
    }

    fun checkPassphrase() = viewModelScope.launch {
        setEvent(PKeyEnterPassphraseEvent.LoadingEvent(true))
        val result = checkPassphrasePrimaryKeyUseCase(
            CheckPassphrasePrimaryKeyUseCase.Param(
                mnemonic = args.mnemonic,
                passphrase = getState().passphrase
            )
        )
        setEvent(PKeyEnterPassphraseEvent.LoadingEvent(false))
        if (result.isSuccess) {
            result.getOrThrow()?.let {
                setEvent(PKeyEnterPassphraseEvent.CheckPassphraseSuccess(it))
            }
        } else {
            setEvent(PKeyEnterPassphraseEvent.CheckPassphraseError)
        }
    }

    @AssistedFactory
    internal interface Factory {
        fun create(args: PKeyEnterPassphraseArgs): PKeyEnterPassphraseViewModel
    }
}