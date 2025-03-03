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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
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
import com.nunchuk.android.compose.fillBeewax
import com.nunchuk.android.compose.fillSlimeT2
import com.nunchuk.android.compose.provider.SignerModelProvider
import com.nunchuk.android.compose.signer.SignerCard
import com.nunchuk.android.compose.strokePrimary
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.core.signer.SignerModel
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
    replacedSigner: SignerModel? = null,
    onAddClicked: () -> Unit,
    onRemoveOrReplaceClicked: (Boolean) -> Unit,
    showBip32Path: Boolean = false,
    onChangeBip32Path: (Int, SignerModel) -> Unit = { _, _ -> },
    avatarColor: Color = avatarColors[0],
    isInReplace: Boolean = false
) {
    if (signer != null && signer.name == KEY_NOT_SYNCED_NAME) {
        KeyNotSyncView(modifier = modifier, index = index)
    } else if (signer != null) {
        KeyAddedView(
            modifier = modifier,
            isInReplace = isInReplace,
            signer = signer,
            replacedSigner = replacedSigner,
            showBip32Path = showBip32Path,
            onChangeBip32Path = onChangeBip32Path,
            index = index,
            avatarColor = avatarColor,
            onRemoveOrReplaceClicked = onRemoveOrReplaceClicked
        )
    } else {
        KeyNotAddedView(
            modifier = modifier,
            index = index,
            isOccupied = isOccupied,
            onAddClicked = onAddClicked
        )
    }
}

@Composable
private fun KeyNotAddedView(
    modifier: Modifier,
    index: Int,
    isOccupied: Boolean,
    onAddClicked: () -> Unit
) {
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

@Composable
private fun KeyAddedView(
    modifier: Modifier,
    isInReplace: Boolean,
    signer: SignerModel,
    replacedSigner: SignerModel?,
    showBip32Path: Boolean,
    onChangeBip32Path: (Int, SignerModel) -> Unit,
    index: Int,
    avatarColor: Color,
    onRemoveOrReplaceClicked: (Boolean) -> Unit
) {
    val targetSigner = replacedSigner ?: signer
    val color = when {
        replacedSigner != null -> MaterialTheme.colorScheme.fillSlimeT2
        isInReplace -> MaterialTheme.colorScheme.fillBeewax
        else -> MaterialTheme.colorScheme.surface
    }
    Column {
        Row(
            modifier = modifier
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.strokePrimary,
                    shape = RoundedCornerShape(8.dp)
                )
                .background(color, shape = RoundedCornerShape(8.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (targetSigner.isVisible) {
                SignerCard(item = targetSigner, modifier = Modifier.weight(1.0f)) {
                    if (showBip32Path) {
                        Row(
                            modifier = Modifier.clickable(
                                onClick = {
                                    onChangeBip32Path(index, targetSigner)
                                },
                                enabled = targetSigner.isMasterSigner,
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                modifier = Modifier.weight(1f, false),
                                text = stringResource(
                                    R.string.nc_bip32_path,
                                    targetSigner.derivationPath
                                ),
                                style = if (targetSigner.isMasterSigner) {
                                    NunchukTheme.typography.bodySmall.copy(textDecoration = TextDecoration.Underline)
                                } else {
                                    NunchukTheme.typography.bodySmall
                                },
                                color = MaterialTheme.colorScheme.textSecondary
                            )
                            if (targetSigner.isMasterSigner) {
                                NcIcon(
                                    modifier = Modifier.size(12.dp),
                                    painter = painterResource(id = R.drawable.ic_edit_small),
                                    contentDescription = "Edit icon"
                                )
                            }
                        }
                    }
                }
            } else {
                NcCircleImage(
                    iconSize = 24.dp,
                    resId = R.drawable.ic_user,
                    color = avatarColor,
                    iconTintColor = Color.White,
                    size = 48.dp
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
                        if (targetSigner.isShowAcctX()) {
                            NcTag(
                                label = stringResource(
                                    com.nunchuk.android.core.R.string.nc_acct_x,
                                    targetSigner.index
                                ),
                                backgroundColor = colorResource(
                                    id = com.nunchuk.android.core.R.color.nc_bg_mid_gray
                                ),
                            )
                        }
                    }
                    if (targetSigner.type != SignerType.SERVER) {
                        Text(
                            text = targetSigner.getXfpOrCardIdLabel(),
                            style = NunchukTheme.typography.bodySmall,
                        )
                    }
                    if (showBip32Path) {
                        Text(
                            text = stringResource(
                                R.string.nc_bip32_path,
                                targetSigner.derivationPath
                            ),
                            style = NunchukTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.textSecondary
                        )
                    }
                }
            }
            if (targetSigner.isVisible) {
                val isReplaced = replacedSigner != null
                NcOutlineButton(
                    modifier = Modifier.height(36.dp),
                    onClick = { onRemoveOrReplaceClicked(isInReplace && !isReplaced) },
                ) {
                    Text(
                        text = stringResource(
                            id =
                            if (isInReplace && !isReplaced)
                                R.string.nc_replace
                            else
                                R.string.nc_remove
                        ),
                        style = NunchukTheme.typography.captionTitle
                    )
                }
            }
        }

        if (replacedSigner != null) {
            Row(
                modifier = Modifier.padding(top = 8.dp, start = 12.dp, end = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                NcIcon(
                    painter = painterResource(id = R.drawable.ic_replace),
                    contentDescription = "Replace icon"
                )

                Text(
                    text = "Replacing ${signer.name} (${signer.getXfpOrCardIdLabel()})",
                    style = NunchukTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun KeyNotSyncView(modifier: Modifier, index: Int) {
    Row(
        modifier = modifier
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
}

@PreviewLightDark
@Composable
private fun PendingReplaceFreeAddKeyCardPreview(
    @PreviewParameter(SignerModelProvider::class) signer: SignerModel,
) {
    NunchukTheme {
        Box(
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            FreeAddKeyCard(
                index = 0,
                signer = signer,
                isInReplace = true,
                isOccupied = false,
                onAddClicked = {},
                onRemoveOrReplaceClicked = {},
                showBip32Path = true
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ReplacedFreeAddKeyCardPreview(
    @PreviewParameter(SignerModelProvider::class) signer: SignerModel,
) {
    NunchukTheme {
        Box(
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            FreeAddKeyCard(
                index = 0,
                signer = signer,
                replacedSigner = signer,
                isInReplace = true,
                isOccupied = false,
                onAddClicked = {},
                onRemoveOrReplaceClicked = {},
                showBip32Path = true
            )
        }
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
                onRemoveOrReplaceClicked = {},
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
                onRemoveOrReplaceClicked = {}
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
                onRemoveOrReplaceClicked = {}
            )
        }
    }
}
