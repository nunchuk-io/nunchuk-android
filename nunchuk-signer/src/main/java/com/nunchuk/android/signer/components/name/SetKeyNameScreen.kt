package com.nunchuk.android.signer.components.name

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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.signer.R

/**
 * "Name your key" step, shared by the hardware add-key flows (Trezor, Ledger) where the user
 * names the key themselves. Assisted/group membership flows skip this screen and auto-name
 * the key instead.
 */
@Composable
fun SetKeyNameScreen(
    defaultName: String = "",
    onBack: () -> Unit = {},
    onContinue: (String) -> Unit = {}
) {
    var name by rememberSaveable(defaultName) { mutableStateOf(defaultName) }

    NcScaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            NcTopAppBar(
                title = stringResource(id = R.string.nc_name_your_key),
                textStyle = NunchukTheme.typography.titleLarge,
                onBackPress = onBack
            )
        },
        bottomBar = {
            NcPrimaryDarkButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                enabled = name.isNotBlank(),
                onClick = { onContinue(name.trim()) },
            ) {
                Text(text = stringResource(id = com.nunchuk.android.core.R.string.nc_text_continue))
            }
        }
    ) { paddingValues ->
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
                    imeAction = ImeAction.Done,
                    capitalization = KeyboardCapitalization.Sentences,
                ),
                maxLength = 20,
                enableMaxLength = true,
                rightContent = {
                    if (name.isNotEmpty()) {
                        IconButton(onClick = { name = "" }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_close),
                                contentDescription = "Clear key name"
                            )
                        }
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SetKeyNameScreenPreview() {
    NunchukTheme {
        SetKeyNameScreen(defaultName = "Ledger")
    }
}
