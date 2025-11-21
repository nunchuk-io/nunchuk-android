package com.nunchuk.android.core.mapper

import com.nunchuk.android.core.data.model.byzantine.toWalletType
import com.nunchuk.android.core.data.model.membership.BeneficiaryNotificationDto
import com.nunchuk.android.core.data.model.membership.InheritanceClaimingInitResponse
import com.nunchuk.android.core.data.model.membership.InheritanceDto
import com.nunchuk.android.core.data.model.membership.InheritanceKeyDto
import com.nunchuk.android.core.data.model.membership.InheritanceNotificationPreferencesDto
import com.nunchuk.android.model.Inheritance
import com.nunchuk.android.model.InheritanceClaimingInit
import com.nunchuk.android.model.InheritanceKey
import com.nunchuk.android.model.InheritancePendingRequest
import com.nunchuk.android.model.InheritanceStatus
import com.nunchuk.android.model.inheritance.EmailNotificationSettings
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
        timezone = timezone.orEmpty()
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
        inheritanceKeyCount = inheritanceKeyCount ?: 0
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