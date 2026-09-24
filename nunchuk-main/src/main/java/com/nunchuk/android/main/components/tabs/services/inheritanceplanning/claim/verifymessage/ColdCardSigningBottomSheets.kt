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
import com.nunchuk.android.widget.R as WidgetR

/**
 * Callbacks for ColdCard signing actions
 */
/** How a signed message or PSBT comes back into the app. */
enum class ImportSignatureVia { FILE, QR, NFC }

data class ColdCardSigningCallbacks(
    val onExportViaQr: () -> Unit = {},
    val onExportViaNfc: () -> Unit = {},
    val onImportViaFile: () -> Unit = {},
    val onImportViaQr: () -> Unit = {},
    val onImportViaNfc: () -> Unit = {},
    val onSaveFile: () -> Unit = {},
    val onShareFile: () -> Unit = {},
) {
    fun onImport(via: ImportSignatureVia) = when (via) {
        ImportSignatureVia.FILE -> onImportViaFile()
        ImportSignatureVia.QR -> onImportViaQr()
        ImportSignatureVia.NFC -> onImportViaNfc()
    }
}

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
 * Reusable component that manages all bottom sheets for ColdCard signing flow.
 * Handles the navigation between different option sheets:
 * - ColdCard options (Export/Import)
 * - Export options (File/QR/NFC)
 * - Import options (File/QR/NFC)
 * - Save/Share options (Save/Share)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColdCardSigningBottomSheets(
    showColdCardOptions: Boolean,
    isMessage: Boolean = false,
    /**
     * Skip the File/QR/NFC pickers and go straight to [ColdCardSigningCallbacks.onExportViaQr] /
     * [ColdCardSigningCallbacks.onImportViaQr] — for a device that only speaks QR (Jade).
     */
    isQrOnly: Boolean = false,
    /**
     * Skip the pickers and go straight to the save/share sheet on export and
     * [ColdCardSigningCallbacks.onImportViaFile] on import — for a device that signs from a
     * memory card only (Passport's message signing).
     */
    isFileOnly: Boolean = false,
    /** Whether the File/QR/NFC pickers offer NFC; false for a device without it (Passport). */
    supportsNfc: Boolean = true,
    onDismissColdCardOptions: () -> Unit,
    callbacks: ColdCardSigningCallbacks,
) {
    var showExportOptionsSheet by remember { mutableStateOf(false) }
    var showImportOptionsSheet by remember { mutableStateOf(false) }
    var showSaveShareSheet by remember { mutableStateOf(false) }
    
    val coldCardOptionsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val exportOptionsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val saveShareSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // ColdCard Options Sheet (Export/Import)
    if (showColdCardOptions) {
        NcSelectableBottomSheetWithIcon(
            sheetState = coldCardOptionsSheetState,
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
                when (index) {
                    0 -> {
                        onDismissColdCardOptions()
                        when {
                            isQrOnly -> callbacks.onExportViaQr()
                            isFileOnly -> showSaveShareSheet = true
                            else -> showExportOptionsSheet = true
                        }
                    }
                    1 -> {
                        onDismissColdCardOptions()
                        when {
                            isQrOnly -> callbacks.onImportViaQr()
                            isFileOnly -> callbacks.onImportViaFile()
                            else -> showImportOptionsSheet = true
                        }
                    }
                }
            },
            onDismiss = onDismissColdCardOptions
        )
    }

    // Export Options Sheet (File/QR/NFC)
    if (showExportOptionsSheet) {
        NcSelectableBottomSheetWithIcon(
            sheetState = exportOptionsSheetState,
            items = listOfNotNull(
                SelectableItem(
                    resId = WidgetR.drawable.ic_export,
                    text = stringResource(R.string.nc_export_via_file)
                ),
                SelectableItem(
                    resId = WidgetR.drawable.ic_qr,
                    text = stringResource(R.string.nc_export_via_qr)
                ),
                SelectableItem(
                    resId = WidgetR.drawable.ic_nfc,
                    text = stringResource(R.string.nc_export_via_nfc)
                ).takeIf { supportsNfc }
            ),
            onSelected = { index ->
                showExportOptionsSheet = false
                when (index) {
                    0 -> showSaveShareSheet = true
                    1 -> callbacks.onExportViaQr()
                    2 -> callbacks.onExportViaNfc()
                }
            },
            onDismiss = {
                showExportOptionsSheet = false
            }
        )
    }

    // Import Options Sheet (File/QR/NFC)
    if (showImportOptionsSheet) {
        ImportSignatureOptionsSheet(
            routes = listOfNotNull(
                ImportSignatureVia.FILE,
                ImportSignatureVia.QR,
                ImportSignatureVia.NFC.takeIf { supportsNfc },
            ),
            onSelected = { route ->
                showImportOptionsSheet = false
                callbacks.onImport(route)
            },
            onDismiss = { showImportOptionsSheet = false }
        )
    }

    // Save/Share Options Sheet
    if (showSaveShareSheet) {
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
                showSaveShareSheet = false
                when (index) {
                    0 -> callbacks.onSaveFile()
                    1 -> callbacks.onShareFile()
                }
            },
            onDismiss = {
                showSaveShareSheet = false
            }
        )
    }
}
