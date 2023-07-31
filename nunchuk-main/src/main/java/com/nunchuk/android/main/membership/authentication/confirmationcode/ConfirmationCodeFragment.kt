package com.nunchuk.android.main.membership.authentication.confirmationcode

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.main.R
import com.nunchuk.android.share.result.GlobalResultKey
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConfirmationCodeFragment : Fragment() {

    private val viewModel: ConfirmationCodeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ConfirmChangeScreen(viewModel)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) { event ->
            when (event) {
                is ConfirmChangeEvent.Error -> showError(message = event.message)
                is ConfirmChangeEvent.Loading -> showOrHideLoading(loading = event.loading)
                ConfirmChangeEvent.RequestCodeError -> requireActivity().finish()
                is ConfirmChangeEvent.VerifyCodeSuccess -> {
                    requireActivity().setResult(Activity.RESULT_OK, Intent().apply {
                        putExtra(
                            GlobalResultKey.SIGNATURE_EXTRA,
                            HashMap<String, String>()
                        )
                        putExtra(GlobalResultKey.SECURITY_QUESTION_TOKEN, "")
                        putExtra(GlobalResultKey.CONFIRM_CODE, HashMap<String, String>().apply {
                            put(GlobalResultKey.CONFIRM_CODE_TOKEN, event.token)
                            put(GlobalResultKey.CONFIRM_CODE_NONCE, event.nonce)
                        })
                    })
                    requireActivity().finish()
                }
            }
        }
    }

}

@Composable
fun ConfirmChangeScreen(
    viewModel: ConfirmationCodeViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ConfirmChangeScreenContent(
        code = state.code,
        email = viewModel.email,
        onInputChange = viewModel::onCodeChange,
        onContinueClick = viewModel::verifyCode
    )
}

@Composable
fun ConfirmChangeScreenContent(
    code: String = "",
    email: String = "",
    onInputChange: (String) -> Unit = {},
    onContinueClick: () -> Unit = {},
) {
    NunchukTheme {
        Scaffold(
            modifier = Modifier
                .navigationBarsPadding()
                .statusBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = stringResource(id = R.string.nc_confirm_changes),
                    textStyle = NunchukTheme.typography.titleLarge,
                    actions = {
                        Spacer(modifier = Modifier.size(LocalViewConfiguration.current.minimumTouchTargetSize))
                    })
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxHeight()
            ) {
                NcHintMessage(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                    messages = listOf(
                        ClickAbleText(
                            content = stringResource(
                                R.string.nc_confirm_changes_msg,
                                email
                            )
                        )
                    )
                )
                NcTextField(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    title = stringResource(id = R.string.nc_code),
                    value = code,
                    onValueChange = onInputChange
                )
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    enabled = code.isNotEmpty(),
                    onClick = {
                        onContinueClick()
                    }
                ) {
                    Text(text = stringResource(id = R.string.nc_text_continue))
                }
            }
        }
    }
}

@Preview
@Composable
private fun ConfirmChangeScreenContentPreview() {
    ConfirmChangeScreenContent(email = "nugenthomas@gmail.com")
}