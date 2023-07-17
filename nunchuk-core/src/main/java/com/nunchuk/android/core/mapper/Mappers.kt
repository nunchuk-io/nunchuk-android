package com.nunchuk.android.core.mapper

import com.nunchuk.android.core.data.model.membership.CalculateRequiredSignaturesResponse
import com.nunchuk.android.core.data.model.membership.InheritanceDto
import com.nunchuk.android.core.data.model.membership.PeriodResponse
import com.nunchuk.android.core.util.orDefault
import com.nunchuk.android.core.util.orFalse
import com.nunchuk.android.model.BackupKey
import com.nunchuk.android.model.CalculateRequiredSignatures
import com.nunchuk.android.model.Inheritance
import com.nunchuk.android.model.InheritanceStatus
import com.nunchuk.android.model.KeyResponse
import com.nunchuk.android.model.Period

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

internal fun PeriodResponse.Data.toPeriod() = Period(
    id = id.orEmpty(),
    interval = interval.orEmpty(),
    intervalCount = intervalCount.orDefault(0),
    enabled = enabled.orFalse(),
    displayName = displayName.orEmpty(),
    isRecommended = isRecommended.orFalse()
)

internal fun InheritanceDto.toInheritance(): Inheritance {
    val status = when (this.status) {
        "ACTIVE" -> InheritanceStatus.ACTIVE
        "CLAIMED" -> InheritanceStatus.CLAIMED
        else -> InheritanceStatus.PENDING_CREATION
    }
    return Inheritance(
        walletId = walletId.orEmpty(), walletLocalId = walletLocalId.orEmpty(),
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