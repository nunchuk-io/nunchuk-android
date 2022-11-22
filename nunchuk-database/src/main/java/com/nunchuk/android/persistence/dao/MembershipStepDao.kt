package com.nunchuk.android.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.persistence.BaseDao
import com.nunchuk.android.persistence.TABLE_MEMBERSHIP_STEP
import com.nunchuk.android.persistence.entity.MembershipStepEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MembershipStepDao : BaseDao<MembershipStepEntity> {
    @Query("SELECT * FROM $TABLE_MEMBERSHIP_STEP WHERE email = :email AND `plan` = :plan")
    fun getSteps(email: String, plan: MembershipPlan): Flow<List<MembershipStepEntity>>

    @Query("SELECT * FROM $TABLE_MEMBERSHIP_STEP WHERE email = :email AND step = :step")
    suspend fun getStep(email: String, step: MembershipStep): MembershipStepEntity?

    @Query("SELECT * FROM $TABLE_MEMBERSHIP_STEP WHERE email = :email AND master_signer_id = :masterSignerId")
    suspend fun getStepByMasterSignerId(
        email: String,
        masterSignerId: String
    ): MembershipStepEntity?

    @Query("DELETE FROM $TABLE_MEMBERSHIP_STEP WHERE email = :email AND master_signer_id = :masterSignerId")
    suspend fun deleteByMasterSignerId(email: String, masterSignerId: String)

    @Query("DELETE FROM $TABLE_MEMBERSHIP_STEP WHERE email = :email")
    suspend fun deleteStepByEmail(email: String)
}