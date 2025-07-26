package com.nunchuk.android.compose.provider

import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.nunchuk.android.core.miniscript.ScriptNodeType
import com.nunchuk.android.model.ScriptNode

class ScriptNodeProvider : CollectionPreviewParameterProvider<ScriptNode>(
    listOf(
        // Complex ANDOR node with multiple levels
        ScriptNode(
            id = listOf(1),
            data = byteArrayOf(),
            type = ScriptNodeType.ANDOR.name,
            keys = listOf(),
            k = 0,
            timeLock = null,
            subs = listOf(
                ScriptNode(
                    type = ScriptNodeType.AFTER.name,
                    keys = listOf(),
                    k = 7776000, // 90 days in seconds
                    subs = emptyList(),
                    id = listOf(1, 1),
                    data = byteArrayOf(),
                    timeLock = null
                ),
                ScriptNode(
                    type = ScriptNodeType.THRESH.name,
                    keys = listOf("key_3", "key_4", "key_5"),
                    k = 3,
                    subs = emptyList(),
                    id = listOf(1, 2),
                    data = byteArrayOf(),
                    timeLock = null
                ),
                ScriptNode(
                    type = ScriptNodeType.THRESH.name,
                    keys = listOf("key_0", "key_1", "key_2"),
                    k = 2,
                    subs = emptyList(),
                    id = listOf(1, 3),
                    data = byteArrayOf(),
                    timeLock = null
                )
            )
        )
    )
) 