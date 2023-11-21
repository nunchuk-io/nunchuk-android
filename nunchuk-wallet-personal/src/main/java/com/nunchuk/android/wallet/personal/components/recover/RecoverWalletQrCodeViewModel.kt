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

package com.nunchuk.android.wallet.personal.components.recover

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.domain.wallet.ParseKeystoneWalletUseCase
import com.nunchuk.android.usecase.ImportKeystoneWalletUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class RecoverWalletQrCodeViewModel @Inject constructor(
    private val importKeystoneWalletUseCase: ImportKeystoneWalletUseCase,
    private val parseKeystoneWalletUseCase: ParseKeystoneWalletUseCase,
) : NunchukViewModel<Unit, RecoverWalletQrCodeEvent>() {

    private var isProcessing = false

    private val qrDataList = HashSet<String>()

    override val initialState = Unit

    fun updateQRCode(isParseOnly: Boolean, qrData: String, description: String) {
        qrDataList.add(qrData)
        if (!isProcessing) {
            if (isParseOnly) {
                viewModelScope.launch {
                    parseKeystoneWalletUseCase(qrDataList.toList())
                        .onSuccess {
                            setEvent(RecoverWalletQrCodeEvent.ImportQRCodeSuccess(it))
                        }
                }
            } else {
                viewModelScope.launch {
                    importKeystoneWalletUseCase.execute(
                        description = description,
                        qrData = qrDataList.toList()
                    ).onStart { isProcessing = true }
                        .flowOn(IO)
                        .onException { }
                        .flowOn(Main)
                        .onCompletion { isProcessing = false }
                        .collect { event(RecoverWalletQrCodeEvent.ImportQRCodeSuccess(it)) }
                }
            }
        }
    }

}