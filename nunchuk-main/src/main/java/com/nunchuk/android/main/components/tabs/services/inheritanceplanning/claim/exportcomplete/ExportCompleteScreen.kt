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
import androidx.annotation.StringRes
import com.nunchuk.android.type.SignerTag

/**
 * "Export completed" after the message request was saved or shared: what to do on the device,
 * then Import signature. One screen for every device that signs from a memory card; only the
 * instructions differ.
 */
@Composable
fun ExportCompleteScreen(
    signerTag: SignerTag? = null,
    onImportSignature: () -> Unit = {},
    onCancel: () -> Unit = {},
) {
    ExportCompleteContent(
        signerTag = signerTag,
        onImportSignature = onImportSignature,
        onCancel = onCancel,
    )
}

@Composable
fun ExportCompleteContent(
    signerTag: SignerTag? = null,
    onImportSignature: () -> Unit = {},
    onCancel: () -> Unit = {},
) {
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
                        onClick = onImportSignature
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
                    text = stringResource(signerTag.exportCompletedInstructionsRes),
                    style = NunchukTheme.typography.body,
                )
            }
        }
    }
}

/**
 * On-device steps per signer. A Coldcard is the default because it is the only device this
 * screen served before, and a `COLDCARD_NFC` signer carries no tag. Krux signs from an SD card
 * too and will take its own copy here once design provides it.
 */
@get:StringRes
private val SignerTag?.exportCompletedInstructionsRes: Int
    get() = when (this) {
        SignerTag.PASSPORT -> MainR.string.nc_export_completed_instructions_passport
        else -> MainR.string.nc_export_completed_instructions
    }

@Preview
@Composable
private fun ExportCompleteContentPreview() {
    ExportCompleteContent()
}

@Preview
@Composable
private fun ExportCompleteContentPassportPreview() {
    ExportCompleteContent(signerTag = SignerTag.PASSPORT)
}
