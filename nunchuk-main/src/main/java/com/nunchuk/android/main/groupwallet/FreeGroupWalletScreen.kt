package com.nunchuk.android.main.groupwallet

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.nunchuk.android.compose.NcCircleImage
import com.nunchuk.android.compose.NcDashLineBox
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.provider.SignersModelProvider
import com.nunchuk.android.compose.provider.WalletExtendedProvider
import com.nunchuk.android.compose.signer.SignerCard
import com.nunchuk.android.compose.strokePrimary
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.main.R
import com.nunchuk.android.model.GroupSandbox
import com.nunchuk.android.model.WalletExtended
import com.nunchuk.android.wallet.util.toReadableString

const val freeGroupWalletRoute = "free_group_wallet/{group_id}"
private val avatarColors = listOf(
    Color(0xFF1C652D),
    Color(0xFFA66800),
    Color(0xFFCF4018),
    Color(0xFF7E519B),
    Color(0xFF2F466C),
    Color(0xFFF1AE00),
    Color(0xFF757575),
)

fun NavGraphBuilder.freeGroupWallet(
    onEditClicked: (String) -> Unit = {},
    groupId: String
) {
    composable(
        route = freeGroupWalletRoute,
        arguments = listOf(
            navArgument("group_id") {
                type = NavType.StringType
                defaultValue = groupId
            },
        )
    ) {
        val viewModel: FreeGroupWalletViewModel = hiltViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()

        LifecycleResumeEffect(Unit) {
            viewModel.getGroupSandbox()
            onPauseOrDispose { }
        }

        FreeGroupWalletScreen(
            state = state,
            onAddClicked = {

            },
            onMoreClicked = {},
            onContinueClicked = {},
            onEditClicked = {
                state.group?.let {
                    onEditClicked(it.id)
                }
            },
            onRemoveClicked = {

            },

        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreeGroupWalletScreen(
    state: FreeGroupWalletUiState = FreeGroupWalletUiState(),
    onAddClicked: (Int) -> Unit = {},
    onRemoveClicked: (Int) -> Unit = {},
    onMoreClicked: () -> Unit = {},
    onContinueClicked: () -> Unit = {},
    onEditClicked: () -> Unit = {},
) {
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
                        IconButton(onClick = onMoreClicked) {
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
                    onEditClicked = onEditClicked
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
                AddKeyCard(
                    index = index,
                    signer = signer,
                    onAddClicked = { onAddClicked(index) },
                    onRemoveClicked = { onRemoveClicked(index) }
                )
            }
        }
    }
}

@Composable
private fun AddKeyCard(
    index: Int,
    modifier: Modifier = Modifier,
    signer: SignerModel? = null,
    onAddClicked: () -> Unit,
    onRemoveClicked: () -> Unit,
) {
    if (signer != null) {
        Row(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.strokePrimary,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (signer.isVisible) {
                SignerCard(item = signer, modifier = Modifier.weight(1.0f))
            } else {
                NcCircleImage(
                    iconSize = 48.dp,
                    resId = R.drawable.ic_user,
                    color = avatarColors[index % avatarColors.size]
                )
                Column(
                    modifier = Modifier
                        .weight(1.0f)
                        .padding(start = 8.dp)
                ) {
                    Text(
                        text = "XFP: ${signer.fingerPrint}",
                        style = NunchukTheme.typography.body
                    )
                }
            }
            NcOutlineButton(
                modifier = Modifier.height(36.dp),
                onClick = onRemoveClicked,
            ) {
                Text(text = stringResource(id = R.string.nc_remove))
            }
        }
    } else {
        NcDashLineBox(
            modifier = modifier,
            content = {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NcCircleImage(resId = R.drawable.ic_key, iconSize = 24.dp)
                    Column(
                        modifier = Modifier
                            .weight(1.0f)
                            .padding(start = 8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.nc_key_with_index, "#${index + 1}"),
                            style = NunchukTheme.typography.body
                        )
                    }
                    NcOutlineButton(
                        modifier = Modifier.height(36.dp),
                        onClick = onAddClicked,
                    ) {
                        Text(text = stringResource(id = R.string.nc_add_key))
                    }
                }
            }
        )
    }
}

@Composable
internal fun WalletInfo(
    groupSandbox: GroupSandbox? = null,
    onEditClicked: () -> Unit = {},
) {
    val requireSigns = groupSandbox?.m ?: 0
    val totalSigns = groupSandbox?.n ?: 0
    Box(
        modifier = Modifier
            .clip(shape = RoundedCornerShape(12.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        colorResource(id = R.color.cl_084B7B),
                        colorResource(id = R.color.cl_2B74A9)
                    ), start = Offset.Zero, end = Offset.Infinite
                )
            )
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1.0f)
                ) {
                    Text(
                        text = groupSandbox?.name.orEmpty(),
                        style = NunchukTheme.typography.titleLarge
                            .copy(color = colorResource(id = R.color.nc_white_color))
                    )
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            modifier = Modifier.padding(end = 8.dp),
                            text = "$requireSigns/$totalSigns ${stringResource(R.string.nc_wallet_multisig)}",
                            style = NunchukTheme.typography.bodySmall
                                .copy(color = colorResource(id = R.color.nc_white_color))
                        )

                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(shape = CircleShape)
                                .background(Color(0xFFF5F5F5))
                        )

                        Text(
                            modifier = Modifier.padding(start = 8.dp),
                            text = groupSandbox?.addressType?.toReadableString(LocalContext.current)
                                ?: "",
                            style = NunchukTheme.typography.bodySmall.copy(
                                color = colorResource(
                                    id = R.color.nc_white_color
                                )
                            )
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(top = 12.dp),
                thickness = 0.5.dp,
                color = colorResource(id = R.color.nc_bg_mid_gray)
            )

            Row(
                modifier = Modifier.padding(top = 12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NcIcon(
                        modifier = Modifier.size(16.dp),
                        painter = painterResource(id = R.drawable.ic_chain),
                        contentDescription = "Wallet icon",
                        tint = colorResource(id = R.color.nc_white_color)
                    )

                    Text(
                        modifier = Modifier.padding(start = 4.dp),
                        text = stringResource(id = R.string.nc_copy_wallet_link),
                        style = NunchukTheme.typography.titleSmall.copy(color = colorResource(id = R.color.nc_white_color))
                    )
                }

                Row(
                    modifier = Modifier.padding(start = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NcIcon(
                        modifier = Modifier.size(16.dp),
                        painter = painterResource(id = R.drawable.ic_qrcode_2),
                        contentDescription = "Wallet icon",
                        tint = colorResource(id = R.color.nc_white_color)
                    )

                    Text(
                        modifier = Modifier.padding(start = 4.dp),
                        text = stringResource(id = R.string.nc_show_qr),
                        style = NunchukTheme.typography.titleSmall.copy(color = colorResource(id = R.color.nc_white_color))
                    )
                }

                Row(
                    modifier = Modifier
                        .padding(start = 20.dp)
                        .clickable { onEditClicked() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NcIcon(
                        modifier = Modifier.size(16.dp),
                        painter = painterResource(id = R.drawable.ic_setting_2),
                        contentDescription = "Wallet icon",
                        tint = colorResource(id = R.color.nc_white_color)
                    )

                    Text(
                        modifier = Modifier.padding(start = 4.dp),
                        text = stringResource(id = R.string.nc_settings),
                        style = NunchukTheme.typography.titleSmall.copy(color = colorResource(id = R.color.nc_white_color))
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun WalletInfoPreview(
    @PreviewParameter(WalletExtendedProvider::class) walletsExtended: WalletExtended,
) {
    NunchukTheme {
        WalletInfo()
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

