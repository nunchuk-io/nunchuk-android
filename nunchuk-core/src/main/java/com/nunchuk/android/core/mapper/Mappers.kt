package com.nunchuk.android.core.mapper

import com.nunchuk.android.core.data.model.byzantine.AlertResponse
import com.nunchuk.android.core.data.model.byzantine.GroupChatDto
import com.nunchuk.android.core.data.model.byzantine.GroupResponse
import com.nunchuk.android.core.data.model.byzantine.HistoryPeriodResponseOrRequest
import com.nunchuk.android.core.data.model.byzantine.MemberRequest
import com.nunchuk.android.core.data.model.byzantine.toModel
import com.nunchuk.android.core.data.model.membership.CalculateRequiredSignaturesResponse
import com.nunchuk.android.core.data.model.membership.InheritanceDto
import com.nunchuk.android.core.data.model.membership.PeriodResponse
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.util.orDefault
import com.nunchuk.android.core.util.orFalse
import com.nunchuk.android.model.Alert
import com.nunchuk.android.model.BackupKey
import com.nunchuk.android.model.ByzantineGroup
import com.nunchuk.android.model.ByzantineMember
import com.nunchuk.android.model.CalculateRequiredSignatures
import com.nunchuk.android.model.GroupChat
import com.nunchuk.android.model.HistoryPeriod
import com.nunchuk.android.model.Inheritance
import com.nunchuk.android.model.InheritanceStatus
import com.nunchuk.android.model.KeyResponse
import com.nunchuk.android.model.Period
import com.nunchuk.android.model.User
import com.nunchuk.android.model.byzantine.AssistedMember
import com.nunchuk.android.model.byzantine.toAlertType
import com.nunchuk.android.model.transaction.AlertPayload

internal fun KeyResponse.toBackupKey(): BackupKey {
    return BackupKey(
        keyId = keyId,
        keyCheckSum = keyCheckSum,
        keyBackUpBase64 = keyBackUpBase64,
        keyChecksumAlgorithm = keyChecksumAlgorithm.orEmpty(),
        keyName = keyName.orEmpty(),
        keyXfp = keyXfp.orEmpty(),
        cardId = cardId.orEmpty(),
        verificationType = verificationType.orEmpty(),
        verifiedTimeMilis = verifiedTimeMilis ?: 0L,
        derivationPath = derivationPath.orEmpty()
    )
}

internal fun CalculateRequiredSignaturesResponse.Data?.toCalculateRequiredSignatures(): CalculateRequiredSignatures {
    return CalculateRequiredSignatures(
        type = this?.type.orEmpty(),
        requiredSignatures = this?.requiredSignatures.orDefault(0),
        requiredAnswers = this?.requiredAnswers.orDefault(0)
    )
}

internal fun InheritanceDto.toInheritance(): Inheritance {
    val status = when (this.status) {
        "ACTIVE" -> InheritanceStatus.ACTIVE
        "CLAIMED" -> InheritanceStatus.CLAIMED
        else -> InheritanceStatus.PENDING_CREATION
    }
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
        bufferPeriod = bufferPeriod?.toPeriod()
    )
}

internal fun PeriodResponse.Data.toPeriod() = Period(
    id = id.orEmpty(),
    interval = interval.orEmpty(),
    intervalCount = intervalCount.orDefault(0),
    enabled = enabled.orFalse(),
    displayName = displayName.orEmpty(),
    isRecommended = isRecommended.orFalse()
)

internal fun GroupResponse.toByzantineGroup(): ByzantineGroup {
    return ByzantineGroup(
        createdTimeMillis = createdTimeMillis ?: 0,
        id = id.orEmpty(),
        setupPreference = setupPreference.orEmpty(),
        walletConfig = walletConfig.toModel(),
        status = status.orEmpty(),
        members = members?.map {
            ByzantineMember(emailOrUsername = it.emailOrUsername.orEmpty(),
                membershipId = it.membershipId.orEmpty(),
                permissions = it.permissions ?: emptyList(),
                role = it.role.orEmpty(),
                status = it.status.orEmpty(),
                inviterUserId = it.inviterUserId.orEmpty(),
                user = it.user?.let { user ->
                    User(
                        id = user.id,
                        name = user.name,
                        email = user.email,
                        gender = user.gender.orEmpty(),
                        avatar = user.avatar.orEmpty(),
                        status = user.status.orEmpty(),
                        chatId = user.chatId,
                        loginType = user.loginType.orEmpty(),
                        username = user.username.orEmpty(),
                    )
                })
        } ?: emptyList(),
    )
}

internal fun AlertResponse.toAlert(): Alert {
    return Alert(
        viewable = viewable.orFalse(),
        body = body.orEmpty(),
        createdTimeMillis = createdTimeMillis ?: 0,
        id = id.orEmpty(),
        status = status.orEmpty(),
        title = title.orEmpty(),
        type = type.toAlertType(),
        payload = AlertPayload(
            masterName = payload?.masterName.orEmpty(),
            pendingKeysCount = payload?.pendingKeysCount.orDefault(0),
            dummyTransactionId = payload?.dummyTransactionId.orEmpty(),
            xfps = payload?.xfps.orEmpty(),
            claimKey = payload?.claimKey.orFalse()
        )
    )
}

internal fun GroupChatDto.toGroupChat(): GroupChat {
    return GroupChat(
        createdTimeMillis = createdTimeMillis ?: 0,
        groupId = groupId.orEmpty(),
        historyPeriod = historyPeriod?.toHistoryPeriod() ?: HistoryPeriod(),
        roomId = roomId.orEmpty()
    )
}

internal fun HistoryPeriodResponseOrRequest?.toHistoryPeriod(): HistoryPeriod {
    return HistoryPeriod(
        displayName = this?.displayName.orEmpty(),
        enabled = this?.enabled.orFalse(),
        id = this?.id.orEmpty(),
        interval = this?.interval.orEmpty(),
        intervalCount = this?.intervalCount.orDefault(0)
    )
}

internal fun AssistedMember.toMemberRequest(): MemberRequest {
    return MemberRequest(
        emailOrUsername = if (loginType == SignInMode.PRIMARY_KEY.name) name.orEmpty() else email, permissions = emptyList(), role = role
    )
}