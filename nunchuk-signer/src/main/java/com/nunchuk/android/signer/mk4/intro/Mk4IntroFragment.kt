package com.nunchuk.android.signer.mk4.intro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
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
import com.nunchuk.android.signer.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter

@AndroidEntryPoint
class Mk4IntroFragment : Fragment(), BottomSheetOptionListener {
    private val nfcViewModel by activityViewModels<NfcViewModel>()
    private val viewModel by viewModels<Mk4IntroViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        WindowCompat.setDecorFitsSystemWindows(requireActivity().window, false)
        return ComposeView(requireContext()).apply {
            setContent {
                Mk4IntroScreen(viewModel)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observer()
    }

    override fun onDestroyView() {
        WindowCompat.setDecorFitsSystemWindows(requireActivity().window, true)
        super.onDestroyView()
    }

    override fun onOptionClicked(option: SheetOption) {
        val signer = viewModel.mk4Signers.getOrNull(option.type) ?: return
        findNavController().navigate(Mk4IntroFragmentDirections.actionMk4IntroFragmentToAddMk4NameFragment(signer))
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


@Composable
private fun Mk4IntroScreen(viewModel: Mk4IntroViewModel = viewModel()) = NunchukTheme {
    NunchukTheme {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .navigationBarsPadding()
            ) {
                NcImageAppBar(backgroundRes = R.drawable.nc_bg_coldcard_intro)
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
                NcPrimaryDarkButton(modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                    onClick = { viewModel.onContinueClicked() }) {
                    Text(text = stringResource(id = R.string.nc_text_continue))
                }
            }
        }
    }
}

@Preview
@Composable
private fun Mk4IntroScreenPreview() {
    Mk4IntroScreen()
}