package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.exportcomplete

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcCircleImage
import com.nunchuk.android.compose.NcHighlightText
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.R
import com.nunchuk.android.main.R as MainR
import timber.log.Timber
import com.nunchuk.android.core.signing.FileSigningInstructions
import com.nunchuk.android.core.signing.SigningDevice
import com.nunchuk.android.core.signing.SigningMethod
import com.nunchuk.android.core.signing.profile
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.verifymessage.ImportSignatureOptionsSheet
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.verifymessage.ImportSignatureVia

/**
 * "Export completed" after the message request was saved or shared: what to do on the device,
 * then Import signature. One screen for every device that signs from a memory card; only the
 * instructions differ.
 */
@Composable
fun ExportCompleteScreen(
    device: SigningDevice = SigningDevice.COLDCARD,
    onImportSignature: (ImportSignatureVia) -> Unit = {},
    onCancel: () -> Unit = {},
) {
    ExportCompleteContent(
        device = device,
        onImportSignature = onImportSignature,
        onCancel = onCancel,
    )
}

@Composable
fun ExportCompleteContent(
    device: SigningDevice = SigningDevice.COLDCARD,
    onImportSignature: (ImportSignatureVia) -> Unit = {},
    onCancel: () -> Unit = {},
) {
    // Reached only after a message file export, so the device's file route says what comes next.
    val afterExport = (device.profile().message as? SigningMethod.ExportImport)?.fileRoute?.afterExport
    if (afterExport == null) {
        LaunchedEffect(device) {
            Timber.e("Export completed opened for %s, which exports no message file", device)
            onCancel()
        }
        return
    }
    // A device that answers one way goes straight there; one that answers by QR or file asks.
    val importRoutes = afterExport.imports
    val instructions = when (afterExport.instructions) {
        FileSigningInstructions.COLDCARD -> MainR.string.nc_export_completed_instructions
        FileSigningInstructions.PASSPORT -> MainR.string.nc_export_completed_instructions_passport
        FileSigningInstructions.KRUX -> MainR.string.nc_export_completed_instructions_krux
        FileSigningInstructions.GENERIC -> MainR.string.nc_export_completed_instructions_generic
    }
    var showImportOptions by remember { mutableStateOf(false) }
    NunchukTheme {
        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = stringResource(R.string.nc_export_via_file),
                    textStyle = NunchukTheme.typography.titleLarge
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        onClick = {
                            importRoutes.singleOrNull()
                                ?.let(onImportSignature)
                                ?: run { showImportOptions = true }
                        }
                    ) {
                        Text(text = stringResource(R.string.nc_import_signature))
                    }

                    NcOutlineButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onCancel
                    ) {
                        Text(text = stringResource(R.string.nc_cancel))
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
            ) {
                Spacer(modifier = Modifier.padding(top = 48.dp))

                NcCircleImage(
                    modifier = Modifier
                        .size(96.dp)
                        .align(Alignment.CenterHorizontally),
                    iconSize = 60.dp,
                    iconTintColor = Color(0xFF1C652D),
                    color = colorResource(id = R.color.nc_green_color),
                    resId = R.drawable.ic_check,
                )

                Text(
                    modifier = Modifier.padding(top = 24.dp),
                    text = stringResource(MainR.string.nc_export_completed),
                    style = NunchukTheme.typography.heading
                )

                NcHighlightText(
                    modifier = Modifier.padding(top = 16.dp),
                    text = stringResource(instructions),
                    style = NunchukTheme.typography.body,
                )
            }
        }

        if (showImportOptions) {
            ImportSignatureOptionsSheet(
                routes = importRoutes,
                onSelected = { route ->
                    showImportOptions = false
                    onImportSignature(route)
                },
                onDismiss = { showImportOptions = false }
            )
        }
    }
}

@Preview
@Composable
private fun ExportCompleteContentPreview() {
    ExportCompleteContent()
}

@Preview
@Composable
private fun ExportCompleteContentPassportPreview() {
    ExportCompleteContent(device = SigningDevice.PASSPORT)
}

@Preview
@Composable
private fun ExportCompleteContentKruxPreview() {
    ExportCompleteContent(device = SigningDevice.KRUX)
}

@Preview
@Composable
private fun ExportCompleteContentGenericAirgapPreview() {
    ExportCompleteContent(device = SigningDevice.GENERIC_AIRGAP)
}
