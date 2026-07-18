package com.nunchuk.android.signer.ledger

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.signer.R
import kotlinx.serialization.Serializable

@Serializable
internal data object LedgerInstructionRoute

fun NavGraphBuilder.ledgerInstruction(
    onBack: () -> Unit = {},
    onContinue: () -> Unit = {}
) {
    composable<LedgerInstructionRoute> {
        LedgerInstructionScreen(
            onBack = onBack,
            onContinue = onContinue
        )
    }
}

fun NavHostController.navigateToLedgerInstruction() {
    navigate(LedgerInstructionRoute)
}

@Composable
private fun LedgerInstructionScreen(
    onBack: () -> Unit = {},
    onContinue: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            NcImageAppBar(
                backgroundRes = R.drawable.bg_ledger_illustration,
                onClosedClicked = onBack
            )
        },
        bottomBar = {
            NcPrimaryDarkButton(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .navigationBarsPadding()
                    .fillMaxWidth(),
                onClick = onContinue
            ) {
                Text(text = stringResource(id = com.nunchuk.android.core.R.string.nc_text_continue))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(top = 24.dp, start = 16.dp, end = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.nc_add_your_ledger),
                style = NunchukTheme.typography.heading
            )

            Text(
                text = stringResource(id = R.string.nc_ensure_to_following_ledger),
                style = NunchukTheme.typography.body
            )

            LedgerStep(index = 1) {
                Text(
                    text = stringResource(id = R.string.nc_open_ledger_wallet),
                    style = NunchukTheme.typography.title
                )
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = stringResource(id = R.string.nc_ledger_wallet_mobile_link),
                    style = NunchukTheme.typography.body
                )
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = stringResource(id = R.string.nc_ledger_setup_desc),
                    style = NunchukTheme.typography.body
                )
            }

            LedgerStep(index = 2) {
                Text(
                    text = stringResource(id = R.string.nc_enable_bluetooth),
                    style = NunchukTheme.typography.title
                )
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = stringResource(id = R.string.nc_ledger_enable_bluetooth_desc),
                    style = NunchukTheme.typography.body
                )
            }

            LedgerStep(index = 3) {
                Text(
                    text = stringResource(id = R.string.nc_return_to_nunchuk),
                    style = NunchukTheme.typography.title
                )
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = stringResource(id = R.string.nc_return_to_nunchuk_ledger_desc),
                    style = NunchukTheme.typography.body
                )
            }
        }
    }
}

@Composable
private fun LedgerStep(
    index: Int,
    content: @Composable ColumnScope.() -> Unit
) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = index.toString(),
                style = NunchukTheme.typography.titleSmall.copy(fontWeight = FontWeight.W700)
            )
        }

        Column(
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f)
        ) {
            content()
        }
    }
}

@PreviewLightDark
@Composable
private fun LedgerInstructionScreenPreview() {
    NunchukTheme {
        LedgerInstructionScreen()
    }
}
