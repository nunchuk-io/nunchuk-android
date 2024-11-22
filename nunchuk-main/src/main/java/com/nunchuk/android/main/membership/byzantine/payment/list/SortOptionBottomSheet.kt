@file:OptIn(ExperimentalMaterial3Api::class)

package com.nunchuk.android.main.membership.byzantine.payment.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcRadioButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.main.R

enum class SortBy {
    NONE,
    OLDEST,
    NEWEST,
    AZ,
    ZA,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortOptionButtonSheet(
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onDismiss: () -> Unit,
    initValue: SortBy = SortBy.NONE,
    sortRecurringpaymentItem: (sortBy: SortBy) -> Unit = {},
) {
    ModalBottomSheet(
        modifier = Modifier.height((LocalConfiguration.current.screenHeightDp * 9 / 10).dp),
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        onDismissRequest = onDismiss,
        content = {
            SortOptionsBottomSheetScreen(
                sortRecurringpaymentItem = sortRecurringpaymentItem,
                initValue = initValue,
            )
        },
        dragHandle = {}
    )
}

@Composable
fun SortOptionsBottomSheetScreen(
    sortRecurringpaymentItem: (sortBy: SortBy) -> Unit = {},
    initValue: SortBy = SortBy.NONE,
) {
    var sortBy by remember(initValue) {
        mutableStateOf(initValue)
    }
    Column(
        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp),
    ) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(vertical = 12.dp),
                    text = stringResource(R.string.nc_sort_title),
                    color = Color.Black,
                    style = NunchukTheme.typography.titleLarge
                )
                Text(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(vertical = 12.dp)
                        .clickable(onClick = { sortBy = SortBy.NONE }),
                    text = stringResource(R.string.nc_clear_all),
                    color = Color.Black,
                    style = NunchukTheme.typography.titleLarge.copy(textDecoration = TextDecoration.Underline)
                )
            }
            Text(
                modifier = Modifier.padding(top = 16.dp),
                text = stringResource(R.string.nc_sort_by_start_date),
                textAlign = TextAlign.Center,
                color = Color.Black,
                style = NunchukTheme.typography.title
            )
            SortRadioButtonItem(
                modifier = Modifier.padding(top = 16.dp),
                text = "Sort oldest to newest",
                isSelected = sortBy == SortBy.NEWEST,
                onClick = { sortBy = SortBy.NEWEST })
            SortRadioButtonItem(
                modifier = Modifier,
                text = "Sort newest to oldest",
                isSelected = sortBy == SortBy.OLDEST,
                onClick = { sortBy = SortBy.OLDEST })
            Text(
                modifier = Modifier.padding(top = 24.dp),
                text = "Sort by payment name",
                textAlign = TextAlign.Center,
                color = Color.Black,
                style = NunchukTheme.typography.title
            )
            SortRadioButtonItem(
                modifier = Modifier.padding(top = 16.dp),
                text = "Sort A → Z",
                isSelected = sortBy == SortBy.AZ,
                onClick = { sortBy = SortBy.AZ })
            SortRadioButtonItem(
                modifier = Modifier,
                text = "Sort Z → A",
                isSelected = sortBy == SortBy.ZA,
                onClick = { sortBy = SortBy.ZA })
            Spacer(modifier = Modifier.weight(1f))
            NcPrimaryDarkButton(
                modifier = Modifier
                    .padding(bottom = 20.dp)
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                onClick = {
                    sortRecurringpaymentItem(sortBy)
                },
                enabled = true,
            ) {
                Text(text = "Apply")
            }
        }

    }
}

@Composable
fun SortRadioButtonItem(
    modifier: Modifier,
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = text,
            style = NunchukTheme.typography.body
        )
        NcRadioButton(selected = isSelected, onClick = onClick)
    }
}