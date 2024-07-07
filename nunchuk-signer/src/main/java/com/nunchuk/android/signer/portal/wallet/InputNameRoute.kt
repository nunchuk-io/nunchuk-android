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
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.signer.R

const val inputNameRoute = "input_name"

fun NavGraphBuilder.inputName(
    snackState: SnackbarHostState = SnackbarHostState(),
    onInputName: (String) -> Unit = { },
) {
    composable(inputNameRoute) {
        InputNameScreen(
            onInputName = onInputName,
            snackState = snackState
        )
    }
}

fun NavController.navigateToInputName(
    navOptions: NavOptions? = null
) {
    navigate(inputNameRoute, navOptions)
}

@Composable
fun InputNameScreen(
    modifier: Modifier = Modifier,
    snackState: SnackbarHostState = SnackbarHostState(),
    onInputName: (String) -> Unit = { },
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
                onClick = { onInputName(name) },
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
private fun InputNameScreenPreview() {
    NunchukTheme {
        InputNameScreen()
    }
}