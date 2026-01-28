package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.exportcomplete

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object ExportCompleteRoute

fun NavGraphBuilder.exportComplete(
    onImportSignature: () -> Unit = {},
    onCancel: () -> Unit = {},
) {
    composable<ExportCompleteRoute> {
        ExportCompleteScreen(
            onImportSignature = onImportSignature,
            onCancel = onCancel,
        )
    }
}

fun NavController.navigateToExportComplete() {
    navigate(ExportCompleteRoute)
}
