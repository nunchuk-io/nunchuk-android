package com.nunchuk.android.signer.mk4.inheritance.backup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import androidx.fragment.compose.content
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.HighlightMessageType
import com.nunchuk.android.compose.NCLabelWithIndex
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.controlTextPrimary
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.mk4.Mk4ViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ColdCardEncryptBackUpFileFragment : MembershipFragment() {

    private val mk4ViewModel: Mk4ViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = content {
        val remainTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
        ColdCardEncryptBackUpFileScreen(remainTime = remainTime,
            isHasPassphrase = mk4ViewModel.coldCardBackUpParam.isHasPassphrase) {
            findNavController().navigate(ColdCardEncryptBackUpFileFragmentDirections.actionColdCardEncryptBackUpFileFragmentToColdCardImportEncryptedBackUpFragment())
        }
    }
}


@Composable
internal fun ColdCardEncryptBackUpFileScreen(
    remainTime: Int = 0,
    isHasPassphrase: Boolean = false,
    onContinue: () -> Unit = {}
) {
    NunchukTheme {
        Scaffold(modifier = Modifier.navigationBarsPadding(), topBar = {
            NcImageAppBar(
                backgroundRes = R.drawable.bg_coldcard_encrypt_backup_illustration,
                title = stringResource(
                    id = R.string.nc_estimate_remain_time,
                    remainTime
                )
            )
        }, bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {

                NcHintMessage(
                    type = HighlightMessageType.HINT,
                    content = {
                        Text(
                            text = "Please note that the 12-word Backup Password is different from your seed phrase, even though both may consist of 12 or 24 words.",
                            style = NunchukTheme.typography.titleSmall
                        )
                    }
                )

                NcPrimaryDarkButton(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth(),
                    onClick = onContinue
                ) {
                    Text(
                        text = "Continue",
                        style = NunchukTheme.typography.title.copy(color = MaterialTheme.colorScheme.controlTextPrimary)
                    )
                }
            }

        }) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(innerPadding)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
            ) {

                Text(
                    modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                    text = "Encrypt the backup file",
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = "Please follow the instructions below on your COLDCARD:",
                    style = NunchukTheme.typography.body
                )

                NCLabelWithIndex(
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    index = 1,
                    title = "Record Backup Password",
                    label = if (isHasPassphrase) "Navigate to Advanced/Tools → Backup → Backup System → 2 [B](with passphrase).[/B]" else "Navigate to Advanced/Tools → Backup → Backup System",
                )

                Box(
                    modifier = Modifier
                        .padding(start = 50.dp, top = 16.dp, end = 16.dp)
                        .background(
                            color = colorResource(R.color.nc_fill_beewax),
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = "Note down the Backup Password (12 words) and store it in a safe place.",
                            style = NunchukTheme.typography.body,
                            textAlign = TextAlign.Start
                        )
                    }
                }

                NCLabelWithIndex(
                    modifier = Modifier.padding(16.dp),
                    index = 2,
                    title = "Encrypt the backup file",
                    label = "Confirm backup file password →OK.",
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ColdCardEncryptBackUpFileScreenPreview() {
    ColdCardEncryptBackUpFileScreen(isHasPassphrase = true)
}