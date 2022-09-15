package com.nunchuk.android.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.core.R
import com.nunchuk.android.core.util.ClickAbleText

@Composable
fun NcHintMessage(
    modifier: Modifier = Modifier,
    messages: List<ClickAbleText>,
    type: HighlightMessageType = HighlightMessageType.HINT
) {
    val annotatedString = buildAnnotatedString {
        messages.forEachIndexed { index, message ->
            if (message.onClick != null) {
                withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline)) {
                    append(message.content)
                }
            } else {
                append(message.content)
            }
            if (index != messages.lastIndex) {
                append(" ")
            }
        }
    }
    val backgroundColor = when(type) {
        HighlightMessageType.WARNING -> colorResource(id = R.color.nc_beeswax_tint)
        HighlightMessageType.HINT -> colorResource(id = R.color.nc_whisper_color)
    }
    val icon =  when(type) {
        HighlightMessageType.HINT -> painterResource(id = R.drawable.ic_info)
        HighlightMessageType.WARNING -> painterResource(id = R.drawable.ic_warning_amber)
    }
    Card(
        modifier = modifier,
        backgroundColor = backgroundColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                modifier = Modifier.size(36.dp),
                contentScale = ContentScale.Crop,
                painter = icon,
                contentDescription = "Info icon"
            )
            ClickableText(
                modifier = Modifier.padding(start = 8.dp),
                text = annotatedString,
                style = NunchukTheme.typography.titleSmall
            ) { offset ->
                var count = offset
                messages.forEach {
                    count -= it.content.length.inc()
                    if (count <= 0) {
                        it.onClick?.invoke()
                        return@forEach
                    }
                }
            }
        }
    }
}

enum class HighlightMessageType {
    HINT, WARNING
}

@Preview
@Composable
fun NcHintMessagePreview() {
    NunchukTheme {
        NcHintMessage(
            Modifier.padding(16.dp), messages = listOf(
                ClickAbleText("This step requires hardware keys to complete. If you have not received your hardware after a while, please contact us at support@nunchuk.io")
            ),
            type = HighlightMessageType.WARNING
        )
    }
}
