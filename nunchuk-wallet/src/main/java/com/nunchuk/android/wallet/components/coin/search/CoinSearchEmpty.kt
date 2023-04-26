package com.nunchuk.android.wallet.components.coin.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcColor
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.wallet.R

@Composable
fun ColumnScope.EmptySearchState() {
    Icon(
        modifier = Modifier.align(CenterHorizontally)
            .padding(top = 24.dp)
            .background(color = NcColor.greyLight, shape = CircleShape)
            .padding(18.dp),
        painter = painterResource(id = R.drawable.ic_search_not_found),
        contentDescription = "Search not found"
    )
    Text(
        modifier = Modifier
            .padding(top = 16.dp)
            .align(CenterHorizontally),
        text = stringResource(R.string.nc_no_results_found),
        style = NunchukTheme.typography.body
    )
}

@Preview(showBackground = true)
@Composable
fun EmptySearchStatePreview() {
    MaterialTheme {
        Column {
            EmptySearchState()
        }
    }
}