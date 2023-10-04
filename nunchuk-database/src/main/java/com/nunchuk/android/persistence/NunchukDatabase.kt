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

package com.nunchuk.android.persistence

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nunchuk.android.persistence.dao.*
import com.nunchuk.android.persistence.entity.*
import com.nunchuk.android.persistence.spec.AutoMigrationSpec12to13

@Database(
    entities = [
        ContactEntity::class,
        SyncFileEntity::class,
        MembershipStepEntity::class,
        SyncEventEntity::class,
        HandledEventEntity::class,
        AssistedWalletEntity::class,
        RequestAddKeyEntity::class,
        GroupEntity::class,
        AlertEntity::class,
        KeyHealthStatusEntity::class,
        DummyTransactionEntity::class,
        GroupChatEntity::class,
    ],
    version = DATABASE_VERSION,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 8, to = 9),
        AutoMigration(from = 9, to = 10),
        AutoMigration(from = 10, to = 11),
        AutoMigration(from = 11, to = 12),
        AutoMigration(from = 12, to = 13, AutoMigrationSpec12to13::class),
        AutoMigration(from = 13, to = 14),
        AutoMigration(from = 14, to = 15),
        AutoMigration(from = 15, to = 16),
    ]
)
@TypeConverters(Converters::class)
internal abstract class NunchukDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun syncFileDao(): SyncFileDao
    abstract fun membershipDao(): MembershipStepDao
    abstract fun syncEventDao(): SyncEventDao
    abstract fun handledEventDao(): HandledEventDao
    abstract fun assistedWalletDao(): AssistedWalletDao
    abstract fun requestAddKeyDao(): RequestAddKeyDao
    abstract fun groupDao(): GroupDao
    abstract fun alertDao(): AlertDao
    abstract fun keyHealthStatusDao(): KeyHealthStatusDao
    abstract fun dummyTransactionDao(): DummyTransactionDao
    abstract fun groupChatDao(): GroupChatDao
}