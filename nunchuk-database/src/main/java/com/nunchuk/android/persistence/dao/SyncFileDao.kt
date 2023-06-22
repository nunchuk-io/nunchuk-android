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

package com.nunchuk.android.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import com.nunchuk.android.persistence.BaseDao
import com.nunchuk.android.persistence.TABLE_SYNC_FILE
import com.nunchuk.android.persistence.entity.SyncFileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncFileDao : BaseDao<SyncFileEntity> {

    @Query("SELECT * FROM $TABLE_SYNC_FILE WHERE user_id = :userId")
    fun getSyncFiles(userId: String): Flow<List<SyncFileEntity>>

    @Query("DELETE FROM $TABLE_SYNC_FILE WHERE id IN (:syncFileIds)")
    fun deleteSyncFiles(syncFileIds: List<String>)

    @Query("DELETE FROM $TABLE_SYNC_FILE WHERE file_json_info IN (:syncFileInfo)")
    fun deleteSyncFilesByInfo(syncFileInfo: List<String>)

}
