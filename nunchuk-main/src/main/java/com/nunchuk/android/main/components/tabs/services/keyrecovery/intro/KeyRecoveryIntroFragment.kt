/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.main.components.tabs.services.keyrecovery.intro

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.clearFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.compose.NCLabelWithIndex
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.domain.membership.TargetAction
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.services.keyrecovery.KeyRecoverySuccessState
import com.nunchuk.android.main.components.tabs.services.keyrecovery.securityquestionanswer.AnswerSecurityQuestionFragment
import com.nunchuk.android.model.CalculateRequiredSignatureStep
import com.nunchuk.android.model.VerificationType
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.utils.serializable
import com.nunchuk.android.widget.NCInfoDialog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class KeyRecoveryIntroFragment : Fragment() {

    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel by viewModels<KeyRecoveryIntroViewModel>()

    private val signLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it.data?.extras
            if (it.resultCode == Activity.RESULT_OK && data != null) {
                val extraInfo =
                    data.serializable<HashMap<String, String>>(GlobalResultKey.SECURITY_QUESTION_EXTRA_INFO)
                val securityQuestionToken =
                    data.getString(GlobalResultKey.SECURITY_QUESTION_TOKEN).orEmpty()
                val confirmCodeMap =
                    data.serializable<HashMap<String, String>>(GlobalResultKey.CONFIRM_CODE)
                        .orEmpty()
                val signatureMap =
                    data.serializable<HashMap<String, String>>(GlobalResultKey.SIGNATURE_EXTRA)
                if (!extraInfo.isNullOrEmpty()) {
                    viewModel.downloadBackupKey(
                        questionId = extraInfo[AnswerSecurityQuestionFragment.QUESTION_ID].orEmpty(),
                        answer = extraInfo[AnswerSecurityQuestionFragment.QUESTION_ANSWER].orEmpty()
                    )
                } else if (confirmCodeMap.isNotEmpty()) {
                    viewModel.requestRecover(
                        signatureMap ?: hashMapOf(),
                        securityQuestionToken,
                        confirmCodeMap[GlobalResultKey.CONFIRM_CODE_TOKEN].orEmpty(),
                        confirmCodeMap[GlobalResultKey.CONFIRM_CODE_NONCE].orEmpty()
                    )
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                KeyRecoveryIntroScreen(viewModel)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) { event ->
            when (event) {
                is KeyRecoveryIntroEvent.Loading -> showOrHideLoading(loading = event.isLoading)
                is KeyRecoveryIntroEvent.GetTapSignerSuccess -> {
                    if (event.signers.isEmpty()) {
                        NCInfoDialog(requireActivity()).showDialog(message = getString(R.string.nc_do_not_have_tapsigner))
                    } else {
                        findNavController().navigate(
                            KeyRecoveryIntroFragmentDirections.actionKeyRecoveryIntroFragmentToRecoverTapSignerListBottomSheetFragment(
                                event.signers.toTypedArray()
                            )
                        )
                    }
                }

                is KeyRecoveryIntroEvent.Error -> {
                    showError(message = event.message)
                }

                is KeyRecoveryIntroEvent.DownloadBackupKeySuccess -> {
                    findNavController().navigate(
                        KeyRecoveryIntroFragmentDirections.actionKeyRecoveryIntroFragmentToBackupDownloadFragment(
                            backupKey = event.backupKey
                        )
                    )
                }

                is KeyRecoveryIntroEvent.CalculateRequiredSignaturesSuccess -> {
                    val step = event.calculateRequiredSignaturesExt.step
                    if (step == CalculateRequiredSignatureStep.PENDING_APPROVAL) {
                        NCInfoDialog(requireActivity()).showDialog(message = getString(R.string.nc_recovery_request_already_exists))
                    } else if (step == CalculateRequiredSignatureStep.REQUEST_RECOVER && event.calculateRequiredSignaturesExt.data != null) {
                        navigator.openWalletAuthentication(
                            walletId = "",
                            userData = "",
                            requiredSignatures = event.calculateRequiredSignaturesExt.data!!.requiredSignatures,
                            type = event.calculateRequiredSignaturesExt.data!!.type,
                            action = TargetAction.DOWNLOAD_KEY_BACKUP.name,
                            launcher = signLauncher,
                            activityContext = requireActivity()
                        )
                    } else if (step == CalculateRequiredSignatureStep.RECOVER) {
                        viewModel.recoverKey()
                    }
                }

                KeyRecoveryIntroEvent.RequestRecoverSuccess -> {
                    findNavController().navigate(
                        KeyRecoveryIntroFragmentDirections.actionKeyRecoveryIntroFragmentToKeyRecoverySuccessStateFragment(
                            type = KeyRecoverySuccessState.KEY_RECOVERY_REQUEST_SENT.name
                        )
                    )
                }
            }
        }

        setFragmentResultListener(RecoveryTapSignerListBottomSheetFragment.REQUEST_KEY) { _, bundle ->
            bundle.parcelable<SignerModel>(RecoveryTapSignerListBottomSheetFragment.EXTRA_SIGNER)
                ?.let {
                    viewModel.setSelectedSigner(it)
                    if (viewModel.isHasGroup.not()) {
                        navigator.openWalletAuthentication(
                            walletId = "",
                            requiredSignatures = 0,
                            activityContext = requireActivity(),
                            type = VerificationType.SECURITY_QUESTION,
                            launcher = signLauncher
                        )
                    } else {
                        viewModel.calculateRequiredSignatures()
                    }
                } ?: run {
                clearFragmentResult(RecoveryTapSignerListBottomSheetFragment.REQUEST_KEY)
            }
        }
    }
}

@Composable
fun KeyRecoveryIntroScreen(
    viewModel: KeyRecoveryIntroViewModel = viewModel()
) {
    KeyRecoveryIntroScreenContent(onContinueClicked = {
        viewModel.getTapSignerList()
    })
}

@Composable
fun KeyRecoveryIntroScreenContent(
    onContinueClicked: () -> Unit = {}
) {
    NunchukTheme {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
            ) {
                NcImageAppBar(
                    backgroundRes = R.drawable.nc_bg_key_recovery
                )
                Text(
                    modifier = Modifier.padding(
                        top = 16.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
                    text = stringResource(R.string.nc_key_recovery),
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(
                        top = 16.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
                    text = stringResource(R.string.nc_key_recovery_intro_desc),
                    style = NunchukTheme.typography.body
                )
                NCLabelWithIndex(
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 24.dp
                    ),
                    index = 1,
                    label = stringResource(R.string.nc_key_recovery_intro_info_1)
                )
                NCLabelWithIndex(
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 24.dp,
                        bottom = 24.dp
                    ),
                    index = 2,
                    label = stringResource(R.string.nc_key_recovery_intro_info_2)
                )
                Spacer(modifier = Modifier.weight(1.0f))
                NcHintMessage(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    messages = listOf(ClickAbleText(content = stringResource(R.string.nc_key_recovery_intro_notice)))
                )
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onContinueClicked,
                ) {
                    Text(text = stringResource(id = R.string.nc_text_continue))
                }
            }
        }
    }
}

@Preview
@Composable
private fun KeyRecoveryIntroScreenPreview() {
    KeyRecoveryIntroScreenContent()
}