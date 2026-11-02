package com.nunchuk.android.compose.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.backgroundPrimary
import com.nunchuk.android.compose.beeswaxDark
import com.nunchuk.android.compose.greyLight
import com.nunchuk.android.compose.signer.SignerCard
import com.nunchuk.android.compose.strokePrimary
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.core.R
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.pathKey

/** Same key format as [GetNonStandardPathSignersUseCase]: `xfp|path`. */
fun SignerModel.pathKey(): String = pathKey(fingerPrint, derivationPath)

/**
 * Inline flag shown under a key whose BIP32 path isn't the standard one for the wallet
 * ("Not the standard path for this wallet type.").
 */
@Composable
fun NonStandardPathFlag(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        NcIcon(
            modifier = Modifier.size(16.dp),
            painter = painterResource(id = R.drawable.ic_warning_outline),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.beeswaxDark,
        )
        Text(
            text = stringResource(R.string.nc_non_standard_path_flag),
            style = NunchukTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.beeswaxDark,
        )
    }
}

/**
 * "Review key compatibility" sheet shown on Create wallet when any key uses a non-standard
 * BIP32 path. The key list scrolls; the explanation and actions stay visible.
 *
 * @param signers every flagged key, including other group members' keys.
 * @param removableCount how many of [signers] "Remove" can drop (group wallet: only keys local to
 * this device). When it is 0 only "Create anyway" is shown.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyCompatibilityWarningBottomSheet(
    signers: List<SignerModel>,
    removableCount: Int,
    onRemoveNonStandardKeys: () -> Unit,
    onCreateAnyway: () -> Unit,
    onDismiss: () -> Unit,
    nativeSegwitMultisigPath: String? = null,
) {
    ModalBottomSheet(
        containerColor = MaterialTheme.colorScheme.backgroundPrimary,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        onDismissRequest = onDismiss,
        tonalElevation = 0.dp,
    ) {
        KeyCompatibilityWarningContent(
            signers = signers,
            removableCount = removableCount,
            onRemoveNonStandardKeys = onRemoveNonStandardKeys,
            onCreateAnyway = onCreateAnyway,
            nativeSegwitMultisigPath = nativeSegwitMultisigPath,
        )
    }
}

@Composable
private fun KeyCompatibilityWarningContent(
    signers: List<SignerModel>,
    removableCount: Int,
    onRemoveNonStandardKeys: () -> Unit = {},
    onCreateAnyway: () -> Unit = {},
    nativeSegwitMultisigPath: String? = null,
) {
    val maxListHeight = (LocalConfiguration.current.screenHeightDp * 0.45f).dp
    Column(modifier = Modifier.navigationBarsPadding()) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.nc_review_key_compatibility),
                    style = NunchukTheme.typography.title,
                )
                Text(
                    // Native SegWit multisig names its standard path; every other wallet stays generic.
                    text = if (nativeSegwitMultisigPath != null) {
                        pluralStringResource(
                            R.plurals.nc_review_key_compatibility_desc_native_segwit_multisig,
                            signers.size,
                            nativeSegwitMultisigPath,
                        )
                    } else {
                        pluralStringResource(
                            R.plurals.nc_review_key_compatibility_desc,
                            signers.size,
                        )
                    },
                    style = NunchukTheme.typography.body,
                )
            }
            LazyColumn(
                modifier = Modifier.heightIn(max = maxListHeight),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(signers, key = { "${it.fingerPrint}|${it.derivationPath}" }) { signer ->
                    NonStandardKeyCard(signer = signer)
                }
            }
            Text(
                modifier = Modifier.padding(bottom = 16.dp),
                text = stringResource(R.string.nc_review_key_compatibility_create_anyway_desc),
                style = NunchukTheme.typography.body,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.strokePrimary, thickness = 0.5.dp)
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (removableCount == 0) {
                NcPrimaryDarkButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onCreateAnyway,
                ) {
                    Text(text = stringResource(R.string.nc_create_anyway))
                }
                return@Column
            }
            NcPrimaryDarkButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onRemoveNonStandardKeys,
            ) {
                Text(
                    text = pluralStringResource(
                        R.plurals.nc_remove_non_standard_keys,
                        removableCount,
                    )
                )
            }
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                onClick = onCreateAnyway,
                shape = RoundedCornerShape(44.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.greyLight,
                    contentColor = MaterialTheme.colorScheme.textPrimary,
                ),
            ) {
                Text(
                    text = stringResource(R.string.nc_create_anyway),
                    style = NunchukTheme.typography.title,
                )
            }
        }
    }
}

@Composable
private fun NonStandardKeyCard(signer: SignerModel) {
    SignerCard(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.greyLight,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(12.dp),
        item = signer,
        circleColor = MaterialTheme.colorScheme.backgroundPrimary,
        // Another group member's key: shown like on Review wallet.
        signerIcon = if (signer.isVisible) null else R.drawable.ic_signer_empty_state,
        isShowKeyTypeBadge = signer.isVisible,
    ) {
        Text(
            text = stringResource(R.string.nc_bip32_path, signer.derivationPath),
            style = NunchukTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.beeswaxDark,
        )
    }
}

@PreviewLightDark
@Composable
private fun KeyCompatibilityWarningContentPreview() {
    NunchukTheme {
        Column(modifier = Modifier.background(MaterialTheme.colorScheme.backgroundPrimary)) {
            KeyCompatibilityWarningContent(
                signers = listOf(
                    SignerModel(
                        id = "1",
                        name = "Coldcard",
                        derivationPath = "m/84h/0h/0h",
                        fingerPrint = "f23a9c10",
                        type = SignerType.COLDCARD_NFC,
                        isMasterSigner = false,
                    ),
                    SignerModel(
                        id = "2",
                        name = "BitBox02",
                        derivationPath = "m/44h/0h/0h",
                        fingerPrint = "5c20d8a4",
                        type = SignerType.HARDWARE,
                        isMasterSigner = false,
                    ),
                ),
                removableCount = 1,
                nativeSegwitMultisigPath = "m/48'/0'/0'/2'",
            )
            NonStandardPathFlag(modifier = Modifier.padding(16.dp))
        }
    }
}
