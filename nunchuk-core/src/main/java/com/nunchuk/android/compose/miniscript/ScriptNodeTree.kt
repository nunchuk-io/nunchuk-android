package com.nunchuk.android.compose.miniscript

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcBadgeOutline
import com.nunchuk.android.compose.NcColor
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.core.R
import com.nunchuk.android.core.miniscript.ScriptNoteType
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.ScriptNode
import com.nunchuk.android.model.SigningPath
import com.nunchuk.android.model.TimeLock
import java.util.Locale

enum class ScriptMode {
    VIEW,
    CONFIG,
    SIGN
}

@Composable
fun ScriptNodeTree(
    node: ScriptNode,
    index: String = "1",
    isLastItem: Boolean = false,
    level: Int = 0,
    currentBlockHeight: Int = 0,
    onChangeBip32Path: (String, SignerModel) -> Unit = { _, _ -> },
    onActionKey: (String, SignerModel?) -> Unit = { _, _ -> },
    data: ScriptNodeData = ScriptNodeData()
) {
    val isNormalNode =
        data.signingPath.path.isEmpty() || signingPathContainsNodeId(data.signingPath.path, node.id)
    val isSatisfiableNode = data.satisfiableMap[node.idString] == true

    val nodeModifier = when {
        data.mode == ScriptMode.CONFIG || node.type == ScriptNoteType.AFTER.name || node.type == ScriptNoteType.OLDER.name -> Modifier
        data.mode == ScriptMode.SIGN && isSatisfiableNode -> Modifier
        data.mode == ScriptMode.VIEW && isNormalNode -> Modifier
        else -> Modifier.alpha(0.4f)
    }
    when (node.type) {
        ScriptNoteType.ANDOR.name, ScriptNoteType.AND.name, ScriptNoteType.OR.name, ScriptNoteType.OR_TAPROOT.name -> {
            TreeBranchContainer(
                modifier = nodeModifier,
                drawLine = isLastItem.not(),
                indentationLevel = level
            ) { modifier, showThreadCurve ->
                AndOrView(
                    isShowCurve = showThreadCurve,
                    isShowTapscriptBadge = ScriptNoteType.OR_TAPROOT.name == node.type,
                    padStart = 0,
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
            return
        }

        ScriptNoteType.AFTER.name, ScriptNoteType.OLDER.name -> {
            TreeBranchContainer(
                modifier = nodeModifier,
                drawLine = isLastItem.not(),
                indentationLevel = level,
            ) { modifier, showThreadCurve ->
                TimelockItem(
                    modifier = modifier,
                    currentBlockHeight = currentBlockHeight,
                    showThreadCurve = showThreadCurve,
                    isSatisfiableNode = isSatisfiableNode,
                    mode = data.mode,
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
            return
        }

        ScriptNoteType.PK.name -> {
            TreeBranchContainer(
                modifier = nodeModifier,
                drawLine = isLastItem.not(),
                indentationLevel = level
            ) { modifier, showThreadCurve ->
                CreateKeyItem(
                    key = node.keys.firstOrNull() ?: "",
                    signer = data.signers[node.keys.firstOrNull().orEmpty()],
                    position = index,
                    onChangeBip32Path = onChangeBip32Path,
                    onActionKey = onActionKey,
                    data = data,
                    showThreadCurve = showThreadCurve,
                    modifier = modifier,
                    isSatisfiable = isSatisfiableNode
                )
            }
            return
        }

        ScriptNoteType.MULTI.name, ScriptNoteType.THRESH.name -> {
            TreeBranchContainer(
                modifier = nodeModifier,
                drawLine = isLastItem.not(),
                indentationLevel = level
            ) { modifier, showThreadCurve ->
                ThreshMultiItem(
                    topPadding = if (showThreadCurve) 10 else 0,
                    showThreadCurve = showThreadCurve,
                    modifier = modifier,
                    isSatisfiable = isSatisfiableNode,
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
            return
        }

        ScriptNoteType.HASH160.name, ScriptNoteType.HASH256.name, ScriptNoteType.RIPEMD160.name, ScriptNoteType.SHA256.name -> {
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
            return
        }
    }
    NodeSubs(
        node = node,
        onChangeBip32Path = onChangeBip32Path,
        onActionKey = onActionKey,
        data = data,
        level = level
    )
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
    data: ScriptNodeData
) {
    KeyItem(
        title = signer?.name ?: key,
        xfp = signer?.getXfpOrCardIdLabel().orEmpty(),
        position = position,
        modifier = modifier,
        showThreadCurve = showThreadCurve,
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

                data.mode == ScriptMode.SIGN && signer != null && isSatisfiable && data.signedSigners[signer.fingerPrint] == true -> {
                    CheckedLabel(
                        text = stringResource(R.string.nc_transaction_signed),
                    )
                }

                data.mode == ScriptMode.SIGN && signer != null -> {
                    NcPrimaryDarkButton(
                        height = 36.dp,
                        onClick = { onActionKey(key, signer) },
                        enabled = isSatisfiable
                    ) {
                        Text(stringResource(R.string.nc_sign))
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
    node.keys.forEachIndexed { i, key ->
        val keyPosition = "${node.idString}.${i + 1}"
        TreeBranchContainer(
            modifier = modifier,
            drawLine = i != node.keys.size - 1 || node.subs.isNotEmpty(),
            indentationLevel = level + 1 // Keys are one level deeper than their parent node
        ) { modifier, showThreadCurve ->
            val signer = data.signers[key]
            CreateKeyItem(
                key = key,
                signer = signer,
                position = keyPosition,
                onChangeBip32Path = onChangeBip32Path,
                onActionKey = onActionKey,
                data = data,
                showThreadCurve = showThreadCurve,
                modifier = modifier,
                isSatisfiable = data.satisfiableMap[node.idString] == true
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
    node.subs.forEachIndexed { i, sub ->
        ScriptNodeTree(
            node = sub,
            index = "${node.idString}.${node.keys.size + i + 1}",
            isLastItem = i == node.subs.size - 1,
            level = level + 1,
            onChangeBip32Path = onChangeBip32Path,
            onActionKey = onActionKey,
            data = data
        )
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
    val topLevelDisableNode: ScriptNode? = null,
    val onPreImageClick: (ScriptNode) -> Unit = {},
)

@Composable
fun AndOrView(
    modifier: Modifier = Modifier,
    isShowCurve: Boolean = true,
    isShowTapscriptBadge: Boolean = false,
    padStart: Int = 0,
    node: ScriptNode,
    content: @Composable () -> Unit = {},
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

                Text(
                    text = node.descriptionText,
                    style = NunchukTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.textSecondary
                    ),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
        content()
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
    content: @Composable () -> Unit = {},
) {
    // Only calculate signed signatures when in SIGN mode
    var showDetail by remember(node.id) {
        mutableStateOf(data.topLevelDisableNode?.id != node.id)
    }
    val pendingSigners = if (data.mode == ScriptMode.SIGN && isSatisfiable) {
        val signedCount = when (node.type) {
            ScriptNoteType.THRESH.name -> {
                node.subs.count { sub ->
                    val firstKey = sub.keys.firstOrNull()
                    if (firstKey != null) {
                        val signer = data.signers[firstKey]
                        val xfp = signer?.fingerPrint
                        xfp != null && data.signedSigners[xfp] == true
                    } else false
                }
            }

            ScriptNoteType.MULTI.name -> {
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

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
        ) {
            if (showThreadCurve) {
                CurveView()
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = topPadding.dp)
            ) {
                Text(
                    text = "${node.idString}. ${node.displayName}",
                    style = NunchukTheme.typography.body
                )
                Text(
                    text = node.descriptionText,
                    style = NunchukTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.textSecondary
                    )
                )
            }

            // Show pending conditions or enough conditions collected
            if (data.mode == ScriptMode.SIGN && isSatisfiable) {
                Row(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .padding(top = topPadding.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (pendingSigners > 0) {
                        NcIcon(
                            painter = painterResource(id = R.drawable.ic_pending_signatures),
                            contentDescription = "Warning",
                        )
                        Text(
                            text = pluralStringResource(
                                R.plurals.nc_transaction_pending_conditions,
                                pendingSigners,
                                pendingSigners
                            ),
                            style = NunchukTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    } else {
                        NcIcon(
                            painter = painterResource(id = R.drawable.ic_check_circle),
                            contentDescription = "Check",
                        )
                        Text(
                            text = stringResource(R.string.nc_transaction_enough_conditions),
                            style = NunchukTheme.typography.bodySmall,
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
                        text = if (showDetail) stringResource(R.string.nc_view_less) else stringResource(
                            R.string.nc_view_all
                        ),
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
    isSatisfiableNode: Boolean,
    mode: ScriptMode,
    node: ScriptNode,
    content: @Composable () -> Unit = {},
) {
    val title = node.getTimelockDisplayName()
    val description = node.getTimelockDescription(currentBlockHeight)

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
                    if (mode != ScriptMode.SIGN || node.timeLock?.isTimestamp() != true) {
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
                    if (isSatisfiableNode) {
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
    val description = node.getHashlockDescription(data.showBip32Path)
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            if (showThreadCurve) {
                CurveView()
            }
            Row(modifier = Modifier.padding(top = 10.dp)) {
                NcIcon(
                    painter = painterResource(R.drawable.ic_hash),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Column(
                    modifier = Modifier
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
                    if (data.satisfiableMap[node.idString] == true) {
                        CheckedLabel(text = "Unlocked")
                    } else {
                        NcPrimaryDarkButton(
                            height = 36.dp,
                            onClick = { data.onPreImageClick(node) },
                        ) {
                            Text(stringResource(R.string.nc_sign))
                        }
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
    showThreadCurve: Boolean = true,
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
            NcIcon(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .size(20.dp),
                painter = painterResource(R.drawable.ic_key),
                contentDescription = null,
            )
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

@Preview
@Composable
fun CurveView() {
    Column {
        VerticalDivider(
            modifier = Modifier
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
fun ConditionTreeUIPreview() {
    val sampleScriptNode = ScriptNode(
        id = listOf(1),
        data = byteArrayOf(),
        type = ScriptNoteType.ANDOR.name,
        keys = listOf(),
        k = 0,
        timeLock = null,
        subs = listOf(
            ScriptNode(
                type = ScriptNoteType.AFTER.name,
                keys = listOf(),
                k = 7776000, // 90 days in seconds
                subs = emptyList(),
                id = listOf(1, 1),
                data = byteArrayOf(),
                timeLock = null
            ),
            ScriptNode(
                type = ScriptNoteType.THRESH.name,
                keys = listOf("key_3", "key_4", "key_5"),
                k = 3,
                subs = emptyList(),
                id = listOf(1, 2),
                data = byteArrayOf(),
                timeLock = null
            ),
            ScriptNode(
                type = ScriptNoteType.THRESH.name,
                keys = listOf("key_0", "key_1", "key_2"),
                k = 2,
                subs = emptyList(),
                id = listOf(1, 3),
                data = byteArrayOf(),
                timeLock = null
            )
        )
    )
    NunchukTheme {
        Column(
            Modifier
                .background(Color.White)
                .padding(16.dp)
        ) {
            ScriptNodeTree(
                node = sampleScriptNode,
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

private fun signingPathContainsNodeId(path: List<List<Int>>, nodeId: List<Int>): Boolean {
    return path.any { idList ->
        (nodeId.size <= idList.size && idList.take(nodeId.size) == nodeId) ||
                (idList.size <= nodeId.size && nodeId.take(idList.size) == idList)
    }
}