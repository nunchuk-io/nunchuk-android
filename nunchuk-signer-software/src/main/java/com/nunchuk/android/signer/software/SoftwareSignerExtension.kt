package com.nunchuk.android.signer.software

import android.app.Activity
import com.nunchuk.android.core.manager.ActivityManager
import com.nunchuk.android.core.signer.PrimaryKeyFlow.isReplaceFlow
import com.nunchuk.android.core.signer.PrimaryKeyFlow.isReplaceKeyInFreeWalletFlow
import com.nunchuk.android.core.signer.PrimaryKeyFlow.isSignUpFlow
import com.nunchuk.android.core.util.hideLoading
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.nav.NunchukNavigator

fun Activity.onCreateSignerCompleted(
    navigator: NunchukNavigator,
    passphrase: String,
    mnemonic: String,
    signerName: String,
    masterSigner: MasterSigner?,
    skipPassphrase: Boolean,
    primaryKeyFlow: Int,
    replacedXfp: String,
    groupId: String,
) {
    hideLoading()
    if (primaryKeyFlow.isSignUpFlow()) {
        navigator.openPrimaryKeyChooseUserNameScreen(
            activityContext = this,
            mnemonic = mnemonic,
            passphrase = passphrase,
            signerName = signerName
        )
    } else if (primaryKeyFlow.isReplaceFlow()) {
        ActivityManager.popToLevel(2)
        navigator.openSignerInfoScreen(
            activityContext = this,
            isMasterSigner = true,
            id = masterSigner!!.id,
            masterFingerprint = masterSigner.device.masterFingerprint,
            name = masterSigner.name,
            type = masterSigner.type,
            justAdded = true,
            setPassphrase = !skipPassphrase,
            isReplacePrimaryKey = true
        )
    } else if (groupId.isNotEmpty() || replacedXfp.isNotEmpty() || primaryKeyFlow.isReplaceKeyInFreeWalletFlow()) {
        ActivityManager.popUntil(SoftwareSignerIntroActivity::class.java, true)
    } else {
        navigator.returnToMainScreen()
        navigator.openSignerInfoScreen(
            activityContext = this,
            isMasterSigner = true,
            id = masterSigner!!.id,
            masterFingerprint = masterSigner.device.masterFingerprint,
            name = masterSigner.name,
            type = masterSigner.type,
            justAdded = true,
            setPassphrase = !skipPassphrase
        )
    }
}