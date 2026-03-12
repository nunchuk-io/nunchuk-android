package com.nunchuk.android.core.mapper

import com.nunchuk.android.core.data.model.byzantine.toWalletType
import com.nunchuk.android.core.data.model.membership.BeneficiaryNotificationDto
import com.nunchuk.android.core.data.model.membership.InheritanceBeneficiaryDto
import com.nunchuk.android.core.data.model.membership.InheritanceClaimSigningChallengeResponse
import com.nunchuk.android.core.data.model.membership.InheritanceClaimingInitResponse
import com.nunchuk.android.core.data.model.membership.InheritanceDto
import com.nunchuk.android.core.data.model.membership.InheritanceExpandedInstallmentDto
import com.nunchuk.android.core.data.model.membership.InheritanceFallbackPolicyDto
import com.nunchuk.android.core.data.model.membership.InheritanceKeyDto
import com.nunchuk.android.core.data.model.membership.InheritanceNotificationPreferencesDto
import com.nunchuk.android.core.data.model.membership.InheritanceStageDto
import com.nunchuk.android.core.data.model.membership.KeyOriginDto
import com.nunchuk.android.model.Inheritance
import com.nunchuk.android.model.InheritanceClaimingInit
import com.nunchuk.android.model.InheritanceKey
import com.nunchuk.android.model.InheritancePendingRequest
import com.nunchuk.android.model.InheritanceStatus
import com.nunchuk.android.model.KeyOrigin
import com.nunchuk.android.model.inheritance.ClaimSigningChallenge
import com.nunchuk.android.model.inheritance.EmailNotificationSettings
import com.nunchuk.android.model.inheritance.InheritancePlanBeneficiary
import com.nunchuk.android.model.inheritance.InheritancePlanExpandedInstallment
import com.nunchuk.android.model.inheritance.InheritancePlanFallbackPolicy
import com.nunchuk.android.model.inheritance.InheritancePlanStage
import com.nunchuk.android.model.inheritance.InheritanceNotificationSettings
import com.nunchuk.android.type.WalletType

internal fun InheritanceDto.toInheritance(): Inheritance {
    val status = when (this.status) {
        "ACTIVE" -> InheritanceStatus.ACTIVE
        "CLAIMED" -> InheritanceStatus.CLAIMED
        "PENDING_APPROVAL" -> InheritanceStatus.PENDING_APPROVAL
        else -> InheritanceStatus.PENDING_CREATION
    }
    val pendingRequests = pendingRequests?.map {
        InheritancePendingRequest(
            id = it.id.orEmpty(),
            membershipId = it.membershipId.orEmpty(),
            dummyTransactionId = it.dummyTransactionId.orEmpty()
        )
    } ?: emptyList()

    val notificationSettings = notificationPreferences?.toInheritanceNotificationSettings()
    val inheritanceKeys = inheritanceKeys?.map { it.toInheritanceKey() } ?: emptyList()

    return Inheritance(
        walletId = walletId.orEmpty(),
        walletLocalId = walletLocalId.orEmpty(),
        magic = magic.orEmpty(),
        note = note.orEmpty(),
        notificationEmails = notificationEmails.orEmpty(),
        status = status,
        activationTimeMilis = activationTimeMilis ?: 0,
        createdTimeMilis = createdTimeMilis ?: 0,
        lastModifiedTimeMilis = lastModifiedTimeMilis ?: 0,
        bufferPeriod = bufferPeriod?.toPeriod(),
        ownerId = ownerId.orEmpty(),
        pendingRequests = pendingRequests,
        walletType = walletType.toWalletType() ?: WalletType.MULTI_SIG,
        notificationPreferences = notificationSettings,
        inheritanceKeys = inheritanceKeys,
        timezone = timezone.orEmpty(),
        distributionMethod = distributionMethod,
        beneficiaryMode = beneficiaryMode,
        bufferApplyOn = bufferApplyOn,
        releaseMethod = releaseMethod,
        fallbackPolicy = fallbackPolicy?.toInheritancePlanFallbackPolicy(),
        stages = stages?.map { it.toInheritancePlanStage() }.orEmpty(),
        beneficiaries = beneficiaries?.map { it.toInheritancePlanBeneficiary() }.orEmpty(),
    )
}

internal fun InheritanceNotificationPreferencesDto.toInheritanceNotificationSettings(): InheritanceNotificationSettings {
    val perEmailSettings = beneficiaryNotifications?.map { it.toEmailNotificationSettings() } ?: emptyList()
    
    return InheritanceNotificationSettings(
        emailMeWalletConfig = emailMeWalletConfig ?: false,
        perEmailSettings = perEmailSettings
    )
}

internal fun InheritanceClaimingInitResponse.toInheritanceClaimingInit(): InheritanceClaimingInit {
    return InheritanceClaimingInit(
        walletType = walletType.toWalletType() ?: WalletType.MULTI_SIG,
        walletLocalId = walletLocalId.orEmpty(),
        inheritanceKeyCount = inheritanceKeyCount ?: 0,
        keyOrigins = keyOrigins?.map { it.toKeyOrigin() } ?: emptyList()
    )
}

private fun KeyOriginDto.toKeyOrigin(): KeyOrigin {
    return KeyOrigin(
        xfp = xfp.orEmpty(),
        derivationPath = derivationPath.orEmpty()
    )
}

internal fun BeneficiaryNotificationDto.toEmailNotificationSettings(): EmailNotificationSettings {
    return EmailNotificationSettings(
        email = email.orEmpty(),
        notifyOnTimelockExpiry = notifyTimelockExpires ?: false,
        notifyOnWalletChanges = notifyWalletChanges ?: false,
        includeWalletConfiguration = includeWalletConfig ?: false
    )
}

private fun InheritanceKeyDto.toInheritanceKey(): InheritanceKey {
    return InheritanceKey(
        xfp = xfp.orEmpty()
    )
}

private fun InheritanceFallbackPolicyDto.toInheritancePlanFallbackPolicy(): InheritancePlanFallbackPolicy {
    return InheritancePlanFallbackPolicy(
        type = type.orEmpty(),
        inactivityInterval = inactivityInterval,
        inactivityIntervalCount = inactivityIntervalCount,
        fallbackTimeMillis = fallbackTimeMillis,
    )
}

internal fun InheritanceStageDto.toInheritancePlanStage(): InheritancePlanStage {
    return InheritancePlanStage(
        amountPerReleasePercentage = amountPerReleasePercentage ?: 0,
        repeatInterval = repeatInterval.orEmpty(),
        repeatIntervalCount = repeatIntervalCount ?: 0,
        totalStageAllocationPercentage = totalStageAllocationPercentage ?: 0,
        firstWithdrawalTimeMillis = firstWithdrawalTimeMillis ?: 0,
        expandedInstallments = expandedInstallments?.map { it.toInheritancePlanExpandedInstallment() }
            .orEmpty(),
    )
}

private fun InheritanceExpandedInstallmentDto.toInheritancePlanExpandedInstallment(): InheritancePlanExpandedInstallment {
    return InheritancePlanExpandedInstallment(
        index = index ?: 0,
        withdrawalTimeMillis = withdrawalTimeMillis ?: 0,
        allocationPercentage = allocationPercentage ?: 0,
    )
}

private fun InheritanceBeneficiaryDto.toInheritancePlanBeneficiary(): InheritancePlanBeneficiary {
    return InheritancePlanBeneficiary(
        email = email.orEmpty(),
        assetPercentage = assetPercentage ?: 0,
        magic = magic.orEmpty(),
        note = note.orEmpty(),
        bufferPeriodId = bufferPeriodId,
        bufferApplyOn = bufferApplyOn,
        stages = stages?.map { it.toInheritancePlanStage() }.orEmpty(),
    )
}

internal fun InheritanceClaimSigningChallengeResponse.toClaimSigningChallenge(): ClaimSigningChallenge {
    val message = message
        ?: throw IllegalStateException("Message is missing in signing challenge response")
    return ClaimSigningChallenge(
        id = message.id.orEmpty(),
        message = message.message.orEmpty()
    )
}
