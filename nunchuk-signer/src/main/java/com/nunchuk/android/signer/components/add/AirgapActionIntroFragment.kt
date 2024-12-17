package com.nunchuk.android.signer.components.add

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.fragment.compose.content
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.ActionItem
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.signer.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AirgapActionIntroFragment : MembershipFragment() {

    @Inject
    lateinit var navigator: NunchukNavigator

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = content {
        AirgapActionIntroScreen {
            when (it) {
                JADEAction.QR -> {
                    findNavController().navigate(
                        AirgapActionIntroFragmentDirections.actionAirgapActionIntroFragmentToAirgapIntroFragment()
                    )
                }

                JADEAction.USB -> {

                }
            }
        }
    }
}


@Composable
internal fun AirgapActionIntroScreen(
    onAction: (JADEAction) -> Unit = {}
) {
    NunchukTheme {
        Scaffold(topBar = {
            NcImageAppBar(
                backgroundRes = R.drawable.bg_airgap_jade_intro
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
                    text = stringResource(R.string.nc_add_jade),
                    style = NunchukTheme.typography.heading
                )
                Spacer(modifier = Modifier.padding(top = 24.dp))
                ActionItem(
                    title = stringResource(R.string.nc_add_jade_via_qr),
                    iconId = R.drawable.ic_nfc_card,
                    onClick = { onAction(JADEAction.QR) }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp
                )

                ActionItem(
                    title = stringResource(R.string.nc_add_jade_via_usb),
                    iconId = R.drawable.ic_usb,
                    onClick = { onAction(JADEAction.USB) },
                    isEnable = false,
                    subtitle = stringResource(R.string.nc_desktop_only)
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun AirgapActionIntroScreenPreview() {
    AirgapActionIntroScreen()
}

enum class JADEAction {
    QR,
    USB
}