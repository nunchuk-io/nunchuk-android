package com.nunchuk.android.signer.mk4.info

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.nfc.NfcActionListener
import com.nunchuk.android.core.nfc.NfcViewModel
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.util.*
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.databinding.FragmentMk4InfoBinding
import com.nunchuk.android.widget.util.setOnDebounceClickListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter

@AndroidEntryPoint
class Mk4InfoFragment : BaseFragment<FragmentMk4InfoBinding>(), BottomSheetOptionListener {
    private val nfcViewModel by activityViewModels<NfcViewModel>()
    private val viewModel by viewModels<Mk4InfoViewModel>()

    override fun initializeBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentMk4InfoBinding {
        return FragmentMk4InfoBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        observer()
    }

    override fun onOptionClicked(option: SheetOption) {
        val signer = viewModel.mk4Signers.getOrNull(option.type) ?: return
        findNavController().navigate(Mk4InfoFragmentDirections.actionMk4InfoFragmentToAddMk4NameFragment(signer))
    }

    private fun observer() {
        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == BaseNfcActivity.REQUEST_MK4_ADD_KEY }) {
            viewModel.getMk4Signer(it.records)
            nfcViewModel.clearScanInfo()
        }

        flowObserver(viewModel.event) {
            when (it) {
                is Mk4InfoViewEvent.LoadMk4SignersSuccess -> openSignerSheet(it.signers)
                is Mk4InfoViewEvent.Loading -> showOrHideLoading(it.isLoading)
                is Mk4InfoViewEvent.ShowError -> showError(it.message)
            }
        }
    }

    private fun openSignerSheet(signer: List<SingleSigner>) {
        if (signer.isNotEmpty()) {
            val fragment = BottomSheetOption.newInstance(signer.mapIndexed { index, singleSigner ->
                SheetOption(
                    type = index,
                    label = singleSigner.derivationPath
                )
            }, title = getString(R.string.nc_mk4_signer_title))
            fragment.show(childFragmentManager, "BottomSheetOption")
        }
    }

    private fun initView() {
        binding.toolbar.setNavigationOnClickListener {
            activity?.finish()
        }
        binding.tvDescOne.makeTextLink(
            ClickAbleText(content = getString(R.string.nc_refer_to)),
            ClickAbleText(content = getString(R.string.nc_this_starter_guide), onClick = {
                requireActivity().openExternalLink(COLDCARD_GUIDE_URL)
            })
        )
        binding.btnContinue.setOnDebounceClickListener {
            (requireActivity() as NfcActionListener).startNfcFlow(BaseNfcActivity.REQUEST_MK4_ADD_KEY)
        }
    }

    companion object {
        private const val COLDCARD_GUIDE_URL = "https://coldcard.com/docs/quick"
    }
}