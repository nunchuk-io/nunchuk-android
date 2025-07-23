package com.nunchuk.android.compose.miniscript

import com.nunchuk.android.core.miniscript.ScriptNoteType
import com.nunchuk.android.model.ScriptNode

val ScriptNode.displayName: String
    get() = when (this.type) {
        ScriptNoteType.PK.name -> "PK"
        ScriptNoteType.OLDER.name -> "Older"
        ScriptNoteType.AFTER.name -> "After"
        ScriptNoteType.HASH160.name -> "HASH160"
        ScriptNoteType.HASH256.name -> "HASH256"
        ScriptNoteType.RIPEMD160.name -> "RIPEMD160"
        ScriptNoteType.SHA256.name -> "SHA256"
        ScriptNoteType.AND.name -> "AND"
        ScriptNoteType.OR.name -> "OR"
        ScriptNoteType.ANDOR.name -> "AND OR"
        ScriptNoteType.THRESH.name -> "Thresh ${this.k}/${this.subs.size}"
        ScriptNoteType.MULTI.name -> "Multisig ${this.k}/${this.keys.size}"
        ScriptNoteType.OR_TAPROOT.name -> "OR"
        else -> "Unknown"
    }

val ScriptNode.descriptionText: String
    get() = when (this.type) {
        ScriptNoteType.PK.name -> "Public key"
        ScriptNoteType.OLDER.name -> "Older"
        ScriptNoteType.AFTER.name -> "After"
        ScriptNoteType.HASH160.name -> "Requires HASH160 preimage."
        ScriptNoteType.HASH256.name -> "Requires SHA256 preimage."
        ScriptNoteType.RIPEMD160.name -> "Requires RIPEMD160 preimage."
        ScriptNoteType.SHA256.name -> "Requires SHA256 preimage."
        ScriptNoteType.AND.name -> "Both sub-conditions must be satisfied."
        ScriptNoteType.OR.name -> "Only one sub-condition needs to be satisfied."
        ScriptNoteType.ANDOR.name -> "If the first sub-condition is true, the second must also be satisfied. If the first is false, the third must be satisfied."
        ScriptNoteType.THRESH.name -> "Requires M of N sub‑conditions."
        ScriptNoteType.MULTI.name -> "Requires M of N keys."
        ScriptNoteType.OR_TAPROOT.name -> "Only one tapscript needs to be satisfied."
        else -> ""
    } 