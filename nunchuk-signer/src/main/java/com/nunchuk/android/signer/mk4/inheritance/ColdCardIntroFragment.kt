package com.nunchuk.android.signer.mk4.inheritance

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.fragment.compose.content
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.ActionItem
import com.nunchuk.android.compose.NcHighlightText
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.signer.isOnChainTimelockKey
import com.nunchuk.android.core.signer.isVerifyOnChainTimelockBackup
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.mk4.Mk4Activity
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.widget.NCInfoDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ColdCardIntroFragment : MembershipFragment(), BottomSheetOptionListener {

    private val isFromAddKey by lazy { (requireActivity() as Mk4Activity).isFromAddKey }

    private val mk4Activity by lazy { requireActivity() as Mk4Activity }
    private val isMembershipFlow by lazy {
        mk4Activity.isMembershipFlow || mk4Activity.onChainAddSignerParam != null
    }
    private val isAddInheritanceKey by lazy {
        mk4Activity.onChainAddSignerParam?.isAddInheritanceSigner() ?: isFromAddKey.not()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = content {
        val remainTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
        ColdCardIntroScreen(
            remainTime = remainTime,
            isMembershipFlow = isMembershipFlow,
            isFromAddKey = isFromAddKey,
            mk4Activity = mk4Activity
        ) {
            when (it) {
                ColdCardAction.NFC -> {
                    findNavController().navigate(
                        ColdCardIntroFragmentDirections.actionColdCardIntroFragmentToMk4InfoFragment(
                            isMembershipFlow = isMembershipFlow,
                            isAddInheritanceKey = isAddInheritanceKey
                        )
                    )
                }

                ColdCardAction.USB -> {
                    val onChainAddSignerParam = mk4Activity.onChainAddSignerParam
                    if (onChainAddSignerParam?.isClaiming == true) {
                        navigator.openAddDesktopKey(
                            requireActivity(),
                            signerTag = SignerTag.COLDCARD,
                            groupId = (requireActivity() as Mk4Activity).groupId,
                            step = MembershipStep.SETUP_INHERITANCE,
                            isInheritanceKey = isAddInheritanceKey,
                            magic = onChainAddSignerParam.magic
                        )
                    } else if (mk4Activity.replacedXfp.isNullOrEmpty().not()) {
                        NCInfoDialog(requireActivity())
                            .showDialog(
                                message = getString(R.string.nc_info_hardware_key_not_supported),
                            )
                        return@ColdCardIntroScreen
                    } else {
                        membershipStepManager.currentStep?.let { step ->
                            navigator.openAddDesktopKey(
                                requireActivity(),
                                signerTag = SignerTag.COLDCARD,
                                groupId = (requireActivity() as Mk4Activity).groupId,
                                step = step,
                                isInheritanceKey = isAddInheritanceKey
                            )
                        }
                    }
                    requireActivity().finish()
                }

                ColdCardAction.QR, ColdCardAction.FILE -> {
                    findNavController().navigate(
                        ColdCardIntroFragmentDirections.actionColdCardIntroFragmentToColdcardRecoverFragment(
                            isMembershipFlow = isMembershipFlow,
                            scanQrCode = it == ColdCardAction.QR,
                            isAddInheritanceKey = isAddInheritanceKey
                        )
                    )
                }

                else -> {}
            }
        }
    }

    companion object {
        const val REQUEST_KEY = "ColdCardIntroFragment"
    }
}


@Composable
internal fun ColdCardIntroScreen(
    remainTime: Int = 0,
    isFromAddKey: Boolean = false,
    isMembershipFlow: Boolean = false,
    mk4Activity: Mk4Activity? = null,
    onColdCardAction: (ColdCardAction) -> Unit = {}
) {
    val onChainAddSignerParam = mk4Activity?.onChainAddSignerParam
    // Only the verification of an on-chain timelock key may name a spending path and an account;
    // an off-chain inheritance key is re-added like any other key.
    val isVerifyOnChainTimelockBackup = onChainAddSignerParam.isVerifyOnChainTimelockBackup()
    val isVerifyBackupSeedPhrase = onChainAddSignerParam?.isVerifyBackupSeedPhrase() == true
    val isClaiming = onChainAddSignerParam?.isClaiming == true
    val isAddInheritanceOffChainSigner =
        onChainAddSignerParam?.isAddInheritanceOffChainSigner() == true
    // Only the on-chain timelock wallet adds each Coldcard twice (Acct X / Acct Y), so only it
    // gets the "(n/2)" title and the two-keys copy. An off-chain inheritance key is a single
    // ordinary add, the same as any other membership key.
    val isOnChainTimelockKey = onChainAddSignerParam.isOnChainTimelockKey()
    val onChainKeyIndex = onChainAddSignerParam?.keyIndex?.takeIf { it >= 0 } ?: 0
    NunchukTheme {
        Scaffold(topBar = {
            NcImageAppBar(
                backgroundRes = R.drawable.bg_add_coldcard_view_nfc_intro,
                title = if (isMembershipFlow && remainTime > 0) {
                    stringResource(
                        id = R.string.nc_estimate_remain_time,
                        remainTime
                    )
                } else {
                    ""
                }
            )
        }) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(innerPadding)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                    text = if (isOnChainTimelockKey) {
                        "Add COLDCARD (${onChainKeyIndex + 1}/2)"
                    } else {
                        stringResource(R.string.nc_add_coldcard_mk4)
                    },
                    style = NunchukTheme.typography.heading
                )
                // Every description here is on-chain copy: it names a spending path and the
                // account to pick for it. Off the on-chain timelock flow there is no such path,
                // so the screen carries no description at all rather than a stand-in.
                val description = when {
                    isClaiming -> null

                    isVerifyOnChainTimelockBackup ->
                        stringResource(R.string.nc_coldcard_onchain_verify_backup_desc)

                    isOnChainTimelockKey && onChainKeyIndex == 0 ->
                        stringResource(R.string.nc_coldcard_onchain_first_key_desc)

                    isOnChainTimelockKey ->
                        stringResource(
                            R.string.nc_coldcard_onchain_second_key_desc,
                            onChainKeyIndex
                        )

                    // An off-chain inheritance key — added, or having its backup verified — is an
                    // ordinary single add, and the generic "you can add via NFC, QR or file" line
                    // only repeats the actions listed right below it.
                    isVerifyBackupSeedPhrase || isAddInheritanceOffChainSigner -> null

                    else -> stringResource(R.string.nc_add_coldcard_mk4_desc)
                }
                if (description != null) {
                    NcHighlightText(
                        modifier = Modifier.padding(16.dp),
                        text = description,
                        style = NunchukTheme.typography.body
                    )
                }

                ActionItem(
                    title = stringResource(R.string.nc_add_coldcard_via_nfc),
                    iconId = R.drawable.ic_nfc_indicator_small,
                    onClick = { onColdCardAction(ColdCardAction.NFC) }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp
                )

                ActionItem(
                    title = stringResource(R.string.nc_add_coldcard_via_qr),
                    iconId = R.drawable.ic_qr,
                    onClick = { onColdCardAction(ColdCardAction.QR) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp
                )

                // The desktop hand-off asks the server for the key and waits for the desktop app to
                // add it, which is how the inheritance slot of a wallet being set up has always
                // been fillable from a Coldcard over USB. It is hidden for the two off-chain runs
                // that have no such request: a Beneficiary's claim, where the key the desktop app
                // returns does not match the plan (NUN-9558), and an inheritance replace, whose
                // dispatcher has no desktop path at all.
                val canRequestKeyFromDesktop = !isAddInheritanceOffChainSigner ||
                        (!isClaiming && onChainAddSignerParam?.isReplaceKeyFlow() != true)
                if (canRequestKeyFromDesktop) {
                    ActionItem(
                        title = stringResource(R.string.nc_add_coldcard_via_usb),
                        iconId = R.drawable.ic_usb,
                        onClick = { onColdCardAction(ColdCardAction.USB) },
                        isEnable = isFromAddKey.not() || (onChainAddSignerParam != null && !isVerifyBackupSeedPhrase),
                        subtitle = if (isFromAddKey) stringResource(R.string.nc_desktop_only) else ""
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp
                )

                ActionItem(
                    title = stringResource(R.string.nc_add_coldcard_via_file),
                    iconId = R.drawable.ic_import,
                    onClick = { onColdCardAction(ColdCardAction.FILE) }
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ColdCardIntroScreenPreview() {
    ColdCardIntroScreen()
}