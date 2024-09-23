/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
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

package com.nunchuk.android.wallet.components.backup

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.domain.wallet.GetWalletBsmsUseCase
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.Result
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.usecase.CreateShareFileUseCase
import com.nunchuk.android.wallet.components.backup.BackupWalletEvent.Failure
import com.nunchuk.android.wallet.components.backup.BackupWalletEvent.Success
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
internal class BackupWalletViewModel @Inject constructor(
    private val createShareFileUseCase: CreateShareFileUseCase,
    private val getWalletBsmsUseCase: GetWalletBsmsUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : NunchukViewModel<Unit, BackupWalletEvent>() {

    override val initialState = Unit

    private lateinit var wallet: Wallet

    fun init(wallet: Wallet) {
        this.wallet = wallet
    }

    fun handleBackupDescriptorEvent() {
        viewModelScope.launch {
            when (val event = createShareFileUseCase.execute("${wallet.id}.bsms")) {
                is Result.Success -> exportWallet(event.data)
                is Result.Error -> {
                    event(Failure(event.exception.message.orUnknownError()))
                }
            }
        }
    }

    private fun exportWallet(filePath: String) {
        viewModelScope.launch {
            getWalletBsmsUseCase(wallet).onSuccess {
                withContext(ioDispatcher) {
                    File(filePath).writeText(it)
                }
                event(Success(filePath))
            }.onFailure {
                event(Failure(it.message.orUnknownError()))
            }
        }
    }

}