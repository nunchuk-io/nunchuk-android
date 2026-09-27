package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.exportcomplete

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import androidx.navigation.toRoute
import com.nunchuk.android.core.signing.SigningDevice
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.verifymessage.ImportSignatureVia

/** Carries the classified device, not an arbitrary first signer tag. */
@Serializable
data class ExportCompleteRoute(val device: SigningDevice = SigningDevice.COLDCARD)

fun NavGraphBuilder.exportComplete(
    onImportSignature: (ImportSignatureVia) -> Unit = {},
    onCancel: () -> Unit = {},
) {
    composable<ExportCompleteRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<ExportCompleteRoute>()
        ExportCompleteScreen(
            device = route.device,
            onImportSignature = onImportSignature,
            onCancel = onCancel,
        )
    }
}

fun NavController.navigateToExportComplete(device: SigningDevice = SigningDevice.COLDCARD) {
    navigate(ExportCompleteRoute(device = device))
}
