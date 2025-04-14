package com.nunchuk.android.settings.walletsecurity.decoy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import com.nunchuk.android.compose.NCLabelWithIndex
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.guestmode.SignInModeHolder
import com.nunchuk.android.core.util.navigateToSelectWallet
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.settings.R
import com.nunchuk.android.settings.walletsecurity.WalletSecuritySettingActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DecoyPinNoteFragment : Fragment() {
    @Inject
    lateinit var signInModeHolder: SignInModeHolder

    @Inject
    lateinit var navigator: NunchukNavigator

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = content {
        DecoyPinNoteScreen(
            isSignedAccount = signInModeHolder.getCurrentMode() == SignInMode.EMAIL
        ) {
            requireActivity().navigateToSelectWallet(
                navigator = navigator,
                quickWalletParam = (activity as? WalletSecuritySettingActivity)?.args?.quickWalletParam,
            ) {
                navigator.returnToMainScreen(requireActivity())
                navigator.openUnlockPinScreen(requireActivity())
            }
        }
    }
}

@Composable
fun DecoyPinNoteScreen(
    modifier: Modifier = Modifier,
    isSignedAccount: Boolean = false,
    onAction: () -> Unit = {}
) {
    NunchukTheme {
        NcScaffold(
            topBar = {
            NcImageAppBar(
                backgroundRes = R.drawable.bg_decoy_pin_note
            )
        }, bottomBar = {
            NcPrimaryDarkButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                onClick = onAction,
            ) {
                Text(text = stringResource(R.string.nc_text_got_it))
            }
        }, modifier = modifier.navigationBarsPadding()
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.nc_text_decoy_pin_note),
                    style = NunchukTheme.typography.heading,
                    modifier = Modifier.padding(top = 16.dp)
                )

                if (isSignedAccount) {
                    NCLabelWithIndex(
                        index = 1,
                        title = stringResource(R.string.nc_using_decoy_pin_title),
                        label = stringResource(R.string.nc_using_decoy_pin_for_signed_account_desc)
                    )

                    NCLabelWithIndex(
                        index = 2,
                        title = stringResource(R.string.nc_disabling_pin_title),
                        label = stringResource(R.string.nc_disabling_pin_for_signed_account_desc)
                    )
                } else {
                    NCLabelWithIndex(
                        index = 1,
                        title = stringResource(R.string.nc_using_decoy_pin_title),
                        label = stringResource(R.string.nc_using_decoy_pin_for_guest_account_desc)
                    )

                    NCLabelWithIndex(
                        index = 2,
                        title = stringResource(R.string.nc_disabling_pin_title),
                        label = stringResource(R.string.nc_disabling_pin_for_guest_account_desc)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun DecoyPinNoteScreenPreview() {
    DecoyPinNoteScreen()
}

@Preview
@Composable
private fun DecoyPinNoteScreenGuestPreview() {
    DecoyPinNoteScreen(isSignedAccount = false)
}