package com.nunchuk.android.signer.mk4.inheritance.backup.viaapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTextField
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideNfcLoading
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.mk4.Mk4Activity
import com.nunchuk.android.signer.mk4.Mk4ViewModel
import com.nunchuk.android.signer.mk4.inheritance.backup.ColdCardEncryptBackUpFileFragmentDirections
import com.nunchuk.android.signer.tapsigner.backup.verify.byapp.CheckBackUpByAppEvent
import com.nunchuk.android.signer.tapsigner.backup.verify.byapp.CheckBackUpByAppFragmentDirections
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ColdCardVerifyBackupViaAppFragment : MembershipFragment() {

    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel: ColdCardVerifyBackupViaAppViewModel by viewModels()
    private val mk4ViewModel: Mk4ViewModel by activityViewModels()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.event.flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { event ->
                    when (event) {
                        ColdCardVerifyBackupViaAppEvent.OnVerifyBackUpKeySuccess -> {
                            handleBackUpKeySuccess()
                        }
                        ColdCardVerifyBackupViaAppEvent.OnVerifyFailedTooMuch -> TODO()
                        is ColdCardVerifyBackupViaAppEvent.ShowError -> {
                            showError(event.throwable?.message)
                        }
                    }
                }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = content {
        val remainTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
        val state by viewModel.state.collectAsStateWithLifecycle()
        ColdCardVerifyBackupViaAppScreen(remainTime = remainTime, suggestions = state.suggestions,
            backUpPassword = state._backUpPassword,
            onBackUpPasswordTextChange = {
                viewModel.handleInputEvent(it)
            }, onSuggestClick = {
                viewModel.handleSelectWord(it)
            }, onContinue = {
                val keyId = mk4ViewModel.coldCardBackUpParam.keyId
                val groupId = (requireActivity() as Mk4Activity).groupId
                val masterSignerId = mk4ViewModel.coldCardBackUpParam.xfp
                val filePath = mk4ViewModel.coldCardBackUpParam.filePath
                if (keyId.isNotEmpty()) {
                    viewModel.onReplaceKeyVerified(masterSignerId, keyId, filePath)
                } else {
                    viewModel.onContinueClicked(groupId, masterSignerId, filePath)
                }
            })
    }

    private fun handleBackUpKeySuccess() {
        findNavController().navigate(ColdCardVerifyBackupViaAppFragmentDirections.actionColdCardVerifyBackupViaAppFragmentToBackUpResultHealthyFragment())
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ColdCardVerifyBackupViaAppScreen(
    remainTime: Int = 0,
    backUpPassword: String = "",
    suggestions: List<String> = emptyList(),
    isShowVerifyError: Boolean = false,
    onContinue: () -> Unit = {},
    onSuggestClick: (String) -> Unit = {},
    onBackUpPasswordTextChange: (String) -> Unit = {},
) {

    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()

    NunchukTheme {
        Scaffold(modifier = Modifier.navigationBarsPadding()
            , topBar = {
            NcImageAppBar(
                backgroundRes = R.drawable.bg_coldcard_backup_pass_illustration,
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
                        style = NunchukTheme.typography.title.copy(color = Color.White)
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
                    text = "Verify backup via the app",
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = "Enter your Backup Password below and the app will check it for you.\n\nAll verification data will stay locally on the device and will be erased immediately afterward.",
                    style = NunchukTheme.typography.body
                )

                NcTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    title = "Backup Password",
                    inputBoxHeight = 128.dp,
                    error = if (isShowVerifyError) "Decryption failed, please try again." else "",
                    value = TextFieldValue(
                        text = backUpPassword,
                        selection = TextRange(backUpPassword.length)
                    ),
                    onValueChange = {
                        onBackUpPasswordTextChange(it.text)
                    },
                    onFocusEvent = { isFocused ->
                        if (isFocused) {
                            coroutineScope.launch {
                                delay(500L)
                                bringIntoViewRequester.bringIntoView()
                            }
                        }
                    }
                )

                LazyRow(modifier = Modifier.bringIntoViewRequester(bringIntoViewRequester)) {
                    items(suggestions) {
                        Card(
                            modifier = Modifier
                                .padding(vertical = 16.dp, horizontal = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            onClick = {
                                onSuggestClick(it)
                            }
                        ) {
                            Text(
                                text = it,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun ColdCardVerifyBackupViaAppScreenPreview() {
    ColdCardVerifyBackupViaAppScreen()
}