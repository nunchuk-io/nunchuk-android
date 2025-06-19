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

    fun getComponent(name: String): ScripNoteType {
        return when (name) {
            "NONE" -> ScripNoteType.NONE
            "PK" -> ScripNoteType.PK
            "OLDER" -> ScripNoteType.OLDER
            "AFTER" -> ScripNoteType.AFTER
            "HASH160" -> ScripNoteType.HASH160
            "HASH256" -> ScripNoteType.HASH256
            "RIPEMD160" -> ScripNoteType.RIPEMD160
            "SHA256" -> ScripNoteType.SHA256
            "AND" -> ScripNoteType.AND
            "OR" -> ScripNoteType.OR
            "ANDOR" -> ScripNoteType.ANDOR
            "THRESH" -> ScripNoteType.THRESH
            "MULTI" -> ScripNoteType.MULTI
            "OR_TAPROOT" -> ScripNoteType.OR_TAPROOT
            else -> throw IllegalArgumentException("Unknown component: $name")
        }
    }

    fun fromComponent(name: String): ComponentInfo {
        val component = getComponent(name)
        return when (component) {
            ScripNoteType.NONE -> None()
            ScripNoteType.PK -> PK()
            ScripNoteType.OLDER -> OLDER()
            ScripNoteType.AFTER -> AFTER()
            ScripNoteType.HASH160 -> HASH160()
            ScripNoteType.HASH256 -> HASH256()
            ScripNoteType.RIPEMD160 -> RIPEMD160()
            ScripNoteType.SHA256 -> SHA256()
            ScripNoteType.AND -> AND()
            ScripNoteType.OR -> OR()
            ScripNoteType.ANDOR -> ANDOR()
            ScripNoteType.THRESH -> THRESH()
            ScripNoteType.MULTI -> MULTI()
            ScripNoteType.OR_TAPROOT -> OR_TAPROOT()
        }
    }
}

sealed class ComponentInfo(
    val scripNoteType: ScripNoteType,
    val icon: Int = 0,
    val name: String,
    val description: String,
) {
    class None : ComponentInfo(
        scripNoteType = ScripNoteType.NONE,
        name = "None",
        description = "No component"
    )

    class PK : ComponentInfo(
        scripNoteType = ScripNoteType.PK,
        name = "PK",
        description = "Public key"
    )

    class OLDER : ComponentInfo(
        scripNoteType = ScripNoteType.OLDER,
        icon = R.drawable.ic_timer,
        name = "OLDER",
        description = "Older"
    )

    class AFTER : ComponentInfo(
        scripNoteType = ScripNoteType.AFTER,
        name = "AFTER",
        icon = R.drawable.ic_timer,
        description = "After"
    )

    class HASH160 : ComponentInfo(
        scripNoteType = ScripNoteType.HASH160,
        icon = R.drawable.ic_hash,
        name = "HASH160",
        description = "Hash160"
    )

    class HASH256 : ComponentInfo(
        scripNoteType = ScripNoteType.HASH256,
        icon = R.drawable.ic_hash,
        name = "HASH256",
        description = "Hash256"
    )

    class RIPEMD160 : ComponentInfo(
        scripNoteType = ScripNoteType.RIPEMD160,
        icon = R.drawable.ic_hash,
        name = "RIPEMD160",
        description = "RIPEMD160"
    )

    class SHA256 : ComponentInfo(
        scripNoteType = ScripNoteType.SHA256,
        icon = R.drawable.ic_hash,
        name = "SHA256",
        description = "SHA256"
    )

    class AND : ComponentInfo(
        scripNoteType = ScripNoteType.AND,
        name = "AND",
        description = "Both sub‑conditions must be satisfied."
    )

    class OR : ComponentInfo(
        scripNoteType = ScripNoteType.OR,
        name = "OR",
        description = "Only one sub‑condition needs to be satisfied."
    )

    class ANDOR : ComponentInfo(
        scripNoteType = ScripNoteType.ANDOR,
        name = "AND OR",
        description = "If the first sub‑condition is true, the second must also be satisfied. If the first one is false, the third must be satisfied."
    )

    class THRESH : ComponentInfo(
        scripNoteType = ScripNoteType.THRESH,
        name = "THRESH",
        description = "THRESH"
    )

    class MULTI : ComponentInfo(
        scripNoteType = ScripNoteType.MULTI,
        name = "Multisig 3/4",
        description = "Requires M of N keys."
    )

    class OR_TAPROOT : ComponentInfo(
        scripNoteType = ScripNoteType.OR_TAPROOT,
        name = "OR",
        description = "Only one tapscript needs to be satisfied."
    )
}

@Keep
enum class ScripNoteType {
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

