package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.keycount

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcRadioOption
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.main.R
import com.nunchuk.android.signer.R as SignerR

/**
 * CLM-KC: shown only when the plan's claim conditions need different numbers of keys. Lists each
 * count once, without naming primary or fallback; the backend still decides which condition the
 * keys satisfy.
 */
@Composable
fun ClaimKeyCountScreen(
    keyCounts: List<Int>,
    snackState: SnackbarHostState = remember { SnackbarHostState() },
    onBackPressed: () -> Unit = {},
    onContinue: (Int) -> Unit = {},
) {
    var selected by rememberSaveable { mutableIntStateOf(keyCounts.firstOrNull() ?: 1) }

    NcScaffold(
        modifier = Modifier.navigationBarsPadding(),
        snackState = snackState,
        topBar = {
            NcImageAppBar(
                backgroundRes = R.drawable.bg_add_inheritance_to_claim_first,
                onClosedClicked = onBackPressed,
            )
        },
        bottomBar = {
            NcPrimaryDarkButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                enabled = selected in keyCounts,
                onClick = { onContinue(selected) },
            ) {
                Text(text = stringResource(SignerR.string.nc_text_continue))
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                modifier = Modifier.padding(top = 24.dp),
                text = stringResource(R.string.nc_claim_key_count_title),
                style = NunchukTheme.typography.heading,
            )
            Text(
                text = stringResource(R.string.nc_claim_key_count_desc),
                style = NunchukTheme.typography.body,
            )
            keyCounts.forEach { count ->
                NcRadioOption(
                    modifier = Modifier.fillMaxWidth(),
                    isSelected = selected == count,
                    onClick = { selected = count },
                ) {
                    Text(
                        text = pluralStringResource(R.plurals.nc_claim_key_count_option, count, count),
                        style = NunchukTheme.typography.title,
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ClaimKeyCountScreenPreview() {
    NunchukTheme {
        ClaimKeyCountScreen(keyCounts = listOf(1, 2))
    }
}
