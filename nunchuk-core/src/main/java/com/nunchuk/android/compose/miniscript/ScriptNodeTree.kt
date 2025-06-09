package com.nunchuk.android.compose.miniscript

import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class ScriptMode {
    VIEW,
    CONFIG,
    SIGN
}

@Composable
private fun CreateKeyItem(
    key: String,
    signer: SignerModel?,
    position: String,
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
                        color = MaterialTheme.colorScheme.textSecondary
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
                    NcPrimaryDarkButton(
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
                    )

                    NcIcon(
                        modifier = Modifier.padding(start = 8.dp),
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
    modifier: Modifier = Modifier
) {
    node.keys.forEachIndexed { i, key ->
        val keyPosition = "$index.${i + 1}"
        TreeBranchContainer(
            modifier = modifier.padding(start = 20.dp),
            drawLine = i != node.keys.size - 1 || node.subs.isNotEmpty(),
        ) {
            val signer = data.signers[key]
            CreateKeyItem(
                key = key,
                signer = signer,
                position = keyPosition,
                onChangeBip32Path = onChangeBip32Path,
                onActionKey = onActionKey,
                data = data
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
    val signedSigners: Map<String, Boolean> = emptyMap()
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
    val info = MiniscriptDataComponent.fromComponent(node.type)
    when (node.type) {
        ScripNoteType.ANDOR.name, ScripNoteType.AND.name, ScripNoteType.OR.name -> {
            if (level == 0) {
                AndOrView(
                    scripNoteTypeInfo = info,
                    isShowCurve = false,
                    padStart = 0,
                    index = index
                ) {
                    NodeContent(
                        node = node,
                        index = index,
                        onChangeBip32Path = onChangeBip32Path,
                        onActionKey = onActionKey,
                        data = data,
                        level = level
                    )
                }
            } else {
                AndOrView(
                    scripNoteTypeInfo = info,
                    index = index
                ) {
                    NodeContent(
                        node = node,
                        index = index,
                        onChangeBip32Path = onChangeBip32Path,
                        onActionKey = onActionKey,
                        data = data,
                        level = level
                    )
                }
            }
            return
        }

        ScripNoteType.AFTER.name, ScripNoteType.OLDER.name -> {
            TreeBranchContainer(
                drawLine = isLastItem.not()
            ) { modifier ->
                TimelockItem(
                    index = index,
                    k = node.k,
                    currentBlockHeight = currentBlockHeight,
                    nodeType = node.type,
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

        ScripNoteType.MULTI.name, ScripNoteType.THRESH.name -> {
            TreeBranchContainer(
                drawLine = isLastItem.not()
            ) { modifier ->
                ThreadItem(
                    index = index,
                    threshold = node.k,
                    totalKeys = node.keys.size + node.subs.size
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
                drawLine = isLastItem.not()
            ) { modifier ->
                HashlockItem(
                    index = index,
                    hashType = node.type
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
    scripNoteTypeInfo: ComponentInfo = MiniscriptDataComponent.fromComponent(ScripNoteType.ANDOR.name),
    isShowCurve: Boolean = true,
    index: String = "",
    padStart: Int = 0,
    content: @Composable () -> Unit = {},
) {
    Column {
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
                Text(
                    text = if (index.isNotEmpty()) "$index. ${scripNoteTypeInfo.name}" else scripNoteTypeInfo.name,
                    style = NunchukTheme.typography.body
                )
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
fun ThreadItem(
    index: String,
    threshold: Int,
    totalKeys: Int,
    topPadding: Int = 10,
    content: @Composable () -> Unit = {},
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topPadding.dp, bottom = 4.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_thread_curve),
                contentDescription = null,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            ) {
                Text(
                    text = "$index. Thresh $threshold/$totalKeys",
                    style = NunchukTheme.typography.body
                )
                Text(
                    text = "Requires $threshold of $totalKeys sub-conditions.",
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
    index: String,
    k: Int,
    currentBlockHeight: Int = 0,
    nodeType: String,
    content: @Composable () -> Unit = {},
) {
    val currentTimeSeconds = System.currentTimeMillis() / 1000

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
            val diff = k - currentTimeSeconds
            if (diff > 0) { // k is timestamp in seconds
                val targetDate = calculateDateFromSeconds(k.toLong())
                val daysFromNow = ((diff / 86400).toInt())
                Pair("After $targetDate", "$daysFromNow days from today.")
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

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = 10.dp,
                )
        ) {
            Image(
                painter = painterResource(R.drawable.ic_thread_curve),
                contentDescription = null,
            )
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
    index: String,
    hashType: String,
    content: @Composable () -> Unit = {},
) {
    val description = when (hashType) {
        ScripNoteType.HASH160.name -> "Requires a preimage that hashes to a given value with HASH160"
        ScripNoteType.HASH256.name -> "Requires a preimage that hashes to a given value with SHA256"
        ScripNoteType.RIPEMD160.name -> "Requires a preimage that hashes to a given value with RIPEMD160"
        ScripNoteType.SHA256.name -> "Requires a preimage that hashes to a given value with SHA256"
        else -> "Requires a preimage that hashes to a given value"
    }
    Column {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = painterResource(R.drawable.ic_thread_curve),
                contentDescription = null,
            )
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
    bip32PathContent: @Composable () -> Unit = {},
    actionContent: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .padding(bottom = 10.dp, top = 10.dp)
            .fillMaxWidth()
    ) {
        Image(
            painter = painterResource(R.drawable.ic_thread_curve),
            contentDescription = null,
        )
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
            Text(
                text = "XFP: $xfp",
                style = NunchukTheme.typography.bodySmall,
            )
            bip32PathContent?.invoke()
        }
        actionContent()
    }
}

@Composable
fun TreeBranchContainer(
    modifier: Modifier = Modifier,
    drawLine: Boolean = true,
    itemHeight: Float = 0f,
    content: @Composable (modifier: Modifier) -> Unit
) {
    Box(
        modifier = modifier
            .drawBehind {
                val stroke = Stroke(width = 3.5f)
                val lineX = 2f
                val color = Color(0xFF757575)
                if (drawLine) {
                    drawLine(
                        color = color,
                        start = Offset(lineX, 0f),
                        end = Offset(lineX, size.height + 40 - itemHeight),
                        strokeWidth = stroke.width
                    )
                }
            }
    ) {
        content(modifier)
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
        type = ScripNoteType.ANDOR.name,
        keys = listOf(),
        k = 0,
        subs = listOf(
            ScriptNode(
                type = ScripNoteType.AFTER.name,
                keys = listOf(),
                k = 7776000, // 90 days in seconds
                subs = emptyList()
            ),
            ScriptNode(
                type = ScripNoteType.THRESH.name,
                keys = listOf("key_3", "key_4", "key_5"),
                k = 3,
                subs = emptyList()
            ),
            ScriptNode(
                type = ScripNoteType.THRESH.name,
                keys = listOf("key_0", "key_1", "key_2"),
                k = 2,
                subs = emptyList()
            )
        )
    )
    NunchukTheme {
        Column(Modifier.padding(16.dp)) {
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
fun SigningStatusCard(
    modifier: Modifier = Modifier,
    coins: Int = 3,
    amount: Double = 0.00424422,
    policyStatus: PolicyStatus = PolicyStatus.INACTIVE,
    startDate: String? = null,
    endDate: String? = null,
) {
    val backgroundColor = when (policyStatus) {
        PolicyStatus.ACTIVE -> MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)
        PolicyStatus.ACTIVE_WITH_DATE -> Color(0xFFFFF5E6)
        PolicyStatus.INACTIVE -> Color(0xFFF5F5F5)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .drawBehind {
                drawRoundRect(
                    color = backgroundColor,
                    cornerRadius = CornerRadius(8.dp.toPx())
                )
            }
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NcIcon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(id = R.drawable.ic_btc),
                contentDescription = "Bitcoin icon"
            )
            Text(
                text = "Signing for $coins coins ($amount BTC)",
                style = NunchukTheme.typography.bodySmall
            )
        }

        if (policyStatus != PolicyStatus.INACTIVE) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NcIcon(
                    modifier = Modifier.size(16.dp),
                    painter = painterResource(id = R.drawable.ic_timer),
                    contentDescription = "Timer icon"
                )
                Text(
                    text = when (policyStatus) {
                        PolicyStatus.ACTIVE -> "Active policy after $startDate"
                        PolicyStatus.ACTIVE_WITH_DATE -> "Active policy from $startDate until $endDate"
                        else -> ""
                    },
                    style = NunchukTheme.typography.bodySmall
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NcIcon(
                    modifier = Modifier.size(16.dp),
                    painter = painterResource(id = R.drawable.ic_close),
                    contentDescription = "Close icon"
                )
                Text(
                    text = "Inactive policy",
                    style = NunchukTheme.typography.bodySmall
                )
            }
        }
    }
}

enum class PolicyStatus {
    ACTIVE,
    ACTIVE_WITH_DATE,
    INACTIVE
}

@Preview
@Composable
fun SigningStatusCardPreview() {
    NunchukTheme {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SigningStatusCard(policyStatus = PolicyStatus.ACTIVE, startDate = "05/29/2025")
            SigningStatusCard(
                policyStatus = PolicyStatus.ACTIVE_WITH_DATE,
                startDate = "05/29/2025",
                endDate = "06/07/2025"
            )
            SigningStatusCard(policyStatus = PolicyStatus.INACTIVE)
        }
    }
}