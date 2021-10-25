package com.nunchuk.android.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.nunchuk.android.core.base.BaseBottomSheet
import com.nunchuk.android.settings.databinding.BottomSheetEditNameBinding
import com.nunchuk.android.settings.databinding.BottomSheetEditUserPhotoBinding

internal class EditPhotoUserBottomSheet : BaseBottomSheet<BottomSheetEditUserPhotoBinding>() {

    lateinit var listener: (EditPhotoOption) -> Unit

    override fun initializeBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetEditUserPhotoBinding {
        return BottomSheetEditUserPhotoBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
    }

    private fun setupViews() {
        binding.btnChooseAlbum.setOnClickListener { onSaveClicked(EditPhotoOption.SelectAlbum) }
        binding.btnTakePhoto.setOnClickListener { onSaveClicked(EditPhotoOption.TakePhoto) }
        binding.btnRemovePhoto.setOnClickListener { onSaveClicked(EditPhotoOption.RemovePhoto) }
    }

    private fun onSaveClicked(option: EditPhotoOption) {
        listener(option)
        dismiss()
    }

    companion object {
        private const val TAG = "WalletUpdateBottomSheet"

        fun show(fragmentManager: FragmentManager): EditPhotoUserBottomSheet {
            return EditPhotoUserBottomSheet().apply { show(fragmentManager, TAG) }
        }
    }
}

sealed class EditPhotoOption {
    object SelectAlbum: EditPhotoOption()
    object TakePhoto: EditPhotoOption()
    object RemovePhoto: EditPhotoOption()
}


