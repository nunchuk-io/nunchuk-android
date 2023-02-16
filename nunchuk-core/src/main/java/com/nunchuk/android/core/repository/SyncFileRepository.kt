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

package com.nunchuk.android.core.repository

import com.nunchuk.android.core.data.model.SyncFileModel
import com.nunchuk.android.core.data.model.toEntity
import com.nunchuk.android.core.data.model.toModel
import com.nunchuk.android.persistence.dao.SyncFileDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface SyncFileRepository {

    fun getSyncFiles(userId: String): Flow<List<SyncFileModel>>
    fun deleteSyncFile(model: SyncFileModel)
    suspend fun createSyncFile(model: SyncFileModel)
    suspend fun updateSyncFile(model: SyncFileModel)
}

internal class SyncFileRepositoryImpl @Inject constructor(
    private val syncFileDao: SyncFileDao
) : SyncFileRepository {

    override fun getSyncFiles(userId: String) =
        syncFileDao.getSyncFiles(userId).map { entities -> entities.map { entity -> entity.toModel() } }

    override fun deleteSyncFile(model: SyncFileModel) {
        syncFileDao.deleteSyncFilesByInfo(listOf(model.fileJsonInfo))
    }

    override suspend fun createSyncFile(model: SyncFileModel) {
        syncFileDao.insert(model.toEntity())
    }

    override suspend fun updateSyncFile(model: SyncFileModel) {
        syncFileDao.update(model.toEntity())
    }
}

