package com.nunchuk.android.signer.components.details.exportxpub

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.strokePrimary
import com.nunchuk.android.core.qr.convertToQRCode
import com.nunchuk.android.signer.R

private val QR_SIZE = 224.dp

/**
 * QR and text carry the same `[fingerprint/path]xpub` string; the text is selectable so part of it
 * can be copied by long press, while Copy XPUB always copies all of it.
 */
@Composable
internal fun ExportXpubContent(
    descriptor: String = "",
    snackState: SnackbarHostState = remember { SnackbarHostState() },
    onBack: () -> Unit = {},
    onCopy: () -> Unit = {},
) {
    BackHandler(onBack = onBack)
    NcScaffold(
        modifier = Modifier.navigationBarsPadding(),
        snackState = snackState,
        topBar = {
            NcTopAppBar(
                title = stringResource(R.string.nc_export_xpub),
                textStyle = NunchukTheme.typography.titleLarge,
                onBackPress = onBack,
            )
        },
        bottomBar = {
            NcPrimaryDarkButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                enabled = descriptor.isNotEmpty(),
                onClick = onCopy,
            ) {
                Text(text = stringResource(R.string.nc_copy_xpub))
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val qrSizePx = with(LocalDensity.current) { QR_SIZE.roundToPx() }
            val qrCode = remember(descriptor, qrSizePx) {
                descriptor.takeIf { it.isNotEmpty() }
                    ?.convertToQRCode(width = qrSizePx, height = qrSizePx)
                    ?.asImageBitmap()
            }
            // The bitmap carries its own quiet zone; white behind it keeps that zone in dark mode.
            Box(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .size(QR_SIZE),
            ) {
                qrCode?.let {
                    Image(
                        bitmap = it,
                        contentDescription = stringResource(R.string.nc_export_xpub),
                        modifier = Modifier.size(QR_SIZE),
                    )
                }
            }
            SelectionContainer(
                modifier = Modifier
                    .padding(top = 24.dp, bottom = 16.dp)
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.strokePrimary, RoundedCornerShape(8.dp))
                    .padding(12.dp),
            ) {
                Text(
                    text = descriptor,
                    style = NunchukTheme.typography.body,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ExportXpubContentPreview() {
    NunchukTheme {
        ExportXpubContent(
            descriptor = "[3442193e/84h/0h/0h]xpub6C1HVMz946r433QEjZGpYYWYcspxXXBPys5PBGkmQboRXE6RLfFiStEkKbWKCZaPgDrzZh9nUEunxuiuy6MNdw23du2Ek7GoKYMJVH8eK5E",
        )
    }
}
