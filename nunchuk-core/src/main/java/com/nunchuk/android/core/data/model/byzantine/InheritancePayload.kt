package com.nunchuk.android.core.data.model.byzantine

import com.google.gson.annotations.SerializedName
import com.nunchuk.android.core.data.model.inheritance.InheritanceBeneficiaryRequest
import com.nunchuk.android.core.data.model.inheritance.InheritanceFallbackPolicyRequest
import com.nunchuk.android.core.data.model.inheritance.InheritanceStageRequest
import com.nunchuk.android.core.data.model.membership.InheritanceNotificationPreferencesDto
import com.nunchuk.android.core.data.model.membership.PeriodResponse
import com.nunchuk.android.core.mapper.toInheritanceNotificationSettings
import com.nunchuk.android.core.mapper.toPeriod
import com.nunchuk.android.core.util.orFalse
import com.nunchuk.android.model.Period
import com.nunchuk.android.model.inheritance.InheritancePlanBeneficiary
import com.nunchuk.android.model.inheritance.InheritancePlanFallbackPolicy
import com.nunchuk.android.model.inheritance.InheritancePlanStage
import com.nunchuk.android.model.inheritance.InheritanceNotificationSettings

class InheritancePayloadDto(
    @SerializedName("group_id")
    val groupId: String? = null,
    @SerializedName("wallet_id")
    val walletId: String? = null,
    @SerializedName("wallet_local_id")
    val walletLocalId: String? = null,
    @SerializedName("old_data")
    val oldData: InheritanceDataExtendedDto? = null,
    @SerializedName("new_data")
    val newData: InheritanceDataExtendedDto? = null,
)

class InheritanceDataExtendedDto(
    @SerializedName("wallet")
    val walletId: String? = null,
    @SerializedName("note")
    val note: String? = null,
    @SerializedName("notification_emails")
    val notificationEmails: List<String>? = null,
    @SerializedName("notify_today")
    val notifyToday: Boolean? = null,
    @SerializedName("activation_time_milis")
    val activationTimeMilis: Long? = null,
    @SerializedName("group_id")
    val groupId: String? = null,
    @SerializedName("buffer_period")
    val bufferPeriod: PeriodResponse.Data? = null,
    @SerializedName("notification_preferences")
    val notificationPreferences: InheritanceNotificationPreferencesDto? = null,
    @SerializedName("timezone")
    val timezone: String? = null,
    @SerializedName("distribution_method")
    val distributionMethod: String? = null,
    @SerializedName("beneficiary_mode")
    val beneficiaryMode: String? = null,
    @SerializedName("buffer_apply_on")
    val bufferApplyOn: String? = null,
    @SerializedName("release_method")
    val releaseMethod: String? = null,
    @SerializedName("fallback_policy")
    val fallbackPolicy: InheritanceFallbackPolicyRequest? = null,
    @SerializedName("stages")
    val stages: List<InheritanceStageRequest>? = null,
    @SerializedName("beneficiaries")
    val beneficiaries: List<InheritanceBeneficiaryRequest>? = null,
)

class InheritancePayload(
    val groupId: String? = null,
    val walletId: String? = null,
    val walletLocalId: String? = null,
    val oldData: InheritanceDataExtended? = null,
    val newData: InheritanceDataExtended? = null,
)

class InheritanceDataExtended(
    val walletId: String,
    val note: String,
    val notificationEmails: List<String>,
    val notifyToday: Boolean,
    val activationTimeMilis: Long,
    val groupId: String,
    val bufferPeriod: Period?,
    val notificationPreferences: InheritanceNotificationSettings? = null,
    val timezone: String = "",
    val distributionMethod: String? = null,
    val beneficiaryMode: String? = null,
    val bufferApplyOn: String? = null,
    val releaseMethod: String? = null,
    val fallbackPolicy: InheritancePlanFallbackPolicy? = null,
    val stages: List<InheritancePlanStage> = emptyList(),
    val beneficiaries: List<InheritancePlanBeneficiary> = emptyList(),
)

fun InheritancePayloadDto.toInheritancePayload(): InheritancePayload {
    return InheritancePayload(
        groupId = groupId,
        walletId = walletId,
        walletLocalId = walletLocalId,
        oldData = oldData?.toInheritanceDataExtended(),
        newData = newData?.toInheritanceDataExtended()
    )
}

fun InheritanceDataExtendedDto.toInheritanceDataExtended(): InheritanceDataExtended {
    return InheritanceDataExtended(
        walletId = walletId.orEmpty(),
        note = note.orEmpty(),
        notificationEmails = notificationEmails.orEmpty(),
        notifyToday = notifyToday.orFalse(),
        activationTimeMilis = activationTimeMilis ?: 0,
        groupId = groupId.orEmpty(),
        bufferPeriod = bufferPeriod?.toPeriod(),
        notificationPreferences = notificationPreferences?.toInheritanceNotificationSettings(),
        timezone = timezone.orEmpty(),
        distributionMethod = distributionMethod,
        beneficiaryMode = beneficiaryMode,
        bufferApplyOn = bufferApplyOn,
        releaseMethod = releaseMethod,
        fallbackPolicy = fallbackPolicy?.toDomainModel(),
        stages = stages?.map { it.toDomainModel() }.orEmpty(),
        beneficiaries = beneficiaries?.map { it.toDomainModel() }.orEmpty(),
    )
}

private fun InheritanceFallbackPolicyRequest.toDomainModel(): InheritancePlanFallbackPolicy {
    return InheritancePlanFallbackPolicy(
        type = type.orEmpty(),
        inactivityInterval = inactivityInterval,
        inactivityIntervalCount = inactivityIntervalCount,
        fallbackTimeMillis = fallbackTimeMillis,
    )
}

private fun InheritanceStageRequest.toDomainModel(): InheritancePlanStage {
    return InheritancePlanStage(
        amountPerReleasePercentage = amountPerReleasePercentage ?: 0,
        repeatInterval = repeatInterval.orEmpty(),
        repeatIntervalCount = repeatIntervalCount ?: 0,
        totalStageAllocationPercentage = totalStageAllocationPercentage ?: 0,
        firstWithdrawalTimeMillis = firstWithdrawalTimeMillis ?: 0,
        expandedInstallments = expandedInstallments?.map { installment ->
            com.nunchuk.android.model.inheritance.InheritancePlanExpandedInstallment(
                index = installment.index ?: 0,
                withdrawalTimeMillis = installment.withdrawalTimeMillis ?: 0,
                allocationPercentage = installment.allocationPercentage ?: 0,
            )
        }.orEmpty(),
    )
}

private fun InheritanceBeneficiaryRequest.toDomainModel(): InheritancePlanBeneficiary {
    return InheritancePlanBeneficiary(
        email = email.orEmpty(),
        assetPercentage = assetPercentage ?: 0,
        magic = magic.orEmpty(),
        note = note.orEmpty(),
        stages = stages?.map { it.toDomainModel() }.orEmpty(),
    )
}
