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