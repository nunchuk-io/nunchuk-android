package com.nunchuk.android.settings.walletsecurity.unlock

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcCircleImage2
import com.nunchuk.android.compose.NcPasswordTextField
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.showSuccess
import com.nunchuk.android.settings.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
class UnlockPinFragment : Fragment() {
    private val args: UnlockPinFragmentArgs by navArgs()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = content {
        val viewModel = viewModel<UnlockPinViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()

        LaunchedEffect(state.isSuccess) {
            if (state.isSuccess) {
                if (args.isRemovePin) {
                    showSuccess("PIN has been turned off")
                    findNavController().popBackStack()
                } else {
                    requireActivity().finish()
                }
            }
        }

        UnlockPinContent(
            isRemovePinFlow = args.isRemovePin,
            state = state,
            onUnlock = { pin ->
                if (args.isRemovePin) {
                    viewModel.removePin(pin)
                } else {
                    viewModel.unlockPin(pin)
                }
            }
        )
    }
}

@Composable
fun UnlockPinContent(
    modifier: Modifier = Modifier,
    state: UnlockPinUiState = UnlockPinUiState(),
    isRemovePinFlow: Boolean = false,
    onUnlock: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var pin by rememberSaveable {
        mutableStateOf("")
    }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(1000L)
        focusRequester.requestFocus()
    }
    var btnMessage by remember { mutableStateOf("") }
    var enable by remember { mutableStateOf(true) }
    LaunchedEffect(state.attemptCount) {
        if (state.attemptCount >= 3) {
            enable = false
            repeat(30) {
                btnMessage = context.getString(R.string.nc_try_again_after_seconds, 30 - it)
                delay(1000L)
            }
            btnMessage = ""
            enable = true
        }
    }
    BackHandler(!isRemovePinFlow) {
        // disable back
    }
    NunchukTheme {
        NcScaffold(
            modifier = modifier.systemBarsPadding(),
            topBar = {
                if (isRemovePinFlow) {
                    NcTopAppBar(title = "")
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                NcCircleImage2(
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .align(Alignment.CenterHorizontally),
                    resId = R.drawable.ic_pin_lock, size = 96.dp, iconSize = 60.dp
                )

                Text(
                    text = stringResource(id = R.string.nc_enter_your_pin),
                    style = NunchukTheme.typography.heading,
                )

                Text(
                    text = stringResource(R.string.nc_to_continue_please_enter_your_pin),
                    style = NunchukTheme.typography.body,
                )

                NcPasswordTextField(
                    modifier = Modifier.focusRequester(focusRequester),
                    title = "",
                    value = pin,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    hasError = state.isFailed,
                ) {
                    pin = it
                }

                NcPrimaryDarkButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enable,
                    onClick = { onUnlock(pin) }) {
                    Text(text = btnMessage.ifEmpty { stringResource(id = R.string.nc_text_continue) })
                }
            }
        }
    }
}

@Preview
@Composable
private fun UnlockPinContentPreview() {
    UnlockPinContent(
        isRemovePinFlow = true,
        state = UnlockPinUiState(isFailed = true, attemptCount = 3),
    )
}