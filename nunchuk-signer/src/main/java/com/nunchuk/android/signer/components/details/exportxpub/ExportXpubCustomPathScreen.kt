package com.nunchuk.android.signer.components.details.exportxpub

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.signer.R

/** Full derivation path to export; it replaces the preset path rather than extending it. */
@Composable
internal fun ExportXpubCustomPathContent(
    path: String = "",
    isPathValid: Boolean? = null,
    onPathChanged: (String) -> Unit = {},
    onBack: () -> Unit = {},
    onContinue: () -> Unit = {},
) {
    NcScaffold(
        modifier = Modifier
            .navigationBarsPadding()
            .imePadding(),
        topBar = {
            NcTopAppBar(
                title = stringResource(R.string.nc_custom_derivation_path),
                textStyle = NunchukTheme.typography.titleLarge,
                onBackPress = onBack,
            )
        },
        bottomBar = {
            NcPrimaryDarkButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                enabled = isPathValid == true,
                onClick = onContinue,
            ) {
                Text(text = stringResource(com.nunchuk.android.core.R.string.nc_text_continue))
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.nc_custom_derivation_path_desc),
                style = NunchukTheme.typography.body,
            )
            NcTextField(
                modifier = Modifier.padding(top = 16.dp),
                title = stringResource(R.string.nc_signer_derivation_path),
                value = path,
                onValueChange = onPathChanged,
                hasError = isPathValid == false,
                // The design's supporting text is body-size without the default icon; NcTextField
                // provides the error colour while hasError is set.
                bottomContent = {
                    Text(
                        modifier = Modifier.padding(top = 16.dp),
                        text = stringResource(
                            if (isPathValid == false) R.string.nc_custom_derivation_path_invalid
                            else R.string.nc_custom_derivation_path_hint
                        ),
                        style = NunchukTheme.typography.body,
                        color = if (isPathValid == false) LocalContentColor.current else Color.Unspecified,
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Done,
                ),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ExportXpubCustomPathContentPreview() {
    NunchukTheme {
        ExportXpubCustomPathContent(path = "m/84h/0h/0h", isPathValid = true)
    }
}

@PreviewLightDark
@Composable
private fun ExportXpubCustomPathContentInvalidPreview() {
    NunchukTheme {
        ExportXpubCustomPathContent(path = "m/84h/0h/account", isPathValid = false)
    }
}
