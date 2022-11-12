package com.nunchuk.android.signer.tapsigner.recover

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.databinding.FragmentRecoverNfcKeyGuideBinding
import com.nunchuk.android.signer.tapsigner.decryption.NfcDecryptionKeyFragment

class RecoverNfcKeyGuideFragment : BaseFragment<FragmentRecoverNfcKeyGuideBinding>() {
    private val openDocument = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        findNavController().navigate(R.id.nfcDecryptionKeyFragment, NfcDecryptionKeyFragment.buildArguments(uri))
    }

    override fun initializeBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentRecoverNfcKeyGuideBinding {
        return FragmentRecoverNfcKeyGuideBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(requireActivity().window, true)
        binding.btnContinue.setOnClickListener {
            openDocument.launch("application/*")
        }
        binding.toolbar.setNavigationOnClickListener {
            activity?.onBackPressed()
        }
    }

    override fun onDestroyView() {
        WindowCompat.setDecorFitsSystemWindows(requireActivity().window, false)
        super.onDestroyView()
    }
}