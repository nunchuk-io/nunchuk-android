package com.nunchuk.android.signer.bitbox

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.nunchuk.android.signer.components.name.SetKeyNameScreen

const val bitBoxSetKeyNameRoute = "bitbox_set_key_name_route"

/**
 * Design screen 06 — reuses the shared "Name your key" screen. Only the standalone add-key flow
 * lands here; membership flows auto-name the key instead (see BitBoxViewModel).
 */
fun NavGraphBuilder.bitBoxSetKeyName(
    defaultName: () -> String = { "" },
    onBack: () -> Unit = {},
    onContinue: (String) -> Unit = {},
) {
    composable(bitBoxSetKeyNameRoute) {
        SetKeyNameScreen(
            defaultName = defaultName(),
            onBack = onBack,
            onContinue = onContinue,
        )
    }
}

fun NavHostController.navigateToBitBoxSetKeyName() {
    navigate(bitBoxSetKeyNameRoute)
}
