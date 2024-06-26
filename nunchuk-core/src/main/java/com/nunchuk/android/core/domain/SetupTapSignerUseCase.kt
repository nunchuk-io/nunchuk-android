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

package com.nunchuk.android.core.domain

import android.nfc.tech.IsoDep
import com.nunchuk.android.core.domain.utils.NfcFileManager
import com.nunchuk.android.core.util.NFC_DEFAULT_NAME
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.TapSignerStatus
import com.nunchuk.android.nativelib.NunchukNativeSdk
import kotlinx.coroutines.CoroutineDispatcher
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

@Suppress("BlockingMethodInNonBlockingContext")
class SetupTapSignerUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
    private val nfcFileManager: NfcFileManager,
    waitAutoCardUseCase: WaitAutoCardUseCase
) : BaseNfcUseCase<SetupTapSignerUseCase.Data, SetupTapSignerUseCase.Result>(dispatcher, waitAutoCardUseCase) {
    private val tapSignerStatus = AtomicReference<TapSignerStatus?>(null)
    private var state: CardSetupState = CardSetupState.UNINITIATED

    override suspend fun executeNfc(parameters: Data): Result {
        if (state == CardSetupState.UNINITIATED) {
            nunchukNativeSdk.initTapSigner(
                isoDep = parameters.isoDep,
                oldCvc = parameters.oldCvc,
                chainCode = parameters.chainCode
            )
            state = CardSetupState.NOT_BACKUP
        }
        if (state == CardSetupState.NOT_BACKUP) {
            tapSignerStatus.set(
                nunchukNativeSdk.getBackupTapSignerKey(
                    isoDep = parameters.isoDep,
                    cvc = parameters.oldCvc,
                    masterSignerId = ""
                )
            )
            state = CardSetupState.NOT_CHANGED_CVC
        }
        if (state == CardSetupState.NOT_CHANGED_CVC) {
            val changed = nunchukNativeSdk.changeCvcTapSigner(
                isoDep = parameters.isoDep,
                oldCvc = parameters.oldCvc,
                newCvc = parameters.newCvc,
                masterSignerId = ""
            )
            if (changed) {
                state = CardSetupState.NOT_CREATED_KEY
            }
        }
        if (state == CardSetupState.NOT_CREATED_KEY) {
            val masterSigner =
                nunchukNativeSdk.createTapSigner(parameters.isoDep, parameters.newCvc, parameters.name, false)
            if (tapSignerStatus.get()?.backupKey?.isEmpty() == true) throw IllegalArgumentException("Can not get back up key")
            val filePath = nfcFileManager.storeBackupKeyToFile(tapSignerStatus.get()!!)
            state = CardSetupState.SUCCESS
            return Result(filePath, masterSigner)
        }
        throw RuntimeException("Can not setup Tapsigner")
    }

    class Data(
        isoDep: IsoDep,
        val oldCvc: String,
        val newCvc: String,
        val chainCode: String,
        val name: String = NFC_DEFAULT_NAME
    ) : BaseNfcUseCase.Data(isoDep)

    class Result(val backUpKeyPath: String, val masterSigner: MasterSigner)
}

internal enum class CardSetupState {
    UNINITIATED, NOT_BACKUP, NOT_CHANGED_CVC, NOT_CREATED_KEY, SUCCESS
}