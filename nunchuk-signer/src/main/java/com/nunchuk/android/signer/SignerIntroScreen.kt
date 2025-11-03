package com.nunchuk.android.signer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.compose.NcCircleImage
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.lightGray
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.core.signer.KeyFlow
import com.nunchuk.android.core.signer.OnChainAddSignerParam
import com.nunchuk.android.model.signer.SupportedSigner
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType

@Composable
fun SignerIntroScreen(
    keyFlow: Int = KeyFlow.NONE,
    supportedSigners: List<SupportedSigner> = emptyList(),
    viewModel: SignerIntroViewModel,
    onChainAddSignerParam: OnChainAddSignerParam? = null,
    onClick: (KeyType) -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isDisableAll = keyFlow != KeyFlow.NONE
    val isGenericAirgapEnable = (supportedSigners.isEmpty()
            || supportedSigners.any { it.type == SignerType.AIRGAP && it.tag == null }) && isDisableAll.not()
    NunchukTheme {
        Scaffold(topBar = {
            NcTopAppBar(
                title = "",
                isBack = false
            )
        }) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (onChainAddSignerParam?.isAddInheritanceSigner() == true) stringResource(
                        R.string.nc_add_inheritance_key
                    ) else if (onChainAddSignerParam?.isVerifyBackupSeedPhrase() == true) stringResource(
                        R.string.nc_re_add_restored_key
                    ) else stringResource(
                        R.string.nc_add_key
                    ),
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(top = 4.dp),
                    text = stringResource(R.string.nc_select_your_key_type),
                    style = NunchukTheme.typography.body
                )

                if (onChainAddSignerParam != null) {
                    DynamicSignerSelection(
                        dynamicSupportedSigners = state.dynamicSupportedSigners,
                        onClick = onClick
                    )
                } else {
                    StaticSignerSelection(
                        supportedSigners = supportedSigners,
                        isDisableAll = isDisableAll,
                        isGenericAirgapEnable = isGenericAirgapEnable,
                        onClick = onClick
                    )
                }
            }
        }
    }
}

@Composable
internal fun StaticSignerSelection(
    supportedSigners: List<SupportedSigner>,
    isDisableAll: Boolean,
    isGenericAirgapEnable: Boolean,
    onClick: (KeyType) -> Unit
) {
    Row(
        modifier = Modifier
            .padding(top = 16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SignerItem(
            modifier = Modifier.weight(1f),
            iconRes = R.drawable.ic_nfc_card,
            title = stringResource(id = R.string.nc_tapsigner),
            onClick = { onClick(KeyType.TAPSIGNER) },
            isDisabled = supportedSigners.isNotEmpty() && !supportedSigners.any { it.type == SignerType.NFC } || isDisableAll
        )
        SignerItem(
            modifier = Modifier.weight(1f),
            iconRes = R.drawable.ic_coldcard_small,
            title = stringResource(id = R.string.nc_coldcard),
            onClick = { onClick(KeyType.COLDCARD) },
            isDisabled = supportedSigners.isNotEmpty() && !supportedSigners.any { it.type == SignerType.COLDCARD_NFC } || isDisableAll
        )
    }

    Row(
        modifier = Modifier
            .padding(top = 16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SignerItem(
            modifier = Modifier.weight(1f),
            iconRes = R.drawable.ic_air_gapped_jade,
            title = stringResource(id = R.string.nc_jade),
            onClick = { onClick(KeyType.JADE) },
            isDisabled = supportedSigners.isNotEmpty()
                    && !supportedSigners.any { it.type == SignerType.AIRGAP && (it.tag == SignerTag.JADE || it.tag == null) } || isDisableAll
        )
        SignerItem(
            modifier = Modifier.weight(1f),
            iconRes = R.drawable.ic_portal_nfc,
            title = stringResource(id = R.string.nc_portal),
            onClick = { onClick(KeyType.PORTAL) },
            isDisabled = supportedSigners.isNotEmpty()
                    && !supportedSigners.any { it.type == SignerType.PORTAL_NFC } || isDisableAll
        )
    }

    Row(
        modifier = Modifier
            .padding(top = 16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SignerItem(
            modifier = Modifier.weight(1f),
            iconRes = R.drawable.ic_air_gapped_seedsigner,
            title = stringResource(id = R.string.nc_seedsigner),
            onClick = { onClick(KeyType.SEEDSIGNER) },
            isDisabled = supportedSigners.isNotEmpty()
                    && !supportedSigners.any { it.type == SignerType.AIRGAP && (it.tag == SignerTag.SEEDSIGNER || it.tag == null) } || isDisableAll
        )
        SignerItem(
            modifier = Modifier.weight(1f),
            iconRes = R.drawable.ic_air_gapped_keystone,
            title = stringResource(id = R.string.nc_keystone),
            onClick = { onClick(KeyType.KEYSTONE) },
            isDisabled = supportedSigners.isNotEmpty()
                    && !supportedSigners.any { it.type == SignerType.AIRGAP && (it.tag == SignerTag.KEYSTONE || it.tag == null) } || isDisableAll
        )
    }

    Row(
        modifier = Modifier
            .padding(top = 16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SignerItem(
            modifier = Modifier.weight(1f),
            iconRes = R.drawable.ic_air_gapped_passport,
            title = stringResource(id = R.string.nc_foundation),
            onClick = { onClick(KeyType.FOUNDATION) },
            isDisabled = supportedSigners.isNotEmpty()
                    && !supportedSigners.any { it.type == SignerType.AIRGAP && (it.tag == SignerTag.PASSPORT || it.tag == null) } || isDisableAll
        )
        SignerItem(
            modifier = Modifier.weight(1f),
            iconRes = R.drawable.ic_ledger_hardware,
            title = stringResource(id = R.string.nc_ledger),
            isDisabled = true,
            subtitle = stringResource(id = R.string.nc_desktop_only),
        )
    }

    Row(
        modifier = Modifier
            .padding(top = 16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SignerItem(
            modifier = Modifier.weight(1f),
            iconRes = R.drawable.ic_bitbox_hardware,
            title = stringResource(id = R.string.nc_bitbox),
            isDisabled = true,
            subtitle = stringResource(id = R.string.nc_desktop_only)
        )
        SignerItem(
            modifier = Modifier.weight(1f),
            iconRes = R.drawable.ic_trezor_hardware,
            title = stringResource(id = R.string.nc_trezor),
            isDisabled = true,
            subtitle = stringResource(id = R.string.nc_desktop_only)
        )
    }

    HorizontalDivider(
        modifier = Modifier.padding(vertical = 16.dp)
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onClick(KeyType.SOFTWARE) }
    ) {
        NcCircleImage(
            resId = R.drawable.ic_logo_dark_small,
        )

        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = stringResource(R.string.nc_software),
                style = NunchukTheme.typography.body
            )
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = stringResource(R.string.nc_text_ss_desc),
                style = NunchukTheme.typography.bodySmall
                    .copy(color = MaterialTheme.colorScheme.textSecondary)
            )
        }
    }

    Row(
        modifier = Modifier
            .padding(top = 16.dp)
            .fillMaxWidth()
            .alpha(if (isGenericAirgapEnable) 1f else 0.6f)
            .clickable(enabled = isGenericAirgapEnable) {
                onClick(KeyType.GENERIC_AIRGAP)
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        NcCircleImage(
            resId = R.drawable.ic_split,
        )

        Text(
            modifier = Modifier.padding(start = 12.dp),
            text = stringResource(R.string.nc_generic_airgap),
            style = NunchukTheme.typography.body
        )
    }
}

@Composable
internal fun DynamicSignerSelection(
    dynamicSupportedSigners: List<SupportedSigner>,
    onClick: (KeyType) -> Unit
) {
    val signerItemsData = dynamicSupportedSigners.mapNotNull { signer ->
        mapSupportedSignerToItemData(signer)
    }

    // Group items into pairs for rows
    signerItemsData.chunked(2).forEach { rowItems ->
        Row(
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            rowItems.forEachIndexed { index, itemData ->
                SignerItem(
                    modifier = Modifier.weight(1f),
                    iconRes = itemData.iconRes,
                    title = itemData.title,
                    subtitle = itemData.subtitle,
                    onClick = { onClick(itemData.keyType) },
                    isDisabled = false
                )
            }

            // Add invisible spacer if odd number in last row
            if (rowItems.size == 1) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

private data class SignerItemData(
    val iconRes: Int,
    val title: String,
    val subtitle: String = "",
    val keyType: KeyType,
    val isDisabled: Boolean = false
)

@Composable
private fun mapSupportedSignerToItemData(signer: SupportedSigner): SignerItemData? {
    return when (signer.type) {
        SignerType.NFC -> SignerItemData(
            iconRes = R.drawable.ic_nfc_card,
            title = stringResource(id = R.string.nc_tapsigner),
            keyType = KeyType.TAPSIGNER,
            isDisabled = false
        )

        SignerType.COLDCARD_NFC -> SignerItemData(
            iconRes = R.drawable.ic_coldcard_small,
            title = stringResource(id = R.string.nc_coldcard),
            keyType = KeyType.COLDCARD,
            isDisabled = false
        )

        SignerType.AIRGAP -> {
            when (signer.tag) {
                SignerTag.JADE -> SignerItemData(
                    iconRes = R.drawable.ic_air_gapped_jade,
                    title = stringResource(id = R.string.nc_jade),
                    keyType = KeyType.JADE,
                    isDisabled = false
                )

                SignerTag.SEEDSIGNER -> SignerItemData(
                    iconRes = R.drawable.ic_air_gapped_seedsigner,
                    title = stringResource(id = R.string.nc_seedsigner),
                    keyType = KeyType.SEEDSIGNER,
                    isDisabled = false
                )

                SignerTag.KEYSTONE -> SignerItemData(
                    iconRes = R.drawable.ic_air_gapped_keystone,
                    title = stringResource(id = R.string.nc_keystone),
                    keyType = KeyType.KEYSTONE,
                    isDisabled = false
                )

                SignerTag.PASSPORT -> SignerItemData(
                    iconRes = R.drawable.ic_air_gapped_passport,
                    title = stringResource(id = R.string.nc_foundation),
                    keyType = KeyType.FOUNDATION,
                    isDisabled = false
                )

                else -> null
            }
        }

        SignerType.PORTAL_NFC -> SignerItemData(
            iconRes = R.drawable.ic_portal_nfc,
            title = stringResource(id = R.string.nc_portal),
            keyType = KeyType.PORTAL,
            isDisabled = false
        )

        SignerType.HARDWARE -> {
            when (signer.tag) {
                SignerTag.LEDGER -> SignerItemData(
                    iconRes = R.drawable.ic_ledger_hardware,
                    title = stringResource(id = R.string.nc_ledger),
                    subtitle = stringResource(id = R.string.nc_desktop_only),
                    keyType = KeyType.LEDGER,
                    isDisabled = true
                )

                SignerTag.TREZOR -> SignerItemData(
                    iconRes = R.drawable.ic_trezor_hardware,
                    title = stringResource(id = R.string.nc_trezor),
                    subtitle = stringResource(id = R.string.nc_desktop_only),
                    keyType = KeyType.TREZOR,
                    isDisabled = true
                )

                SignerTag.BITBOX -> SignerItemData(
                    iconRes = R.drawable.ic_bitbox_hardware,
                    title = stringResource(id = R.string.nc_bitbox),
                    subtitle = stringResource(id = R.string.nc_desktop_only),
                    keyType = KeyType.BITBOX,
                    isDisabled = true
                )

                else -> null
            }
        }

        SignerType.SOFTWARE -> SignerItemData(
            iconRes = R.drawable.ic_logo_dark_small,
            title = stringResource(id = R.string.nc_software),
            keyType = KeyType.SOFTWARE,
            isDisabled = false
        )

        else -> null
    }
}

@Composable
internal fun SignerItem(
    modifier: Modifier = Modifier,
    iconRes: Int,
    title: String,
    subtitle: String = "",
    isDisabled: Boolean = false,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .background(
                color = if (isDisabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.lightGray,
                shape = RoundedCornerShape(12.dp)
            )
            .alpha(if (isDisabled) 0.6f else 1f)
            .padding(vertical = 18.dp, horizontal = 12.dp)
            .clickable(enabled = !isDisabled) {
                onClick()
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NcIcon(
            painter = painterResource(id = iconRes),
            contentDescription = "Key icon",
            modifier = Modifier.size(24.dp)
        )

        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = title,
                style = NunchukTheme.typography.body,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = NunchukTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

    }
}

@PreviewLightDark
@Composable
fun SignerIntroScreenContentPreview() {
    SignerIntroScreen(
        viewModel = hiltViewModel(),
        supportedSigners = listOf(
            SupportedSigner(
                type = SignerType.SOFTWARE,
                tag = SignerTag.JADE,
                walletType = null,
                addressType = AddressType.TAPROOT
            ),
            SupportedSigner(
                type = SignerType.AIRGAP,
                tag = SignerTag.JADE,
                walletType = null,
                addressType = AddressType.TAPROOT
            ),
        )
    )
}

@PreviewLightDark
@Composable
fun SignerItemPreview() {
    SignerItem(
        iconRes = R.drawable.ic_key,
        title = "tapsigner",
        subtitle = "Desktop only",
        isDisabled = true,
    )
}

enum class KeyType {
    TAPSIGNER,
    COLDCARD,
    JADE,
    PORTAL,
    SEEDSIGNER,
    KEYSTONE,
    FOUNDATION,
    LEDGER,
    BITBOX,
    TREZOR,
    SOFTWARE,
    GENERIC_AIRGAP
}
