package com.nunchuk.android.main.membership.honey.distribution

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcRadioOption
import com.nunchuk.android.compose.NcTag
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.compose.HighlightMessageType
import com.nunchuk.android.main.R
import com.nunchuk.android.model.inheritance.ClaimOption

/**
 * How the owner passes the inheritance key to their Beneficiary.
 *
 * Each option is enabled only when the device supports every artifact it asks for, so "do both"
 * needs both — a device reporting one option leaves it disabled, whichever option that is. The
 * server's `claim_note` explains the limitation whenever something is unavailable.
 */
@Composable
fun KeyDistributionContent(
    remainTime: Int = 0,
    supportedOptions: List<ClaimOption> = ClaimOption.entries,
    claimNote: String? = null,
    selectedChoice: KeyDistributionChoice? = KeyDistributionChoice.BOTH,
    isLoading: Boolean = false,
    onChoiceSelected: (KeyDistributionChoice) -> Unit = {},
    onContinueClicked: () -> Unit = {},
    onBackPressed: () -> Unit = {},
) {
    val canUseSeedPhrase = ClaimOption.SEED_PHRASE in supportedOptions
    val canUseEncryptedBackup = ClaimOption.ENCRYPTED_BACKUP in supportedOptions
    val isEmpty = supportedOptions.isEmpty()
    NunchukTheme {
        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = if (remainTime <= 0) "" else stringResource(
                        R.string.nc_estimate_remain_time, remainTime
                    ),
                    onBackPress = onBackPressed,
                )
            },
            bottomBar = {
                // Nothing to continue to when the server offers no option.
                if (!isEmpty) {
                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        // Each PUT replaces the whole selection, and dropping the encrypted
                        // backup is irreversible, so never let it fire twice.
                        enabled = selectedChoice != null && !isLoading,
                        onClick = onContinueClicked,
                    ) {
                        Text(text = stringResource(R.string.nc_text_continue))
                    }
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                Text(
                    text = stringResource(R.string.nc_key_distribution_title),
                    style = NunchukTheme.typography.heading,
                )
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = stringResource(R.string.nc_key_distribution_desc),
                    style = NunchukTheme.typography.body,
                )

                if (isEmpty) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                        text = stringResource(R.string.nc_claim_options_unavailable),
                        style = NunchukTheme.typography.body.copy(
                            color = MaterialTheme.colorScheme.textSecondary
                        ),
                        textAlign = TextAlign.Center,
                    )
                    return@Column
                }

                ChoiceOption(
                    titleRes = R.string.nc_claim_option_seed_phrase_title,
                    descRes = R.string.nc_claim_option_seed_phrase_desc,
                    isSelected = selectedChoice == KeyDistributionChoice.SEED_PHRASE_ONLY,
                    enabled = canUseSeedPhrase,
                    onClick = { onChoiceSelected(KeyDistributionChoice.SEED_PHRASE_ONLY) },
                )
                ChoiceOption(
                    titleRes = R.string.nc_claim_option_encrypted_backup_title,
                    descRes = R.string.nc_claim_option_encrypted_backup_desc,
                    isSelected = selectedChoice == KeyDistributionChoice.ENCRYPTED_BACKUP_ONLY,
                    enabled = canUseEncryptedBackup,
                    onClick = { onChoiceSelected(KeyDistributionChoice.ENCRYPTED_BACKUP_ONLY) },
                )
                ChoiceOption(
                    titleRes = R.string.nc_claim_option_both_title,
                    descRes = R.string.nc_claim_option_both_desc,
                    isSelected = selectedChoice == KeyDistributionChoice.BOTH,
                    // Both artifacts, so both have to be supported — enabling this off the
                    // encrypted backup alone offered "do both" on a device that cannot do the
                    // seed-phrase half, right below that half greyed out.
                    enabled = canUseSeedPhrase && canUseEncryptedBackup,
                    isRecommended = true,
                    onClick = { onChoiceSelected(KeyDistributionChoice.BOTH) },
                )

                if ((!canUseSeedPhrase || !canUseEncryptedBackup) && !claimNote.isNullOrBlank()) {
                    NcHintMessage(
                        modifier = Modifier.padding(top = 16.dp),
                        messages = listOf(ClickAbleText(content = claimNote)),
                        type = HighlightMessageType.HINT,
                    )
                }

                Spacer(modifier = Modifier.padding(bottom = 16.dp))
            }
        }
    }
}

@Composable
private fun ChoiceOption(
    titleRes: Int,
    descRes: Int,
    isSelected: Boolean,
    enabled: Boolean = true,
    isRecommended: Boolean = false,
    onClick: () -> Unit,
) {
    NcRadioOption(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        isSelected = isSelected,
        enabled = enabled,
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(titleRes),
                style = NunchukTheme.typography.title,
            )
            if (isRecommended) {
                NcTag(
                    label = stringResource(R.string.nc_recommended),
                    backgroundColor = colorResource(id = R.color.nc_bg_mid_gray),
                )
            }
        }
        Text(
            modifier = Modifier.padding(top = 4.dp),
            text = stringResource(descRes),
            style = NunchukTheme.typography.body.copy(
                color = MaterialTheme.colorScheme.textSecondary
            ),
        )
    }
}

@PreviewLightDark
@Composable
private fun KeyDistributionContentPreview() {
    KeyDistributionContent()
}

@PreviewLightDark
@Composable
private fun KeyDistributionContentEmptyPreview() {
    KeyDistributionContent(supportedOptions = emptyList(), selectedChoice = null)
}

@PreviewLightDark
@Composable
private fun KeyDistributionContentEncryptedBackupOnlyPreview() {
    KeyDistributionContent(
        supportedOptions = listOf(ClaimOption.ENCRYPTED_BACKUP),
        selectedChoice = KeyDistributionChoice.ENCRYPTED_BACKUP_ONLY,
        claimNote = "This device cannot export its seed phrase, so the encrypted backup must be used.",
    )
}

@PreviewLightDark
@Composable
private fun KeyDistributionContentSeedOnlyPreview() {
    KeyDistributionContent(
        supportedOptions = listOf(ClaimOption.SEED_PHRASE),
        selectedChoice = KeyDistributionChoice.SEED_PHRASE_ONLY,
        claimNote = "SeedSigner cannot create an encrypted backup on the device, so the seed phrase must be shared directly.",
    )
}
