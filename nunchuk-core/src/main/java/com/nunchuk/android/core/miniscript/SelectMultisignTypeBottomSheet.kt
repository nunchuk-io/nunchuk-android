package com.nunchuk.android.core.miniscript

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.compose.textSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectMultisignTypeBottomSheet(
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onSelect: (MultisignType) -> Unit = {},
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
                SelectMultisignTypeBottomSheetContent {
                    coroutineScope.launch { sheetState.hide() }
                    onSelect(it)
                    onDismiss()
                }
            }
        }
    )
}

@Composable
fun SelectMultisignTypeBottomSheetContent(
    onSelect: (MultisignType) -> Unit = {},
) {
    Column(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            )
            .nestedScroll(rememberNestedScrollInteropConnection())
    ) {
        LazyColumn(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Select multisig template",
                    style = NunchukTheme.typography.titleSmall,
                )
            }

            items(MultisignType.entries.take(4).size) {
                val multisignType = MultisignType.entries[it]
                SelectMultisignTypeItem(
                    modifier = Modifier.clickable {
                        onSelect(multisignType)
                    },
                    title = multisignType.title,
                    description = multisignType.description,
                    icon = multisignType.icon
                )
            }

            item {
                HorizontalDivider()
            }

            item {
                Text(
                    text = "Enter custom miniscript",
                    style = NunchukTheme.typography.titleSmall,
                )
            }

            items(MultisignType.entries.takeLast(2).size) {
                val multisignType = MultisignType.entries[it + 4]
                SelectMultisignTypeItem(
                    modifier = Modifier.fillMaxWidth().clickable {
                        onSelect(multisignType)
                    },
                    title = multisignType.title,
                    description = multisignType.description,
                    icon = multisignType.icon
                )
            }
        }
    }
}

@Composable
fun SelectMultisignTypeItem(modifier: Modifier, title: String, description: String, icon: Int) {
    Row(modifier = modifier) {
        NcIcon(
            modifier = Modifier.size(24.dp),
            painter = painterResource(id = icon),
            contentDescription = null,
        )

        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(
                text = title,
                style = NunchukTheme.typography.body,
                color = MaterialTheme.colorScheme.textPrimary
            )

            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = description,
                style = NunchukTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.textSecondary)
            )
        }
    }
}

@PreviewLightDark
@Composable
fun SelectMultisignTypeBottomSheetPreview() {
    SelectMultisignTypeBottomSheetContent()
} 