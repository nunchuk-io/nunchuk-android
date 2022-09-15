package com.nunchuk.android.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.MembershipStepInfo
import com.nunchuk.android.persistence.TABLE_MEMBERSHIP_STEP

@Entity(tableName = TABLE_MEMBERSHIP_STEP)
data class MembershipStepEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "email")
    val email: String,
    @ColumnInfo(name = "step")
    val step: MembershipStep,
    @ColumnInfo(name = "master_signer_id")
    val masterSignerId: String = "",
    @ColumnInfo(name = "key_id_in_server")
    val keyIdInServer: String = "",
    @ColumnInfo(name = "key_id_check_sum")
    val checkSum: String = "",
    @ColumnInfo(name = "extra_json_data")
    val extraJson: String = "",
    @ColumnInfo(name = "is_verify")
    val isVerify: Boolean = false
)

fun MembershipStepEntity.toModel() = MembershipStepInfo(
    id = id,
    step = step,
    masterSignerId = masterSignerId,
    isVerify = isVerify,
    keyIdInServer = keyIdInServer,
    extraData = extraJson
)