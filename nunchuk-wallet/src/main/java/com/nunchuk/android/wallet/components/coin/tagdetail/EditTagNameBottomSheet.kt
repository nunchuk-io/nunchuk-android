package com.nunchuk.android.wallet.components.coin.tagdetail

import android.os.Bundle
import android.text.Selection
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import com.nunchuk.android.core.base.BaseBottomSheet
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.databinding.BottomSheetEditTagNameBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EditTagNameBottomSheet : BaseBottomSheet<BottomSheetEditTagNameBinding>() {

    private val viewModel: EditTagNameBottomSheetViewModel by viewModels()

    private val coinTag: CoinTag
        get() = arguments?.parcelable(ARG_COIN_TAG)!!

    private val walletId: String
        get() = arguments?.getString(ARG_WALLET_ID).orEmpty()

    var listener: (String) -> Unit = {}

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): BottomSheetEditTagNameBinding {
        return BottomSheetEditTagNameBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.setCanceledOnTouchOutside(false)
        setupViews()
        viewModel.init(walletId, coinTag)

        flowObserver(viewModel.event) { event ->
            when (event) {
                is EditTagNameBottomSheetEvent.UpdateTagNameSuccess -> {
                    listener(event.tagName)
                    dismissAllowingStateLoss()
                }

                EditTagNameBottomSheetEvent.ExistingTagNameError -> {
                    binding.errorText.text = getString(R.string.nc_tag_name_already_exists)
                }

                is EditTagNameBottomSheetEvent.Error -> showError(message = event.message)
            }
        }
    }

    private fun setupViews() {
        binding.edtName.setText(coinTag.name)
        binding.closeBtn.setOnClickListener {
            cleanUp()
        }
        binding.saveBtn.setOnClickListener {
            save()
        }
        binding.edtName.setText(coinTag.name)
        Selection.setSelection(binding.edtName.text, binding.edtName.text?.length ?: 0)
        binding.edtName.doAfterTextChanged {
            if (!it.toString().startsWith("#")) {
                binding.edtName.setText("#")
                Selection.setSelection(binding.edtName.text, binding.edtName.text?.length ?: 0)
            }
        }
    }

    private fun cleanUp() {
        binding.edtName.text?.clear()
        dismiss()
    }

    private fun save() {
        viewModel.onSaveClick(binding.edtName.text.toString().trim())
    }

    companion object {
        private const val TAG = "EditTagNameBottomSheet"
        private const val ARG_COIN_TAG = "ARG_COIN_TAG"
        private const val ARG_WALLET_ID = "ARG_WALLET_ID"
        fun show(coinTag: CoinTag, walletId: String, fragmentManager: FragmentManager) =
            EditTagNameBottomSheet().apply {
                arguments = bundleOf(ARG_COIN_TAG to coinTag, ARG_WALLET_ID to walletId)
                show(fragmentManager, TAG)
            }
    }
}