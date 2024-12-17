package com.nunchuk.android.signer.tapsigner.intro

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
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.signer.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TapSignerIntroFragment : MembershipFragment() {

    @Inject
    lateinit var navigator: NunchukNavigator

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = content {
        TapSignerIntroScreen {
            when (it) {
                TapSignerAction.NFC -> {
                    findNavController().navigate(
                        TapSignerIntroFragmentDirections.actionTapSignerIntroFragmentToAddTapSignerIntroFragment(false)
                    )
                }

                TapSignerAction.RECOVER -> {
                    findNavController().navigate(
                        TapSignerIntroFragmentDirections.actionTapSignerIntroFragmentToRecoverNfcKeyGuideFragment()
                    )
                }
            }
        }
    }
}


@Composable
internal fun TapSignerIntroScreen(
    onAction: (TapSignerAction) -> Unit = {}
) {
    NunchukTheme {
        Scaffold(topBar = {
            NcImageAppBar(
                backgroundRes = R.drawable.nc_bg_tap_signer_chip
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
                    text = stringResource(R.string.nc_add_a_tapsigner),
                    style = NunchukTheme.typography.heading
                )
                Spacer(modifier = Modifier.padding(top = 24.dp))
                ActionItem(
                    title = stringResource(R.string.nc_add_tapsigner_via_nfc),
                    iconId = R.drawable.ic_nfc_card,
                    onClick = { onAction(TapSignerAction.NFC) }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp
                )

                ActionItem(
                    title = stringResource(R.string.nc_recover_tapsigner_key_from_backup),
                    iconId = R.drawable.ic_key_recovery,
                    onClick = { onAction(TapSignerAction.RECOVER) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun TapSignerIntroScreenPreview() {
    TapSignerIntroScreen()
}

enum class TapSignerAction {
    NFC,
    RECOVER
}