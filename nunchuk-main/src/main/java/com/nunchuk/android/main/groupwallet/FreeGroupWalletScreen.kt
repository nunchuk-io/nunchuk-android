package com.nunchuk.android.main.groupwallet

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.compose.NcDashLineBox
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.provider.SignersModelProvider
import com.nunchuk.android.compose.provider.WalletExtendedProvider
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.model.AddKeyData
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.WalletExtended
import com.nunchuk.android.wallet.util.toReadableString

const val freeGroupWalletRoute = "free_group_wallet_route"

fun NavGraphBuilder.freeGroupWallet(
    onEditClicked: () -> Unit = {},
) {
    composable(freeGroupWalletRoute) {
        val viewModel: FreeGroupWalletViewModel = hiltViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        FreeGroupWalletScreen(
            state = state,
            onAddClicked = {},
            onMoreClicked = {},
            onContinueClicked = {},
            onEditClicked = onEditClicked
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreeGroupWalletScreen(
    state: FreeGroupWalletUiState = FreeGroupWalletUiState(),
    onAddClicked: (data: AddKeyData) -> Unit = {},
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
                    walletsExtended = WalletExtended(),
                    onEditClicked = onEditClicked
                )
            }

        }
    }
}

@Composable
private fun AddKeyCard(
    modifier: Modifier = Modifier,
    signer: SingleSigner? = null,
) {
    NcDashLineBox(
        modifier = modifier,
        content = {

        }
    )
}

@Composable
internal fun WalletInfo(
    walletsExtended: WalletExtended,
    onEditClicked: () -> Unit = {},
) {
    val wallet = walletsExtended.wallet

    val requireSigns = wallet.totalRequireSigns
    val totalSigns = wallet.signers.size
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
                        text = stringResource(id = R.string.nc_group_wallet),
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
                            text = wallet.addressType.toReadableString(LocalContext.current),
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
        WalletInfo(walletsExtended)
    }
}

@PreviewLightDark
@Composable
private fun GroupWalletScreenPreview(
    @PreviewParameter(SignersModelProvider::class) signers: List<SignerModel>,
) {
    NunchukTheme {
        FreeGroupWalletScreen()
    }
}

