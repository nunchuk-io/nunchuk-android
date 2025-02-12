package com.nunchuk.android.main.groupwallet.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcCircleImage
import com.nunchuk.android.compose.NcDashLineBox
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcTag
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.beeswaxDark
import com.nunchuk.android.compose.provider.SignerModelProvider
import com.nunchuk.android.compose.signer.SignerCard
import com.nunchuk.android.compose.strokePrimary
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.toReadableSignerType
import com.nunchuk.android.main.R
import com.nunchuk.android.main.groupwallet.KEY_NOT_SYNCED_NAME
import com.nunchuk.android.main.groupwallet.avatarColors
import com.nunchuk.android.type.SignerType

@Composable
fun FreeAddKeyCard(
    index: Int,
    isOccupied: Boolean,
    modifier: Modifier = Modifier,
    signer: SignerModel? = null,
    onAddClicked: () -> Unit,
    onRemoveClicked: () -> Unit,
    showBip32Path: Boolean = false,
    onChangeBip32Path: (Int, SignerModel) -> Unit = { _, _ -> }
) {
    if (signer != null && signer.name == KEY_NOT_SYNCED_NAME) {
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
            NcCircleImage(resId = R.drawable.ic_key, iconSize = 24.dp)
            Text(
                modifier = Modifier
                    .weight(1.0f)
                    .padding(start = 8.dp),
                text = stringResource(R.string.nc_key_with_index, "#${index + 1}"),
                style = NunchukTheme.typography.body
            )
            NcIcon(
                modifier = Modifier.size(18.dp),
                painter = painterResource(id = R.drawable.nc_circle_checked),
                contentDescription = "Checked icon"
            )
            Text(
                modifier = Modifier.padding(start = 4.dp),
                style = NunchukTheme.typography.titleSmall,
                text = stringResource(
                    R.string.nc_added
                )
            )
        }
    } else if (signer != null) {
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
                SignerCard(item = signer, modifier = Modifier.weight(1.0f)) {
                    if (showBip32Path && signer.isMasterSigner) {
                        Row(
                            modifier = Modifier.clickable(onClick = {
                                onChangeBip32Path(index, signer)
                            }),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.nc_bip32_path,
                                    signer.derivationPath
                                ),
                                style = NunchukTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.textSecondary
                            )

                            NcIcon(
                                modifier = Modifier.size(12.dp),
                                painter = painterResource(id = R.drawable.ic_edit_small),
                                contentDescription = "Edit icon"
                            )
                        }
                    }
                }
            } else {
                NcCircleImage(
                    iconSize = 48.dp,
                    resId = R.drawable.ic_user,
                    color = avatarColors[index % avatarColors.size]
                )
                Column(
                    modifier = Modifier
                        .weight(1.0f)
                        .padding(start = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.nc_key_with_index, "#${index + 1}"),
                        style = NunchukTheme.typography.body
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (signer.type != SignerType.SERVER) {
                            NcTag(
                                label = signer.toReadableSignerType(context = LocalContext.current),
                                backgroundColor = colorResource(
                                    id = com.nunchuk.android.core.R.color.nc_bg_mid_gray
                                ),
                            )
                        }
                        if (signer.isShowAcctX()) {
                            NcTag(
                                label = stringResource(
                                    com.nunchuk.android.core.R.string.nc_acct_x,
                                    signer.index
                                ),
                                backgroundColor = colorResource(
                                    id = com.nunchuk.android.core.R.color.nc_bg_mid_gray
                                ),
                            )
                        }
                    }
                    if (signer.type != SignerType.SERVER) {
                        Text(
                            text = signer.getXfpOrCardIdLabel(),
                            style = NunchukTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.textSecondary
                        )
                    }
                }
            }
            if (signer.isVisible) {
                NcOutlineButton(
                    modifier = Modifier.height(36.dp),
                    onClick = onRemoveClicked,
                ) {
                    Text(text = stringResource(id = R.string.nc_remove))
                }
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
                            .padding(start = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.nc_key_with_index, "#${index + 1}"),
                            style = NunchukTheme.typography.body
                        )

                        if (isOccupied) {
                            Text(
                                text = stringResource(id = R.string.nc_occupied),
                                style = NunchukTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.beeswaxDark
                            )
                        }
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

@PreviewLightDark
@Composable
private fun FreeAddKeyCardPreview(
    @PreviewParameter(SignerModelProvider::class) signer: SignerModel,
) {
    NunchukTheme {
        Box(
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            FreeAddKeyCard(
                index = 0,
                signer = signer,
                isOccupied = false,
                onAddClicked = {},
                onRemoveClicked = {},
                showBip32Path = true
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun FreeAddKeyCardPreviewOtherKey(
    @PreviewParameter(SignerModelProvider::class) signer: SignerModel,
) {
    NunchukTheme {
        Box(
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            FreeAddKeyCard(
                index = 0,
                signer = signer.copy(isVisible = false),
                isOccupied = false,
                onAddClicked = {},
                onRemoveClicked = {}
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun FreeAddKeyCardPreviewAdded(
    @PreviewParameter(SignerModelProvider::class) signer: SignerModel,
) {
    NunchukTheme {
        Box(
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            FreeAddKeyCard(
                index = 0,
                signer = signer.copy(isVisible = false, name = KEY_NOT_SYNCED_NAME),
                isOccupied = false,
                onAddClicked = {},
                onRemoveClicked = {}
            )
        }
    }
}
