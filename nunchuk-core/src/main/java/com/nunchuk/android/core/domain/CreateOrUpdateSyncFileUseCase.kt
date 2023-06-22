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

import com.nunchuk.android.core.data.model.SyncFileModel
import com.nunchuk.android.core.repository.SyncFileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface CreateOrUpdateSyncFileUseCase {
    fun execute(syncFileModel: SyncFileModel): Flow<Unit>
}

internal class CreateOrUpdateSyncFileUseCaseImpl @Inject constructor(
    private val syncFileRepository: SyncFileRepository
) : CreateOrUpdateSyncFileUseCase {

    override fun execute(syncFileModel: SyncFileModel) = syncFileRepository.getSyncFiles(syncFileModel.userId)
        .map { existedSyncFiles->
            if (existedSyncFiles.any { file -> file.fileJsonInfo == syncFileModel.fileJsonInfo }) {
                syncFileRepository.updateSyncFile(syncFileModel)
            } else {
                syncFileRepository.createSyncFile(syncFileModel)
            }
        }

}
