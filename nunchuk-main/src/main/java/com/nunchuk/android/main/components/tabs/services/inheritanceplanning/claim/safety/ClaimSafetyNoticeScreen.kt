package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.safety

import androidx.annotation.StringRes
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.HighlightMessageType
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.main.R
import com.nunchuk.android.signer.R as SignerR

/** Safety notice before a key is recovered for a claim, by recovery method (Figma Row 8). */
enum class ClaimSafetyNotice(
    @StringRes val title: Int,
    @StringRes val lead: Int,
    @StringRes val body: Int,
    @StringRes val warning: Int,
) {
    /** CLM-01: restore the seed phrase on a hardware device. */
    RESTORE_ON_DEVICE(
        title = R.string.nc_claim_safety_recover_title,
        lead = R.string.nc_claim_safety_restore_lead,
        body = R.string.nc_claim_safety_restore_body,
        warning = R.string.nc_claim_safety_restore_warning,
    ),

    /** CLM-02: a hardware device received already set up with the key. */
    EXISTING_DEVICE(
        title = R.string.nc_device_safety_title,
        lead = R.string.nc_claim_safety_existing_lead,
        body = R.string.nc_claim_safety_existing_body,
        warning = R.string.nc_claim_safety_existing_warning,
    ),

    /** CLM-03: the seed phrase entered in the app on this phone. */
    SOFTWARE_SEED(
        title = R.string.nc_device_safety_title,
        lead = R.string.nc_claim_safety_software_lead,
        body = R.string.nc_claim_safety_software_body,
        warning = R.string.nc_claim_safety_software_warning,
    ),
}

@Composable
fun ClaimSafetyNoticeScreen(
    notice: ClaimSafetyNotice,
    snackState: SnackbarHostState = remember { SnackbarHostState() },
    onBackPressed: () -> Unit = {},
    onContinue: () -> Unit = {},
) {
    NcScaffold(
        modifier = Modifier.navigationBarsPadding(),
        snackState = snackState,
        topBar = {
            NcImageAppBar(
                backgroundRes = R.drawable.bg_device_safety_notice_illustration,
                onClosedClicked = onBackPressed,
            )
        },
        bottomBar = {
            NcPrimaryDarkButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                onClick = onContinue,
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
                text = stringResource(notice.title),
                style = NunchukTheme.typography.heading,
            )
            Text(
                text = stringResource(notice.lead),
                style = NunchukTheme.typography.title,
            )
            Text(
                text = stringResource(notice.body),
                style = NunchukTheme.typography.body,
            )
            NcHintMessage(
                modifier = Modifier.padding(bottom = 16.dp),
                type = HighlightMessageType.WARNING,
            ) {
                Text(
                    text = stringResource(notice.warning),
                    style = NunchukTheme.typography.titleSmall,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ClaimSafetyNoticeRestorePreview() {
    NunchukTheme {
        ClaimSafetyNoticeScreen(notice = ClaimSafetyNotice.RESTORE_ON_DEVICE)
    }
}

@PreviewLightDark
@Composable
private fun ClaimSafetyNoticeExistingPreview() {
    NunchukTheme {
        ClaimSafetyNoticeScreen(notice = ClaimSafetyNotice.EXISTING_DEVICE)
    }
}

@PreviewLightDark
@Composable
private fun ClaimSafetyNoticeSoftwarePreview() {
    NunchukTheme {
        ClaimSafetyNoticeScreen(notice = ClaimSafetyNotice.SOFTWARE_SEED)
    }
}
