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

package com.nunchuk.android.messages.components.create

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nunchuk.android.core.base.BaseBottomSheet
import com.nunchuk.android.core.util.hideLoading
import com.nunchuk.android.core.util.showLoading
import com.nunchuk.android.messages.R
import com.nunchuk.android.messages.components.create.CreateRoomEvent.*
import com.nunchuk.android.messages.databinding.BottomSheetCreateRoomBinding
import com.nunchuk.android.model.Contact
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.addTextChangedCallback
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CreateRoomBottomSheet : BaseBottomSheet<BottomSheetCreateRoomBinding>() {

    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel: CreateRoomViewModel by viewModels()

    private lateinit var adapter: ContactsAdapter

    override fun initializeBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetCreateRoomBinding {
        return BottomSheetCreateRoomBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.setCanceledOnTouchOutside(false)
        setupViews()
        observeEvent()
    }

    private fun observeEvent() {
        viewModel.state.observe(this, ::handleState)
        viewModel.event.observe(this, ::handleEvent)
    }

    private fun handleState(state: CreateRoomState) {
        bindReceiptList(state.receipts)
        bindContactList(state.suggestions)
    }

    private fun bindContactList(suggestions: List<Contact>) {
        adapter.items = suggestions
    }

    private fun bindReceiptList(receipts: List<Contact>) {
        if (receipts.isEmpty()) {
            binding.doneBtn.isVisible = false
            binding.receipts.removeAllViews()
            binding.receipts.isVisible = false
        } else {
            binding.doneBtn.isVisible = true
            binding.receipts.isVisible = true
            ReceiptsViewBinder(binding.receipts, receipts, viewModel::handleRemove).bindItems()
        }
    }

    private fun handleEvent(event: CreateRoomEvent) {
        when (event) {
            NoContactsEvent -> showNoContactsError()
            is CreateRoomSuccessEvent -> onRoomCreated(event.roomId)
            is CreateRoomErrorEvent -> showRoomCreateError(event.message)
        }
    }

    private fun onRoomCreated(roomId: String) {
        hideLoading()
        cleanUp()
        navigator.openRoomDetailActivity(requireActivity(), roomId = roomId)
    }

    private fun showRoomCreateError(message: String) {
        hideLoading()
        NCToastMessage(requireActivity()).showWarning(message)
    }

    private fun showNoContactsError() {
        NCToastMessage(requireActivity()).show(getString(R.string.nc_message_empty_contacts))
        cleanUp()
    }

    private fun setupViews() {
        adapter = ContactsAdapter {
            viewModel.handleSelectContact(it)
            binding.input.setText("")
        }

        binding.contactList.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        binding.contactList.adapter = adapter

        binding.closeBtn.setOnClickListener {
            cleanUp()
        }
        binding.input.addTextChangedCallback(viewModel::handleInput)
        binding.doneBtn.setOnClickListener {
            showLoading()
            viewModel.handleDone()
        }
    }

    private fun cleanUp() {
        viewModel.cleanUp()
        binding.input.setText("")
        adapter.items = emptyList()
        binding.receipts.removeAllViews()
        dismiss()
    }

    companion object {
        private const val TAG = "CreateRoomBottomSheet"
        fun show(fragmentManager: FragmentManager) = CreateRoomBottomSheet().apply {
            show(fragmentManager, TAG)
        }
    }

}