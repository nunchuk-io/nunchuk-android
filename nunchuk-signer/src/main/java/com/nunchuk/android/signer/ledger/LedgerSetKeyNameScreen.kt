package com.nunchuk.android.signer.ledger

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.nunchuk.android.signer.components.name.SetKeyNameScreen

const val ledgerSetKeyNameRoute = "ledger_set_key_name_route"

fun NavGraphBuilder.ledgerSetKeyName(
    defaultName: () -> String = { "" },
    onBack: () -> Unit = {},
    onContinue: (String) -> Unit = {}
) {
    composable(ledgerSetKeyNameRoute) {
        SetKeyNameScreen(
            defaultName = defaultName(),
            onBack = onBack,
            onContinue = onContinue
        )
    }
}

fun NavHostController.navigateToLedgerSetKeyName() {
    navigate(ledgerSetKeyNameRoute)
}
