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

package com.nunchuk.android.core.domain

import android.content.Context
import android.nfc.tech.IsoDep
import com.nunchuk.android.core.domain.utils.NfcFile
import com.nunchuk.android.core.util.NFC_DEFAULT_NAME
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.TapSignerStatus
import com.nunchuk.android.nativelib.NunchukNativeSdk
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

@Suppress("BlockingMethodInNonBlockingContext")
class SetupTapSignerUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    @ApplicationContext private val context: Context,
    private val nunchukNativeSdk: NunchukNativeSdk,
    waitAutoCardUseCase: WaitAutoCardUseCase
) : BaseNfcUseCase<SetupTapSignerUseCase.Data, SetupTapSignerUseCase.Result>(dispatcher, waitAutoCardUseCase) {
    private val tapSignerStatus = AtomicReference<TapSignerStatus?>(null)

    override suspend fun executeNfc(parameters: Data): Result {
        if (tapSignerStatus.get() == null) {
            val tapStatus = nunchukNativeSdk.setupTapSigner(parameters.isoDep, parameters.oldCvc, parameters.newCvc, parameters.chainCode)
            tapSignerStatus.set(tapStatus)
        }
        val masterSigner = nunchukNativeSdk.createTapSigner(parameters.isoDep, parameters.newCvc, NFC_DEFAULT_NAME)
        val filePath = NfcFile.storeBackupKeyToFile(context, tapSignerStatus.get()!!)
        return Result(filePath, masterSigner)
    }

    class Data(isoDep: IsoDep, val oldCvc: String, val newCvc: String, val chainCode: String) : BaseNfcUseCase.Data(isoDep)
    class Result(val backUpKeyPath: String, val masterSigner: MasterSigner)
}