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

package com.nunchuk.android.main.components.tabs.services.keyrecovery.backupdownload

import com.nunchuk.android.core.signer.SignerModel

sealed class BackupDownloadEvent {
    data class Loading(val isLoading: Boolean) : BackupDownloadEvent()
    data class ProcessFailure(val message: String) : BackupDownloadEvent()
    data class ImportTapsignerSuccess(val signer: SignerModel) : BackupDownloadEvent()
}

data class BackupDownloadState(
    val keyName: String = "",
    val error: String = "",
    val password: String = ""
)