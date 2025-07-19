package com.nunchuk.android.compose.miniscript

import com.nunchuk.android.core.miniscript.ScriptNoteType
import com.nunchuk.android.model.ScriptNode

val ScriptNode.info: String
    get() = when (type) {
        ScriptNoteType.THRESH.name -> "${k}/${subs.size}"
        ScriptNoteType.MULTI.name -> "${k}/${keys.size}"
        else -> ""
    }