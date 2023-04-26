package com.nunchuk.android.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.core.R

@Composable
fun TransactionNoteView(modifier: Modifier = Modifier, note: String) {
    var isTextOverFlow by remember { mutableStateOf(false) }
    var isNoteExpand by remember { mutableStateOf(false) }
    val onTextClick = {
        if (isTextOverFlow) {
            isNoteExpand = isNoteExpand.not()
        }
    }
    Row(
        modifier = modifier
            .clickable(onClick = onTextClick),
    ) {
        Icon(
            modifier = Modifier
                .border(1.dp, color = NcColor.border, shape = CircleShape)
                .padding(4.dp),
            tint = MaterialTheme.colors.primary,
            painter = painterResource(id = R.drawable.ic_transaction_note),
            contentDescription = "Transaction Note"
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically)
        ) {
            NcLinkifyText(
                modifier = Modifier.padding(horizontal = 8.dp),
                text = note,
                maxLines = if (isNoteExpand) Int.MAX_VALUE else 1,
                overflow = TextOverflow.Ellipsis,
                style = NunchukTheme.typography.bodySmall,
                onTextLayout = {
                    if (it.hasVisualOverflow) {
                        isTextOverFlow = true
                    }
                },
                onClick = onTextClick
            )
            if (isNoteExpand) {
                Text(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .background(
                            color = NcColor.greyLight,
                            shape = RoundedCornerShape(24.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = NcColor.border,
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .clickable { isNoteExpand = false },
                    text = stringResource(R.string.nc_show_less),
                    style = NunchukTheme.typography.bodySmall,
                )
            }
        }
        if (isTextOverFlow && isNoteExpand.not()) {
            Text(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .background(
                        color = NcColor.greyLight,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = NcColor.border,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .clickable { isNoteExpand = true },
                text = stringResource(R.string.nc_more),
                style = NunchukTheme.typography.bodySmall,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TransactionNoteViewPreview() {
    NunchukTheme {
        TransactionNoteView(note = "My name is Jayce")
    }
}