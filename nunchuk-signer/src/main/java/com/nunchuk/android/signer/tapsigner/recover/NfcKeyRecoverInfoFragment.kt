package com.nunchuk.android.signer.tapsigner.recover

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.databinding.FragmentNfcKeyRecoverInfoBinding
import com.nunchuk.android.widget.NCToastMessage

class NfcKeyRecoverInfoFragment : BaseFragment<FragmentNfcKeyRecoverInfoBinding>() {
    override fun initializeBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentNfcKeyRecoverInfoBinding {
        return FragmentNfcKeyRecoverInfoBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(requireActivity().window, true)
        NCToastMessage(requireActivity()).show(getString(R.string.nc_cvc_has_been_changed))
        NCToastMessage(requireActivity()).show(getString(R.string.nc_master_private_key_init))
        registerEvents()
    }

    override fun onDestroyView() {
        WindowCompat.setDecorFitsSystemWindows(requireActivity().window, false)
        super.onDestroyView()
    }

    private fun registerEvents() {
        binding.toolbar.setNavigationOnClickListener {
            activity?.onBackPressed()
        }
        binding.btnContinue.setOnClickListener {
            findNavController().popBackStack()
            findNavController().navigate(R.id.addNfcNameFragment)
        }
    }
}