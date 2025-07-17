package com.nunchuk.android.compose.miniscript

import com.nunchuk.android.core.miniscript.ScripNoteType
import com.nunchuk.android.model.ScriptNode

val ScriptNode.info: String
    get() = when (type) {
        ScripNoteType.THRESH.name -> "${k}/${subs.size}"
        ScripNoteType.MULTI.name -> "${k}/${keys.size}"
        else -> ""
    }