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

package com.nunchuk.android.core.domain.wallet

import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.type.DescriptorPath
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import java.io.File
import java.io.FileWriter
import javax.inject.Inject

class GetWalletDescriptorUseCase @Inject constructor(
    private val nunchukNativeSdk: NunchukNativeSdk,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UseCase<GetWalletDescriptorUseCase.Param, String>(ioDispatcher) {

    override suspend fun execute(parameters: Param): String {
        try {
            val file = File(parameters.filePath)
            val fileWriter = FileWriter(file)
            val descriptor = nunchukNativeSdk.getWalletDescriptor(
                walletId = parameters.walletId, 
                descriptorPath = DescriptorPath.EXTERNAL_ALL
            )
            fileWriter.write(descriptor)
            fileWriter.close()
            return parameters.filePath
        } catch (e: Exception) {
            throw IllegalStateException(e)
        }
    }

    class Param(val walletId: String, val filePath: String)
} 