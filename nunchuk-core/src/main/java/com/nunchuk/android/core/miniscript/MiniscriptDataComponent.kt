package com.nunchuk.android.core.miniscript

import androidx.annotation.Keep
import com.nunchuk.android.core.R
import com.nunchuk.android.core.miniscript.ComponentInfo.AFTER
import com.nunchuk.android.core.miniscript.ComponentInfo.AND
import com.nunchuk.android.core.miniscript.ComponentInfo.ANDOR
import com.nunchuk.android.core.miniscript.ComponentInfo.HASH160
import com.nunchuk.android.core.miniscript.ComponentInfo.HASH256
import com.nunchuk.android.core.miniscript.ComponentInfo.MULTI
import com.nunchuk.android.core.miniscript.ComponentInfo.None
import com.nunchuk.android.core.miniscript.ComponentInfo.OLDER
import com.nunchuk.android.core.miniscript.ComponentInfo.OR
import com.nunchuk.android.core.miniscript.ComponentInfo.OR_TAPROOT
import com.nunchuk.android.core.miniscript.ComponentInfo.PK
import com.nunchuk.android.core.miniscript.ComponentInfo.RIPEMD160
import com.nunchuk.android.core.miniscript.ComponentInfo.SHA256
import com.nunchuk.android.core.miniscript.ComponentInfo.THRESH

object MiniscriptDataComponent {

    fun getComponent(name: String): ScriptNoteType {
        return when (name) {
            "NONE" -> ScriptNoteType.NONE
            "PK" -> ScriptNoteType.PK
            "OLDER" -> ScriptNoteType.OLDER
            "AFTER" -> ScriptNoteType.AFTER
            "HASH160" -> ScriptNoteType.HASH160
            "HASH256" -> ScriptNoteType.HASH256
            "RIPEMD160" -> ScriptNoteType.RIPEMD160
            "SHA256" -> ScriptNoteType.SHA256
            "AND" -> ScriptNoteType.AND
            "OR" -> ScriptNoteType.OR
            "ANDOR" -> ScriptNoteType.ANDOR
            "THRESH" -> ScriptNoteType.THRESH
            "MULTI" -> ScriptNoteType.MULTI
            "OR_TAPROOT" -> ScriptNoteType.OR_TAPROOT
            else -> throw IllegalArgumentException("Unknown component: $name")
        }
    }

    fun fromComponent(name: String): ComponentInfo {
        val component = getComponent(name)
        return when (component) {
            ScriptNoteType.NONE -> None()
            ScriptNoteType.PK -> PK()
            ScriptNoteType.OLDER -> OLDER()
            ScriptNoteType.AFTER -> AFTER()
            ScriptNoteType.HASH160 -> HASH160()
            ScriptNoteType.HASH256 -> HASH256()
            ScriptNoteType.RIPEMD160 -> RIPEMD160()
            ScriptNoteType.SHA256 -> SHA256()
            ScriptNoteType.AND -> AND()
            ScriptNoteType.OR -> OR()
            ScriptNoteType.ANDOR -> ANDOR()
            ScriptNoteType.THRESH -> THRESH()
            ScriptNoteType.MULTI -> MULTI()
            ScriptNoteType.OR_TAPROOT -> OR_TAPROOT()
        }
    }
}

sealed class ComponentInfo(
    val icon: Int = 0,
    val name: String,
    val description: String,
) {
    class None : ComponentInfo(
        name = "None",
        description = "No component"
    )

    class PK : ComponentInfo(
        name = "PK",
        description = "Public key"
    )

    class OLDER : ComponentInfo(
        icon = R.drawable.ic_timer,
        name = "OLDER",
        description = "Older"
    )

    class AFTER : ComponentInfo(
        icon = R.drawable.ic_timer,
        name = "AFTER",
        description = "After"
    )

    class HASH160 : ComponentInfo(
        icon = R.drawable.ic_hash,
        name = "HASH160",
        description = "Hash160"
    )

    class HASH256 : ComponentInfo(
        icon = R.drawable.ic_hash,
        name = "HASH256",
        description = "Hash256"
    )

    class RIPEMD160 : ComponentInfo(
        icon = R.drawable.ic_hash,
        name = "RIPEMD160",
        description = "RIPEMD160"
    )

    class SHA256 : ComponentInfo(
        icon = R.drawable.ic_hash,
        name = "SHA256",
        description = "SHA256"
    )

    class AND : ComponentInfo(
        name = "AND",
        description = "Both sub‑conditions must be satisfied."
    )

    class OR : ComponentInfo(
        name = "OR",
        description = "Only one sub‑condition needs to be satisfied."
    )

    class ANDOR : ComponentInfo(
        name = "AND OR",
        description = "If the first sub‑condition is true, the second must also be satisfied. If the first one is false, the third must be satisfied."
    )

    class THRESH : ComponentInfo(
        name = "Thresh",
        description = "THRESH"
    )

    class MULTI : ComponentInfo(
        name = "Multisig",
        description = "Requires M of N keys."
    )

    class OR_TAPROOT : ComponentInfo(
        name = "OR",
        description = "Only one tapscript needs to be satisfied."
    )
}

@Keep
enum class ScriptNoteType {
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
    OR_TAPROOT
}
