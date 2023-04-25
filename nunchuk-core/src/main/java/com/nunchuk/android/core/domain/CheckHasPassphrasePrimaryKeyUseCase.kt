package com.nunchuk.android.core.domain

import com.nunchuk.android.core.account.PrimaryKeySignerInfoHolder
import com.nunchuk.android.core.domain.membership.VerifiedPasswordTargetAction
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.repository.PremiumWalletRepository
import com.nunchuk.android.repository.SignerSoftwareRepository
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class CheckHasPassphrasePrimaryKeyUseCase @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val nunchukNativeSdk: NunchukNativeSdk,
    private val primaryKeySignerInfoHolder: PrimaryKeySignerInfoHolder,
) : UseCase<Unit, Boolean>(dispatcher) {
    override suspend fun execute(parameters: Unit): Boolean {
        val primaryKeyInfo = primaryKeySignerInfoHolder.getPrimaryKeyInfo() ?: return false
        nunchukNativeSdk.clearSignerPassphrase(primaryKeyInfo.masterFingerprint)
        return primaryKeySignerInfoHolder.isNeedPassphraseSent(forceNewData = true)
    }
}