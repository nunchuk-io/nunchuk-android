package com.nunchuk.android.wallet.components.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.controlTextPrimary
import com.nunchuk.android.compose.fillBeewax
import com.nunchuk.android.compose.fillDenim2
import com.nunchuk.android.wallet.R

@Composable
fun ReplacedGroupView(
    modifier: Modifier = Modifier,
    replacedGroups: Map<String, Boolean>,
    onAcceptOrDeny: (String, Boolean) -> Unit = { _, _ -> },
    onOpenSetupGroupWallet: (String) -> Unit = {}
) {
    val isJoined = replacedGroups.values.all { it }
    val groupId = replacedGroups.keys.first()
    NunchukTheme {
        if (isJoined) {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .background(
                        color = MaterialTheme.colorScheme.fillDenim2,
                        shape = MaterialTheme.shapes.medium
                    )
                    .clickable { onOpenSetupGroupWallet(groupId) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.nc_key_replacement_in_progress),
                        style = NunchukTheme.typography.titleSmall
                    )

                    Text(
                        text = stringResource(R.string.nc_set_up_a_new_group_wallet),
                        style = NunchukTheme.typography.bodySmall
                    )
                }

                NcIcon(
                    painter = painterResource(R.drawable.ic_arrow_right_new),
                    contentDescription = "Arrow",
                )
            }
        } else {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .background(
                        color = MaterialTheme.colorScheme.fillBeewax,
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(
                    text = stringResource(R.string.nc_key_replacement_request),
                    style = NunchukTheme.typography.titleSmall
                )

                Text(
                    modifier = Modifier.padding(top = 4.dp),
                    text = stringResource(R.string.nc_set_up_a_new_group_wallet),
                    style = NunchukTheme.typography.bodySmall
                )

                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NcOutlineButton(
                        onClick = { onAcceptOrDeny(groupId, false) },
                        height = 36.dp
                    ) {
                        Text(
                            text = stringResource(R.string.nc_deny),
                            style = NunchukTheme.typography.captionTitle
                        )
                    }

                    NcPrimaryDarkButton(
                        onClick = { onAcceptOrDeny(groupId, true) },
                        height = 36.dp
                    ) {
                        Text(
                            text = stringResource(R.string.nc_accept),
                            style = NunchukTheme.typography.captionTitle
                                .copy(color = MaterialTheme.colorScheme.controlTextPrimary)
                        )
                    }
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
fun ReplacedGroupViewPreview() {
    ReplacedGroupView(
        replacedGroups = mapOf("group1" to true)
    )
}

@PreviewLightDark
@Composable
fun ReplacedGroupViewPendingPreview() {
    ReplacedGroupView(
        replacedGroups = mapOf("group1" to false)
    )
}