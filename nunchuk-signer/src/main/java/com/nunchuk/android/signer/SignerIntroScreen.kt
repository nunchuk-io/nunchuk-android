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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.compose.NcCircleImage
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.lightGray
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.core.signer.OnChainAddSignerParam
import com.nunchuk.android.core.signer.getSelectKeyTypeSubtitleRes

@Composable
fun SignerIntroScreen(
    viewModel: SignerIntroViewModel,
    onChainAddSignerParam: OnChainAddSignerParam? = null,
    onClick: (KeyType) -> Unit = {},
    onMoreClicked: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SignerIntroScreenContent(
        state = state,
        onChainAddSignerParam = onChainAddSignerParam,
        onClick = onClick,
        onMoreClicked = onMoreClicked,
    )
}

@Composable
fun SignerIntroScreenContent(
    state: SignerIntroState,
    onChainAddSignerParam: OnChainAddSignerParam? = null,
    onClick: (KeyType) -> Unit = {},
    onMoreClicked: () -> Unit = {},
) {
    Scaffold(topBar = {
        NcTopAppBar(
            title = "",
            isBack = false,
            actions = {
                if (onChainAddSignerParam != null && onChainAddSignerParam.isClaiming.not()) {
                    IconButton(onClick = onMoreClicked) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_more),
                            contentDescription = "More icon"
                        )
                    }
                }
            }
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
                text = stringResource(onChainAddSignerParam.getSelectKeyTypeSubtitleRes()),
                style = NunchukTheme.typography.body
            )

            SignerSelection(
                displayInfos = state.signerDisplayInfos,
                onClick = onClick,
            )
        }
    }
}

@Composable
internal fun SignerSelection(
    displayInfos: List<SignerDisplayInfo>,
    onClick: (KeyType) -> Unit,
) {
    val cardItems = displayInfos.filter { it.category == SignerDisplayCategory.CARD }
    val rowItems = displayInfos.filter { it.category == SignerDisplayCategory.ROW }
    val simpleRowItems = displayInfos.filter { it.category == SignerDisplayCategory.ROW_SIMPLE }

    cardItems.chunked(2).forEach { pair ->
        Row(
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            pair.forEach { item ->
                SignerItem(
                    modifier = Modifier.weight(1f),
                    iconRes = item.iconRes,
                    title = stringResource(item.titleRes),
                    subtitle = if (item.descriptionRes != 0) stringResource(item.descriptionRes) else "",
                    onClick = { onClick(item.keyType) },
                    isDisabled = item.isDisabled
                )
            }

            if (pair.size == 1) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }

    rowItems.forEach { item ->
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 16.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(enabled = !item.isDisabled) {
                onClick(item.keyType)
            }
        ) {
            NcCircleImage(
                resId = item.iconRes,
            )

            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = stringResource(item.titleRes),
                    style = NunchukTheme.typography.body
                )
                if (item.descriptionRes != 0) {
                    Text(
                        modifier = Modifier.padding(top = 4.dp),
                        text = stringResource(item.descriptionRes),
                        style = NunchukTheme.typography.bodySmall
                            .copy(color = MaterialTheme.colorScheme.textSecondary)
                    )
                }
            }
        }
    }

    simpleRowItems.forEach { item ->
        Row(
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .alpha(if (!item.isDisabled) 1f else 0.6f)
                .clickable(enabled = !item.isDisabled) {
                    onClick(item.keyType)
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            NcCircleImage(
                resId = item.iconRes,
            )

            Text(
                modifier = Modifier.padding(start = 12.dp),
                text = stringResource(item.titleRes),
                style = NunchukTheme.typography.body
            )
        }
    }
}

@Composable
private fun SignerItem(
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
    NunchukTheme {
        SignerIntroScreenContent(
            state = SignerIntroState(
                supportedSigners = defaultSupportedSigners,
                signerDisplayInfos = defaultSupportedSigners.mapNotNull { it.toDisplayInfo() }
                    + SignerDisplayInfo(
                        iconRes = R.drawable.ic_split,
                        titleRes = R.string.nc_generic_airgap,
                        keyType = KeyType.GENERIC_AIRGAP,
                        category = SignerDisplayCategory.ROW_SIMPLE,
                    ),
            ),
            onChainAddSignerParam = null,
            onClick = {},
            onMoreClicked = {},
        )
    }
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
    GENERIC_AIRGAP,
    PLATFORM_KEY,
}
