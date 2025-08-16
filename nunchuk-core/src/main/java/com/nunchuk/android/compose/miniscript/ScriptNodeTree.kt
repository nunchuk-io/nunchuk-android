package com.nunchuk.android.compose.miniscript

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcBadgeOutline
import com.nunchuk.android.compose.NcCircleImage
import com.nunchuk.android.compose.NcColor
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.backgroundMidGray
import com.nunchuk.android.compose.beeswaxDark
import com.nunchuk.android.compose.fillBeeswax
import com.nunchuk.android.compose.fillDenim
import com.nunchuk.android.compose.fillPink
import com.nunchuk.android.compose.provider.ScriptNodeProvider
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.core.R
import com.nunchuk.android.core.miniscript.ScriptNodeType
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.signDone
import com.nunchuk.android.core.util.toAmount
import com.nunchuk.android.model.CoinsGroup
import com.nunchuk.android.model.KeySetStatus
import com.nunchuk.android.model.ScriptNode
import com.nunchuk.android.model.SigningPath
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.type.MiniscriptTimelockBased
import com.nunchuk.android.type.TransactionStatus
import com.nunchuk.android.utils.dateTimeFormat
import timber.log.Timber
import java.util.Date
import java.util.Locale

enum class ScriptMode {
    VIEW,
    CONFIG,
    SIGN
}

val avatarColors = listOf(
    Color(0xFF1C652D),
    Color(0xFFA66800),
    Color(0xFFCF4018),
    Color(0xFF7E519B),
    Color(0xFF2F466C),
    Color(0xFFF1AE00),
    Color(0xFF757575),
)

@Composable
fun ScriptNodeTree(
    node: ScriptNode,
    index: String = "1",
    isLastItem: Boolean = false,
    level: Int = 0,
    onChangeBip32Path: (String, SignerModel) -> Unit = { _, _ -> },
    onActionKey: (String, SignerModel?) -> Unit = { _, _ -> },
    data: ScriptNodeData = ScriptNodeData()
) {
    val isNormalNode =
        data.signingPath.path.isEmpty() || signingPathContainsNodeId(data.signingPath.path, node.id)
    val isSatisfiableNode = data.satisfiableMap[node.idString] != false

    val nodeModifier = when {
        data.mode == ScriptMode.CONFIG || node.type == ScriptNodeType.AFTER.name || node.type == ScriptNodeType.OLDER.name -> Modifier
        data.mode == ScriptMode.SIGN && isSatisfiableNode -> Modifier
        data.mode == ScriptMode.VIEW && isNormalNode -> Modifier
        else -> Modifier.alpha(0.4f)
    }
    when (node.type) {
        ScriptNodeType.ANDOR.name, ScriptNodeType.AND.name, ScriptNodeType.OR.name, ScriptNodeType.OR_TAPROOT.name -> {
            TreeBranchContainer(
                modifier = nodeModifier,
                drawLine = isLastItem.not(),
                indentationLevel = level
            ) { modifier, showThreadCurve ->
                AndOrView(
                    isShowCurve = showThreadCurve,
                    isShowTapscriptBadge = ScriptNodeType.OR_TAPROOT.name == node.type,
                    padStart = 0,
                    node = node
                ) {
                    ActivePolicyAfterView(
                        isSatisfiableNode = isSatisfiableNode,
                        id = node.idString,
                        coinsGroup = data.coinGroups,
                        lockBased = data.lockBased,
                        numberOfInputCoin = data.inputCoins.size
                    )
                    NodeContent(
                        node = node,
                        onChangeBip32Path = onChangeBip32Path,
                        onActionKey = onActionKey,
                        data = data,
                        level = level,
                        modifier = modifier
                    )
                }
            }
        }

        ScriptNodeType.AFTER.name, ScriptNodeType.OLDER.name -> {
            TreeBranchContainer(
                modifier = nodeModifier,
                drawLine = isLastItem.not(),
                indentationLevel = level,
            ) { modifier, showThreadCurve ->
                TimelockItem(
                    modifier = modifier,
                    currentBlockHeight = data.currentBlockHeight,
                    showThreadCurve = showThreadCurve,
                    data = data,
                    node = node
                ) {
                    NodeContent(
                        node = node,
                        onChangeBip32Path = onChangeBip32Path,
                        onActionKey = onActionKey,
                        data = data,
                        level = level,
                        modifier = modifier
                    )
                }
            }
        }

        ScriptNodeType.PK.name -> {
            TreeBranchContainer(
                modifier = nodeModifier,
                drawLine = isLastItem.not(),
                indentationLevel = level
            ) { modifier, showThreadCurve ->
                val signer = data.signers[node.keys.firstOrNull().orEmpty()]
                val avatarColor = if (signer?.isVisible == false) {
                    avatarColors[level % avatarColors.size]
                } else {
                    avatarColors[0]
                }
                CreateKeyItem(
                    key = node.keys.firstOrNull() ?: "",
                    signer = signer,
                    position = index,
                    onChangeBip32Path = onChangeBip32Path,
                    onActionKey = onActionKey,
                    data = data,
                    showThreadCurve = showThreadCurve,
                    modifier = modifier,
                    isSatisfiable = isSatisfiableNode,
                    avatarColor = avatarColor
                )
            }
        }

        ScriptNodeType.MULTI.name, ScriptNodeType.THRESH.name -> {
            TreeBranchContainer(
                modifier = Modifier,
                drawLine = isLastItem.not(),
                indentationLevel = level
            ) { modifier, showThreadCurve ->
                ThreshMultiItem(
                    topPadding = if (showThreadCurve) 10 else 0,
                    showThreadCurve = showThreadCurve,
                    modifier = modifier.then(nodeModifier),
                    isSatisfiable = isSatisfiableNode,
                    data = data,
                    node = node
                ) {
                    ActivePolicyAfterView(
                        lockBased = data.lockBased,
                        id = node.idString,
                        coinsGroup = data.coinGroups,
                        isSatisfiableNode = isSatisfiableNode,
                        numberOfInputCoin = data.inputCoins.size,
                    )
                    NodeContent(
                        node = node,
                        onChangeBip32Path = onChangeBip32Path,
                        onActionKey = onActionKey,
                        data = data,
                        level = level,
                        modifier = modifier.then(nodeModifier)
                    )
                }
            }
        }

        ScriptNodeType.HASH160.name, ScriptNodeType.HASH256.name, ScriptNodeType.RIPEMD160.name, ScriptNodeType.SHA256.name -> {
            TreeBranchContainer(
                modifier = nodeModifier,
                drawLine = isLastItem.not(),
                indentationLevel = level
            ) { modifier, showThreadCurve ->
                HashlockItem(
                    data = data,
                    showThreadCurve = showThreadCurve,
                    node = node
                ) {
                    NodeContent(
                        node = node,
                        onChangeBip32Path = onChangeBip32Path,
                        onActionKey = onActionKey,
                        data = data,
                        level = level,
                        modifier = modifier
                    )
                }
            }
        }

        ScriptNodeType.MUSIG.name -> {
            TreeBranchContainer(
                modifier = Modifier,
                drawLine = isLastItem.not(),
                indentationLevel = level
            ) { modifier, showThreadCurve ->
                MusigItem(
                    topPadding = if (showThreadCurve) 10 else 0,
                    showThreadCurve = showThreadCurve,
                    modifier = modifier.then(nodeModifier),
                    isSatisfiable = isSatisfiableNode,
                    data = data,
                    node = node
                ) {
                    ActivePolicyAfterView(
                        lockBased = data.lockBased,
                        id = node.idString,
                        coinsGroup = data.coinGroups,
                        isSatisfiableNode = isSatisfiableNode,
                        numberOfInputCoin = data.inputCoins.size
                    )
                    NodeContent(
                        node = node,
                        onChangeBip32Path = onChangeBip32Path,
                        onActionKey = onActionKey,
                        data = data,
                        level = level,
                        modifier = modifier.then(nodeModifier)
                    )
                }
            }
        }

        else -> NodeSubs(
            node = node,
            onChangeBip32Path = onChangeBip32Path,
            onActionKey = onActionKey,
            data = data,
            level = level
        )
    }
}

@Composable
internal fun CreateKeyItem(
    modifier: Modifier = Modifier,
    key: String,
    signer: SignerModel?,
    position: String,
    showThreadCurve: Boolean = true,
    onChangeBip32Path: (String, SignerModel) -> Unit,
    onActionKey: (String, SignerModel?) -> Unit,
    isSatisfiable: Boolean = true,
    keySetStatus: KeySetStatus? = null,
    data: ScriptNodeData,
    avatarColor: Color = avatarColors[0]
) {
    val isSigned: Boolean =
        data.mode == ScriptMode.SIGN && signer != null && if (keySetStatus != null) {
            keySetStatus.signerStatus[signer.fingerPrint] == true || keySetStatus.status.signDone()
        } else {
            data.signedSigners[signer.fingerPrint] == true
        }
    KeyItem(
        title = signer?.name ?: key,
        xfp = signer?.getXfpOrCardIdLabel().orEmpty(),
        position = position,
        modifier = modifier,
        showThreadCurve = showThreadCurve,
        data = data,
        avatarColor = avatarColor,
        isOccupied = data.isSlotOccupied(signer?.name ?: key),
        bip32PathContent = {
            if (data.showBip32Path && signer != null) {
                val isDuplicateSigner =
                    data.duplicateSignerKeys.contains("${signer.fingerPrint}:${signer.derivationPath}")
                Row(
                    modifier = if (data.mode == ScriptMode.CONFIG) Modifier.clickable(
                        onClick = {
                            onChangeBip32Path(key, signer)
                        },
                        enabled = signer.isMasterSigner,
                    ) else Modifier,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        modifier = Modifier.weight(1f, false),
                        text = stringResource(
                            R.string.nc_bip32_path,
                            signer.derivationPath
                        ),
                        style = if (data.mode == ScriptMode.CONFIG && signer.isMasterSigner) {
                            NunchukTheme.typography.bodySmall.copy(textDecoration = TextDecoration.Underline)
                        } else {
                            NunchukTheme.typography.bodySmall
                        },
                        color = if (isDuplicateSigner) {
                            Color.Red
                        } else {
                            MaterialTheme.colorScheme.textSecondary
                        }
                    )
                    if (data.mode == ScriptMode.CONFIG && signer.isMasterSigner) {
                        NcIcon(
                            modifier = Modifier.size(12.dp),
                            painter = painterResource(id = R.drawable.ic_edit_small),
                            contentDescription = "Edit icon",
                            tint = if (isDuplicateSigner) {
                                Color.Red
                            } else {
                                MaterialTheme.colorScheme.textSecondary
                            }
                        )
                    }
                }
            }
        },
        actionContent = {
            when {
                data.mode == ScriptMode.CONFIG && signer == null -> {
                    NcOutlineButton(
                        height = 36.dp,
                        onClick = { onActionKey(key, null) },
                    ) {
                        Text(stringResource(R.string.nc_add))
                    }
                }

                data.mode == ScriptMode.CONFIG && signer != null -> {
                    NcOutlineButton(
                        height = 36.dp,
                        onClick = { onActionKey(key, signer) },
                    ) {
                        Text(stringResource(R.string.nc_remove))
                    }
                }

                isSigned && isSatisfiable -> {
                    if (keySetStatus != null && keySetStatus.status == TransactionStatus.PENDING_NONCE) {
                        CheckedLabel(
                            text = stringResource(R.string.nc_committed),
                        )
                    } else {
                        CheckedLabel(
                            text = stringResource(R.string.nc_transaction_signed),
                        )
                    }
                }

                data.mode == ScriptMode.SIGN && signer != null -> {
                    NcPrimaryDarkButton(
                        height = 36.dp,
                        onClick = { onActionKey(key, signer) },
                        enabled = isSatisfiable
                    ) {
                        if (keySetStatus != null && keySetStatus.status == TransactionStatus.PENDING_NONCE) {
                            Text(stringResource(R.string.nc_commit))
                        } else {
                            Text(stringResource(R.string.nc_sign))
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun NodeKeys(
    node: ScriptNode,
    onChangeBip32Path: (String, SignerModel) -> Unit,
    onActionKey: (String, SignerModel?) -> Unit,
    data: ScriptNodeData,
    level: Int,
    modifier: Modifier = Modifier
) {
    var localColorIndex = data.colorIndex
    Timber.tag("miniscript-feature").d("NodeKeys: Starting with colorIndex = $localColorIndex for node ${node.idString}")
    node.keys.forEachIndexed { i, key ->
        val keyPosition = "${node.idString}.${i + 1}"
        TreeBranchContainer(
            modifier = modifier,
            drawLine = i != node.keys.size - 1 || node.subs.isNotEmpty(),
            indentationLevel = level + 1 // Keys are one level deeper than their parent node
        ) { modifier, showThreadCurve ->
            val signer = data.signers[key]
            val avatarColor = if (signer?.isVisible == false) {
                val colorIndex = localColorIndex++ % avatarColors.size
                avatarColors[colorIndex]
            } else {
                avatarColors[0]
            }
            CreateKeyItem(
                key = key,
                signer = signer,
                position = keyPosition,
                onChangeBip32Path = onChangeBip32Path,
                onActionKey = onActionKey,
                data = data,
                showThreadCurve = showThreadCurve,
                modifier = modifier,
                keySetStatus = if (node.type == ScriptNodeType.MUSIG.name) data.keySetStatues[node.idString] else null,
                isSatisfiable = data.satisfiableMap[node.idString] != false,
                avatarColor = avatarColor
            )
        }
    }
}

@Composable
private fun NodeSubs(
    node: ScriptNode,
    onChangeBip32Path: (String, SignerModel) -> Unit,
    onActionKey: (String, SignerModel?) -> Unit,
    data: ScriptNodeData,
    level: Int
) {
    var localColorIndex = data.colorIndex
    Timber.tag("miniscript-feature").d("NodeSubs: Starting with colorIndex = $localColorIndex for node ${node.idString}")
    node.subs.forEachIndexed { i, sub ->
        // Create a new data object with incremented colorIndex for each child
        val childData = data.copy(colorIndex = localColorIndex)
        Timber.tag("miniscript-feature").d("NodeSubs: Child $i using colorIndex = $localColorIndex")
        ScriptNodeTree(
            node = sub,
            index = "${node.idString}.${node.keys.size + i + 1}",
            isLastItem = i == node.subs.size - 1,
            level = level + 1,
            onChangeBip32Path = onChangeBip32Path,
            onActionKey = onActionKey,
            data = childData
        )
        // Increment the colorIndex for the next child
        localColorIndex++
    }
}

@Composable
private fun NodeContent(
    node: ScriptNode,
    onChangeBip32Path: (String, SignerModel) -> Unit,
    onActionKey: (String, SignerModel?) -> Unit,
    data: ScriptNodeData,
    level: Int,
    modifier: Modifier = Modifier
) {
    NodeKeys(
        node = node,
        onChangeBip32Path = onChangeBip32Path,
        onActionKey = onActionKey,
        data = data,
        level = level,
        modifier = modifier
    )
    NodeSubs(
        node = node,
        onChangeBip32Path = onChangeBip32Path,
        onActionKey = onActionKey,
        data = data,
        level = level
    )
}

data class ScriptNodeData(
    val mode: ScriptMode = ScriptMode.CONFIG,
    val signers: Map<String, SignerModel?> = emptyMap(),
    val showBip32Path: Boolean = false,
    val signedSigners: Map<String, Boolean> = emptyMap(),
    val duplicateSignerKeys: Set<String> = emptySet(),
    val signingPath: SigningPath = SigningPath(path = emptyList()),
    val satisfiableMap: Map<String, Boolean> = emptyMap(),
    val keySetStatues: Map<String, KeySetStatus> = emptyMap(),
    val coinGroups: Map<String, CoinsGroup> = emptyMap(),
    val topLevelDisableNode: ScriptNode? = null,
    val onPreImageClick: (ScriptNode) -> Unit = {},
    val currentBlockHeight: Int = 0,
    val signedHash: Map<String, Boolean> = emptyMap(),
    val lockBased: MiniscriptTimelockBased = MiniscriptTimelockBased.NONE,
    val inputCoins: List<UnspentOutput> = emptyList(),
    val isGroupWallet: Boolean = false,
    val occupiedSlots: Set<String> = emptySet(),
    val colorIndex: Int = 0,
) {
    fun isSlotOccupied(position: String): Boolean {
        return occupiedSlots.contains(position)
    }
}

@Composable
fun AndOrView(
    modifier: Modifier = Modifier,
    isShowCurve: Boolean = true,
    isShowTapscriptBadge: Boolean = false,
    padStart: Int = 0,
    node: ScriptNode,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            if (isShowCurve) {
                CurveView()
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = if (isShowCurve) 10.dp else 0.dp)
                    .padding(start = padStart.dp)
            ) {
                Row {
                    Text(
                        text = "${node.idString}. ${node.displayName}",
                        style = NunchukTheme.typography.body
                    )
                    if (isShowTapscriptBadge) {
                        NcBadgeOutline(
                            modifier = Modifier.padding(start = 8.dp),
                            text = "Tapscripts"
                        )
                    }
                }

                if (node.descriptionText.isNotEmpty()) {
                    Text(
                        text = node.descriptionText,
                        style = NunchukTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.textSecondary
                        ),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            }
        }
        content()
    }
}

@Composable
fun MusigItem(
    modifier: Modifier = Modifier,
    topPadding: Int = 10,
    showThreadCurve: Boolean = true,
    isSatisfiable: Boolean,
    node: ScriptNode,
    data: ScriptNodeData = ScriptNodeData(),
    content: @Composable ColumnScope.() -> Unit = {},
) {
    var showDetail by remember(node.id) {
        mutableStateOf(data.topLevelDisableNode?.id != node.id)
    }

    val keySet: KeySetStatus? = data.keySetStatues[node.idString]
    val requiredSignatures = node.keys.size
    val signedCountFromKeySet = keySet?.signerStatus?.count { it.value } ?: 0
    val pendingFromKeySet = requiredSignatures - signedCountFromKeySet
    val round = if (keySet?.status == TransactionStatus.PENDING_NONCE) 1 else 2
    val isCompleted = keySet?.status?.signDone() == true

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
        ) {
            if (showThreadCurve) {
                CurveView(Modifier.then(modifier))
            }
            Column(
                modifier = Modifier
                    .then(modifier)
                    .weight(1f)
                    .padding(top = topPadding.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${node.idString}. ${node.displayName}",
                        style = NunchukTheme.typography.body
                    )
                    // Pending info: nonces in round 1, signatures in round 2
                    if (data.mode == ScriptMode.SIGN && isSatisfiable && keySet != null && !isCompleted) {
                        NcIcon(
                            painter = painterResource(id = R.drawable.ic_pending_signatures),
                            contentDescription = "Pending",
                            tint = MaterialTheme.colorScheme.textSecondary
                        )
                        val pluralId =
                            if (round == 1) R.plurals.nc_transaction_pending_nonce else R.plurals.nc_transaction_pending_signature
                        Text(
                            text = pluralStringResource(
                                pluralId,
                                pendingFromKeySet,
                                pendingFromKeySet
                            ),
                            style = NunchukTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.textSecondary,
                        )
                    }
                }
                if (node.descriptionText.isNotEmpty()) {
                    Text(
                        text = node.descriptionText,
                        style = NunchukTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.textSecondary
                        )
                    )
                }
            }
            // Badge on the right: Round or Completed
            if (keySet != null) {
                val badgeColor = when {
                    isCompleted -> colorResource(R.color.nc_slime_dark)
                    round == 1 && requiredSignatures == pendingFromKeySet -> MaterialTheme.colorScheme.backgroundMidGray
                    round == 1 -> MaterialTheme.colorScheme.fillBeeswax
                    else -> colorResource(R.color.nc_primary_y0)
                }
                val textColor = when {
                    isCompleted -> Color.White
                    round == 1 && requiredSignatures == pendingFromKeySet -> MaterialTheme.colorScheme.textSecondary
                    else -> colorResource(R.color.nc_grey_g7)
                }
                Row(modifier = Modifier.align(Alignment.CenterVertically)) {
                    Text(
                        text = if (isCompleted) "Completed" else "Round ${round}/2",
                        style = NunchukTheme.typography.titleSmall.copy(color = textColor),
                        modifier = Modifier
                            .background(color = badgeColor, shape = RoundedCornerShape(20.dp))
                            .padding(horizontal = 8.dp)
                    )
                }
            }

            if (data.topLevelDisableNode?.id == node.id) {
                Row(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .padding(top = topPadding.dp)
                        .clickable { showDetail = !showDetail },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showDetail) stringResource(R.string.nc_collapse)
                        else stringResource(R.string.nc_expand),
                        style = NunchukTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.textPrimary
                    )
                    NcIcon(
                        painter = painterResource(id = if (showDetail) R.drawable.ic_collapse else R.drawable.ic_expand),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(16.dp),
                        tint = MaterialTheme.colorScheme.textPrimary
                    )
                }
            }
        }
        if (showDetail) {
            content()
        }
    }
}

@Composable
fun ThreshMultiItem(
    modifier: Modifier = Modifier,
    topPadding: Int = 10,
    showThreadCurve: Boolean = true,
    isSatisfiable: Boolean,
    node: ScriptNode,
    data: ScriptNodeData = ScriptNodeData(),
    content: @Composable ColumnScope.() -> Unit = {},
) {
    // Only calculate signed signatures when in SIGN mode
    var showDetail by remember(node.id) {
        mutableStateOf(data.topLevelDisableNode?.id != node.id)
    }
    val pendingSigners = if (data.mode == ScriptMode.SIGN && isSatisfiable) {
        val signedCount = when (node.type) {
            ScriptNodeType.THRESH.name -> {
                node.subs.count { sub ->
                    val firstKey = sub.keys.firstOrNull()
                    if (firstKey != null) {
                        val signer = data.signers[firstKey]
                        val xfp = signer?.fingerPrint
                        xfp != null && data.signedSigners[xfp] == true
                    } else false
                }
            }

            ScriptNodeType.MULTI.name -> {
                node.keys.count { key ->
                    val signer = data.signers[key]
                    val xfp = signer?.fingerPrint
                    xfp != null && data.signedSigners[xfp] == true
                }
            }

            else -> 0
        }
        node.k - signedCount
    } else {
        0
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
        ) {
            if (showThreadCurve) {
                CurveView(Modifier.then(modifier))
            }
            Column(
                modifier = Modifier
                    .then(modifier)
                    .weight(1f)
                    .padding(top = topPadding.dp)
            ) {
                Text(
                    text = "${node.idString}. ${node.displayName}",
                    style = NunchukTheme.typography.body
                )
                if (node.descriptionText.isNotEmpty()) {
                    Text(
                        text = node.descriptionText,
                        style = NunchukTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.textSecondary
                        )
                    )
                }
            }

            // Show pending conditions or enough conditions collected
            if (data.mode == ScriptMode.SIGN && isSatisfiable) {
                Row(
                    modifier = Modifier
                        .then(modifier)
                        .padding(start = 8.dp)
                        .padding(top = topPadding.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (pendingSigners > 0) {
                        NcIcon(
                            painter = painterResource(id = R.drawable.ic_pending_signatures),
                            contentDescription = "Warning",
                            tint = MaterialTheme.colorScheme.textSecondary
                        )
                        Text(
                            text = pluralStringResource(
                                R.plurals.nc_transaction_pending_conditions,
                                pendingSigners,
                                pendingSigners
                            ),
                            style = NunchukTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.textSecondary,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    } else {
                        NcIcon(
                            painter = painterResource(id = R.drawable.ic_check_circle),
                            contentDescription = "Check",
                            tint = MaterialTheme.colorScheme.textSecondary
                        )
                        Text(
                            text = stringResource(R.string.nc_transaction_enough_conditions),
                            style = NunchukTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.textSecondary,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
            } else if (data.topLevelDisableNode?.id == node.id) {
                Row(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .padding(top = topPadding.dp)
                        .clickable { showDetail = !showDetail },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showDetail) stringResource(R.string.nc_collapse)
                        else stringResource(R.string.nc_expand),
                        style = NunchukTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.textPrimary
                    )
                    NcIcon(
                        painter = painterResource(id = if (showDetail) R.drawable.ic_collapse else R.drawable.ic_expand),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(16.dp),
                        tint = MaterialTheme.colorScheme.textPrimary
                    )
                }
            }
        }
        if (showDetail) {
            content()
        }
    }
}

@Composable
fun TimelockItem(
    modifier: Modifier = Modifier,
    currentBlockHeight: Int = 0,
    showThreadCurve: Boolean = true,
    node: ScriptNode,
    data: ScriptNodeData,
    content: @Composable () -> Unit = {},
) {
    val mode: ScriptMode = data.mode
    val title = node.displayName
    val description = node.getAfterBlockDescription(currentBlockHeight)

    // Calculate if timelock is unlocked based on type
    val isUnlocked = if (mode == ScriptMode.SIGN) when (node.type) {
        ScriptNodeType.AFTER.name -> {
            // AFTER: Check if current time/block has passed the timelock value
            val timelockValue = node.timeLock?.value ?: 0L
            when (data.lockBased) {
                MiniscriptTimelockBased.TIME_LOCK -> {
                    val currentTime = System.currentTimeMillis() / 1000L
                    currentTime >= timelockValue
                }
                MiniscriptTimelockBased.HEIGHT_LOCK -> {
                    currentBlockHeight >= timelockValue
                }
                else -> false
            }
        }
        ScriptNodeType.OLDER.name -> {
            // OLDER: Check if all coins have passed the relative timelock
            val timelockValue = node.timeLock?.value ?: 0L
            data.inputCoins.all { coin ->
                when (data.lockBased) {
                    MiniscriptTimelockBased.TIME_LOCK -> {
                        val currentTime = System.currentTimeMillis() / 1000L
                        (coin.time + timelockValue) <= currentTime
                    }
                    MiniscriptTimelockBased.HEIGHT_LOCK -> {
                        (coin.height + timelockValue) <= currentBlockHeight
                    }
                    else -> false
                }
            }
        }
        else -> false
    } else false

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (showThreadCurve) {
                CurveView()
            }

            Row(modifier = Modifier.padding(top = 10.dp)) {
                NcIcon(
                    painter = painterResource(R.drawable.ic_timer),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                ) {
                    Text(
                        text = "${node.idString}. $title",
                        style = NunchukTheme.typography.body
                    )

                    // Hide description for timestamp timelocks in SIGN mode
                    if (description.isNotEmpty()) {
                        Text(
                            text = description,
                            style = NunchukTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.textSecondary
                            )
                        )
                    }
                }

                // Show locked/unlocked status in SIGN mode
                if (mode == ScriptMode.SIGN) {
                    if (isUnlocked) {
                        CheckedLabel(text = "Unlocked")
                    } else {
                        Text(
                            modifier = Modifier
                                .align(Alignment.CenterVertically),
                            text = "Locked",
                            style = NunchukTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.textSecondary
                        )
                        NcIcon(
                            painter = painterResource(R.drawable.ic_lock),
                            contentDescription = "Locked",
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .size(24.dp)
                                .align(Alignment.CenterVertically),
                            tint = MaterialTheme.colorScheme.textSecondary
                        )
                    }
                }
            }

        }
        content()
    }
}

@Composable
fun HashlockItem(
    data: ScriptNodeData,
    modifier: Modifier = Modifier,
    showThreadCurve: Boolean = true,
    node: ScriptNode,
    content: @Composable () -> Unit = {},
) {
    val description = if (data.showBip32Path) node.descriptionText else ""
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (showThreadCurve) {
                CurveView()
            }
            NcIcon(
                painter = painterResource(R.drawable.ic_hash),
                contentDescription = null,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .size(20.dp)
            )
            Column(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .weight(1f)
                    .padding(start = 8.dp)
            ) {
                Text(
                    text = "${node.idString}. ${node.type.capitalize()}",
                    style = NunchukTheme.typography.body
                )
                if (description.isNotEmpty()) {
                    Text(
                        text = description,
                        style = NunchukTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.textSecondary
                        )
                    )
                }
            }

            if (data.mode == ScriptMode.SIGN) {
                if (data.signedHash[node.idString] == true) {
                    CheckedLabel(text = "Unlocked")
                } else {
                    NcPrimaryDarkButton(
                        height = 36.dp,
                        onClick = { data.onPreImageClick(node) },
                    ) {
                        Text(stringResource(R.string.nc_enter))
                    }
                }
            }
        }
        content()
    }
}

@Composable
fun KeyItem(
    modifier: Modifier = Modifier,
    title: String = "",
    xfp: String = "",
    position: String = "",
    data: ScriptNodeData,
    showThreadCurve: Boolean = true,
    avatarColor: Color = avatarColors[0],
    isOccupied: Boolean = false,
    bip32PathContent: @Composable () -> Unit = {},
    actionContent: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .padding(vertical = 10.dp)
            .fillMaxWidth()
    ) {
        if (showThreadCurve) {
            CurveView()
        }
        Row(
            modifier = Modifier
                .weight(1f),
        ) {
            if (data.isGroupWallet && xfp.isNotEmpty()) {
                NcCircleImage(
                    modifier = Modifier.padding(top = 8.dp),
                    iconSize = 16.dp,
                    resId = R.drawable.ic_user,
                    color = avatarColor,
                    iconTintColor = Color.White,
                    size = 20.dp
                )
            } else {
                NcIcon(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .size(20.dp),
                    painter = painterResource(R.drawable.ic_key),
                    contentDescription = null,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, top = if (xfp.isNotEmpty()) 8.dp else 0.dp)
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = if (position.isNotEmpty()) "$position. $title" else title,
                        style = NunchukTheme.typography.body,
                        modifier = Modifier
                    )
                    if (xfp.isNotEmpty()) {
                        Text(
                            text = xfp,
                            style = NunchukTheme.typography.bodySmall,
                        )
                    }
                    if (isOccupied) {
                        Text(
                            text = stringResource(id = R.string.nc_occupied),
                            style = NunchukTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.beeswaxDark
                        )
                    }
                    bip32PathContent.invoke()
                }
                actionContent()
            }
        }
    }
}

@Composable
fun TreeBranchContainer(
    modifier: Modifier = Modifier,
    drawLine: Boolean = true,
    itemHeight: Float = 0f,
    indentationLevel: Int = 0,
    content: @Composable (modifier: Modifier, showThreadCurve: Boolean) -> Unit
) {
    val indentationPadding = if (indentationLevel > 0) (indentationLevel * 10).dp else 0.dp
    val shouldDrawLine = drawLine && indentationLevel > 0
    val showThreadCurve = indentationLevel > 0

    Box(
        modifier = modifier
            .padding(start = indentationPadding)
            .drawBehind {
                val stroke = Stroke(width = 3.5f)
                val lineX = 2.5f
                val color = Color(0xFF757575)
                if (shouldDrawLine) {
                    drawLine(
                        color = color,
                        start = Offset(lineX, 0f),
                        end = Offset(lineX, size.height + 40 - itemHeight),
                        strokeWidth = stroke.width
                    )
                }
            }
    ) {
        content(Modifier, showThreadCurve)
    }
}

@Composable
fun CurveView(
    modifier: Modifier = Modifier
) {
    Column {
        VerticalDivider(
            modifier = modifier
                .height(10.dp)
                .width(2.dp)
                .padding(start = 0.5.dp),
            color = NcColor.boulder
        )

        Image(
            painter = painterResource(R.drawable.ic_thread_curve),
            contentDescription = null,
        )
    }
}


private fun String.capitalize(): String {
    return this.lowercase().replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }
}

@Preview
@Composable
fun ConditionTreeUIPreview(
    @PreviewParameter(ScriptNodeProvider::class) scriptNode: ScriptNode
) {
    NunchukTheme {
        Column(
            Modifier
                .background(Color.White)
                .padding(16.dp)
        ) {
            ScriptNodeTree(
                node = scriptNode,
                data = ScriptNodeData(
                    mode = ScriptMode.CONFIG,
                    signers = emptyMap(),
                    showBip32Path = true
                ),
                onChangeBip32Path = { _, _ -> },
                onActionKey = { _, _ -> }
            )
        }
    }
}

@Composable
fun RowScope.CheckedLabel(
    text: String = "",
) {
    Text(
        text = text,
        style = NunchukTheme.typography.captionTitle,
        modifier = Modifier.align(Alignment.CenterVertically)
    )

    NcIcon(
        modifier = Modifier
            .align(Alignment.CenterVertically)
            .padding(start = 4.dp),
        painter = painterResource(R.drawable.ic_check_circle_24),
        contentDescription = "Signed",
    )
}

@Composable
fun ActivePolicyAfterView(
    lockBased: MiniscriptTimelockBased,
    id: String,
    coinsGroup: Map<String, CoinsGroup>,
    isSatisfiableNode: Boolean,
    numberOfInputCoin: Int,
) {
    if (false) { // TODO open later
        coinsGroup[id]?.let { group ->
            val timestamp = group.start
            val formattedText = when {
                lockBased == MiniscriptTimelockBased.TIME_LOCK && timestamp > 0 -> {
                    "Active policy after ${Date(timestamp * 1000L).dateTimeFormat()}"
                }

                lockBased == MiniscriptTimelockBased.HEIGHT_LOCK && timestamp > 0 -> {
                    "Active policy after block $timestamp"
                }

                else -> ""
            }

            if (formattedText.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .background(
                            color = MaterialTheme.colorScheme.fillPink,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(4.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NcIcon(
                            painter = painterResource(R.drawable.ic_timer),
                            contentDescription = "Lock icon",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.textPrimary
                        )
                        Text(
                            text = formattedText,
                            style = NunchukTheme.typography.bodySmall,
                        )
                    }

                    if (group.coins.isNotEmpty()) {
                        val amount = group.coins.sumOf { it.amount.value }.toAmount()
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NcIcon(
                                painter = painterResource(R.drawable.ic_btc),
                                contentDescription = "Info icon",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.textPrimary
                            )
                            Text(
                                text = stringResource(
                                    R.string.nc_signing_for_coins,
                                    pluralStringResource(
                                        R.plurals.nc_coins_with_count,
                                        group.coins.size,
                                        group.coins.size
                                    ),
                                    amount.getBTCAmount()
                                ),
                                style = NunchukTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            } else if (group.coins.isNotEmpty() && group.coins.size < numberOfInputCoin) {
                val amount = group.coins.sumOf { it.amount.value }.toAmount()
                Row(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .background(
                            color = MaterialTheme.colorScheme.fillDenim,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(4.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NcIcon(
                        painter = painterResource(R.drawable.ic_btc),
                        contentDescription = "Info icon",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.textPrimary
                    )
                    Text(
                        text = stringResource(
                            R.string.nc_signing_for_coins,
                            pluralStringResource(
                                R.plurals.nc_coins_with_count,
                                group.coins.size,
                                group.coins.size
                            ),
                            amount.getBTCAmount()
                        ),
                        style = NunchukTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

private fun signingPathContainsNodeId(path: List<List<Int>>, nodeId: List<Int>): Boolean {
    return path.any { idList ->
        (nodeId.size <= idList.size && idList.take(nodeId.size) == nodeId) ||
                (idList.size <= nodeId.size && nodeId.take(idList.size) == idList)
    }
}