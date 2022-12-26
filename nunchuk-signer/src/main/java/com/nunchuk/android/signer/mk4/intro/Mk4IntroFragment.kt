package com.nunchuk.android.signer.mk4.intro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.nfc.NfcActionListener
import com.nunchuk.android.core.nfc.NfcViewModel
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.signer.R
import com.nunchuk.android.widget.NCInfoDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter

@AndroidEntryPoint
class Mk4IntroFragment : MembershipFragment(), BottomSheetOptionListener {
    private val nfcViewModel by activityViewModels<NfcViewModel>()
    private val viewModel by viewModels<Mk4IntroViewModel>()
    private val args: Mk4IntroFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                Mk4IntroScreen(viewModel, args.isMembershipFlow)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observer()
    }

    override fun onOptionClicked(option: SheetOption) {
        val signer = viewModel.mk4Signers.getOrNull(option.type) ?: return
        findNavController().navigate(
            Mk4IntroFragmentDirections.actionMk4IntroFragmentToAddMk4NameFragment(
                signer
            )
        )
    }

    private fun observer() {
        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == BaseNfcActivity.REQUEST_MK4_ADD_KEY }) {
            viewModel.getMk4Signer(it.records)
            nfcViewModel.clearScanInfo()
        }

        flowObserver(viewModel.event) {
            when (it) {
                is Mk4IntroViewEvent.LoadMk4SignersSuccess -> openSignerSheet(it.signers)
                is Mk4IntroViewEvent.Loading -> showOrHideLoading(it.isLoading)
                is Mk4IntroViewEvent.ShowError -> showError(it.message)
                Mk4IntroViewEvent.OnContinueClicked -> (requireActivity() as NfcActionListener).startNfcFlow(
                    BaseNfcActivity.REQUEST_MK4_ADD_KEY
                )
                Mk4IntroViewEvent.OnCreateSignerSuccess -> requireActivity().finish()
                Mk4IntroViewEvent.OnSignerExistInAssistedWallet -> showError(getString(R.string.nc_error_add_same_key))
                Mk4IntroViewEvent.ErrorMk4TestNet -> NCInfoDialog(requireActivity())
                    .showDialog(
                        title = getString(R.string.nc_invalid_network),
                        message = getString(R.string.nc_error_device_in_testnet_msg)
                    )
            }
        }
    }

    private fun openSignerSheet(signer: List<SingleSigner>) {
        if (signer.isNotEmpty()) {
            val fragment = BottomSheetOption.newInstance(signer.mapIndexed { index, singleSigner ->
                SheetOption(
                    type = index, label = singleSigner.derivationPath
                )
            }, title = getString(R.string.nc_mk4_signer_title))
            fragment.show(childFragmentManager, "BottomSheetOption")
        }
    }
}

@OptIn(ExperimentalLifecycleComposeApi::class)
@Composable
private fun Mk4IntroScreen(viewModel: Mk4IntroViewModel = viewModel(), isMembershipFlow: Boolean) {
    val remainTime by viewModel.remainTime.collectAsStateWithLifecycle()
    Mk4IntroContent(remainTime, isMembershipFlow, viewModel::onContinueClicked)
}

@Composable
private fun Mk4IntroContent(
    remainTime: Int = 0,
    isMembershipFlow: Boolean = true,
    onContinueClicked: () -> Unit = {}
) =
    NunchukTheme {
        NunchukTheme {
            Scaffold { innerPadding ->
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .navigationBarsPadding()
                ) {
                    NcImageAppBar(
                        backgroundRes = R.drawable.nc_bg_coldcard_intro,
                        title = if (isMembershipFlow) stringResource(id = R.string.nc_estimate_remain_time, remainTime) else ""
                    )
                    Text(
                        modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                        text = stringResource(R.string.nc_coldcard_nfc_tip),
                        style = NunchukTheme.typography.heading
                    )
                    Text(
                        modifier = Modifier.padding(16.dp),
                        text = stringResource(R.string.nc_coldcard_nfc_intro_desc),
                        style = NunchukTheme.typography.body
                    )
                    Spacer(modifier = Modifier.weight(1.0f))
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
private fun Mk4IntroScreenPreview() {
    Mk4IntroContent()
}