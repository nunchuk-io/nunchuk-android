package com.nunchuk.android.app.miniscript.contractpolicy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.main.R
import com.nunchuk.android.wallet.personal.components.add.KeyManagementSection
import kotlinx.coroutines.launch

private const val MAX_REQUIRED_KEYS = 20
private const val MAX_TOTAL_KEYS = 20

/**
 * Configuration for the EditPolicyBottomSheet
 */
data class EditPolicyConfig(
    val initialM: Int = 2,
    val initialN: Int = 3,
    val showTotalKeys: Boolean = true,
    val showRequiredKeys: Boolean = true,
    val minM: Int = 1,
    val maxM: Int = MAX_REQUIRED_KEYS,
    val minN: Int = 2,
    val maxN: Int = MAX_TOTAL_KEYS,
    val title: String = "Edit multisig policy"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPolicyBottomSheet(
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    config: EditPolicyConfig = EditPolicyConfig(),
    onSave: (m: Int, n: Int) -> Unit = { _, _ -> },
    onDismiss: () -> Unit = {},
) {
    val coroutineScope = rememberCoroutineScope()

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        dragHandle = {},
        content = {
            Column(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                    )
                    .nestedScroll(rememberNestedScrollInteropConnection())
            ) {
                EditPolicyBottomSheetContent(
                    config = config,
                    onSave = { m, n ->
                        coroutineScope.launch { sheetState.hide() }
                        onSave(m, n)
                        onDismiss()
                    },
                    onDismiss = onDismiss
                )
            }
        }
    )
}

@Composable
fun EditPolicyBottomSheetContent(
    config: EditPolicyConfig = EditPolicyConfig(),
    onSave: (Int, Int) -> Unit = { _, _ -> },
    onDismiss: () -> Unit = {},
) {
    var m by remember { mutableIntStateOf(config.initialM) }
    var n by remember { mutableIntStateOf(config.initialN) }
    
    Column(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            )
            .padding(vertical = 24.dp, horizontal = 16.dp)
            .nestedScroll(rememberNestedScrollInteropConnection())
    ) {
        Text(
            text = config.title,
            style = NunchukTheme.typography.title,
        )
        
        KeysAndRequiredKeysScreen(
            m = m,
            n = n,
            showTotalKeys = config.showTotalKeys,
            showRequiredKeys = config.showRequiredKeys,
            minM = config.minM,
            maxM = config.maxM,
            minN = config.minN,
            maxN = config.maxN,
            onNumberChange = { requiredKeys, totalKeys ->
                m = requiredKeys
                n = totalKeys
            }
        )

        NcPrimaryDarkButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            onClick = { onSave(m, n) },
        ) {
            Text(text = stringResource(id = R.string.nc_text_save))
        }
    }
}

@Composable
fun KeysAndRequiredKeysScreen(
    m: Int,
    n: Int,
    showTotalKeys: Boolean = true,
    showRequiredKeys: Boolean = true,
    minM: Int = 1,
    maxM: Int = 5,
    minN: Int = 2,
    maxN: Int = 5,
    onNumberChange: (Int, Int) -> Unit = { _, _ -> }
) {
    Column {
        var requiredKeys by remember { mutableIntStateOf(m) }
        var totalKeys by remember { mutableIntStateOf(n) }
        
        LaunchedEffect(requiredKeys, totalKeys) {
            onNumberChange(requiredKeys, totalKeys)
        }

        if (showRequiredKeys) {
            KeyManagementSection(
                title = "Required keys",
                description = "Number of signatures required",
                value = requiredKeys,
                enableIncrement = requiredKeys < if (showTotalKeys) totalKeys else maxM,
                enableDecrement = requiredKeys > minM,
                onIncrement = { if (requiredKeys < if (showTotalKeys) totalKeys else maxM) requiredKeys++ },
                onDecrement = { if (requiredKeys > minM) requiredKeys-- }
            )

            if (showTotalKeys) {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        if (showTotalKeys) {
            KeyManagementSection(
                title = "Total number of keys",
                description = "Total number of public keys in the multisig",
                value = totalKeys,
                enableIncrement = totalKeys < maxN,
                enableDecrement = totalKeys > minN && (!showRequiredKeys || totalKeys > requiredKeys),
                onIncrement = { if (totalKeys < maxN) totalKeys++ },
                onDecrement = { 
                    if (totalKeys > minN) {
                        // Only prevent decrement if it would make total keys less than required keys
                        if (!showRequiredKeys || totalKeys > requiredKeys) {
                            totalKeys--
                        }
                    }
                }
            )
        }
    }
}

@PreviewLightDark
@Composable
fun EditPolicyBottomSheetPreview() {
    NunchukTheme {
        EditPolicyBottomSheetContent()
    }
}

@PreviewLightDark
@Composable
fun EditPolicyBottomSheetSinglePreview() {
    NunchukTheme {
        EditPolicyBottomSheetContent(
            config = EditPolicyConfig(
                showTotalKeys = false,
                initialM = 1,
                maxM = 1
            )
        )
    }
}

@PreviewLightDark
@Composable
fun EditPolicyBottomSheetTotalKeysOnlyPreview() {
    NunchukTheme {
        EditPolicyBottomSheetContent(
            config = EditPolicyConfig(
                showTotalKeys = true,
                showRequiredKeys = false,
                initialN = 2,
                minN = 1
            )
        )
    }
}