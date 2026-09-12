package com.nunchuk.android.main.membership.backupseedphrase

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.signer.SignerCard
import com.nunchuk.android.core.signer.SignerModel

/**
 * The key a verification result is talking about, with a sentence in place of its fingerprint
 * line. Both outcomes of the seed-phrase verification show it the same way.
 */
@Composable
internal fun VerificationSignerCard(
    signer: SignerModel,
    statusText: String,
    modifier: Modifier = Modifier,
) {
    SignerCard(
        modifier = modifier.fillMaxWidth(),
        item = signer,
        xfpContent = {
            Text(
                text = statusText,
                style = NunchukTheme.typography.bodySmall,
            )
        },
    )
}
