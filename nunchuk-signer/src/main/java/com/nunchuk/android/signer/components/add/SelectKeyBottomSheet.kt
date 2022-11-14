/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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

package com.nunchuk.android.signer.components.add

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nunchuk.android.arch.args.FragmentArgs
import com.nunchuk.android.core.base.BaseBottomSheet
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.databinding.DialogSelectKeyBottomSheetBinding

class SelectKeyBottomSheet : BaseBottomSheet<DialogSelectKeyBottomSheetBinding>() {

    private lateinit var listener: (SingleSigner) -> Unit

    private val args: SelectKeyBottomSheetArgs by lazy {
        SelectKeyBottomSheetArgs.deserializeFrom(arguments, Gson())
    }

    override fun initializeBinding(inflater: LayoutInflater, container: ViewGroup?): DialogSelectKeyBottomSheetBinding {
        return DialogSelectKeyBottomSheetBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.let { disableBottomSheetDraggable(it as BottomSheetDialog) }
        setupViews()
    }

    private fun disableBottomSheetDraggable(bottomSheetDialog: BottomSheetDialog) {
        bottomSheetDialog.findViewById<View?>(R.id.design_bottom_sheet)?.let {
            val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(it)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.isDraggable = false
        }
    }

    private fun setupViews() {
        KeysViewBinder(binding.signerList, args.keys) {
            listener(it)
            dismiss()
        }.bindItems()
    }

    fun setListener(listener: (SingleSigner) -> Unit) {
        this.listener = listener
    }

    companion object {
        private const val TAG = "SelectKeyBottomSheet"

        private fun newInstance(keys: List<SingleSigner>) = SelectKeyBottomSheet().apply {
            arguments = SelectKeyBottomSheetArgs(keys).buildBundle()
        }

        fun show(fragmentManager: FragmentManager, keys: List<SingleSigner>): SelectKeyBottomSheet {
            return newInstance(keys).apply { show(fragmentManager, TAG) }
        }
    }
}

data class SelectKeyBottomSheetArgs(val keys: List<SingleSigner>) : FragmentArgs {
    private val gson = Gson()

    override fun buildBundle() = Bundle().apply {
        putString(EXTRA_KEYS, gson.toJson(keys))
    }

    companion object {
        private const val EXTRA_KEYS = "EXTRA_KEYS"

        fun deserializeFrom(data: Bundle?, gson: Gson) = SelectKeyBottomSheetArgs(
            data?.getString(EXTRA_KEYS).toKeys(gson)
        )
    }
}

internal fun String?.toKeys(gson: Gson): List<SingleSigner> {
    if (this.isNullOrEmpty()) return emptyList()
    return gson.fromJson(this, object : TypeToken<List<SingleSigner>>() {}.type)
}