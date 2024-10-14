package com.nunchuk.android.settings.network

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcSnackBarHost
import com.nunchuk.android.compose.NcSnackbarVisuals
import com.nunchuk.android.compose.NcToastType
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.sheet.input.InputBottomSheet
import com.nunchuk.android.core.sheet.input.InputBottomSheetListener
import com.nunchuk.android.model.ElectrumServer
import com.nunchuk.android.model.RemoteElectrumServer
import com.nunchuk.android.model.StateEvent
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.settings.R
import com.nunchuk.android.type.Chain
import com.nunchuk.android.widget.NCWarningDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SelectElectrumServerActivity : FragmentActivity(), InputBottomSheetListener {
    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel: SelectElectrumServerViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SelectElectrumServerScreen(
                viewModel = viewModel,
                onSelectServer = { url, name, showMessage ->
                    setResult(
                        Activity.RESULT_OK,
                        Intent().apply {
                            putExtra(EXTRA_SERVER, url)
                            putExtra(EXTRA_NAME, name)
                            putExtra(EXTRA_SHOW_MESSAGE, showMessage)
                        },
                    )
                    if (showMessage) {
                        handleUpdateAppSettingsSuccess()
                    } else {
                        finish()
                    }
                },
                onPreSelectServer = { url, name ->
                    setResult(
                        Activity.RESULT_OK,
                        Intent().apply {
                            putExtra(EXTRA_SERVER, url)
                            putExtra(EXTRA_NAME, name)
                        },
                    )
                },
                showAddSeverBottomSheet = {
                    InputBottomSheet.show(
                        fragmentManager = supportFragmentManager,
                        title = "Enter the server’s address",
                        currentInput = ""
                    )
                },
                onSignOutSuccess = {
                    restartApp()
                }
            )
        }
    }

    private fun handleUpdateAppSettingsSuccess() {
        NCWarningDialog(this).showDialog(
            title = getString(R.string.nc_text_app_restart_required),
            message = getString(R.string.nc_text_app_restart_des),
            btnYes = getString(R.string.nc_text_restart),
            btnNo = getString(R.string.nc_text_discard),
            onYesClick = {
                viewModel.signOut()
            },
            onNoClick = {
                finish()
            }
        )
    }

    private fun restartApp() {
        navigator.restartApp(this)
    }

    override fun onInputDone(newInput: String) {
        viewModel.onAddNewServer(newInput)
    }

    companion object {
        internal const val EXTRA_CHAIN = "chain"
        internal const val EXTRA_SERVER = "server"
        internal const val EXTRA_NAME = "name"
        internal const val EXTRA_SHOW_MESSAGE = "show_message"
        fun buildIntent(activity: Activity, chain: Chain, server: String) =
            Intent(
                activity,
                SelectElectrumServerActivity::class.java
            ).apply {
                putExtra(EXTRA_CHAIN, chain)
                putExtra(EXTRA_SERVER, server)
            }
    }
}

@Composable
private fun SelectElectrumServerScreen(
    viewModel: SelectElectrumServerViewModel = hiltViewModel(),
    onSelectServer: (String, String, Boolean) -> Unit = { _, _, _ -> },
    onPreSelectServer: (String, String) -> Unit = { _, _ -> },
    showAddSeverBottomSheet: () -> Unit = {},
    onSignOutSuccess: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.addSuccessEvent) {
        if (uiState.addSuccessEvent is StateEvent.String) {
            val url = (uiState.addSuccessEvent as StateEvent.String).data
            onSelectServer((uiState.addSuccessEvent as StateEvent.String).data, url, true)
            viewModel.onHandleAddSuccessEvent()
        }
    }
    LaunchedEffect(uiState.autoSelectServer) {
        if (uiState.autoSelectServer is StateEvent.Unit) {
            uiState.remoteServers.firstOrNull()?.let { remote ->
                onPreSelectServer(remote.url, remote.name)
            }
            viewModel.onHandleAutoSelectServer()
        }
    }
    LaunchedEffect(uiState.logoutEvent) {
        if (uiState.logoutEvent is StateEvent.Unit) {
            onSignOutSuccess()
            viewModel.onHandleLogoutEvent()
        }
    }
    SelectElectrumServerContent(
        uiState = uiState,
        onSave = viewModel::onSave,
        onSelectServer = onSelectServer,
        onRemove = viewModel::onRemove,
        showAddSeverBottomSheet = showAddSeverBottomSheet,
    )
}

@Composable
private fun SelectElectrumServerContent(
    uiState: SelectElectrumServerUiState = SelectElectrumServerUiState(),
    onSave: () -> Unit = {},
    onSelectServer: (String, String, Boolean) -> Unit = { _, _, _ -> },
    onRemove: (Long) -> Unit = {},
    showAddSeverBottomSheet: () -> Unit = {},
) {
    var isEditing by rememberSaveable { mutableStateOf(false) }
    BackHandler(isEditing) {
        isEditing = false
    }
    val snackBarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    NunchukTheme {
        Scaffold(
            snackbarHost = {
                NcSnackBarHost(state = snackBarHostState)
            },
            topBar = {
                NcTopAppBar(
                    title = if (isEditing) {
                        stringResource(id = R.string.nc_mainnet_server)
                    } else {
                        stringResource(R.string.nc_select_mainnet_server)
                    },
                    textStyle = NunchukTheme.typography.titleLarge,
                    actions = {
                        TextButton(
                            onClick = {
                                if (isEditing) {
                                    if (uiState.pendingRemoveIds.isNotEmpty()) {
                                        onSave()
                                        coroutineScope.launch {
                                            snackBarHostState.showSnackbar(
                                                NcSnackbarVisuals(
                                                    type = NcToastType.SUCCESS,
                                                    message = context.getString(R.string.nc_server_list_updated),
                                                )
                                            )
                                        }
                                    }
                                }
                                isEditing = !isEditing
                            },
                            enabled = uiState.localElectrumServers.isNotEmpty(),
                        ) {
                            Text(
                                text = if (isEditing)
                                    stringResource(id = R.string.nc_text_save)
                                else
                                    stringResource(id = R.string.nc_edit),
                                style = NunchukTheme.typography.textLink,
                            )
                        }
                    },
                )
            }, bottomBar = {
                if (!isEditing) {
                    NcOutlineButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        onClick = { showAddSeverBottomSheet() }
                    ) {
                        Text(text = stringResource(id = R.string.nc_add_electrum_server))
                    }
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                items(uiState.remoteServers) {
                    ElectrumSeverItem(
                        isRemote = true,
                        name = it.name,
                        isSelected = it.url == uiState.server,
                        isEditing = false,
                        onSelectServer = { onSelectServer(it.url, it.name, false) },
                        onRemove = {}
                    )
                }
                items(
                    uiState.localElectrumServers.filter { !uiState.pendingRemoveIds.contains(it.id) },
                    key = { it.id },
                ) {
                    ElectrumSeverItem(
                        isRemote = false,
                        name = it.url,
                        isSelected = it.url == uiState.server,
                        isEditing = isEditing,
                        onSelectServer = {
                            onSelectServer(it.url, it.url, false)
                        },
                        onRemove = {
                            onRemove(it.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ElectrumSeverItem(
    modifier: Modifier = Modifier,
    isRemote: Boolean,
    name: String,
    isSelected: Boolean,
    isEditing: Boolean,
    onSelectServer: () -> Unit = {},
    onRemove: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .clickable(enabled = !isEditing, onClick = onSelectServer)
            .padding(
                vertical = 12.dp,
                horizontal = 16.dp
            ),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!isRemote) {
            Image(
                modifier = Modifier.size(24.dp),
                painter = painterResource(id = R.drawable.ic_accounts_settings),
                contentDescription = "Icon user",
            )
        }
        Text(
            modifier = Modifier.weight(1f),
            style = NunchukTheme.typography.body,
            text = name,
        )
        if (isEditing) {
            Image(
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onRemove),
                painter = painterResource(id = R.drawable.ic_close),
                contentDescription = "Icon close",
            )
        } else if (isSelected) {
            Image(
                modifier = Modifier.size(24.dp),
                painter = painterResource(id = R.drawable.ic_check),
                contentDescription = "Icon check",
            )
        }
    }
}

@Composable
@Preview
private fun SelectElectrumServerContentPreview() {
    SelectElectrumServerContent(
        SelectElectrumServerUiState(
            remoteServers = listOf(
                RemoteElectrumServer("Server 1", "https://server1.com"),
                RemoteElectrumServer("Server 2", "https://server2.com"),
                RemoteElectrumServer("Server 3", "https://server3.com"),
            ),
            localElectrumServers = listOf(
                ElectrumServer(url = "https://server4.com", chain = Chain.MAIN),
                ElectrumServer(url = "https://server5.com", chain = Chain.MAIN),
                ElectrumServer(url = "https://server6.com", chain = Chain.MAIN),
            ),
        )
    )
}