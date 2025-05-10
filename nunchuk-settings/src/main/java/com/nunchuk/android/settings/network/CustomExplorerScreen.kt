package com.nunchuk.android.settings.network

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcRadioButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcSnackbarVisuals
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcToastType
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.greyLight
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.settings.R
import kotlinx.coroutines.launch

@Composable
fun CustomExplorerScreen(
    viewModel: CustomExplorerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CustomExplorerContent(
        uiState = uiState,
        onSave = { url, isCustom ->
            viewModel.saveCustomExplorer(url, isCustom)
        },
    )
}

@Composable
fun CustomExplorerContent(
    uiState: CustomExplorerUiState = CustomExplorerUiState(),
    onSave: (String, Boolean) -> Unit = { _, _ -> },
) {
    var isCustomSelected by remember(uiState.isCustomSelected) { mutableStateOf(uiState.isCustomSelected) }
    var customText by remember(uiState.customUrl) { mutableStateOf(uiState.customUrl) }
    val snackState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    NunchukTheme {
        NcScaffold(
            snackState = snackState,
            modifier = Modifier.systemBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = stringResource(R.string.nc_custom_blockchain_explorer),
                    textStyle = NunchukTheme.typography.titleLarge,
                )
            },
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = {
                        onSave(customText, isCustomSelected)
                        coroutineScope.launch {
                            snackState.showSnackbar(
                                NcSnackbarVisuals(
                                    type = NcToastType.SUCCESS,
                                    message = context.getString(R.string.nc_update_saved),
                                )
                            )
                        }
                    },
                    enabled = !isCustomSelected || customText.isNotBlank()
                ) {
                    Text(text = stringResource(R.string.nc_text_save))
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    NcTextField(
                        title = stringResource(R.string.nc_default),
                        value = uiState.defaultUrl,
                        singleLine = true,
                        readOnly = true,
                        enabled = false,
                        modifier = Modifier.weight(1f),
                        disableBackgroundColor = MaterialTheme.colorScheme.greyLight,
                        textStyle = NunchukTheme.typography.body.copy(color = MaterialTheme.colorScheme.textSecondary)
                    ) {
                    }
                    NcRadioButton(
                        modifier = Modifier
                            .align(Alignment.Bottom)
                            .padding(bottom = 12.dp)
                            .size(24.dp),
                        selected = !isCustomSelected,
                        onClick = { isCustomSelected = false },
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    NcTextField(
                        title = stringResource(R.string.nc_customize_blockchain_explorer),
                        value = customText,
                        singleLine = true,
                        enabled = isCustomSelected,
                        disableBackgroundColor = MaterialTheme.colorScheme.greyLight,
                        modifier = Modifier.weight(1f)
                    ) {
                        customText = it
                    }
                    NcRadioButton(
                        modifier = Modifier
                            .align(Alignment.Bottom)
                            .padding(bottom = 12.dp)
                            .size(24.dp),
                        selected = isCustomSelected,
                        onClick = { isCustomSelected = true },
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun CustomExplorerScreenPreview() {
    CustomExplorerContent()
}