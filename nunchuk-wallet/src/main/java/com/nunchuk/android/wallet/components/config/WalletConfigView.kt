package com.nunchuk.android.wallet.components.config

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcBadgePrimary
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.getWalletColors
import com.nunchuk.android.compose.isLimitAccess
import com.nunchuk.android.compose.miniscript.MiniscriptTaproot
import com.nunchuk.android.compose.miniscript.PolicyHeader
import com.nunchuk.android.compose.miniscript.ScriptMode
import com.nunchuk.android.compose.miniscript.ScriptNodeData
import com.nunchuk.android.compose.miniscript.ScriptNodeTree
import com.nunchuk.android.compose.provider.SignersModelProvider
import com.nunchuk.android.compose.signer.SignerCard
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.core.miniscript.ScriptNodeType
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.isTaproot
import com.nunchuk.android.model.ByzantineGroup
import com.nunchuk.android.model.ScriptNode
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.model.WalletConfig
import com.nunchuk.android.model.WalletExtended
import com.nunchuk.android.model.byzantine.isFacilitatorAdmin
import com.nunchuk.android.model.byzantine.toRole
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.WalletTemplate
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.util.toReadableString

@Composable
internal fun WalletConfigView(
    modifier: Modifier = Modifier,
    state: WalletConfigState,
    onEditWalletName: () -> Unit = {},
    onShowMore: () -> Unit = {},
    onChangeAlias: () -> Unit = {},
    openWalletConfig: (SignerModel) -> Unit = {},
) {
    val wallet = state.walletExtended.wallet
    val isLimitAccess = state.isDeprecatedGroupWallet || isLimitAccess(
        state.group,
        state.role,
        state.assistedWallet?.status
    )
    val isTaproot = state.walletExtended.wallet.addressType.isTaproot()
    val isValueKeySetDisabled =
        state.walletExtended.wallet.walletTemplate == WalletTemplate.DISABLE_KEY_PATH
    val totalRequireSigns = state.walletExtended.wallet.totalRequireSigns
    val isMultisig = state.walletExtended.wallet.signers.size > 1
    val requiredSignInfo =
        if (isMultisig) "${wallet.totalRequireSigns}/${wallet.signers.size} " else ""
    val walletTypeString = if (isMultisig) {
        stringResource(id = R.string.nc_wallet_multisig)
    } else {
        stringResource(id = R.string.nc_wallet_single_sig)
    }
    NunchukTheme {
        NcScaffold(
            modifier = modifier.navigationBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = stringResource(id = R.string.nc_text_wallet_config_title),
                    textStyle = NunchukTheme.typography.titleLarge,
                    isBack = false,
                    actions = {
                        IconButton(
                            onClick = onShowMore
                        ) {
                            NcIcon(
                                painter = painterResource(id = R.drawable.ic_more),
                                contentDescription = "More",
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
            ) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = getWalletColors(
                                    wallet = state.walletExtended.wallet,
                                    isLimitAccess = isLimitAccess,
                                    isAssistedWallet = state.isAssistedWallet,
                                    hasGroup = state.group != null,
                                    isFreeGroupWallet = state.isGroupSandboxWallet
                                ),
                                start = Offset.Zero,
                                end = Offset.Infinite,
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(vertical = 24.dp, horizontal = 16.dp)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (state.isAssistedWallet) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                NcIcon(
                                    painter = painterResource(id = R.drawable.ic_wallets),
                                    contentDescription = "Wallet",
                                    modifier = Modifier.size(16.dp),
                                    tint = colorResource(id = R.color.nc_white_color)
                                )

                                Text(
                                    text = stringResource(id = R.string.nc_assisted),
                                    style = NunchukTheme.typography.caption
                                        .copy(color = colorResource(id = R.color.nc_white_color))
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (state.isDeprecatedGroupWallet) "[DEPRECATED] ${wallet.name}" else wallet.name,
                                style = NunchukTheme.typography.titleLarge
                                    .copy(color = colorResource(id = R.color.nc_white_color))
                            )

                            NcIcon(
                                modifier = Modifier.clickable(onClick = onEditWalletName),
                                painter = painterResource(id = R.drawable.ic_edit),
                                tint = colorResource(id = R.color.nc_white_color),
                                contentDescription = "Wallet",
                            )
                        }
                        Row(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                modifier = Modifier.padding(end = 8.dp),
                                text = if (state.walletExtended.wallet.miniscript.isEmpty()) "$requiredSignInfo$walletTypeString" else "Miniscript",
                                style = NunchukTheme.typography.bodySmall
                                    .copy(color = colorResource(id = R.color.nc_white_color))
                            )

                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(shape = CircleShape)
                                    .background(colorResource(id = R.color.nc_white_color))
                            )

                            Text(
                                modifier = Modifier.padding(start = 8.dp),
                                text = state.walletExtended.wallet.addressType.toReadableString(
                                    LocalContext.current
                                ),
                                style = NunchukTheme.typography.bodySmall.copy(
                                    color = colorResource(
                                        id = R.color.nc_white_color
                                    )
                                )
                            )
                        }

                        if (state.group != null) {
                            NcOutlineButton(
                                modifier = Modifier
                                    .padding(top = 12.dp)
                                    .height(40.dp),
                                borderColor = colorResource(id = R.color.nc_white_color),
                                onClick = onChangeAlias
                            ) {
                                Text(
                                    text = if (state.alias.isEmpty()) {
                                        stringResource(id = R.string.nc_set_alias)
                                    } else {
                                        stringResource(R.string.nc_change_alias)
                                    },
                                    color = colorResource(
                                        id = R.color.nc_white_color
                                    )
                                )
                            }
                        }
                    }
                }
                if (state.scriptNode != null && state.signerMap.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        item {
                            Column(
                                modifier = Modifier.padding(
                                    horizontal = 16.dp,
                                    vertical = 24.dp
                                )
                            ) {

                                PolicyHeader(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp, bottom = 20.dp)
                                )
                                
                                TaprootAddressContent(state, wallet)

                                ScriptNodeTree(
                                    node = state.scriptNode,
                                    data = ScriptNodeData(
                                        mode = ScriptMode.VIEW,
                                        signers = state.signerMap,
                                        showBip32Path = true,
                                    ),
                                    onChangeBip32Path = { _, _ -> },
                                    onActionKey = { _, _ -> }
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        item("key") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                NcIcon(
                                    modifier = Modifier.size(24.dp),
                                    painter = painterResource(id = R.drawable.ic_mulitsig_dark),
                                    contentDescription = "Key",
                                    tint = MaterialTheme.colorScheme.textPrimary
                                )

                                Text(
                                    text = stringResource(id = R.string.nc_title_signers),
                                    style = NunchukTheme.typography.body
                                )
                            }
                        }

                        itemsIndexed(state.signers) { index, signer ->
                            WalletSignerCard(
                                signer = signer,
                                state = state,
                                isValueKey = isTaproot && !isValueKeySetDisabled && index < totalRequireSigns,
                                openWalletConfig = openWalletConfig
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaprootAddressContent(
    state: WalletConfigState,
    wallet: Wallet
) {
    if (state.walletExtended.wallet.addressType == AddressType.TAPROOT) {
        // assuming that the keyPath is the name of the first signer
        val scriptNodeMuSig = state.scriptNodeMuSig
        if (scriptNodeMuSig != null) {
            NcBadgePrimary(
                modifier = Modifier.padding(
                    vertical = 8.dp
                ),
                text = "Key path",
                enabled = true,
            )

            ScriptNodeTree(
                node = scriptNodeMuSig,
                data = ScriptNodeData(
                    mode = ScriptMode.VIEW,
                    signers = state.muSigSignerMap,
                    showBip32Path = true,
                ),
                onChangeBip32Path = { _, _ -> },
                onActionKey = { _, _ -> }
            )
        } else {
            val keyPath =
                if (state.walletExtended.wallet.walletTemplate == WalletTemplate.DISABLE_KEY_PATH) {
                    ""
                } else {
                    state.walletExtended.wallet.signers.firstOrNull()?.name
                }
            MiniscriptTaproot(
                keyPath = keyPath.orEmpty(),
                data = ScriptNodeData(
                    mode = ScriptMode.VIEW,
                    signers = state.signerMap,
                    showBip32Path = true
                ),
                signer = wallet.signers.firstOrNull()?.toModel(),
                onChangeBip32Path = { _, _ -> },
                onActionKey = { _, _ -> }
            )
        }

        // Add Script path badge
        NcBadgePrimary(
            modifier = Modifier.padding(
                top = 16.dp,
                bottom = 8.dp,
                end = 16.dp
            ),
            text = "Script path",
            enabled = true
        )
    }
}

@Composable
fun WalletSignerCard(
    signer: SignerModel,
    state: WalletConfigState,
    isValueKey: Boolean,
    modifier: Modifier = Modifier,
    openWalletConfig: (SignerModel) -> Unit = {}
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SignerCard(
            item = signer,
            modifier = Modifier.weight(1f),
            showValueKey = isValueKey
        ) {
            Text(
                text = stringResource(
                    R.string.nc_bip32_path,
                    signer.derivationPath
                ),
                style = NunchukTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textSecondary
            )
        }
        if (signer.type == SignerType.SERVER && !state.isInactiveAssistedWallet && !state.role.toRole.isFacilitatorAdmin) {
            NcOutlineButton(
                modifier = Modifier.height(36.dp),
                onClick = { openWalletConfig(signer) },
                enabled = state.isAssistedWallet
            ) {
                Text(
                    text = stringResource(id = R.string.nc_view_policies),
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun WalletConfigViewPreview(
    @PreviewParameter(SignersModelProvider::class) signers: List<SignerModel>,
) {
    WalletConfigView(
        state = WalletConfigState(
            walletExtended = WalletExtended(
                wallet = Wallet(
                    name = "Wallet",
                )
            ),
            signers = signers,
            group = ByzantineGroup(
                id = "id",
                members = emptyList(),
                setupPreference = "setupPreference",
                status = "status",
                walletConfig = WalletConfig(
                    n = 0,
                    m = 0,
                    requiredServerKey = true,
                    allowInheritance = false,
                ),
                isViewPendingWallet = false,
                isLocked = false,
                slug = "slug",
                createdTimeMillis = 0,
            )
        ),
    )
}

@PreviewLightDark
@Composable
private fun WalletConfigViewMiniscriptPreview(
    @PreviewParameter(SignersModelProvider::class) sampleSigners: List<SignerModel>,
) {
    // Create sample script node with miniscript structure
    val sampleScriptNode = ScriptNode(
        id = emptyList(),
        data = byteArrayOf(),
        type = ScriptNodeType.ANDOR.name,
        keys = listOf(),
        k = 0,
        timeLock = null,
        subs = listOf(
            ScriptNode(
                type = ScriptNodeType.THRESH.name,
                keys = listOf("key_0_0", "key_1_0"),
                subs = emptyList(),
                k = 2,
                id = emptyList(),
                data = byteArrayOf(),
                timeLock = null
            ),
            ScriptNode(
                type = ScriptNodeType.OLDER.name,
                keys = listOf("key_0_1"),
                subs = emptyList(),
                k = 0,
                id = emptyList(),
                data = byteArrayOf(),
                timeLock = null
            )
        )
    )

    // Create signer map that maps keys to signers
    val sampleSignerMap = mapOf(
        "key_0_0" to sampleSigners[0],
        "key_1_0" to sampleSigners[1],
        "key_0_1" to sampleSigners[2]
    )

    WalletConfigView(
        state = WalletConfigState(
            walletExtended = WalletExtended(
                wallet = Wallet(
                    name = "Miniscript Wallet",
                    addressType = AddressType.TAPROOT,
                    miniscript = "andor(thresh(2,pk(key_0_0),pk(key_1_0)),older(4320))"
                )
            ),
            signers = sampleSigners,
            scriptNode = sampleScriptNode,
            signerMap = sampleSignerMap,
            group = ByzantineGroup(
                id = "id",
                members = emptyList(),
                setupPreference = "setupPreference",
                status = "status",
                walletConfig = WalletConfig(
                    n = 0,
                    m = 0,
                    requiredServerKey = true,
                    allowInheritance = false,
                ),
                isViewPendingWallet = false,
                isLocked = false,
                slug = "slug",
                createdTimeMillis = 0,
            )
        ),
    )
}