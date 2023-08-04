package com.nunchuk.android.persistence.spec

import androidx.room.DeleteColumn
import androidx.room.migration.AutoMigrationSpec

@DeleteColumn(tableName = "assisted_wallet", columnName = "is_register_coldcard")
@DeleteColumn(tableName = "assisted_wallet", columnName = "is_register_airgap")
class AutoMigrationSpec12to13 : AutoMigrationSpec