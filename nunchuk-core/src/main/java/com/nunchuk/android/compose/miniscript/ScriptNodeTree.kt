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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcBadgeOutline
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.core.R
import com.nunchuk.android.core.miniscript.ComponentInfo
import com.nunchuk.android.core.miniscript.MiniscriptDataComponent
import com.nunchuk.android.core.miniscript.ScripNoteType
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.ScriptNode
import com.nunchuk.android.model.SigningPath
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class ScriptMode {
    VIEW,
    CONFIG,
    SIGN
}

@Composable
internal fun CreateKeyItem(
    key: String,
    signer: SignerModel?,
    position: String,
    showThreadCurve: Boolean = true,
    onChangeBip32Path: (String, SignerModel) -> Unit,
    onActionKey: (String, SignerModel?) -> Unit,
    data: ScriptNodeData,
    modifier: Modifier = Modifier
) {
    KeyItem(
        title = signer?.name ?: key,
        xfp = signer?.getXfpOrCardIdLabel().orEmpty(),
        position = position,
        modifier = modifier,
        showThreadCurve = showThreadCurve,
        bip32PathContent = {
            if (data.showBip32Path && signer != null) {
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
                        color = if (data.duplicateSignerKeys.contains("${signer.fingerPrint}:${signer.derivationPath}")) {
                            Color.Red
                        } else {
                            MaterialTheme.colorScheme.textSecondary
                        }
                    )
                    if (data.mode == ScriptMode.CONFIG && signer.isMasterSigner) {
                        NcIcon(
                            modifier = Modifier.size(12.dp),
                            painter = painterResource(id = R.drawable.ic_edit_small),
                            contentDescription = "Edit icon"
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

                data.mode == ScriptMode.SIGN && signer != null && data.signedSigners[signer.fingerPrint] == true -> {
                    Text(
                        text = stringResource(R.string.nc_transaction_signed),
                        style = NunchukTheme.typography.captionTitle,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )

                    NcIcon(
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .padding(start = 8.dp),
                        painter = painterResource(R.drawable.ic_check_circle_24),
                        contentDescription = "Signed",
                    )
                }

                data.mode == ScriptMode.SIGN && signer != null -> {
                    NcPrimaryDarkButton(
                        height = 36.dp,
                        onClick = { onActionKey(key, signer) },
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
    index: String,
    onChangeBip32Path: (String, SignerModel) -> Unit,
    onActionKey: (String, SignerModel?) -> Unit,
    data: ScriptNodeData,
    level: Int,
    modifier: Modifier = Modifier
) {
    node.keys.forEachIndexed { i, key ->
        val keyPosition = "$index.${i + 1}"
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
                modifier = modifier
            )
        }
    }
}

@Composable
private fun NodeSubs(
    node: ScriptNode,
    index: String,
    onChangeBip32Path: (String, SignerModel) -> Unit,
    onActionKey: (String, SignerModel?) -> Unit,
    data: ScriptNodeData,
    level: Int
) {
    node.subs.forEachIndexed { i, sub ->
        ScriptNodeTree(
            node = sub,
            index = "$index.${node.keys.size + i + 1}",
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
    index: String,
    onChangeBip32Path: (String, SignerModel) -> Unit,
    onActionKey: (String, SignerModel?) -> Unit,
    data: ScriptNodeData,
    level: Int,
    modifier: Modifier = Modifier
) {
    NodeKeys(
        node = node,
        index = index,
        onChangeBip32Path = onChangeBip32Path,
        onActionKey = onActionKey,
        data = data,
        level = level,
        modifier = modifier
    )
    NodeSubs(
        node = node,
        index = index,
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
)

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

    val info = MiniscriptDataComponent.fromComponent(node.type)
    val nodeModifier = when {
        data.mode == ScriptMode.CONFIG -> Modifier
        data.mode == ScriptMode.SIGN && (isSatisfiableNode || node.type != ScripNoteType.AFTER.name || node.type != ScripNoteType.OLDER.name) -> Modifier
        data.mode == ScriptMode.VIEW && isNormalNode -> Modifier
        else -> Modifier.alpha(0.4f)
    }
    when (node.type) {
        ScripNoteType.ANDOR.name, ScripNoteType.AND.name, ScripNoteType.OR.name, ScripNoteType.OR_TAPROOT.name -> {
            TreeBranchContainer(
                modifier = nodeModifier,
                drawLine = isLastItem.not(),
                indentationLevel = level
            ) { modifier, showThreadCurve ->
                AndOrView(
                    scripNoteTypeInfo = info,
                    isShowCurve = showThreadCurve,
                    padStart = 0,
                    isShowTapscriptBadge = ScripNoteType.OR_TAPROOT.name == node.type,
                    index = index,
                    modifier = modifier
                ) {
                    NodeContent(
                        node = node,
                        index = index,
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

        ScripNoteType.AFTER.name, ScripNoteType.OLDER.name -> {
            TreeBranchContainer(
                modifier = nodeModifier,
                drawLine = isLastItem.not(),
                indentationLevel = level
            ) { modifier, showThreadCurve ->
                TimelockItem(
                    index = index,
                    k = node.k,
                    currentBlockHeight = currentBlockHeight,
                    nodeType = node.type,
                    showThreadCurve = showThreadCurve,
                    modifier = modifier
                ) {
                    NodeContent(
                        node = node,
                        index = index,
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

        ScripNoteType.PK.name -> {
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
                    modifier = modifier
                )
            }
            return
        }

        ScripNoteType.MULTI.name, ScripNoteType.THRESH.name -> {
            TreeBranchContainer(
                modifier = nodeModifier,
                drawLine = isLastItem.not(),
                indentationLevel = level
            ) { modifier, showThreadCurve ->
                ThreadMultiItem(
                    index = index,
                    type = node.type,
                    topPadding = if (showThreadCurve) 10 else 0,
                    showThreadCurve = showThreadCurve,
                    modifier = modifier
                ) {
                    NodeContent(
                        node = node,
                        index = index,
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

        ScripNoteType.HASH160.name, ScripNoteType.HASH256.name, ScripNoteType.RIPEMD160.name, ScripNoteType.SHA256.name -> {
            TreeBranchContainer(
                modifier = nodeModifier,
                drawLine = isLastItem.not(),
                indentationLevel = level
            ) { modifier, showThreadCurve ->
                HashlockItem(
                    index = index,
                    hashType = node.type,
                    showThreadCurve = showThreadCurve,
                    modifier = modifier
                ) {
                    NodeContent(
                        node = node,
                        index = index,
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
        index = index,
        onChangeBip32Path = onChangeBip32Path,
        onActionKey = onActionKey,
        data = data,
        level = level
    )
}

@Composable
fun AndOrView(
    modifier: Modifier = Modifier,
    scripNoteTypeInfo: ComponentInfo = MiniscriptDataComponent.fromComponent(ScripNoteType.ANDOR.name),
    isShowCurve: Boolean = true,
    isShowTapscriptBadge: Boolean = false,
    index: String = "",
    padStart: Int = 0,
    content: @Composable () -> Unit = {},
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            if (isShowCurve) {
                Image(
                    painter = painterResource(R.drawable.ic_thread_curve),
                    contentDescription = null,
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = padStart.dp)
            ) {
                Row {
                    Text(
                        text = if (index.isNotEmpty()) "$index. ${scripNoteTypeInfo.name}" else scripNoteTypeInfo.name,
                        style = NunchukTheme.typography.body
                    )
                    if (isShowTapscriptBadge) {
                        NcBadgeOutline(
                            modifier = Modifier.padding(start = 8.dp),
                            text = "Tapscript"
                        )
                    }
                }

                Text(
                    scripNoteTypeInfo.description,
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
fun ThreadMultiItem(
    modifier: Modifier = Modifier,
    index: String,
    type: String,
    topPadding: Int = 10,
    showThreadCurve: Boolean = true,
    content: @Composable () -> Unit = {},
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topPadding.dp, bottom = 4.dp)
        ) {
            if (showThreadCurve) {
                Image(
                    painter = painterResource(R.drawable.ic_thread_curve),
                    contentDescription = null,
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = if (showThreadCurve) 8.dp else 0.dp)
            ) {
                val text = when (type) {
                    ScripNoteType.THRESH.name -> "Thresh"
                    ScripNoteType.MULTI.name -> "Multisig"
                    else -> ""
                }
                Text(
                    text = "$index. $text",
                    style = NunchukTheme.typography.body
                )
                val keyText = when (type) {
                    ScripNoteType.THRESH.name -> "Requires M of N sub‑conditions."
                    ScripNoteType.MULTI.name -> "Requires M of N keys."
                    else -> ""
                }
                Text(
                    text = keyText,
                    style = NunchukTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.textSecondary
                    )
                )
            }
        }
        content()
    }
}

@Composable
fun TimelockItem(
    modifier: Modifier = Modifier,
    index: String,
    k: Int,
    currentBlockHeight: Int = 0,
    nodeType: String,
    showThreadCurve: Boolean = true,
    content: @Composable () -> Unit = {},
) {
    val (title, description) = when (nodeType) {
        ScripNoteType.OLDER.name -> {
            if (k > 86400) { // k is timestamp in seconds
                val days = k / 86400
                Pair("Older $days days", "From the time the coins are received.")
            } else { // k is block height
                val formattedBlocks = String.format("%,d", k)
                Pair("Older $formattedBlocks blocks", "From the time the coins are received.")
            }
        }

        ScripNoteType.AFTER.name -> {
            val currentTimeSeconds = System.currentTimeMillis() / 1000
            val diff = k - currentTimeSeconds
            if (diff > 0) { // k is timestamp in seconds
                val targetDate = calculateDateFromSeconds(k.toLong())
                val daysFromNow = ((diff / 86400).toInt())
                val dayText = if (daysFromNow > 1) "days" else "day"
                Pair("After $targetDate", "$daysFromNow $dayText from today.")
            } else { // k is block height
                val formattedBlocks = String.format("%,d", k)
                val blockDiff = k - currentBlockHeight
                val formattedBlockDiff = String.format("%,d", blockDiff)
                Pair(
                    "After block $formattedBlocks",
                    "$formattedBlockDiff blocks from the current block."
                )
            }
        }

        else -> Pair("Unknown timelock", "")
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = 10.dp,
                )
        ) {
            if (showThreadCurve) {
                Image(
                    painter = painterResource(R.drawable.ic_thread_curve),
                    contentDescription = null,
                )
            }
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
                    text = "$index. $title",
                    style = NunchukTheme.typography.body
                )
                Text(
                    text = description,
                    style = NunchukTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.textSecondary
                    )
                )
            }
        }
        content()
    }
}

@Composable
fun HashlockItem(
    modifier: Modifier = Modifier,
    index: String,
    hashType: String,
    showThreadCurve: Boolean = true,
    content: @Composable () -> Unit = {},
) {
    val description = when (hashType) {
        ScripNoteType.HASH160.name -> "Requires a preimage that hashes to a given value with HASH160"
        ScripNoteType.HASH256.name -> "Requires a preimage that hashes to a given value with SHA256"
        ScripNoteType.RIPEMD160.name -> "Requires a preimage that hashes to a given value with RIPEMD160"
        ScripNoteType.SHA256.name -> "Requires a preimage that hashes to a given value with SHA256"
        else -> "Requires a preimage that hashes to a given value"
    }
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .padding(
                    top = 10.dp,
                )
                .fillMaxWidth()
        ) {
            if (showThreadCurve) {
                Image(
                    painter = painterResource(R.drawable.ic_thread_curve),
                    contentDescription = null,
                )
            }
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
                    text = "$index. ${hashType.capitalize()}",
                    style = NunchukTheme.typography.body
                )
                Text(
                    text = description,
                    style = NunchukTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.textSecondary
                    )
                )
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
            Image(
                painter = painterResource(R.drawable.ic_thread_curve),
                contentDescription = null,
            )
        }
        NcIcon(
            modifier = Modifier.size(20.dp),
            painter = painterResource(R.drawable.ic_key),
            contentDescription = null,
        )
        Column(
            modifier = Modifier
                .padding(start = 8.dp)
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
                val lineX = 2f
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

private fun calculateDateFromSeconds(timestampSeconds: Long): String {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timestampSeconds * 1000
    val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)
    return dateFormat.format(calendar.time)
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
        id = emptyList(),
        data = byteArrayOf(),
        type = ScripNoteType.ANDOR.name,
        keys = listOf(),
        k = 0,
        subs = listOf(
            ScriptNode(
                type = ScripNoteType.AFTER.name,
                keys = listOf(),
                k = 7776000, // 90 days in seconds
                subs = emptyList(),
                id = emptyList(),
                data = byteArrayOf()
            ),
            ScriptNode(
                type = ScripNoteType.THRESH.name,
                keys = listOf("key_3", "key_4", "key_5"),
                k = 3,
                subs = emptyList(),
                id = emptyList(),
                data = byteArrayOf()
            ),
            ScriptNode(
                type = ScripNoteType.THRESH.name,
                keys = listOf("key_0", "key_1", "key_2"),
                k = 2,
                subs = emptyList(),
                id = emptyList(),
                data = byteArrayOf()
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

private fun signingPathContainsNodeId(path: List<List<Int>>, nodeId: List<Int>): Boolean {
    return path.any { idList ->
        (nodeId.size <= idList.size && idList.take(nodeId.size) == nodeId) ||
                (idList.size <= nodeId.size && nodeId.take(idList.size) == idList)
    }
}