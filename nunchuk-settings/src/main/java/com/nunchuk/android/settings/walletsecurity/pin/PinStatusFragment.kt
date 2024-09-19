package com.nunchuk.android.settings.walletsecurity.pin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.settings.R
import com.nunchuk.android.widget.NCWarningDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PinStatusFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = content {
        val viewModel = viewModel<PinStatusViewModel>()
        val uiState by viewModel.state.collectAsStateWithLifecycle()

        PinStatusContent(
            state = uiState,
            onEnablePinChange = { enable ->
                if (enable) {
                    val currentMode = viewModel.getCurrentMode()
                    when {
                        currentMode == SignInMode.EMAIL && uiState.settings.protectWalletPassword -> {
                            NCWarningDialog(requireActivity())
                                .showDialog(
                                    title = getString(R.string.nc_text_confirmation),
                                    message = getString(R.string.nc_text_disable_password),
                                    btnNo = getString(R.string.nc_cancel),
                                    btnYes = getString(R.string.nc_text_continue),
                                    onYesClick = {
                                        viewModel.disablePasswordOrPassphrase()
                                        openCreateWalletPin()
                                    }
                                )
                        }
                        currentMode == SignInMode.PRIMARY_KEY && uiState.settings.protectWalletPassphrase -> {
                            NCWarningDialog(requireActivity())
                                .showDialog(
                                    title = getString(R.string.nc_text_confirmation),
                                    message = getString(R.string.nc_text_disable_passphrase),
                                    btnNo = getString(R.string.nc_cancel),
                                    btnYes = getString(R.string.nc_text_continue),
                                    onYesClick = {
                                        viewModel.disablePasswordOrPassphrase()
                                        openCreateWalletPin()
                                    }
                                )
                        }
                        else -> openCreateWalletPin()
                    }
                } else {
                    findNavController().navigate(
                        PinStatusFragmentDirections.actionPinStatusFragmentToUnlockPinFragment(
                            isRemovePin = true
                        )
                    )
                }
            },
            onChangePin = {
                findNavController().navigate(
                    PinStatusFragmentDirections.actionPinStatusFragmentToWalletSecurityCreatePinFragment(
                        currentPin = uiState.pin
                    )
                )
            }
        )
    }

    private fun openCreateWalletPin() {
        findNavController().navigate(
            PinStatusFragmentDirections.actionPinStatusFragmentToWalletSecurityCreatePinFragment()
        )
    }
}

@Composable
fun PinStatusContent(
    modifier: Modifier = Modifier,
    state: PinStatusUiState = PinStatusUiState(),
    onEnablePinChange: (Boolean) -> Unit = { },
    onChangePin: () -> Unit = { }
) {
    val isEnable = state.pin.isNotBlank()
    NunchukTheme {
        NcScaffold(
            topBar = {
                NcTopAppBar(
                    title = stringResource(R.string.nc_protect_app_with_pin),
                    textStyle = NunchukTheme.typography.titleLarge
                )
            }
        ) { innerPadding ->
            Column(
                modifier = modifier.padding(innerPadding),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            onClick = {
                                onEnablePinChange(isEnable.not())
                            },
                        )
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = stringResource(R.string.nc_protect_app_with_pin), style = NunchukTheme.typography.body)

                    Switch(checked = isEnable, onCheckedChange = onEnablePinChange)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onChangePin, enabled = isEnable)
                        .alpha(if (isEnable) 1f else 0.4f)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_password),
                        contentDescription = "PIN"
                    )
                    Text(
                        modifier = Modifier.padding(start = 8.dp),
                        text = "Change PIN",
                        style = NunchukTheme.typography.body,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Image(
                        painter = painterResource(id = R.drawable.ic_right_arrow_dark),
                        contentDescription = "Right arrow"
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun PinStatusContentPreview() {
    PinStatusContent()
}