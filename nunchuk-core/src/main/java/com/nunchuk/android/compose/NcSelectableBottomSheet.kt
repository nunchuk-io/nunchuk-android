package com.nunchuk.android.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.core.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NcSelectableBottomSheet(
    sheetState: SheetState = rememberModalBottomSheetState(),
    title: String? = null,
    showSelectIndicator: Boolean = false,
    options: List<String>,
    selectedPos: Int = -1,
    onSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var selectedIndex by remember { mutableIntStateOf(selectedPos) }
    ModalBottomSheet(
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        onDismissRequest = onDismiss,
        tonalElevation = 0.dp,
        content = {
            title?.let {
                Text(
                    text = it,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                    style = NunchukTheme.typography.title
                )
            }
            LazyColumn(modifier = Modifier.padding(vertical = 16.dp)) {
                itemsIndexed(options) { index, s ->
                    NcSelectableBottomSheetItem(
                        text = s,
                        selected = index == selectedIndex && showSelectIndicator,
                        onClick = {
                            selectedIndex = index
                            scope.launch {
                                sheetState.hide()
                            }.invokeOnCompletion {
                                onSelected(index)
                                if (!sheetState.isVisible) {
                                    onDismiss()
                                }
                            }
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        },
        dragHandle = { }
    )
}

@Composable
private fun NcSelectableBottomSheetItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = text, style = NunchukTheme.typography.body
        )
        if (selected) {
            Image(
                painter = painterResource(id = R.drawable.ic_check),
                contentDescription = "Icon Check"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun NcSelectableBottomSheetPreview() {
    NunchukTheme {
        NcSelectableBottomSheet(
            sheetState = rememberModalBottomSheetState(),
            title = "Title",
            options = listOf("Option 1", "Option 2", "Option 3"),
            selectedPos = 1,
            onSelected = {},
            onDismiss = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun NcSelectableBottomSheetNoTitlePreview() {
    NunchukTheme {
        NcSelectableBottomSheet(
            sheetState = rememberModalBottomSheetState(),
            options = listOf("Option 1", "Option 2", "Option 3"),
            selectedPos = 1,
            onSelected = {},
            onDismiss = {}
        )
    }
}
