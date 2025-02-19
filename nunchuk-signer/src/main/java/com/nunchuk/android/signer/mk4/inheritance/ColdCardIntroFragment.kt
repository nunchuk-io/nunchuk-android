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
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.mk4.Mk4Activity
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.widget.NCInfoDialog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ColdCardIntroFragment : MembershipFragment(), BottomSheetOptionListener {

    private val isFromAddKey by lazy { (requireActivity() as Mk4Activity).isFromAddKey }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = content {
        val remainTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
        ColdCardIntroScreen(
            remainTime = remainTime,
            isMembershipFlow = (requireActivity() as Mk4Activity).isMembershipFlow,
            isFromAddKey = isFromAddKey
        ) {
            when (it) {
                ColdCardAction.NFC -> {
                    findNavController().navigate(
                        ColdCardIntroFragmentDirections.actionColdCardIntroFragmentToMk4InfoFragment(
                            isMembershipFlow = isFromAddKey.not(),
                            isAddInheritanceKey = isFromAddKey.not()
                        )
                    )
                }

                ColdCardAction.USB -> {
                    if ((requireActivity() as? Mk4Activity)?.replacedXfp.isNullOrEmpty().not()) {
                        NCInfoDialog(requireActivity())
                            .showDialog(
                                message = getString(R.string.nc_info_hardware_key_not_supported),
                            )
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
                            isMembershipFlow = isFromAddKey.not(),
                            scanQrCode = it == ColdCardAction.QR,
                            isAddInheritanceKey = isFromAddKey.not()
                        )
                    )
                }

                else -> {}
            }
        }
    }
}


@Composable
internal fun ColdCardIntroScreen(
    remainTime: Int = 0,
    isFromAddKey: Boolean = false,
    isMembershipFlow: Boolean = false,
    onColdCardAction: (ColdCardAction) -> Unit = {}
) {
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
                    text = stringResource(R.string.nc_add_coldcard_mk4),
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = stringResource(R.string.nc_add_coldcard_mk4_desc),
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
                    isEnable = isFromAddKey.not(),
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