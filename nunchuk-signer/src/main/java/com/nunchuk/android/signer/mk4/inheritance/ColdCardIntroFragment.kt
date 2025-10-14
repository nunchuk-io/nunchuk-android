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
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.compose.content
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.ActionItem
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.share.result.GlobalResultKey
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
                    if ((requireActivity() as? Mk4Activity)?.replacedXfp.isNullOrEmpty().not()) {
                        NCInfoDialog(requireActivity())
                            .showDialog(
                                message = getString(R.string.nc_info_hardware_key_not_supported),
                            )
                    } else if (mk4Activity.onChainAddSignerParam != null) {
                        // Return hardware signer tag to parent fragment (OnChainTimelockAddKeyListFragment/OnChainTimelockByzantineAddKeyFragment)
                        setFragmentResult(
                            REQUEST_KEY,
                            bundleOf(GlobalResultKey.EXTRA_SIGNER_TAG to SignerTag.COLDCARD)
                        )
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    } else {
                        membershipStepManager.currentStep?.let { step ->
                            navigator.openAddDesktopKey(
                                requireActivity(),
                                signerTag = SignerTag.COLDCARD,
                                groupId = (requireActivity() as Mk4Activity).groupId,
                                step = step,
                                isInheritanceKey = true
                            )
                        }
                    }
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
    val isVerifyBackupSeedPhrase = mk4Activity?.onChainAddSignerParam?.isVerifyBackupSeedPhrase() == true
    NunchukTheme {
        Scaffold(topBar = {
            NcImageAppBar(
                backgroundRes = R.drawable.bg_add_coldcard_view_nfc_intro,
                title = if (isMembershipFlow) {
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
                    text = if (mk4Activity?.onChainAddSignerParam != null && isVerifyBackupSeedPhrase == false) {
                        "Add COLDCARD (${mk4Activity.onChainAddSignerParam!!.keyIndex + 1}/2)"
                    } else {
                        stringResource(R.string.nc_add_coldcard_mk4)
                    },
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = if (mk4Activity?.onChainAddSignerParam?.isAddInheritanceSigner() == true) {
                        "Each hardware device must be added twice, with both keys (before and after the timelock) coming from the same device but using different derivation paths.\n\nPlease add a key for the spending path after the timelock. On your device, select account 0 for this spending path."
                    } else {
                        stringResource(R.string.nc_add_coldcard_mk4_desc)
                    },
                    style = NunchukTheme.typography.body
                )

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

                ActionItem(
                    title = stringResource(R.string.nc_add_coldcard_via_file),
                    iconId = R.drawable.ic_import,
                    onClick = { onColdCardAction(ColdCardAction.FILE) }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp
                )

                ActionItem(
                    title = stringResource(R.string.nc_add_coldcard_via_usb),
                    iconId = R.drawable.ic_usb,
                    onClick = { onColdCardAction(ColdCardAction.USB) },
                    isEnable = isFromAddKey.not() || (mk4Activity?.onChainAddSignerParam != null && isVerifyBackupSeedPhrase == false),
                    subtitle = if (isFromAddKey) stringResource(R.string.nc_desktop_only) else ""
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