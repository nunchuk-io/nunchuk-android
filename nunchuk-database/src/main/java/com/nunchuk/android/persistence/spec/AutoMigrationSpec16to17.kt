package com.nunchuk.android.persistence.spec

import androidx.room.DeleteTable
import androidx.room.migration.AutoMigrationSpec

@DeleteTable(tableName = "group_chat")
class AutoMigrationSpec16to17 : AutoMigrationSpec