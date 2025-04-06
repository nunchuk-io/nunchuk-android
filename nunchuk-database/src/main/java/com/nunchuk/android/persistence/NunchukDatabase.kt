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
import com.nunchuk.android.persistence.dao.AlertDao
import com.nunchuk.android.persistence.dao.AssistedWalletDao
import com.nunchuk.android.persistence.dao.ContactDao
import com.nunchuk.android.persistence.dao.DummyTransactionDao
import com.nunchuk.android.persistence.dao.ElectrumServerDao
import com.nunchuk.android.persistence.dao.GroupDao
import com.nunchuk.android.persistence.dao.HandledEventDao
import com.nunchuk.android.persistence.dao.KeyHealthStatusDao
import com.nunchuk.android.persistence.dao.MembershipStepDao
import com.nunchuk.android.persistence.dao.RequestAddKeyDao
import com.nunchuk.android.persistence.dao.SavedAddressDao
import com.nunchuk.android.persistence.dao.SyncEventDao
import com.nunchuk.android.persistence.dao.SyncFileDao
import com.nunchuk.android.persistence.dao.WalletOrderDao
import com.nunchuk.android.persistence.entity.AlertEntity
import com.nunchuk.android.persistence.entity.AssistedWalletEntity
import com.nunchuk.android.persistence.entity.ContactEntity
import com.nunchuk.android.persistence.entity.DummyTransactionEntity
import com.nunchuk.android.persistence.entity.ElectrumServerEntity
import com.nunchuk.android.persistence.entity.GroupEntity
import com.nunchuk.android.persistence.entity.HandledEventEntity
import com.nunchuk.android.persistence.entity.KeyHealthStatusEntity
import com.nunchuk.android.persistence.entity.MembershipStepEntity
import com.nunchuk.android.persistence.entity.RequestAddKeyEntity
import com.nunchuk.android.persistence.entity.SavedAddressEntity
import com.nunchuk.android.persistence.entity.SyncEventEntity
import com.nunchuk.android.persistence.entity.SyncFileEntity
import com.nunchuk.android.persistence.entity.WalletOrderEntity
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
        ElectrumServerEntity::class,
        SavedAddressEntity::class,
        WalletOrderEntity::class,
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
        AutoMigration(from = 16, to = 17),
        AutoMigration(from = 17, to = 18),
        AutoMigration(from = 18, to = 19),
        AutoMigration(from = 20, to = 21),
        AutoMigration(from = 21, to = 22),
        AutoMigration(from = 22, to = 23),
        AutoMigration(from = 23, to = 24),
        AutoMigration(from = 24, to = 25),
        AutoMigration(from = 25, to = 26),
        AutoMigration(from = 26, to = 27),
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
    abstract fun savedAddressDao(): SavedAddressDao
    abstract fun keyHealthStatusDao(): KeyHealthStatusDao
    abstract fun dummyTransactionDao(): DummyTransactionDao
    abstract fun electrumServerDao(): ElectrumServerDao
    abstract fun walletOrderDao(): WalletOrderDao
}