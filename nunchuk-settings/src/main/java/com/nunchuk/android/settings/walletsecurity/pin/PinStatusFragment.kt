package com.nunchuk.android.settings.walletsecurity.pin

import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcSwitch
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.settings.R
import com.nunchuk.android.settings.walletsecurity.PinStatusRoute
import com.nunchuk.android.widget.NCWarningDialog

@Composable
fun PinStatusContent(
    modifier: Modifier = Modifier,
    state: PinStatusUiState = PinStatusUiState(),
    onEnablePinChange: (Boolean) -> Unit = { },
    onChangePin: () -> Unit = { }
) {
    val isEnable = state.isAppPinEnable && state.isCustomPinEnable
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

                    NcSwitch(checked = isEnable, onCheckedChange = onEnablePinChange)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onChangePin, enabled = isEnable)
                        .alpha(if (isEnable) 1f else 0.4f)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NcIcon(
                        painter = painterResource(id = R.drawable.ic_password),
                        contentDescription = "PIN"
                    )
                    Text(
                        modifier = Modifier.padding(start = 8.dp),
                        text = "Change PIN",
                        style = NunchukTheme.typography.body,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    NcIcon(
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

fun NavController.navigateToPinStatus() {
    navigate(PinStatusRoute)
}

fun NavGraphBuilder.pinStatusScreen(
    activity: FragmentActivity,
    onOpenCreatePin: (Boolean) -> Unit,
    onOpenUnlockPin: () -> Unit,
) {
    composable<PinStatusRoute> {
        val viewModel = hiltViewModel<PinStatusViewModel>()
        val uiState by viewModel.state.collectAsStateWithLifecycle()

        LifecycleResumeEffect(Unit) {
            viewModel.checkCustomPinConfig()
            onPauseOrDispose { }
        }

        PinStatusContent(
            state = uiState,
            onEnablePinChange = { enable ->
                if (enable) {
                    val currentMode = viewModel.getCurrentMode()
                    when {
                        currentMode == SignInMode.EMAIL && uiState.settings.protectWalletPassword -> {
                            NCWarningDialog(activity).showDialog(
                                title = activity.getString(R.string.nc_text_confirmation),
                                message = activity.getString(R.string.nc_text_disable_password),
                                btnNo = activity.getString(R.string.nc_cancel),
                                btnYes = activity.getString(R.string.nc_text_continue),
                                onYesClick = {
                                    viewModel.disablePasswordOrPassphrase()
                                    onOpenCreatePin(false)
                                }
                            )
                        }

                        currentMode == SignInMode.PRIMARY_KEY && uiState.settings.protectWalletPassphrase -> {
                            NCWarningDialog(activity).showDialog(
                                title = activity.getString(R.string.nc_text_confirmation),
                                message = activity.getString(R.string.nc_text_disable_passphrase),
                                btnNo = activity.getString(R.string.nc_cancel),
                                btnYes = activity.getString(R.string.nc_text_continue),
                                onYesClick = {
                                    viewModel.disablePasswordOrPassphrase()
                                    onOpenCreatePin(false)
                                }
                            )
                        }

                        else -> {
                            onOpenCreatePin(false)
                        }
                    }
                } else {
                    onOpenUnlockPin()
                }
            },
            onChangePin = {
                onOpenCreatePin(uiState.isAppPinEnable)
            }
        )
    }
}
