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

@Composable
fun ExportCompleteScreen(
    onImportSignature: () -> Unit = {},
    onCancel: () -> Unit = {},
) {
    ExportCompleteContent(
        onImportSignature = onImportSignature,
        onCancel = onCancel,
    )
}

@Composable
fun ExportCompleteContent(
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
                    text = stringResource(MainR.string.nc_export_completed_instructions),
                    style = NunchukTheme.typography.body,
                )
            }
        }
    }
}

@Preview
@Composable
private fun ExportCompleteContentPreview() {
    ExportCompleteContent()
}
