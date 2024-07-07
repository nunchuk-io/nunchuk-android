package com.nunchuk.android.signer.portal.passphrase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPasswordTextField
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.dialog.NcInfoDialog
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.signer.R

// mnemonic and number of words passed from previous screen
const val setPassphraseRoute = "set_passphrase/{mnemonic}/{numberOfWords}"

fun NavGraphBuilder.setPassphrase(
    onSetupPortal: (String, Int, String) -> Unit = { _, _, _ -> },
) {
    composable(setPassphraseRoute, arguments = listOf(
        navArgument("mnemonic") {
            type = NavType.StringType
        },
        navArgument("numberOfWords") {
            type = NavType.IntType
        }
    )) {
        val mnemonic = it.arguments?.getString("mnemonic").takeIf { mnemonic -> mnemonic != "-" }.orEmpty()
        val numberOfWords = it.arguments?.getInt("numberOfWords") ?: 0
        SetPassphraseScreen(
            mnemonic = mnemonic,
            numberOfWords = numberOfWords,
            onSetupPortal = onSetupPortal
        )
    }
}

fun NavController.navigateToSetPassphrase(mnemonic: String, numberOfWords: Int) {
    navigate("set_passphrase/${mnemonic.ifEmpty { "-" }}/$numberOfWords")
}

@Composable
fun SetPassphraseScreen(
    modifier: Modifier = Modifier,
    mnemonic: String,
    numberOfWords: Int,
    onSetupPortal: (String, Int, String) -> Unit = { _, _, _ -> },
) {
    val context = LocalContext.current
    var pin by rememberSaveable { mutableStateOf("") }
    var confirmPin by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf("") }
    var showConfirmWithoutPinDialog by rememberSaveable { mutableStateOf(false) }
    NcScaffold(
        modifier = modifier.navigationBarsPadding(),
        topBar = {
            NcImageAppBar(backgroundRes = R.drawable.nc_bg_pin)
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth(),
                    enabled = pin.isNotEmpty() && confirmPin.isNotEmpty(),
                    onClick = {
                        if (pin == confirmPin) {
                            onSetupPortal(mnemonic, numberOfWords, pin)
                        } else {
                            error = context.getString(R.string.nc_cvc_not_match)
                        }
                    },
                ) {
                    Text(text = stringResource(id = R.string.nc_text_continue))
                }

                NcOutlineButton(
                    modifier = Modifier
                        .fillMaxWidth(),
                    onClick = { showConfirmWithoutPinDialog = true },
                ) {
                    Text(text = stringResource(R.string.nc_continue_without_a_pin))
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Set up new PIN",
                style = NunchukTheme.typography.heading,
            )

            NcPasswordTextField(title = "New PIN", value = pin) {
                pin = it
            }

            NcPasswordTextField(title = "Confirm new PIN", value = confirmPin, error = error) {
                confirmPin = it
            }

            Spacer(modifier = Modifier.weight(1f))

            NcHintMessage(messages = listOf(ClickAbleText("The PIN code is a security feature that prevents unauthorized access to your key. Please back it up and keep it safe. You’ll need it for signing transactions.")))
        }
    }

    if (showConfirmWithoutPinDialog) {
        NcInfoDialog(
            title = stringResource(id = R.string.nc_continue_without_a_pin),
            message = stringResource(R.string.nc_confirm_setup_portal_without_pin),
            onPositiveClick = {
                showConfirmWithoutPinDialog = false
                onSetupPortal(mnemonic, numberOfWords, "")
            },
            onDismiss = {
                showConfirmWithoutPinDialog = false
            },
        )
    }
}

@Preview
@Composable
private fun SetPassphraseScreenPreview() {
    NunchukTheme {
        SetPassphraseScreen(
            mnemonic = "mnemonic",
            numberOfWords = 12
        )
    }
}