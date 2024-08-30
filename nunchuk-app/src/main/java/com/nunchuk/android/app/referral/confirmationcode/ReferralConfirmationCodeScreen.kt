package com.nunchuk.android.app.referral.confirmationcode

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.nunchuk.android.R
import com.nunchuk.android.app.referral.ConfirmationCodeResultData
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcSnackbarVisuals
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcToastType
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.dialog.NcLoadingDialog

const val referralAddressRoute = "referral_confirmation_code_route/{action}/{address}"
internal const val DEFAULT_ADDRESS = "DEFAULT_ADDRESS"

const val REFERRAL_CONFIRMATION_CODE_RESULT = "referral_confirmation_code_result"

fun NavGraphBuilder.referralConfirmationCode(
    navigationController: NavController,
    snackState: SnackbarHostState
) {
    composable(
        referralAddressRoute,
        arguments = listOf(
            navArgument("address") {
                type = NavType.StringType
            },
            navArgument("action") {
                type = NavType.StringType
            }
        )
    ) {
        val viewModel = hiltViewModel<ReferralConfirmationCodeViewModel>()
        val address = it.arguments?.getString("address") ?: DEFAULT_ADDRESS
        val action = it.arguments?.getString("action") ?: ""

        val state by viewModel.state.collectAsStateWithLifecycle()
        var handledResult by remember { mutableStateOf(false) }

        LaunchedEffect(state.errorMsg) {
            state.errorMsg?.let {
                snackState.showSnackbar(
                    NcSnackbarVisuals(
                        type = NcToastType.ERROR,
                        message = it,
                    )
                )
                viewModel.onErrorMessageEventConsumed()
            }
        }

        if (state.isLoading) {
            NcLoadingDialog(
                onDismiss = {
                    viewModel.onLoadingEventConsumed()
                },
            )
        }

        if (state.token.isNullOrEmpty().not()) {
            if (handledResult.not()) {
                handledResult = true
                navigationController.previousBackStackEntry?.savedStateHandle?.set(
                    REFERRAL_CONFIRMATION_CODE_RESULT,
                    ConfirmationCodeResultData(
                        address = address,
                        action = action,
                        token = state.token!!
                    )
                )
                viewModel.onTokenEventConsumed()
                navigationController.popBackStack()
            }
        }

        ReferralConfirmationCodeScreen(
            state = state,
            snackState = snackState,
            onResendCode = {
                viewModel.sendConfirmationCodeByEmail()
            },
            onContinue = {
                viewModel.verifyConfirmationCode(it)
            }
        )
    }
}

fun NavController.navigateToReferralConfirmationCode(
    navOptions: NavOptions? = null,
    action: String,
    address: String = DEFAULT_ADDRESS
) {
    navigate("referral_confirmation_code_route/${action}/${address}", navOptions)
}

@Composable
fun ReferralConfirmationCodeScreen(
    state: ReferralConfirmationCodeState,
    snackState: SnackbarHostState = SnackbarHostState(),
    onResendCode: () -> Unit = {},
    onContinue: (String) -> Unit = {}
) {
    var code by remember { mutableStateOf("") }

    NunchukTheme {
        NcScaffold(
            modifier = Modifier
                .navigationBarsPadding()
                .statusBarsPadding(),
            snackState = snackState,
            topBar = {
                NcTopAppBar(
                    title = "Enter confirmation code",
                    textStyle = NunchukTheme.typography.titleLarge,
                    actions = {
                        Spacer(modifier = Modifier.size(LocalViewConfiguration.current.minimumTouchTargetSize))
                    })
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .fillMaxHeight()
            ) {
                val annotatedText = buildAnnotatedString {
                    append("Enter the confirmation code we sent to ${state.email}. ")
                    pushStringAnnotation(tag = "RESEND_CODE", annotation = "resend_code")
                    withStyle(
                        style = SpanStyle(
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append("Resend code")
                    }
                    pop()
                }

                Text(
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .clickable {
                            val annotations = annotatedText.getStringAnnotations(
                                tag = "RESEND_CODE",
                                start = 0,
                                end = annotatedText.length
                            )
                            if (annotations.isNotEmpty()) {
                                onResendCode()
                            }
                        },
                    text = annotatedText,
                    style = NunchukTheme.typography.body
                )
                NcTextField(
                    modifier = Modifier.padding(top = 24.dp),
                    title = "Confirmation code",
                    value = code,
                    onValueChange = {
                        code = it
                    }
                )
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    enabled = code.isNotEmpty(),
                    onClick = {
                        onContinue(code)
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
private fun ReferralConfirmationCodeScreenPreview(
) {
    NunchukTheme {
        ReferralConfirmationCodeScreen(
            state = ReferralConfirmationCodeState()
        )
    }
}