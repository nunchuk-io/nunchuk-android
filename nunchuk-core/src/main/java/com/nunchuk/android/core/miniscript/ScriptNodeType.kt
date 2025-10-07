package com.nunchuk.android.core.miniscript

import androidx.annotation.Keep
import com.nunchuk.android.model.ScriptNode

@Keep
enum class ScriptNodeType {
    NONE,
    PK,
    OLDER,
    AFTER,
    HASH160,
    HASH256,
    RIPEMD160,
    SHA256,
    AND,
    OR,
    ANDOR,
    THRESH,
    MULTI,
    OR_TAPROOT,
    MUSIG
}

val ScriptNode.isPreImageNode: Boolean
    get() = type == ScriptNodeType.HASH160.name || type == ScriptNodeType.HASH256.name ||
            type == ScriptNodeType.RIPEMD160.name || type == ScriptNodeType.SHA256.name