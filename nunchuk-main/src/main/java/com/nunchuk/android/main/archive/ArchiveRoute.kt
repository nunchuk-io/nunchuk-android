package com.nunchuk.android.main.archive

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.compose.ActiveWallet
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.disableWalletColor
import com.nunchuk.android.main.R

@Composable
internal fun ArchiveRoute(
    viewModel: ArchiveViewModel = hiltViewModel(),
    openWalletDetail: (String) -> Unit = {},
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    ArchiveScreen(
        uiState = uiState,
        openWalletDetail = openWalletDetail
    )
}

@Composable
internal fun ArchiveScreen(
    uiState: ArchiveUiState,
    openWalletDetail: (String) -> Unit
) {
    NcScaffold(
        topBar = {
            NcTopAppBar(
                title = stringResource(R.string.nc_archived_wallets),
                textStyle = NunchukTheme.typography.titleLarge
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .systemBarsPadding()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.wallets, key = { it.wallet.id }) { wallet ->
                Box(
                    modifier = Modifier
                        .clickable(onClick = { openWalletDetail(wallet.wallet.id) })
                        .background(
                            brush = Brush.linearGradient(
                                colors = disableWalletColor,
                                start = Offset.Zero,
                                end = Offset.Infinite
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                        .fillMaxWidth()
                ) {
                    ActiveWallet(
                        walletsExtended = wallet,
                        hideWalletDetail = false,
                        isAssistedWallet = false,
                        useLargeFont = false,
                    )
                }
            }
        }
    }
}
