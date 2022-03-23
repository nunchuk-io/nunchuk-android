package com.nunchuk.android.settings.devices

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.nunchuk.android.arch.args.FragmentArgs
import com.nunchuk.android.core.base.BaseBottomSheet
import com.nunchuk.android.core.util.getHtmlText
import com.nunchuk.android.settings.R
import com.nunchuk.android.settings.databinding.BottomSheetModifyUserDeviceBinding

internal class ModifyUserDeviceBottomSheet : BaseBottomSheet<BottomSheetModifyUserDeviceBinding>() {

    lateinit var listener: (ModifyDeviceOption) -> Unit

    private val args: ModifyUserDeviceBottomSheetArgs by lazy { ModifyUserDeviceBottomSheetArgs.deserializeFrom(arguments) }

    override fun initializeBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetModifyUserDeviceBinding {
        return BottomSheetModifyUserDeviceBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
    }

    private fun setupViews() {
        binding.deviceInfo.text = args.deviceName
        binding.signOut.text = requireActivity().getHtmlText(R.string.nc_user_device_bottom_sheet_sign_out, args.deviceName)
        binding.markCompromise.text = requireActivity().getHtmlText(R.string.nc_user_device_bottom_sheet_mark_compromise, args.deviceName)
        binding.signOut.setOnClickListener { onSaveClicked(ModifyDeviceOption.SignOut) }
        binding.markCompromise.setOnClickListener { onSaveClicked(ModifyDeviceOption.MarkCompromise) }
    }

    private fun onSaveClicked(option: ModifyDeviceOption) {
        listener(option)
        dismiss()
    }

    companion object {
        private const val TAG = "ModifyUserDeviceBottomSheet.kt"

        private fun newInstance(deviceName: String) = ModifyUserDeviceBottomSheet().apply {
            arguments = ModifyUserDeviceBottomSheetArgs(deviceName).buildBundle()
        }

        fun show(fragmentManager: FragmentManager, deviceName: String): ModifyUserDeviceBottomSheet {
            return newInstance(deviceName).apply { show(fragmentManager, TAG) }
        }

    }
}

sealed class ModifyDeviceOption {
    object SignOut : ModifyDeviceOption()
    object MarkCompromise : ModifyDeviceOption()
}

data class ModifyUserDeviceBottomSheetArgs(val deviceName: String) : FragmentArgs {

    override fun buildBundle() = Bundle().apply {
        putString(EXTRA_DEVICE_NAME, deviceName)
    }

    companion object {
        private const val EXTRA_DEVICE_NAME = "EXTRA_DEVICE_NAME"

        fun deserializeFrom(data: Bundle?) = ModifyUserDeviceBottomSheetArgs(
            data?.getString(EXTRA_DEVICE_NAME).orEmpty()
        )
    }
}