package com.nunchuk.android.signer.mk4.inheritance.backup.myself

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.NCLabelWithIndex
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.controlTextPrimary
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.core.share.IntentSharingController
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.mk4.Mk4Activity
import com.nunchuk.android.signer.mk4.Mk4ViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ColdCardVerifyBackupFragment : MembershipFragment() {

    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel: ColdCardVerifyBackUpMyselfViewModel by viewModels()
    private val mk4ViewModel: Mk4ViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = content {
        val remainTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
        ColdCardVerifyBackupScreen(remainTime, onContinue = {
            findNavController().navigate(
                ColdCardVerifyBackupFragmentDirections.actionColdCardVerifyBackupFragmentToColdCardVerifyRecoveredKeyFragment()
            )
        }, onSkip = {
            requireActivity().finish()
        }, onDownloadFile = {
            viewModel.handleDownloadBackupKey(
                isReplaceKey = mk4ViewModel.coldCardBackUpParam.keyId.isNotEmpty(),
                backUpFileName = mk4ViewModel.coldCardBackUpParam.backUpFileName,
                filePath = mk4ViewModel.coldCardBackUpParam.filePath,
                xfp = mk4ViewModel.coldCardBackUpParam.xfp,
                groupId = (requireActivity() as Mk4Activity).groupId,
                walletId = (requireActivity() as Mk4Activity).walletId.orEmpty()
            )
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.event.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { event ->
                    when (event) {
                        is ColdCardVerifyBackUpMyselfEvent.GetBackUpKeySuccess -> IntentSharingController.from(
                            requireActivity()
                        )
                            .shareFile(event.filePath)
                    }
                }
        }
    }
}


@Composable
internal fun ColdCardVerifyBackupScreen(
    remainTime: Int = 0,
    onContinue: () -> Unit = {},
    onSkip: () -> Unit = {},
    onDownloadFile: () -> Unit = {}
) {
    NunchukTheme {
        Scaffold(modifier = Modifier.navigationBarsPadding(), topBar = {
            NcTopAppBar(
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

                TextButton(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    onClick = {
                        onSkip()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.I_will_comeback_to_this_later),
                        style = NunchukTheme.typography.title.copy(color = MaterialTheme.colorScheme.textPrimary)
                    )
                }
            }

        }) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(innerPadding)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {

                Text(
                    modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                    text = "Test recovery on COLDCARD",
                    style = NunchukTheme.typography.heading
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.textPrimary,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "1",
                            style = NunchukTheme.typography.titleSmall.copy(fontWeight = FontWeight.W900)
                        )
                    }
                    Text(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .clickable {
                                onDownloadFile()
                            },
                        text = "Download the encrypted backup file",
                        style = NunchukTheme.typography.title,
                        textDecoration = TextDecoration.Underline
                    )

                    NcIcon(
                        painter = painterResource(id = R.drawable.ic_download),
                        contentDescription = "",
                        modifier = Modifier.size(24.dp)
                    )
                }

                NCLabelWithIndex(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    index = 2,
                    title = "Prepare COLDCARD",
                    label = "If you have an unused COLDCARD, you can use it to test recovery.\n\nOtherwise, back up your current seed from the COLDCARD, then wipe it to test recovery.\n\nGo to Advanced/Tools → Danger Zone → Seed Functions → Destroy Seed → OK → 4.",
                )

                NCLabelWithIndex(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    index = 3,
                    title = "Recover via backup file on COLDCARD",
                    label = "Import the backup file you just downloaded into your COLDCARD and recover the key from the backup file.\nEnter PIN→ Import Existing → Restore Backup → Select backup file.",
                )

                Box(
                    modifier = Modifier
                        .padding(start = 50.dp, end = 16.dp)
                        .background(
                            color = colorResource(R.color.nc_bg_mid_gray),
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
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ColdCardVerifyBackupScreenPreview() {
    ColdCardVerifyBackupScreen()
}