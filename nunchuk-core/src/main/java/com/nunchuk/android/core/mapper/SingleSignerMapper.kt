package com.nunchuk.android.core.mapper

import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.CardIdManager
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.type.SignerType
import javax.inject.Inject

class SingleSignerMapper @Inject constructor(
    private val accountManager: AccountManager,
    private val cardIdManager: CardIdManager
) {
    suspend operator fun invoke(from: SingleSigner) : SignerModel {
        val accountInfo = accountManager.getAccount()
        val isPrimaryKey =
            accountInfo.loginType == SignInMode.PRIMARY_KEY.value && accountInfo.primaryKeyInfo?.xfp == from.masterFingerprint
        val cardId = if (from.type == SignerType.NFC) cardIdManager.getCardId(from.masterSignerId) else ""
        return from.toModel(isPrimaryKey = isPrimaryKey).copy(cardId = cardId)
    }
}