package com.nunchuk.android.signer.components.details

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.nunchuk.android.core.base.BaseBottomSheet
import com.nunchuk.android.signer.components.details.model.SingerOption
import com.nunchuk.android.signer.databinding.DialogSignerDetailOptionsSheetBinding

class SingerInfoOptionBottomSheet : BaseBottomSheet<DialogSignerDetailOptionsSheetBinding>(), View.OnClickListener {
    private lateinit var listener : OptionClickListener

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
    ): DialogSignerDetailOptionsSheetBinding {
        return DialogSignerDetailOptionsSheetBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        registerEvents()
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            binding.btnTopUpXpu.id -> listener.onOptionClickListener(SingerOption.TOP_UP)
            binding.btnBackUpKey.id -> listener.onOptionClickListener(SingerOption.BACKUP_KEY)
            binding.btnChangeCvc.id -> listener.onOptionClickListener(SingerOption.CHANGE_CVC)
            binding.btnRemoveKey.id -> listener.onOptionClickListener(SingerOption.REMOVE_KEY)
        }
        dismiss()
    }

    private fun registerEvents() {
        binding.btnBackUpKey.setOnClickListener(this)
        binding.btnTopUpXpu.setOnClickListener(this)
        binding.btnChangeCvc.setOnClickListener(this)
        binding.btnRemoveKey.setOnClickListener(this)
    }

    interface OptionClickListener {
        fun onOptionClickListener(option: SingerOption)
    }
}