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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import androidx.fragment.compose.content
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.NcOptionItem
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.controlTextPrimary
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.mk4.Mk4ViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ColdCardPassphraseQuestionFragment : MembershipFragment() {

    private val mk4ViewModel: Mk4ViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = content {
        val remainTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()

        ColdCardPassphraseQuestionScreen(remainTime) { isHasPassphrase ->
            mk4ViewModel.setOrUpdate(mk4ViewModel.coldCardBackUpParam.copy(isHasPassphrase = isHasPassphrase))
            if (isHasPassphrase) {
                findNavController().navigate(
                    ColdCardPassphraseQuestionFragmentDirections.actionColdCardPassphraseQuestionFragmentToColdCardPassphraseImportantNoticeFragment()
                )
            } else if (mk4ViewModel.coldCardBackUpParam.xfp.isNotEmpty()) {
                findNavController().navigate(
                    ColdCardPassphraseQuestionFragmentDirections.actionColdCardPassphraseQuestionFragmentToColdCardPassphraseImportantNoticeFragment()
                )
            } else {
                findNavController().navigate(
                    ColdCardPassphraseQuestionFragmentDirections.actionColdCardPassphraseQuestionFragmentToColdCardIntroFragment()
                )
            }
        }
    }
}


@Composable
fun ColdCardPassphraseQuestionScreen(
    remainTime: Int = 0,
    onContinueClicked: (Boolean) -> Unit = {}
) {

    var selectedOption by remember { mutableIntStateOf(-1) }

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
                        enabled = selectedOption != -1,
                        onClick = {
                            onContinueClicked(selectedOption == 1)
                        }
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
                    painter = painterResource(id = R.drawable.ic_coldcard_passphrase_intro),
                    contentDescription = "Decoy wallet intro",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                )

                Text(
                    text = "Are you using a passphrase with your COLDCARD?",
                    style = NunchukTheme.typography.heading,
                    modifier = Modifier.padding(top = 12.dp)
                )

                Text(
                    text = "A BIP39 passphrase is an extra word added to the seed phrase as another layer of security.",
                    style = NunchukTheme.typography.body,
                )

                NcOptionItem(
                    isSelected = selectedOption == 0,
                    label = "I don’t have a passphrase",
                    onClick = {
                        selectedOption = 0
                    }
                )

                NcOptionItem(
                    isSelected = selectedOption == 1,
                    label = "I have a passphrase",
                    onClick = {
                        selectedOption = 1
                    }
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ColdCardPassphraseQuestionScreenPreview() {
    ColdCardPassphraseQuestionScreen()
}