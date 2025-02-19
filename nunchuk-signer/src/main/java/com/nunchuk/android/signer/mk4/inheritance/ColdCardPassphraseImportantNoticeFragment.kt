package com.nunchuk.android.signer.mk4.inheritance

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
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.NcHighlightText
import com.nunchuk.android.compose.NcOutlineButton
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
class ColdCardPassphraseImportantNoticeFragment : MembershipFragment() {
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = content {
        val remainTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
        ColdCardPassphraseImportantNoticeScreen(
            remainTime = remainTime,
            onUseWithoutPassphrase = {
                requireActivity().finish()
            },
            onContinueWithPassphrase = {
                findNavController().navigate(
                    ColdCardPassphraseImportantNoticeFragmentDirections.actionColdCardPassphraseImportantNoticeFragmentToColdCardPassphraseBackupReminderFragment()
                )
            }
        )
    }
}


@Composable
fun ColdCardPassphraseImportantNoticeScreen(
    remainTime: Int = 0,
    onUseWithoutPassphrase: () -> Unit = {},
    onContinueWithPassphrase: () -> Unit = {}
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
                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .fillMaxWidth(),
                        onClick = onUseWithoutPassphrase
                    ) {
                        Text(
                            text = "Use another key (without passphrase)",
                            style = NunchukTheme.typography.title.copy(color = MaterialTheme.colorScheme.controlTextPrimary)
                        )
                    }

                    NcOutlineButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        onClick = onContinueWithPassphrase
                    ) {
                        Text(
                            "Continue, I know what I’m doing",
                            style = NunchukTheme.typography.title
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
                    text = "Important notice about passphrase",
                    style = NunchukTheme.typography.heading,
                    modifier = Modifier.padding(top = 12.dp)
                )

                NcHighlightText(
                    text = "Using a passphrase for the inheritance key greatly complicates the setup and increases the risk of errors, both for you and the Beneficiary. [B]We strongly recommend using an inheritance key without a passphrase.[/B]"
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ColdCardPassphraseImportantNoticeScreenPreview() {
    ColdCardPassphraseImportantNoticeScreen()
}