package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.verifymessage

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.nunchuk.android.compose.NcSelectableBottomSheetWithIcon
import com.nunchuk.android.compose.SelectableItem
import com.nunchuk.android.core.R
import com.nunchuk.android.core.signing.ExportRoute
import com.nunchuk.android.core.signing.SigningMethod
import com.nunchuk.android.core.signing.SigningTransport
import com.nunchuk.android.widget.R as WidgetR

/** Shared transport identity, retained as an alias for claim navigation events. */
typealias ImportSignatureVia = SigningTransport

/**
 * Required handlers, one per route kind, so a configured route cannot fall through to a no-op. A
 * FILE export never reaches a handler directly: it opens the Save/Share sheet, whose two choices
 * receive the route and with it the file name.
 */
data class ExportImportCallbacks(
    val onExportQr: (ExportRoute.Qr) -> Unit,
    val onExportNfc: (ExportRoute.Nfc) -> Unit,
    val onImport: (SigningTransport) -> Unit,
    val onSaveFile: (ExportRoute.File) -> Unit,
    val onShareFile: (ExportRoute.File) -> Unit,
)

/** "Import signature" picker: one row per route, in the order given. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportSignatureOptionsSheet(
    routes: List<ImportSignatureVia>,
    onSelected: (ImportSignatureVia) -> Unit,
    onDismiss: () -> Unit,
) {
    NcSelectableBottomSheetWithIcon(
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        items = routes.map { route ->
            when (route) {
                ImportSignatureVia.FILE -> SelectableItem(
                    resId = WidgetR.drawable.ic_import,
                    text = stringResource(R.string.nc_import_via_file)
                )

                ImportSignatureVia.QR -> SelectableItem(
                    resId = WidgetR.drawable.ic_qr,
                    text = stringResource(R.string.nc_import_via_qr)
                )

                ImportSignatureVia.NFC -> SelectableItem(
                    resId = WidgetR.drawable.ic_nfc,
                    text = stringResource(R.string.nc_import_via_nfc)
                )
            }
        },
        onSelected = { index -> onSelected(routes[index]) },
        onDismiss = onDismiss
    )
}

/**
 * Transport pickers shared by message and PSBT claim signing. A single export or import route skips
 * its picker; a FILE export still asks Save or Share.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportImportSheets(
    method: SigningMethod.ExportImport,
    showOptions: Boolean,
    isMessage: Boolean = false,
    onDismissOptions: () -> Unit,
    callbacks: ExportImportCallbacks,
) {
    var showExportOptionsSheet by remember { mutableStateOf(false) }
    var showImportOptionsSheet by remember { mutableStateOf(false) }
    var saveShareFileRoute by remember { mutableStateOf<ExportRoute.File?>(null) }

    val optionsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val exportOptionsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val saveShareSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val export: (ExportRoute) -> Unit = { route ->
        when (route) {
            is ExportRoute.File -> saveShareFileRoute = route
            is ExportRoute.Qr -> callbacks.onExportQr(route)
            is ExportRoute.Nfc -> callbacks.onExportNfc(route)
        }
    }

    // Export / Import
    if (showOptions) {
        NcSelectableBottomSheetWithIcon(
            sheetState = optionsSheetState,
            items = listOf(
                SelectableItem(
                    resId = WidgetR.drawable.ic_export,
                    text = if (isMessage) stringResource(R.string.nc_export_message) else stringResource(R.string.nc_transaction_export_transaction)
                ),
                SelectableItem(
                    resId = WidgetR.drawable.ic_import,
                    text = stringResource(R.string.nc_import_signature)
                )
            ),
            onSelected = { index ->
                onDismissOptions()
                when (index) {
                    0 -> method.export.singleOrNull()?.let(export) ?: run { showExportOptionsSheet = true }
                    1 -> method.imports.singleOrNull()?.let(callbacks.onImport) ?: run { showImportOptionsSheet = true }
                }
            },
            onDismiss = onDismissOptions
        )
    }

    // Export via File / QR / NFC
    if (showExportOptionsSheet) {
        NcSelectableBottomSheetWithIcon(
            sheetState = exportOptionsSheetState,
            items = method.export.map { route ->
                when (route.transport) {
                    SigningTransport.FILE -> SelectableItem(
                        resId = WidgetR.drawable.ic_export,
                        text = stringResource(R.string.nc_export_via_file),
                    )
                    SigningTransport.QR -> SelectableItem(
                        resId = WidgetR.drawable.ic_qr,
                        text = stringResource(R.string.nc_export_via_qr),
                    )
                    SigningTransport.NFC -> SelectableItem(
                        resId = WidgetR.drawable.ic_nfc,
                        text = stringResource(R.string.nc_export_via_nfc),
                    )
                }
            },
            onSelected = { index ->
                showExportOptionsSheet = false
                export(method.export[index])
            },
            onDismiss = {
                showExportOptionsSheet = false
            }
        )
    }

    // Import via File / QR / NFC
    if (showImportOptionsSheet) {
        ImportSignatureOptionsSheet(
            routes = method.imports,
            onSelected = { route ->
                showImportOptionsSheet = false
                callbacks.onImport(route)
            },
            onDismiss = { showImportOptionsSheet = false }
        )
    }

    // Save / Share the exported file
    saveShareFileRoute?.let { fileRoute ->
        NcSelectableBottomSheetWithIcon(
            sheetState = saveShareSheetState,
            items = listOf(
                SelectableItem(
                    resId = WidgetR.drawable.ic_export,
                    text = stringResource(R.string.nc_save_file)
                ),
                SelectableItem(
                    resId = WidgetR.drawable.ic_share,
                    text = stringResource(R.string.nc_share_file)
                )
            ),
            onSelected = { index ->
                saveShareFileRoute = null
                when (index) {
                    0 -> callbacks.onSaveFile(fileRoute)
                    1 -> callbacks.onShareFile(fileRoute)
                }
            },
            onDismiss = {
                saveShareFileRoute = null
            }
        )
    }
}
