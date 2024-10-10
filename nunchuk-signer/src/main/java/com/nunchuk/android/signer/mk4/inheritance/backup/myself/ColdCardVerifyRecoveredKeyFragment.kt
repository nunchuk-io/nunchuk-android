package com.nunchuk.android.signer.mk4.inheritance.backup.myself

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.dialog.NcConfirmationDialog
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.mk4.Mk4Activity
import com.nunchuk.android.signer.mk4.Mk4ViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class ColdCardVerifyRecoveredKeyFragment : MembershipFragment() {

    @Inject
    lateinit var navigator: NunchukNavigator

    private val mk4ViewModel: Mk4ViewModel by activityViewModels()
    private val viewModel: ColdCardVerifyRecoveredKeyViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = content {
        val remainTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
        ColdCardVerifyRecoveredKeyScreen(remainTime = remainTime,
            xfp = mk4ViewModel.coldCardBackUpParam.xfp,
            onContinue = {
                val keyId = mk4ViewModel.coldCardBackUpParam.keyId
                if (keyId.isNotEmpty()) {
                    viewModel.setReplaceKeyVerified(
                        keyId,
                        mk4ViewModel.coldCardBackUpParam.filePath
                    )
                } else {
                    viewModel.setKeyVerified(
                        (requireActivity() as? Mk4Activity)?.groupId.orEmpty(),
                        mk4ViewModel.coldCardBackUpParam.xfp
                    )
                }
            },
            onSkip = {
                requireActivity().finish()
            })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.event.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { event ->
                    when (event) {
                        ColdCardVerifyRecoveredKeyEvent.OnExitSelfCheck -> requireActivity().finish()
                        is ColdCardVerifyRecoveredKeyEvent.ShowError -> showError(event.throwable?.message)
                    }
                }
        }
    }
}


@Composable
internal fun ColdCardVerifyRecoveredKeyScreen(
    remainTime: Int = 0, xfp: String = "", onContinue: () -> Unit = {}, onSkip: () -> Unit = {}
) {

    var showConfirmationDialog by remember { mutableStateOf(false) }

    NunchukTheme {
        Scaffold(topBar = {
            NcTopAppBar(
                title = stringResource(
                    id = R.string.nc_estimate_remain_time, remainTime
                )
            )
        }, bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {

                NcHintMessage(modifier = Modifier.padding(top = 16.dp), content = {
                    Text(
                        text = "If the XFP doesn’t match, please recheck the uploaded file. If you are using a passphrase, it must be included in the encrypted backup.",
                        style = NunchukTheme.typography.titleSmall
                    )
                })
                NcPrimaryDarkButton(modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth(),
                    onClick = {
                        showConfirmationDialog = true
                    }) {
                    Text(
                        text = "I have verified the backup",
                        style = NunchukTheme.typography.title.copy(color = Color.White)
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
                    Text(text = stringResource(R.string.I_will_comeback_to_this_later))
                }
            }

        }) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState()),
            ) {

                Text(
                    modifier = Modifier.padding(top = 24.dp),
                    text = "Verify the recovered key",
                    style = NunchukTheme.typography.heading
                )

                Text(
                    modifier = Modifier.padding(top = 24.dp),
                    text = "You should now have recovered the original key on COLDCARD. Please verify that the Master Key Fingerprint (XFP) on your COLDCARD matches the one shown below.",
                    style = NunchukTheme.typography.body
                )

                Box(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .background(
                            color = colorResource(R.color.nc_whisper_color),
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

                Text(
                    modifier = Modifier.padding(top = 24.dp),
                    text = "Master key figngerprint (XFP)",
                    style = NunchukTheme.typography.body
                )
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .background(
                            color = colorResource(R.color.nc_beeswax_tint),
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
                            text = xfp,
                            style = NunchukTheme.typography.body,
                            textAlign = TextAlign.Start
                        )
                    }
                }

            }

            if (showConfirmationDialog) {
                NcConfirmationDialog(title = stringResource(R.string.nc_confirmation),
                    message = "Are you sure you have verified the backup? Inheritance claiming might not work with an unverified backup.",
                    onPositiveClick = {
                        onContinue()
                        showConfirmationDialog = false
                    },
                    onDismiss = {
                        showConfirmationDialog = false
                    })
            }
        }
    }
}

@Preview
@Composable
private fun ColdCardVerifyRecoveredKeyScreenPreview() {
    NunchukTheme {
        ColdCardVerifyRecoveredKeyScreen(xfp = "C33B6C9A")
    }
}