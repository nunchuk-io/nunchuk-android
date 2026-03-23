package com.nunchuk.android.main.groupwallet.keypolicies

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcCircleImage
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.strokePrimary
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.main.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PolicyTypeBottomSheet(
    onSelected: (PolicyType) -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = { },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 24.dp, bottom = 32.dp),
        ) {
            Text(
                text = stringResource(R.string.nc_choose_how_rules_applied),
                style = NunchukTheme.typography.body,
                modifier = Modifier.padding(bottom = 24.dp),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelected(PolicyType.GLOBAL) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                NcCircleImage(
                    resId = R.drawable.ic_policy_keys,
                    size = 36.dp,
                    iconSize = 24.dp,
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.nc_global_policy),
                        style = NunchukTheme.typography.title,
                    )
                    Text(
                        text = stringResource(R.string.nc_global_policy_sheet_desc),
                        style = NunchukTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.textSecondary
                        ),
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.strokePrimary,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelected(PolicyType.PER_KEY) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                NcCircleImage(
                    resId = R.drawable.ic_policy_key,
                    size = 36.dp,
                    iconSize = 24.dp,
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.nc_per_key_policy),
                        style = NunchukTheme.typography.title,
                    )
                    Text(
                        text = stringResource(R.string.nc_per_key_policy_desc),
                        style = NunchukTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.textSecondary
                        ),
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun PolicyTypeBottomSheetPreview() {
    NunchukTheme {
        PolicyTypeBottomSheet()
    }
}
