package com.nunchuk.android.core.mapper

import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.HasSignerUseCase
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.CardIdManager
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.signer.GetSignerUseCase
import javax.inject.Inject

class SingleSignerMapper @Inject constructor(
    accountManager: AccountManager,
    private val cardIdManager: CardIdManager,
    private val hasSignerUseCase: HasSignerUseCase,
    private val getSignerUseCase: GetSignerUseCase,
) {
    private val accountInfo = accountManager.getAccount()

    suspend operator fun invoke(from: SingleSigner): SignerModel {
        val isPrimaryKey =
            accountInfo.loginType == SignInMode.PRIMARY_KEY.value && accountInfo.primaryKeyInfo?.xfp == from.masterFingerprint
        val hasSigner = hasSignerUseCase(from).getOrDefault(false)
        return if ((from.name.isEmpty() || from.name == from.masterFingerprint) && hasSigner) {
            val signer = getSignerUseCase(from).map { it.copy(isVisible = true) }.getOrDefault(from)
            val cardId =
                if (signer.type == SignerType.NFC) cardIdManager.getCardId(signer.masterSignerId) else ""
            signer.toModel(isPrimaryKey = isPrimaryKey).copy(cardId = cardId, isVisible = hasSigner)
        } else {
            val cardId =
                if (from.type == SignerType.NFC) cardIdManager.getCardId(from.masterSignerId) else ""
            from.toModel(isPrimaryKey = isPrimaryKey).copy(cardId = cardId, isVisible = hasSigner)
        }
    }
}