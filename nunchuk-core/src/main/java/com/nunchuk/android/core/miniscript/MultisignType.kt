package com.nunchuk.android.core.miniscript

import com.nunchuk.android.core.R

enum class MultisignType(
    val title: String,
    val description: String,
    val icon: Int
) {
    EXPANDING(
        "Expanding multisig",
        "Keeps the required keys unchanged but adds more keys over time",
        icon = R.drawable.ic_expanding_multisign
    ),
    DECAYING(
        "Decaying multisig",
        "Keeps the total number of keys unchanged but lowers the required keys over time",
        icon = R.drawable.ic_decaying_multisign
    ),
    FLEXIBLE(
        "Flexible multisig", "Freely change the key set over time",
        icon = R.drawable.ic_flexible_multisign
    ),
    CUSTOM(
        "Enter miniscript",
        "Paste or enter your miniscript",
        icon = R.drawable.ic_enter_miniscript
    ),
    IMPORT(
        "Import from file",
        "Upload a file from your device",
        icon = R.drawable.ic_import
    ),
}