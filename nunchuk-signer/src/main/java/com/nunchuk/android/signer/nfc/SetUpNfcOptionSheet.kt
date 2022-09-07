package com.nunchuk.android.signer.nfc

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.nunchuk.android.core.base.BaseBottomSheet
import com.nunchuk.android.signer.databinding.DialogSetUpOptionsSheetBinding

class SetUpNfcOptionSheet : BaseBottomSheet<DialogSetUpOptionsSheetBinding>(), View.OnClickListener {
    private lateinit var listener: OptionClickListener

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = if (context is OptionClickListener) {
            context
        } else if (parentFragment is OptionClickListener) {
            parentFragment as OptionClickListener
        } else {
            throw NullPointerException("Activity or Parent fragment have to implement OptionClickListener")
        }
    }

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): DialogSetUpOptionsSheetBinding {
        return DialogSetUpOptionsSheetBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        registerEvents()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.btnAddNewNfc.id -> listener.onOptionClickListener(SetUpNfcOption.ADD_NEW)
            binding.btnRecoverNfcKey.id -> listener.onOptionClickListener(SetUpNfcOption.RECOVER)
            binding.btnAddMk4.id -> listener.onOptionClickListener(SetUpNfcOption.Mk4)
        }
        dismiss()
    }

    private fun registerEvents() {
        binding.btnAddNewNfc.setOnClickListener(this)
        binding.btnRecoverNfcKey.setOnClickListener(this)
        binding.btnAddMk4.setOnClickListener(this)
    }

    enum class SetUpNfcOption { ADD_NEW, RECOVER, Mk4 }

    interface OptionClickListener {
        fun onOptionClickListener(option: SetUpNfcOption)
    }

    companion object {
        fun newInstance() = SetUpNfcOptionSheet()
    }
}