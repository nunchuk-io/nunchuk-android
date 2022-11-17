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

package com.nunchuk.android.core.data.model

import com.nunchuk.android.persistence.entity.SyncFileEntity


data class SyncFileModel(
    val userId: String,
    val action: String,
    val fileName: String? = null,
    val fileUrl: String? = null,
    val fileJsonInfo: String,
    val fileMineType: String? = null,
    val fileLength: Int? = null,
    val fileData: ByteArray? = null
)

internal fun SyncFileModel.toEntity() = SyncFileEntity(
    id = 0,
    userId = userId,
    action = action,
    fileName = fileName,
    fileJsonInfo = fileJsonInfo,
    fileUrl = fileUrl,
    fileData = fileData,
    fileMineType = fileMineType,
    fileLength = fileLength
)

internal fun SyncFileEntity.toModel() = SyncFileModel(
    userId = userId,
    action = action,
    fileName = fileName,
    fileJsonInfo = fileJsonInfo,
    fileUrl = fileUrl,
    fileData = fileData,
    fileMineType = fileMineType,
    fileLength = fileLength
)