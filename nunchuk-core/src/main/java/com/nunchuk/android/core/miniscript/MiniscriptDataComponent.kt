package com.nunchuk.android.core.miniscript

import androidx.annotation.Keep

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
