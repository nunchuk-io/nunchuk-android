package com.nunchuk.android.main.membership.signer

import android.app.Activity
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.nunchuk.android.core.portal.PortalDeviceArgs
import com.nunchuk.android.core.portal.PortalDeviceFlow
import com.nunchuk.android.core.signer.KeyFlow
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.signer.SignerIntroRequest
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.nav.args.AddAirSignerArgs
import com.nunchuk.android.nav.args.SetupMk4Args
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.signer.SignerHardwareDevice
import com.nunchuk.android.signer.HardwareMode
import com.nunchuk.android.signer.SignerDeviceAction
import com.nunchuk.android.signer.SignerIntroAction
import com.nunchuk.android.signer.bitbox.BitBoxActivity
import com.nunchuk.android.signer.ledger.LedgerActivity
import com.nunchuk.android.signer.mk4.Mk4Activity
import com.nunchuk.android.signer.tapsigner.NfcSetupActivity
import com.nunchuk.android.signer.trezor.TrezorActivity
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.utils.parcelable

/** Android boundary: launch a resolved action and normalize each device's result contract. */
internal class SignerDeviceLauncher(
    private val activity: ComponentActivity,
    private val navigator: NunchukNavigator,
    private val request: SignerIntroRequest,
    private val membershipStep: () -> MembershipStep?,
    private val skipPicker: Boolean,
    private val onSigner: (SignerModel) -> Unit,
    private val onResult: (Int, Intent?) -> Unit,
) {
    private val params = request.deviceParams
    private val signerLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val signer = result.data?.takeIf { result.resultCode == Activity.RESULT_OK }
            ?.parcelable<SignerModel>(GlobalResultKey.EXTRA_SIGNER)
        when {
            signer != null -> onSigner(signer)
            skipPicker -> onResult(result.resultCode, result.data)
        }
    }

    private val verifyLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK || skipPicker) {
            onResult(result.resultCode, result.data)
        }
    }

    // Stable registration order also keeps pending results associated with the correct device
    // after recreation. No mutable "last selected hardware" field is needed.
    private val claimLaunchers = SignerHardwareDevice.entries.associateWith { device ->
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            if (result.resultCode == Activity.RESULT_OK && data != null) {
                val signer = data.parcelable<SingleSigner>(GlobalResultKey.EXTRA_SIGNER)
                if (signer != null) onSigner(signer.toModel())
                else if (device.isDesktopHandoff(data)) {
                    navigator.openAddDesktopKey(
                        activity,
                        signerTag = device.tag,
                        step = MembershipStep.SETUP_INHERITANCE,
                        isInheritanceKey = true,
                        magic = params?.magic.orEmpty(),
                    )
                }
            }
        }
    }

    fun launch(action: SignerDeviceAction) {
        when (action) {
            is SignerIntroAction.Tapsigner -> launchSigner(
                NfcSetupActivity.buildIntent(
                    activity = activity,
                    setUpAction = NfcSetupActivity.SETUP_TAP_SIGNER,
                    walletId = request.walletId,
                    groupId = request.groupId,
                    fromMembershipFlow = params != null,
                    onChainAddSignerParam = params,
                ), action.returnResult
            )
            is SignerIntroAction.Coldcard -> launchSigner(
                Mk4Activity.buildIntent(
                    activity = activity,
                    args = SetupMk4Args(
                        fromMembershipFlow = params != null,
                        isFromAddKey = params == null,
                        groupId = request.groupId,
                        walletId = request.walletId,
                        replacedXfp = params?.replaceInfo?.replacedXfp,
                        onChainAddSignerParam = params,
                    ),
                ), action.returnResult
            )
            is SignerIntroAction.Airgap -> {
                val args = AddAirSignerArgs(
                    isMembershipFlow = params != null,
                    tag = action.tag,
                    groupId = request.groupId,
                    walletId = request.walletId,
                    replacedXfp = action.replacedXfp,
                    onChainAddSignerParam = params,
                    step = membershipStep(),
                )
                if (action.returnResult) {
                    navigator.openAddAirSignerScreenForResult(signerLauncher, activity, args)
                } else {
                    navigator.openAddAirSignerScreen(activity, args)
                    activity.finish()
                }
            }
            is SignerIntroAction.Hardware -> {
                val device = action.device
                val intent = device.buildIntent(activity, request, action.mode)
                when (action.mode) {
                    HardwareMode.Add -> launchSigner(intent, returnResult = false)
                    is HardwareMode.Claim -> claimLaunchers.getValue(device).launch(intent)
                    is HardwareMode.Verify -> verifyLauncher.launch(intent)
                }
            }
            SignerIntroAction.Portal -> {
                navigator.openPortalScreen(
                    activity = activity,
                    args = PortalDeviceArgs(
                        type = PortalDeviceFlow.SETUP,
                        isMembershipFlow = request.walletId.isNotEmpty() || params != null,
                        walletId = request.walletId,
                        groupId = request.groupId,
                    ),
                )
                activity.finish()
            }
            SignerIntroAction.CreateSoftware -> {
                navigator.openAddSoftwareSignerScreen(
                    activityContext = activity,
                    keyFlow = if (request.walletId.isNotEmpty()) KeyFlow.REPLACE_KEY_IN_FREE_WALLET
                        else request.keyFlow,
                    groupId = request.groupId,
                    walletId = request.walletId,
                )
                activity.finish()
            }
        }
    }

    fun recoverTapsigner() = navigator.openRecoverTapSigner(
        launcher = signerLauncher,
        activity = activity,
        fromMembershipFlow = true,
    )

    private fun launchSigner(intent: Intent, returnResult: Boolean) {
        if (returnResult) signerLauncher.launch(intent)
        else {
            activity.startActivity(intent)
            activity.finish()
        }
    }
}

/** Device-specific intent/result details; the flow policy is shared in SignerIntroCoordinator. */
private fun SignerHardwareDevice.isDesktopHandoff(data: Intent): Boolean = when (this) {
    SignerHardwareDevice.LEDGER -> data.getStringExtra(LedgerActivity.EXTRA_RESULT_ACTION) == LedgerActivity.RESULT_ACTION_OPEN_DESKTOP_FLOW
    SignerHardwareDevice.BITBOX -> data.getStringExtra(BitBoxActivity.EXTRA_RESULT_ACTION) == BitBoxActivity.RESULT_ACTION_OPEN_DESKTOP_FLOW
    SignerHardwareDevice.TREZOR -> data.getStringExtra(TrezorActivity.EXTRA_RESULT_ACTION) == TrezorActivity.RESULT_ACTION_OPEN_USB_FLOW
}

private fun SignerHardwareDevice.buildIntent(activity: ComponentActivity, request: SignerIntroRequest, mode: HardwareMode): Intent {
    val isMembershipFlow = mode != HardwareMode.Add
    val accountIndex = (mode as? HardwareMode.Claim)?.accountIndex ?: 0
    val expectedXfp = (mode as? HardwareMode.Verify)?.expectedXfp.orEmpty()
    val verifyXfpOnly = mode is HardwareMode.Verify
    val isFromWalletFlow = request.walletId.isNotEmpty() || request.groupId.isNotEmpty()
    val accountCount = if (mode == HardwareMode.Add) request.accountCount else 1
    return when (this) {
        SignerHardwareDevice.LEDGER -> LedgerActivity.buildIntent(
            activityContext = activity,
            isMembershipFlow = isMembershipFlow,
            accountIndex = accountIndex,
            expectedXfp = expectedXfp,
            verifyXfpOnly = verifyXfpOnly,
            isFromWalletFlow = mode == HardwareMode.Add && isFromWalletFlow,
            accountCount = accountCount,
        )
        SignerHardwareDevice.BITBOX -> BitBoxActivity.buildIntent(
            activityContext = activity,
            isMembershipFlow = isMembershipFlow,
            accountIndex = accountIndex,
            expectedXfp = expectedXfp,
            verifyXfpOnly = verifyXfpOnly,
            isFromWalletFlow = mode == HardwareMode.Add && isFromWalletFlow,
            accountCount = accountCount,
        )
        SignerHardwareDevice.TREZOR -> TrezorActivity.buildIntent(
            activityContext = activity,
            isMembershipFlow = isMembershipFlow,
            accountIndex = accountIndex,
            expectedXfp = expectedXfp,
            verifyXfpOnly = verifyXfpOnly,
        )
    }
}
