package com.nunchuk.android.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nunchuk.android.persistence.TABLE_SYNC_FILE

@Entity(tableName = TABLE_SYNC_FILE)
data class SyncFileEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int,

    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "action")
    val action: String,

    @ColumnInfo(name = "file_name")
    val fileName: String?,

    @ColumnInfo(name = "file_url")
    val fileUrl: String?,

    @ColumnInfo(name = "file_json_info")
    val fileJsonInfo: String,

    @ColumnInfo(name = "file_mine_type")
    val fileMineType: String?,

    @ColumnInfo(name = "file_length")
    val fileLength: Int?,

    @ColumnInfo(name = "file_data", typeAffinity = ColumnInfo.BLOB)
    val fileData: ByteArray?
)