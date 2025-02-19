package com.nunchuk.android.signer.mk4.inheritance.backup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.fragment.compose.content
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.compose.HighlightMessageType
import com.nunchuk.android.compose.NcHighlightText
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.controlTextPrimary
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.signer.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ColdCardSkipVerifyFragment : MembershipFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = content {

        val remainTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()

        ColdCardSkipVerifyScreen(
            remainTime = remainTime,
            onContinueClick = {
                navigator.openSetupMk4(requireActivity(), false)
            }
        )
    }
}


@Composable
fun ColdCardSkipVerifyScreen(
    remainTime: Int = 0,
    onContinueClick: () -> Unit = {},
) {
    NunchukTheme {
        NcScaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = stringResource(
                        id = R.string.nc_estimate_remain_time,
                        remainTime
                    ),
                )
            }, bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {

                    NcHintMessage(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        type = HighlightMessageType.WARNING,
                    ) {
                        Text(
                            text = "Uploading the incorrect backup file will result in the inheritance being unclaimable.",
                            style = NunchukTheme.typography.titleSmall
                        )
                    }

                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth(),
                        onClick = onContinueClick
                    ) {
                        Text(
                            text = stringResource(id = R.string.nc_text_continue),
                            style = NunchukTheme.typography.title.copy(color = MaterialTheme.colorScheme.controlTextPrimary)
                        )
                    }
                }

            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Image(
                    painter = painterResource(id = R.drawable.ic_important_notice),
                    contentDescription = "Decoy wallet intro",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                )

                Text(
                    text = "Are you sure you want to skip the backup verification?",
                    style = NunchukTheme.typography.heading,
                    modifier = Modifier.padding(top = 12.dp)
                )

                NcHighlightText(
                    text = "Please note: [B]if you are using a passphrase with your COLDCARD, ensure that the encrypted backup includes the passphrase so the inheritance protocol can function properly.[/B]",
                    style = NunchukTheme.typography.body,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ColdCardSkipVerifyScreenPreview() {
    ColdCardSkipVerifyScreen()
}