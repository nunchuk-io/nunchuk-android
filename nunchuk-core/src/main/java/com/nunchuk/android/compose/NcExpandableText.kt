package com.nunchuk.android.compose

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.core.R

@Composable
fun NcExpandableText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    maxLines: Int = 1,
) {
    var expandable by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        Text(
            text = text,
            maxLines = if (expandable) Int.MAX_VALUE else maxLines,
            overflow = TextOverflow.Ellipsis,
            style = style,
        )
        Text(
            modifier = Modifier
                .padding(top = 10.dp)
                .border(1.dp, color = NcColor.border, shape = RoundedCornerShape(24.dp))
                .clickable { expandable = !expandable }
                .padding(4.dp),
            text = stringResource(id = if (expandable) R.string.nc_show_less else R.string.nc_more),
            style = NunchukTheme.typography.bodySmall,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NcExpandableTextPreview() {
    NunchukTheme {
        NcExpandableText(
            text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec euismod, nunc sit amet aliquet ultricies, nisl nisl aliquam nunc, quis aliquam nisl nisl quis nisl. Donec euismod, nunc sit amet aliquet ultricies, nisl nisl aliquam nunc, quis aliquam nisl nisl quis nisl.",
            modifier = Modifier.padding(16.dp),
            maxLines = 2,
        )
    }
}