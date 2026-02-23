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
data class ColdCardSigningCallbacks(
    val onExportViaFile: () -> Unit = {},
    val onExportViaQr: () -> Unit = {},
    val onExportViaNfc: () -> Unit = {},
    val onImportViaFile: () -> Unit = {},
    val onImportViaQr: () -> Unit = {},
    val onImportViaNfc: () -> Unit = {},
    val onSaveFile: () -> Unit = {},
    val onShareFile: () -> Unit = {},
)

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
    onDismissColdCardOptions: () -> Unit,
    callbacks: ColdCardSigningCallbacks,
) {
    var showExportOptionsSheet by remember { mutableStateOf(false) }
    var showImportOptionsSheet by remember { mutableStateOf(false) }
    var showSaveShareSheet by remember { mutableStateOf(false) }
    
    val coldCardOptionsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val exportOptionsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val importOptionsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
                        showExportOptionsSheet = true
                    }
                    1 -> {
                        onDismissColdCardOptions()
                        showImportOptionsSheet = true
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
            items = listOf(
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
                )
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
        NcSelectableBottomSheetWithIcon(
            sheetState = importOptionsSheetState,
            items = listOf(
                SelectableItem(
                    resId = WidgetR.drawable.ic_import,
                    text = stringResource(R.string.nc_import_via_file)
                ),
                SelectableItem(
                    resId = WidgetR.drawable.ic_qr,
                    text = stringResource(R.string.nc_import_via_qr)
                ),
                SelectableItem(
                    resId = WidgetR.drawable.ic_nfc,
                    text = stringResource(R.string.nc_import_via_nfc)
                )
            ),
            onSelected = { index ->
                showImportOptionsSheet = false
                when (index) {
                    0 -> callbacks.onImportViaFile()
                    1 -> callbacks.onImportViaQr()
                    2 -> callbacks.onImportViaNfc()
                }
            },
            onDismiss = {
                showImportOptionsSheet = false
            }
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
