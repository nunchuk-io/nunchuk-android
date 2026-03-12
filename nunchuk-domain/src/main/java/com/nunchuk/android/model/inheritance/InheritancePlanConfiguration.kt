package com.nunchuk.android.model.inheritance

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class InheritancePlanFallbackPolicy(
    val type: String,
    val inactivityInterval: String? = null,
    val inactivityIntervalCount: Int? = null,
    val fallbackTimeMillis: Long? = null,
) : Parcelable

@Parcelize
data class InheritancePlanExpandedInstallment(
    val index: Int,
    val withdrawalTimeMillis: Long,
    val allocationPercentage: Int,
) : Parcelable

@Parcelize
data class InheritancePlanStage(
    val amountPerReleasePercentage: Int,
    val repeatInterval: String,
    val repeatIntervalCount: Int,
    val totalStageAllocationPercentage: Int,
    val firstWithdrawalTimeMillis: Long,
    val expandedInstallments: List<InheritancePlanExpandedInstallment> = emptyList(),
) : Parcelable

@Parcelize
data class InheritancePlanBeneficiary(
    val email: String,
    val assetPercentage: Int,
    val magic: String,
    val note: String,
    val bufferPeriodId: String? = null,
    val bufferApplyOn: String? = null,
    val stages: List<InheritancePlanStage> = emptyList(),
) : Parcelable
