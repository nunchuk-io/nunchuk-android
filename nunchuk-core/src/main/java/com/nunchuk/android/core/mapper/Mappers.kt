package com.nunchuk.android.core.mapper

import com.google.gson.reflect.TypeToken
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
import com.nunchuk.android.core.util.gson
import com.nunchuk.android.core.util.orDefault
import com.nunchuk.android.core.util.orFalse
import com.nunchuk.android.model.Alert
import com.nunchuk.android.model.BackupKey
import com.nunchuk.android.model.ByzantineGroup
import com.nunchuk.android.model.ByzantineMember
import com.nunchuk.android.model.ByzantineWalletConfig
import com.nunchuk.android.model.CalculateRequiredSignatureStep
import com.nunchuk.android.model.CalculateRequiredSignatures
import com.nunchuk.android.model.CalculateRequiredSignaturesExt
import com.nunchuk.android.model.GroupChat
import com.nunchuk.android.model.HistoryPeriod
import com.nunchuk.android.model.Inheritance
import com.nunchuk.android.model.InheritancePendingRequest
import com.nunchuk.android.model.InheritanceStatus
import com.nunchuk.android.model.KeyResponse
import com.nunchuk.android.model.Period
import com.nunchuk.android.model.User
import com.nunchuk.android.model.UserResponse
import com.nunchuk.android.model.byzantine.AlertType
import com.nunchuk.android.model.byzantine.AssistedMember
import com.nunchuk.android.model.byzantine.KeyHealthStatus
import com.nunchuk.android.model.byzantine.toAlertType
import com.nunchuk.android.model.transaction.AlertPayload
import com.nunchuk.android.persistence.dao.GroupDao
import com.nunchuk.android.persistence.entity.AlertEntity
import com.nunchuk.android.persistence.entity.GroupEntity
import com.nunchuk.android.persistence.entity.KeyHealthStatusEntity
import com.nunchuk.android.type.Chain

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

internal fun CalculateRequiredSignaturesResponse.toCalculateRequiredSignaturesEx(): CalculateRequiredSignaturesExt {
    val step = when (this.step) {
        "REQUEST_RECOVER" -> CalculateRequiredSignatureStep.REQUEST_RECOVER
        "PENDING_APPROVAL" -> CalculateRequiredSignatureStep.PENDING_APPROVAL
        "RECOVER" -> CalculateRequiredSignatureStep.RECOVER
        else -> null
    }
    return CalculateRequiredSignaturesExt(
        data = result?.toCalculateRequiredSignatures(),
        step = step
    )
}

internal fun InheritanceDto.toInheritance(): Inheritance {
    val status = when (this.status) {
        "ACTIVE" -> InheritanceStatus.ACTIVE
        "CLAIMED" -> InheritanceStatus.CLAIMED
        "PENDING_APPROVAL" -> InheritanceStatus.PENDING_APPROVAL
        else -> InheritanceStatus.PENDING_CREATION
    }
    val pendingRequests = pendingRequests?.map {
        InheritancePendingRequest(
            membershipId = it.membershipId.orEmpty(),
            dummyTransactionId = it.dummyTransactionId.orEmpty()
        )
    } ?: emptyList()

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
        pendingRequests = pendingRequests
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
        isViewPendingWallet = false,
        isLocked = isLocked.orFalse(),
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
        slug = slug.orEmpty()
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
            claimKey = payload?.claimKey.orFalse(),
            keyXfp = payload?.keyXfp.orEmpty(),
            paymentName = payload?.paymentName.orEmpty(),
            requestId = payload?.requestId.orEmpty(),
            membershipId = payload?.membershipId.orEmpty(),
            transactionId = payload?.transactionId.orEmpty(),
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

internal fun AlertEntity.toAlert(): Alert {
    return Alert(
        id = id,
        viewable = viewable,
        body = body,
        createdTimeMillis = createdTimeMillis,
        status = status,
        title = title,
        type = AlertType.valueOf(type),
        payload = gson.fromJson(payload, AlertPayload::class.java)
    )
}

internal fun KeyHealthStatusEntity.toKeyHealthStatus(): KeyHealthStatus {
    return KeyHealthStatus(
        xfp = xfp,
        canRequestHealthCheck = canRequestHealthCheck,
        lastHealthCheckTimeMillis = lastHealthCheckTimeMillis
    )
}

internal fun UserResponse.toUser() = User(
    id = id,
    name = name,
    email = email,
    gender = gender.orEmpty(),
    avatar = avatar.orEmpty(),
    status = status.orEmpty(),
    chatId = chatId,
    loginType = loginType.orEmpty(),
    username = username.orEmpty()
)

internal fun GroupResponse.toGroupEntity(chatId: String, chain: Chain, groupDao: GroupDao): GroupEntity {
    val memberBrief = members.orEmpty().map {
        ByzantineMember(
            emailOrUsername = it.emailOrUsername.orEmpty(),
            role = it.role.orEmpty(),
            inviterUserId = it.inviterUserId.orEmpty(),
            status = it.status.orEmpty(),
            membershipId = it.membershipId.orEmpty(),
            permissions = it.permissions.orEmpty(),
            user = it.user?.toUser()
        )
    }
    val walletConfig = ByzantineWalletConfig(
        allowInheritance = walletConfig?.allowInheritance.orFalse(),
        m = walletConfig?.m.orDefault(0),
        n = walletConfig?.n.orDefault(0),
        requiredServerKey = walletConfig?.requiredServerKey.orFalse()
    )
    groupDao.getGroupById(id.orEmpty(), chatId, chain)?.let {
        return it.copy(
            groupId = id.orEmpty(),
            chatId = chatId,
            status = status.orEmpty(),
            createdTimeMillis = createdTimeMillis ?: 0,
            members = gson.toJson(memberBrief),
            walletConfig = gson.toJson(walletConfig),
            chain = chain,
            setupPreference = setupPreference.orEmpty(),
            isLocked = isLocked.orFalse()
        )
    }
    return GroupEntity(
        groupId = id.orEmpty(),
        chatId = chatId,
        status = status.orEmpty(),
        createdTimeMillis = createdTimeMillis ?: 0,
        members = gson.toJson(memberBrief),
        chain = chain,
        setupPreference = setupPreference.orEmpty(),
        walletConfig = gson.toJson(walletConfig),
        isLocked = isLocked.orFalse(),
        slug = slug.orEmpty()
    )
}

internal fun GroupEntity.toByzantineGroup(): ByzantineGroup {
    val members = gson.fromJson<List<ByzantineMember>>(
        members, object : TypeToken<List<ByzantineMember>>() {}.type
    )
    val walletConfig = gson.fromJson(walletConfig, ByzantineWalletConfig::class.java)
    return ByzantineGroup(
        id = groupId,
        status = status,
        members = members,
        createdTimeMillis = createdTimeMillis,
        isViewPendingWallet = isViewPendingWallet,
        setupPreference = setupPreference,
        walletConfig = walletConfig,
        isLocked = isLocked,
        slug = slug
    )
}