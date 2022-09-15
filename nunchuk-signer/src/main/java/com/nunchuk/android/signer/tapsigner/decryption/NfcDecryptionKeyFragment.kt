package com.nunchuk.android.signer.tapsigner.decryption

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.nfc.NfcViewModel
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.databinding.FragmentDecryptionKeyBinding
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.heightExtended
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NfcDecryptionKeyFragment : BaseFragment<FragmentDecryptionKeyBinding>() {
    private val viewModel by viewModels<NfcDecryptionKeyViewModel>()
    private val nfcViewModel by activityViewModels<NfcViewModel>()
    private val uri: Uri by lazy(LazyThreadSafetyMode.NONE) { Uri.parse(requireArguments().getString(EXTRA_URI_STRING)) }

    override fun initializeBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentDecryptionKeyBinding {
        return FragmentDecryptionKeyBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        registerEvents()
        initViews()
        observer()
    }

    private fun observer() {
        lifecycleScope.launchWhenStarted {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.event.collect {
                    showOrHideLoading(it is NfcDecryptionKeyEvent.Loading)
                    when (it) {
                        is NfcDecryptionKeyEvent.ImportTapSignerFailed -> showError(it.e)
                        is NfcDecryptionKeyEvent.ImportTapSignerSuccess -> handleImportSuccess(it.masterSigner)
                        else -> {}
                    }
                }
            }
        }
    }

    private fun handleImportSuccess(masterSigner: MasterSigner) {
        nfcViewModel.updateMasterSigner(masterSigner)
        findNavController().navigate(R.id.addNfcNameFragment)
    }

    private fun showError(e: Throwable?) {
        if (e?.message.isNullOrEmpty().not()) {
            NCToastMessage(requireActivity()).showError(e?.message.orEmpty())
        }
    }

    private fun initViews() {
        binding.inputDecryptionKey.heightExtended(resources.getDimensionPixelSize(R.dimen.nc_height_120))
        binding.toolbar.setNavigationOnClickListener {
            activity?.onBackPressed()
        }
    }

    private fun registerEvents() {
        binding.btnContinue.setOnClickListener {
            if (binding.inputDecryptionKey.getEditText().isNullOrEmpty()) {
                binding.inputDecryptionKey.setError(getString(R.string.nc_text_required))
                return@setOnClickListener
            }
            binding.inputDecryptionKey.hideError()
            viewModel.decryptBackUpKey(uri, binding.inputDecryptionKey.getEditText())
        }
    }

    companion object {
        private const val EXTRA_URI_STRING = "EXTRA_URI_STRING"
        fun buildArguments(uri: Uri) = Bundle().apply {
            putString(EXTRA_URI_STRING, uri.toString())
        }
    }
}