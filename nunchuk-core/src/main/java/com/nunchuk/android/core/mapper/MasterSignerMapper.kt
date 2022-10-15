package com.nunchuk.android.core.mapper

import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.MasterSigner
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MasterSignerMapper @Inject constructor(private val accountManager: AccountManager) {

    operator fun invoke(from: MasterSigner, derivationPath: String = ""): SignerModel {
        val accountInfo = accountManager.getAccount()
        val isPrimaryKey =
            accountInfo.loginType == SignInMode.PRIMARY_KEY.value && accountInfo.primaryKeyInfo?.xfp == from.device.masterFingerprint
        return SignerModel(
            id = from.id,
            name = from.name,
            derivationPath = derivationPath.ifEmpty { from.device.path },
            fingerPrint = from.device.masterFingerprint,
            type = from.type,
            software = from.software,
            isPrimaryKey = isPrimaryKey,
            isMasterSigner = true
        )
    }
}