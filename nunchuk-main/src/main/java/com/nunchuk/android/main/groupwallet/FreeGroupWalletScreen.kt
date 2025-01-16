package com.nunchuk.android.main.groupwallet

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcSelectableBottomSheet
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.dialog.NcLoadingDialog
import com.nunchuk.android.compose.provider.SignersModelProvider
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.main.R
import com.nunchuk.android.main.groupwallet.component.FreeAddKeyCard
import com.nunchuk.android.main.groupwallet.component.WalletInfo
import com.nunchuk.android.main.membership.key.list.SelectSignerBottomSheet
import com.nunchuk.android.main.membership.key.list.TapSignerListBottomSheetFragmentArgs
import com.nunchuk.android.type.SignerType

const val freeGroupWalletRoute = "free_group_wallet"
val avatarColors = listOf(
    Color(0xFF1C652D),
    Color(0xFFA66800),
    Color(0xFFCF4018),
    Color(0xFF7E519B),
    Color(0xFF2F466C),
    Color(0xFFF1AE00),
    Color(0xFF757575),
)

fun NavGraphBuilder.freeGroupWallet(
    viewModel: FreeGroupWalletViewModel,
    onEditClicked: (String) -> Unit = {},
    onCopyLinkClicked: (String) -> Unit = {},
    onShowQRCodeClicked: (String) -> Unit = {},
    onAddNewKey: (Int) -> Unit = {},
    onAddExistingKey: (SignerModel, Int) -> Unit,
    onDeleteGroup: () -> Unit,
) {
    composable(
        route = freeGroupWalletRoute,
    ) {
        val state by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(state.isGroupDeleted) {
            if (state.isGroupDeleted) {
                onDeleteGroup()
            }
        }

        if (state.isLoading) {
            NcLoadingDialog()
        }

        LifecycleResumeEffect(Unit) {
            viewModel.getGroupSandbox()
            onPauseOrDispose { }
        }

        FreeGroupWalletScreen(
            state = state,
            onAddNewKey = onAddNewKey,
            onContinueClicked = {},
            onEditClicked = {
                state.group?.let {
                    onEditClicked(it.id)
                }
            },
            onCopyLinkClicked = onCopyLinkClicked,
            onShowQRCodeClicked = onShowQRCodeClicked,
            onRemoveClicked = viewModel::removeSignerFromGroup,
            onAddExistingKey = onAddExistingKey,
            onDeleteGroupClicked = viewModel::deleteGroupSandbox
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreeGroupWalletScreen(
    state: FreeGroupWalletUiState = FreeGroupWalletUiState(),
    onAddNewKey: (Int) -> Unit = {},
    onRemoveClicked: (Int) -> Unit = {},
    onContinueClicked: () -> Unit = {},
    onEditClicked: () -> Unit = {},
    onCopyLinkClicked: (String) -> Unit = {},
    onShowQRCodeClicked: (String) -> Unit = {},
    onAddExistingKey: (SignerModel, Int) -> Unit = { _, _ -> },
    onDeleteGroupClicked: () -> Unit = {},
) {
    var showSignerBottomSheet by rememberSaveable { mutableStateOf(false) }
    var showMoreOption by rememberSaveable { mutableStateOf(false) }
    var currentSignerIndex by rememberSaveable { mutableIntStateOf(-1) }
    Scaffold(
        modifier = Modifier.navigationBarsPadding(),
        topBar = {
            val onBackPressOwner = LocalOnBackPressedDispatcherOwner.current
            CenterAlignedTopAppBar(
                modifier = Modifier,
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = {
                    IconButton(onClick = { onBackPressOwner?.onBackPressedDispatcher?.onBackPressed() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_close),
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.textPrimary
                        )
                    }
                },


                title = {
                    Column {
                        Text(
                            text = stringResource(id = R.string.nc_setup_group_wallet),
                            style = NunchukTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            NcIcon(
                                painter = painterResource(id = R.drawable.ic_encrypted),
                                contentDescription = "Encrypted icon",
                                tint = colorResource(id = R.color.nc_text_secondary)
                            )
                            Text(
                                text = stringResource(id = R.string.nc_encrypted),
                                style = NunchukTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.textSecondary),
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }

                },
                actions = {
                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.textPrimary) {
                        IconButton(onClick = {
                            showMoreOption = true
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_more),
                                contentDescription = "More icon"
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NcPrimaryDarkButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                onClick = onContinueClicked,
            ) {
                Text(text = stringResource(id = R.string.nc_wallet_create_wallet))
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(top = 16.dp)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                WalletInfo(
                    groupSandbox = state.group,
                    onEditClicked = onEditClicked,
                )
            }

            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NcIcon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(id = R.drawable.ic_mulitsig_dark),
                        contentDescription = "Key icon",
                    )

                    Text(
                        text = stringResource(id = R.string.nc_title_signers),
                        style = NunchukTheme.typography.title,
                        modifier = Modifier.padding(start = 8.dp)
                    )


                }
            }

            itemsIndexed(state.signers) { index, signer ->
                FreeAddKeyCard(
                    index = index,
                    signer = signer,
                    onAddClicked = {
                        currentSignerIndex = index
                        if (state.allSigners.isNotEmpty()) {
                            showSignerBottomSheet = true
                        } else {
                            onAddNewKey(index)
                        }
                    },
                    onRemoveClicked = { onRemoveClicked(index) }
                )
            }
        }

        if (showMoreOption) {
            NcSelectableBottomSheet(
                options = listOf(stringResource(R.string.nc_cancel_group_wallet_setup)),
                onSelected = {
                    if (it == 0) {
                        onDeleteGroupClicked()
                    }
                },
                onDismiss = {
                    showMoreOption = false
                },
            )
        }

        if (showSignerBottomSheet) {
            SelectSignerBottomSheet(
                onDismiss = { showSignerBottomSheet = false },
                supportedSigners = emptyList(),
                onAddExistKey = {
                    showSignerBottomSheet = false
                    onAddExistingKey(it, currentSignerIndex)
                },
                onAddNewKey = {
                    showSignerBottomSheet = false
                    onAddNewKey(currentSignerIndex)
                },
                args = TapSignerListBottomSheetFragmentArgs(
                    signers = state.allSigners.toTypedArray(),
                    type = SignerType.UNKNOWN
                )
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun GroupWalletScreenPreview(
    @PreviewParameter(SignersModelProvider::class) signers: List<SignerModel>,
) {
    NunchukTheme {
        FreeGroupWalletScreen(
            state = FreeGroupWalletUiState(signers = signers + null)
        )
    }
}

