package com.nunchuk.android.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.MembershipStepInfo
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.persistence.TABLE_MEMBERSHIP_STEP
import com.nunchuk.android.type.Chain

@Entity(tableName = TABLE_MEMBERSHIP_STEP)
data class MembershipStepEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "chat_id")
    val chatId: String,
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
    val verifyType: VerifyType = VerifyType.NONE,
    @ColumnInfo(name = "chain")
    val chain: Chain,
    @ColumnInfo(name = "plan")
    val plan: MembershipPlan
)

fun MembershipStepEntity.toModel() = MembershipStepInfo(
    id = id,
    step = step,
    masterSignerId = masterSignerId,
    verifyType = verifyType,
    keyIdInServer = keyIdInServer,
    extraData = extraJson,
    plan = plan
)