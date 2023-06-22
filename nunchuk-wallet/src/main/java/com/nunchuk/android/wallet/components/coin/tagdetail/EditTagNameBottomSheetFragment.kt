/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.wallet.components.coin.tagdetail

import android.content.Context
import android.os.Bundle
import android.text.InputFilter
import android.text.Selection
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.core.base.BaseBottomSheet
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.showError
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.components.coin.list.CoinListViewModel
import com.nunchuk.android.wallet.components.coin.tag.CoinTagSelectColorBottomSheetFragment
import com.nunchuk.android.wallet.components.coin.tag.CoinTagSelectColorBottomSheetFragmentArgs
import com.nunchuk.android.wallet.databinding.BottomSheetEditTagNameBinding
import com.nunchuk.android.widget.util.setMaxLength
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EditTagNameBottomSheetFragment : BaseBottomSheet<BottomSheetEditTagNameBinding>() {

    private val viewModel: EditTagNameBottomSheetViewModel by viewModels()
    private val args: EditTagNameBottomSheetFragmentArgs by navArgs()
    private val coinListViewModel: CoinListViewModel by activityViewModels()

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

        flowObserver(viewModel.event) { event ->
            when (event) {
                is EditTagNameBottomSheetEvent.UpdateTagNameSuccess -> {
                    setFragmentResult(
                        REQUEST_KEY,
                        bundleOf(EXTRA_COIN_TAG_NAME to event.tagName)
                    )
                    dismissAllowingStateLoss()
                }

                EditTagNameBottomSheetEvent.ExistingTagNameError -> {
                    binding.errorText.text = getString(R.string.nc_tag_name_already_exists)
                    binding.errorText.isVisible = true
                    val inputMethodManager = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
                }

                is EditTagNameBottomSheetEvent.Error -> showError(message = event.message)
            }
        }

        flowObserver(coinListViewModel.state) { coinListState ->
            viewModel.setTags(coinListState.tags.values.toList())
        }
    }

    private fun setupViews() {
        binding.edtName.setText(args.coinTag.name)
        val filter = InputFilter { source, _, _, _, _, _ ->
            source.filterNot { it.isWhitespace() }
        }
        binding.edtName.filters = arrayOf(filter)
        binding.edtName.setMaxLength(40)
        binding.closeBtn.setOnClickListener {
            cleanUp()
        }
        binding.saveBtn.setOnClickListener {
            save()
        }
        binding.edtName.setText(args.coinTag.name)
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
        const val REQUEST_KEY = "EditTagNameBottomSheetFragment"
        const val EXTRA_COIN_TAG_NAME = "EXTRA_COIN_TAG_NAME"
    }
}