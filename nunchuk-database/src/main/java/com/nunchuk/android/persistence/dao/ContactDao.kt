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

package com.nunchuk.android.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import com.nunchuk.android.persistence.BaseDao
import com.nunchuk.android.persistence.TABLE_CONTACT
import com.nunchuk.android.persistence.entity.ContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao : BaseDao<ContactEntity> {

    @Query("SELECT * FROM $TABLE_CONTACT WHERE account_id = :accountId")
    fun getContacts(accountId: String): Flow<List<ContactEntity>>

    @Query("DELETE FROM $TABLE_CONTACT WHERE id IN (:contactIds) AND account_id = :accountId")
    fun deleteItems(accountId: String, contactIds: List<String>)

    @Query("SELECT * FROM $TABLE_CONTACT WHERE account_id = :accountId AND chat_id = :chatId")
    suspend fun getContact(accountId: String, chatId: String): ContactEntity?
}
