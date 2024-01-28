package com.nunchuk.android.wallet.components.alias

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.dialog.NcConfirmationDialog
import com.nunchuk.android.compose.greyLight
import com.nunchuk.android.compose.whisper
import com.nunchuk.android.wallet.R

@Composable
fun SetAliasRoute(
    setOrRemoveSuccess: (String) -> Unit = {},
    viewModel: SetAliasViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SideEffect {
        if (state.setOrRemoveSuccess) {
            setOrRemoveSuccess(state.alias)
            viewModel.onHandledSetOrRemove()
        }
    }

    SetAliasScreen(
        uiState = state,
        onSaveAlias = viewModel::onSaveAlias,
        onRemoveAlias = viewModel::onRemoveAlias,
    )
}

@Composable
fun SetAliasScreen(
    uiState: SetAliasState,
    onSaveAlias: (String) -> Unit = {},
    onRemoveAlias: () -> Unit = {},
) {
    var alias by remember(uiState.alias) { mutableStateOf(uiState.alias) }
    var showRemoveAliasDialog by remember { mutableStateOf(false) }
    NunchukTheme {
        Scaffold(
            topBar = {
                NcTopAppBar(title = "")
            },
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    onClick = { onSaveAlias(alias) },
                    enabled = alias.isNotEmpty()
                ) {
                    Text(text = stringResource(id = R.string.nc_text_save))
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.nc_wallet_alias),
                    style = NunchukTheme.typography.heading
                )

                Text(
                    modifier = Modifier.padding(top = 16.dp),
                    text = stringResource(R.string.nc_wallet_alias_desc),
                    style = NunchukTheme.typography.body
                )

                Box(modifier = Modifier.padding(top = 24.dp)) {
                    NcTextField(
                        title = stringResource(R.string.nc_wallet_alias),
                        titleStyle = NunchukTheme.typography.title,
                        value = alias,
                        onValueChange = {
                            alias = it.take(20)
                        },
                        hint = "20/20".takeIf { alias.length == 20 }
                    )

                    if (uiState.alias.isNotEmpty()) {
                        Text(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .clickable { showRemoveAliasDialog = true },
                            text = stringResource(R.string.nc_remove_alias),
                            style = NunchukTheme.typography.title.copy(textDecoration = TextDecoration.Underline)
                        )
                    }
                }

                if (uiState.alias.isNotEmpty()) {
                    NcTextField(
                        modifier = Modifier.padding(top = 16.dp),
                        title = stringResource(R.string.nc_default_name),
                        value = uiState.defaultName,
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        disableBackgroundColor = MaterialTheme.colorScheme.greyLight,
                    )
                }

                if (uiState.memberAliases.isNotEmpty()) {
                    Spacer(
                        modifier = Modifier
                            .padding(top = 24.dp)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.whisper),
                    )

                    Text(
                        modifier = Modifier.padding(top = 24.dp),
                        text = stringResource(R.string.nc_wallet_aliases_used_by_other_members),
                        style = NunchukTheme.typography.title,
                    )

                    uiState.memberAliases.forEach { (name, alias) ->
                        NcTextField(
                            modifier = Modifier.padding(top = 16.dp),
                            title = name,
                            value = alias,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            disableBackgroundColor = MaterialTheme.colorScheme.greyLight,
                        )
                    }
                }
            }
        }

        if (showRemoveAliasDialog) {
            NcConfirmationDialog(
                message = stringResource(R.string.nc_are_you_sure_you_want_to_remove_this_alias),
                onDismiss = { showRemoveAliasDialog = false },
                onPositiveClick = {
                    onRemoveAlias()
                    showRemoveAliasDialog = false
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SetAliasScreenPreview() {
    NunchukTheme {
        SetAliasScreen(
            uiState = SetAliasState(
                alias = "Alias name",
                defaultName = "Default name",
                memberAliases = mapOf(
                    "John Doe" to "John's alias",
                    "Jane Doe" to "Jane's alias",
                )
            ),
            onSaveAlias = {},
        )
    }
}
