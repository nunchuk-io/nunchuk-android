package com.nunchuk.android.main.membership.honey.distribution

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.model.ClaimOptionState
import com.nunchuk.android.main.membership.model.InheritanceClaimState
import com.nunchuk.android.model.inheritance.ClaimOption

/**
 * The status line under an off-chain inheritance key: one entry per sharing method the owner chose,
 * or a prompt while the choice is still outstanding.
 *
 * Shared by the two assisted key lists and the replace-key screen, which render the same key in
 * the same states — the only difference is whether the server tracks it on the draft wallet or on
 * the wallet's replacement.
 */
@Composable
internal fun InheritanceClaimStatusRow(claimState: InheritanceClaimState) {
    val statuses = claimState.statuses()
    val style = NunchukTheme.typography.bodySmall.copy(
        color = MaterialTheme.colorScheme.textSecondary
    )
    if (statuses.isEmpty()) {
        Text(
            modifier = Modifier.padding(top = 4.dp),
            text = stringResource(R.string.nc_sharing_method_not_set),
            style = style
        )
        return
    }
    Row(
        modifier = Modifier.padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        statuses.forEach { status ->
            Text(
                text = stringResource(
                    when (status.option) {
                        ClaimOption.ENCRYPTED_BACKUP -> R.string.nc_claim_status_encrypted_backup
                        ClaimOption.SEED_PHRASE -> R.string.nc_claim_status_seed_phrase
                    },
                    stringResource(
                        when (status.state) {
                            ClaimOptionState.NOT_UPLOADED -> R.string.nc_claim_status_not_uploaded
                            ClaimOptionState.PENDING -> R.string.nc_claim_status_pending
                            ClaimOptionState.SKIPPED -> R.string.nc_claim_status_skipped
                            ClaimOptionState.VERIFIED -> R.string.nc_claim_status_verified
                        }
                    )
                ),
                style = style
            )
        }
    }
}
