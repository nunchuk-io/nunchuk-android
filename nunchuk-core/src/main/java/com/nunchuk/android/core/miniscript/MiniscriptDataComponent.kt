package com.nunchuk.android.core.miniscript

import androidx.annotation.Keep
import com.nunchuk.android.core.R

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
            else -> throw IllegalArgumentException("Unknown component: $name")
        }
    }

    fun fromComponent(name: String): ComponentInfo {
        val component = getComponent(name)
        return when (component) {
            ScripNoteType.NONE -> ComponentInfo.None()
            ScripNoteType.PK -> ComponentInfo.PK()
            ScripNoteType.OLDER -> ComponentInfo.OLDER()
            ScripNoteType.AFTER -> ComponentInfo.AFTER()
            ScripNoteType.HASH160 -> ComponentInfo.HASH160()
            ScripNoteType.HASH256 -> ComponentInfo.HASH256()
            ScripNoteType.RIPEMD160 -> ComponentInfo.RIPEMD160()
            ScripNoteType.SHA256 -> ComponentInfo.SHA256()
            ScripNoteType.AND -> ComponentInfo.AND()
            ScripNoteType.OR -> ComponentInfo.OR()
            ScripNoteType.ANDOR -> ComponentInfo.ANDOR()
            ScripNoteType.THRESH -> ComponentInfo.THRESH()
            ScripNoteType.MULTI -> ComponentInfo.MULTI()
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
}

