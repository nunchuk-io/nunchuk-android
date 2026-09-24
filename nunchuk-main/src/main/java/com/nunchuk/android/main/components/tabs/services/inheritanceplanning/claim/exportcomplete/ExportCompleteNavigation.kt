package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.exportcomplete

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import androidx.navigation.toRoute
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.verifymessage.ImportSignatureVia

/** @param signerTag [SignerTag.name] of the device the file is for; null reads as a Coldcard. */
@Serializable
data class ExportCompleteRoute(val signerTag: String? = null)

fun NavGraphBuilder.exportComplete(
    onImportSignature: (ImportSignatureVia) -> Unit = {},
    onCancel: () -> Unit = {},
) {
    composable<ExportCompleteRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<ExportCompleteRoute>()
        ExportCompleteScreen(
            signerTag = route.signerTag?.let { SignerTag.valueOf(it) },
            onImportSignature = onImportSignature,
            onCancel = onCancel,
        )
    }
}

fun NavController.navigateToExportComplete(signerTag: SignerTag? = null) {
    navigate(ExportCompleteRoute(signerTag = signerTag?.name))
}
