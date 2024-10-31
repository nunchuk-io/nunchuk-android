
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NCLabelWithIndex
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.greyLight
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.wallet.emptystate.KeyWalletEntryData
import com.nunchuk.android.main.components.tabs.wallet.emptystate.WizardData

@Composable
fun EmptyStateHomeView(
    contentData: WizardData,
    onActionButtonClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color(contentData.backgroundColor),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = contentData.imageResId),
                    contentDescription = "Illustration",
                    modifier = Modifier.size(250.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = contentData.title,
                style = NunchukTheme.typography.titleLarge,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column {
                Text(
                    text = contentData.subtitle,
                    style = NunchukTheme.typography.body,
                )

                if (contentData.instructions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                contentData.instructions.forEachIndexed { index, content ->
                    NCLabelWithIndex(
                        index = index + 1,
                        label = content,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            NcPrimaryDarkButton(
                onClick = onActionButtonClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 36.dp),
                isAutoExpandHeight = true
            ) {
                Text(
                    textAlign = TextAlign.Center,
                    text = contentData.buttonText,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
fun KeyWalletEntryView(data: KeyWalletEntryData, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.greyLight, shape = RoundedCornerShape(8.dp))
            .clickable {
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(id = data.iconResId),
            contentDescription = null,
        )
        Text(
            text = data.title,
            style = NunchukTheme.typography.titleSmall,
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f)
        )
        Icon(
            painter = painterResource(id = R.drawable.ic_right_arrow_wallet),
            contentDescription = null,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewNunchukScreen() {
    NunchukTheme {
        EmptyStateHomeView(
            WizardData(
                title = "Welcome to Nunchuk!",
                subtitle = "To get started:",
                instructions = listOf(
                    "Add a key (or multiple keys if using multisig), then create your wallet.",
                    "Or quickly create a hot wallet for immediate use, then back it up later."
                ),
                buttonText = "Add a key (or multiple keys if using multisig), then create you",
                buttonAction = {},
                imageResId = R.drawable.bg_empty_state_personal_plan,
                backgroundColor = 0xFFFDEBD2
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewKeyWalletEntryView() {
    NunchukTheme {
        KeyWalletEntryView(
            KeyWalletEntryData(
                title = "Create hot wallet",
                buttonAction = {},
                iconResId = R.drawable.ic_hot_wallet_empty_state
            )
        )
    }
}
