package com.nunchuk.android.signer

import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.signer.databinding.FragmentTurnOnNfcBinding

class TurnOnNfcFragment : BaseFragment<FragmentTurnOnNfcBinding>() {
    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (NfcAdapter.getDefaultAdapter(activity).isEnabled) {
                findNavController().navigate(R.id.action_turnOnNfcFragment_to_changeNfcCvcFragment)
            }
        }

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentTurnOnNfcBinding {
        return FragmentTurnOnNfcBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        registerEvents()
    }

    private fun registerEvents() {
        binding.btnGotIt.setOnClickListener {
            try {
                startForResult.launch(Intent(Settings.ACTION_NFC_SETTINGS))
            } catch (ignore: Exception) {
            }
        }
        binding.toolbar.setNavigationOnClickListener {
            activity?.finish()
        }
    }
}