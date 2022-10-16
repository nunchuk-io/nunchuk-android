package com.nunchuk.android.core.mapper

import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.MasterSigner
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MasterSignerMapper @Inject constructor(private val accountManager: AccountManager) :
    Mapper<MasterSigner, SignerModel> {

    override fun map(from: MasterSigner): SignerModel {
        val accountInfo = accountManager.getAccount()
        val isPrimaryKey = accountInfo.loginType == SignInMode.PRIMARY_KEY.value && accountInfo.primaryKeyInfo?.xfp == from.device.masterFingerprint
        return SignerModel(
            id = from.id,
            name = from.name,
            derivationPath = from.device.path,
            fingerPrint = from.device.masterFingerprint,
            type = from.type,
            software = from.software,
            isPrimaryKey = isPrimaryKey
        )
    }
}