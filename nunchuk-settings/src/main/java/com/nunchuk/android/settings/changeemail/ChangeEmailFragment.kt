package com.nunchuk.android.settings.changeemail

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.domain.membership.TargetAction
import com.nunchuk.android.core.manager.NcToastManager
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.hideLoading
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.model.VerificationType
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.settings.R
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.utils.serializable
import com.nunchuk.android.widget.NCInfoDialog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ChangeEmailFragment : Fragment() {

    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel: ChangeEmailViewModel by viewModels()

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it.data?.extras
            if (it.resultCode == Activity.RESULT_OK && data != null) {
                val isDoLater = data.getBoolean(GlobalResultKey.DUMMY_TX_INTRO_DO_LATER, false)
                if (isDoLater) {
                    requireActivity().finish()
                } else {
                    val confirmCodeMap =
                        data.serializable<HashMap<String, String>>(GlobalResultKey.CONFIRM_CODE)
                            .orEmpty()
                    val securityQuestionToken =
                        data.getString(GlobalResultKey.SECURITY_QUESTION_TOKEN).orEmpty()
                    if (confirmCodeMap.isNotEmpty()) {
                        viewModel.calculateRequiredSignatures(
                            confirmCodeToken = confirmCodeMap[GlobalResultKey.CONFIRM_CODE_TOKEN].orEmpty(),
                            confirmCodeNonce = confirmCodeMap[GlobalResultKey.CONFIRM_CODE_NONCE].orEmpty()
                        )
                    } else if (securityQuestionToken.isNotEmpty()) {
                        viewModel.changeEmail(
                            securityQuestionToken = securityQuestionToken
                        )
                    } else {
                        viewModel.handleSignOutEvent()
                    }
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ChangeEmailScreen(viewModel,
                    onEmailChangeClick = { email ->
                        if (viewModel.isEmailValid(email)) {
                            navigator.openWalletAuthentication(
                                walletId = "",
                                userData = viewModel.getUserData(),
                                requiredSignatures = 0,
                                type = VerificationType.CONFIRMATION_CODE,
                                launcher = launcher,
                                action = TargetAction.CHANGE_EMAIL.name,
                                newEmail = email,
                                activityContext = requireActivity()
                            )
                        }
                    })
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) { event ->
            when (event) {
                is ChangeEmailEvent.Error -> {
                    showOrHideLoading(false)
                    NCInfoDialog(requireActivity())
                        .showDialog(
                            title = getString(R.string.nc_error),
                            message = event.message,
                            btnYes = getString(R.string.nc_ok)
                        )
                }

                is ChangeEmailEvent.Loading -> {
                    showOrHideLoading(event.loading)
                }

                is ChangeEmailEvent.CalculateRequiredSignaturesSuccess -> {
                    navigator.openWalletAuthentication(
                        walletId = event.walletId,
                        userData = event.userData,
                        requiredSignatures = event.requiredSignatures,
                        type = event.type,
                        launcher = launcher,
                        action = TargetAction.CHANGE_EMAIL.name,
                        dummyTransactionId = event.dummyTransactionId,
                        newEmail = event.newEmail,
                        groupId = event.groupId,
                        activityContext = requireActivity()
                    )
                }

                ChangeEmailEvent.SignOutEvent -> {
                    handleLogout()
                }
            }
        }
    }

    private fun handleLogout() {
        hideLoading()
        NcToastManager.scheduleShowMessage(
            message = getString(R.string.nc_email_has_been_changed),
        )
        navigator.restartApp(requireActivity())
    }

}

@Composable
fun ChangeEmailScreen(
    viewModel: ChangeEmailViewModel = viewModel(),
    onEmailChangeClick: (String) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ChangeEmailScreenContent(
        email = state.email,
        onEmailChangeClick = onEmailChangeClick,
        onInputChange = viewModel::updateEmail,
        isValidEmail = state.isValidEmail
    )
}

@Composable
fun ChangeEmailScreenContent(
    email: String = "",
    isValidEmail: Boolean = false,
    onEmailChangeClick: (String) -> Unit = {},
    onInputChange: (String) -> Unit = {},
) {
    NunchukTheme {
        NcScaffold(
            modifier = Modifier
                .navigationBarsPadding()
                .statusBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = stringResource(id = R.string.nc_change_email),
                    textStyle = NunchukTheme.typography.titleLarge,
                    actions = {
                        Spacer(modifier = Modifier.size(LocalViewConfiguration.current.minimumTouchTargetSize))
                    },
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxHeight()
            ) {
                NcTextField(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    title = stringResource(id = R.string.nc_new_email),
                    value = email,
                    error = if (isValidEmail.not()) stringResource(id = R.string.nc_text_email_invalid) else "",
                    onValueChange = onInputChange
                )
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    enabled = email.isNotEmpty(),
                    onClick = {
                        onEmailChangeClick(email)
                    }
                ) {
                    Text(text = stringResource(id = R.string.nc_change_email))
                }
            }
        }
    }
}

@Preview
@Composable
private fun ChangeEmailScreenContentPreview() {
    ChangeEmailScreenContent(email = "nugenthomas@gmail.com", isValidEmail = false)
}