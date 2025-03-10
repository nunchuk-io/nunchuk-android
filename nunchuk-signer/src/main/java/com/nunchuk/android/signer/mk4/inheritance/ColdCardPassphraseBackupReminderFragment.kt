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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import androidx.fragment.compose.content
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.NcHighlightText
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.controlTextPrimary
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.mk4.Mk4Activity
import com.nunchuk.android.signer.mk4.Mk4Event
import com.nunchuk.android.signer.mk4.Mk4ViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ColdCardPassphraseBackupReminderFragment : MembershipFragment() {

    private val mk4ViewModel: Mk4ViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = content {
        val remainTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
        ColdCardPassphraseBackupReminderScreen(remainTime) {
            if (mk4ViewModel.coldCardBackUpParam.xfp.isNotEmpty()) {
                if ((activity as Mk4Activity).replacedXfp.isNullOrEmpty().not()) {
                    findNavController().navigate(
                        ColdCardPassphraseBackupReminderFragmentDirections.actionColdCardPassphraseBackupReminderFragmentToColdCardBackUpIntroFragment()
                    )
                } else {
                    mk4ViewModel.saveMembershipExistingColdCard()
                }
            } else {
                findNavController().navigate(
                    ColdCardPassphraseBackupReminderFragmentDirections.actionColdCardPassphraseBackupReminderFragmentToColdCardIntroFragment()
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(mk4ViewModel.event) { event ->
            when (event) {
                is Mk4Event.Loading -> {
                    showOrHideLoading(event.isLoading)
                }

                Mk4Event.Success -> {
                    findNavController().navigate(
                        ColdCardPassphraseBackupReminderFragmentDirections.actionColdCardPassphraseBackupReminderFragmentToColdCardBackUpIntroFragment()
                    )
                }
            }
        }
    }
}


@Composable
fun ColdCardPassphraseBackupReminderScreen(
    remainTime: Int = 0,
    onContinue: () -> Unit = {}
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
                        onClick = onContinue
                    ) {
                        Text(
                            text = "Continue",
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
                    text = "Passphrase backup reminder",
                    style = NunchukTheme.typography.heading,
                    modifier = Modifier.padding(top = 12.dp)
                )

                NcHighlightText(
                    text = "If you use a key with a passphrase, ensure you export the correct file when backing up the inheritance key from the COLDCARD in the next step.\n\n[B]The encrypted backup file must include the passphrase for the inheritance protocol to function correctly.[/B]"
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ColdCardPassphraseBackupReminderScreenPreview() {
    ColdCardPassphraseBackupReminderScreen()
}