package com.nunchuk.android.signer.portal.wallet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.signer.R

const val inputWalletNameRoute = "input_wallet_name/{wallet_id}"

fun NavGraphBuilder.inputWalletName(
    snackState: SnackbarHostState = SnackbarHostState(),
    onUpdateWalletNameSuccess: (String) -> Unit = { },
) {
    composable(inputWalletNameRoute, arguments = listOf(
        navArgument("wallet_id") {
            type = NavType.StringType
        }
    )) {
        val walletId = it.arguments?.getString("wallet_id").orEmpty()
        val viewModel = hiltViewModel<InputWalletNameViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        LaunchedEffect(state.isUpdateNameSuccess) {
            if (state.isUpdateNameSuccess) {
                onUpdateWalletNameSuccess(walletId)
                viewModel.markUpdateNameSuccess()
            }
        }
        InputWalletNameScreen(
            onUpdateWalletName = { name ->
                viewModel.updateWalletName(walletId, name)
            },
            snackState = snackState
        )
    }
}

fun NavController.navigateToInputWalletName(
    navOptions: NavOptions? = null,
    walletId: String,
) {
    navigate("input_wallet_name/$walletId", navOptions)
}

@Composable
fun InputWalletNameScreen(
    modifier: Modifier = Modifier,
    snackState: SnackbarHostState = SnackbarHostState(),
    onUpdateWalletName: (String) -> Unit = { },
) {
    var name by rememberSaveable { mutableStateOf("") }

    NcScaffold(
        snackState = snackState,
        modifier = modifier.systemBarsPadding(),
        topBar = {
            NcTopAppBar(
                title = stringResource(id = R.string.nc_name_your_key),
                textStyle = NunchukTheme.typography.titleLarge,
            )
        },
        bottomBar = {
            NcPrimaryDarkButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                onClick = { onUpdateWalletName(name) },
            ) {
                Text(text = stringResource(id = R.string.nc_text_continue))
            }
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Top,
            ) {
                NcTextField(
                    title = stringResource(id = R.string.nc_ssigner_text_name),
                    value = name,
                    onValueChange = { name = it },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    maxLength = 20,
                    enableMaxLength = true,
                    rightContent = {
                        if (name.isNotEmpty()) {
                            IconButton(onClick = { name = "" }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_close),
                                    contentDescription = "Close"
                                )
                            }
                        }
                    }
                )
            }
        }
    )
}

@Preview
@Composable
private fun InputWalletNameScreenPreview() {
    NunchukTheme {
        InputWalletNameScreen()
    }
}