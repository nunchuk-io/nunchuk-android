package com.nunchuk.android.settings.walletsecurity.decoy

import androidx.fragment.app.FragmentActivity
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
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.compose.NCLabelWithIndex
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.data.model.QuickWalletParam
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.guestmode.SignInModeHolder
import com.nunchuk.android.core.util.navigateToSelectWallet
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.settings.R
import com.nunchuk.android.settings.walletsecurity.DecoyPinNoteRoute

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

fun NavController.navigateToDecoyPinNote() {
    navigate(DecoyPinNoteRoute)
}

fun NavGraphBuilder.decoyPinNoteScreen(
    activity: FragmentActivity,
    signInModeHolder: SignInModeHolder,
    navigator: NunchukNavigator,
    quickWalletParam: QuickWalletParam?,
) {
    composable<DecoyPinNoteRoute> {
        DecoyPinNoteScreen(
            isSignedAccount = signInModeHolder.getCurrentMode() == SignInMode.EMAIL
        ) {
            activity.navigateToSelectWallet(
                navigator = navigator,
                quickWalletParam = quickWalletParam,
            ) {
                navigator.returnToMainScreen(activity)
                navigator.openUnlockPinScreen(activity)
            }
        }
    }
}
