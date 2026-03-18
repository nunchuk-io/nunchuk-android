package com.nunchuk.android.transaction.components.details.view

import android.graphics.Typeface
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.core.R as CoreR

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun InspectAddressBottomSheet(
    address: String,
    sheetState: SheetState = rememberModalBottomSheetState(),
    onCopy: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val andaleMono = remember {
        FontFamily(
            typeface = Typeface.createFromAsset(context.assets, "AndaleMono-Regular.ttf")
        )
    }
    val chunks = remember(address) {
        address.chunked(4)
    }

    ModalBottomSheet(
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
        dragHandle = {},
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(CoreR.string.nc_inspect_address),
                style = NunchukTheme.typography.title,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(24.dp))

            val primaryColor = MaterialTheme.colorScheme.textPrimary
            val secondaryColor = MaterialTheme.colorScheme.textSecondary
            val rows = chunks.chunked(4)

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rows.forEachIndexed { rowIndex, row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        row.forEachIndexed { colIndex, chunk ->
                            val index = rowIndex * 4 + colIndex
                            Text(
                                text = chunk,
                                modifier = Modifier.weight(1f),
                                style = TextStyle(
                                    fontFamily = andaleMono,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = if (index % 2 == 0) primaryColor else secondaryColor,
                                ),
                            )
                        }
                        repeat(4 - row.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            NcOutlineButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onCopy(address) },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    NcIcon(
                        painter = painterResource(id = com.nunchuk.android.widget.R.drawable.ic_copy),
                        contentDescription = "Copy",
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(CoreR.string.nc_copy),
                        style = NunchukTheme.typography.title,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewLightDark
@Composable
private fun InspectAddressBottomSheetPreview() {
    NunchukTheme {
        InspectAddressBottomSheet(
            address = "tb1qgu4hrgq6elva2px86xefkkhsjkeh8a5sellryg",
            onCopy = {},
            onDismiss = {},
        )
    }
}
