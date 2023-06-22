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

package com.nunchuk.android.contact.components.add

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import com.nunchuk.android.contact.R
import com.nunchuk.android.contact.components.add.AddContactsEvent.*
import com.nunchuk.android.contact.databinding.BottomSheetAddContactsBinding
import com.nunchuk.android.core.base.BaseBottomSheet
import com.nunchuk.android.core.util.showOrHideLoading
import com.nunchuk.android.utils.EmailValidator
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setOnEnterOrSpaceListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddContactsBottomSheet : BaseBottomSheet<BottomSheetAddContactsBinding>() {

    var listener: () -> Unit = {}

    private val viewModel: AddContactsViewModel by viewModels()

    override fun initializeBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetAddContactsBinding {
        return BottomSheetAddContactsBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.setCanceledOnTouchOutside(false)
        setupViews()
        observeEvent()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        cleanUp()
    }

    private fun observeEvent() {
        viewModel.state.observe(this, ::handleState)
        viewModel.event.observe(this, ::handleEvent)
    }

    private fun handleState(state: AddContactsState) {
        bindEmailList(state.emails)
    }

    private fun bindEmailList(emails: List<EmailWithState>) {
        if (emails.isEmpty()) {
            binding.emails.removeAllViews()
        } else {
            EmailsViewBinder(binding.emails, emails, viewModel::handleRemove).bindItems()
        }
        val isHasErrorEmail = emails.any { it.valid.not() && EmailValidator.valid(it.email) }
        val isHasErrorUserName = emails.any { it.valid.not() && !EmailValidator.valid(it.email) }
        binding.tvErrorEmail.isVisible = isHasErrorEmail
        binding.tvErrorUserName.isVisible = isHasErrorUserName
    }

    private fun handleEvent(event: AddContactsEvent) {
        showOrHideLoading(event is LoadingEvent)
        when (event) {
            InviteFriendSuccessEvent -> cleanUp()
            InvalidEmailEvent -> showErrorMessage(true)
            AllEmailValidEvent -> showErrorMessage(false)
            AddContactSuccessEvent -> showAddContactSuccess()
            is AddContactsErrorEvent -> showAddContactError(event.message)
            is FailedSendEmailsEvent -> showDialogConfirmationEmailInvitation(event.emailsAndUserNames)
            else -> {}
        }
    }

    private fun showAddContactError(message: String) {
        if (message.isNotEmpty()) {
            NCToastMessage(requireActivity()).showError(message)
        }
        cleanUp()
    }

    private fun showAddContactSuccess() {
        NCToastMessage(requireActivity()).showMessage(getString(R.string.nc_contact_add_contact_success))
        cleanUp()
    }

    private fun showErrorMessage(show: Boolean) {
        binding.tvErrorEmail.isVisible = show
    }

    private fun setupViews() {
        binding.input.setOnEnterOrSpaceListener {
            viewModel.handleAddEmail(binding.input.text.toString())
            binding.input.setText("")
        }
        binding.input.addTextChangedListener { text ->
            val currentText = text.toString()
            if (currentText.isNotEmpty() && (currentText.last() == ',' || currentText.last() == ' ')) {
                viewModel.handleAddEmail(currentText.dropLast(1))
                binding.input.setText("")
            }
        }
        binding.closeBtn.setOnClickListener {
            cleanUp()
        }

        binding.sendBtn.setOnClickListener {
            val currentText = binding.input.text.toString().trim()
            if (currentText.isNotEmpty()) {
                viewModel.handleAddEmail(currentText)
                binding.input.setText("")
            }

            viewModel.handleSend()
        }
    }

    private fun cleanUp() {
        viewModel.cleanUp()
        binding.input.setText("")
        binding.emails.removeAllViews()
        listener()
        dialog?.dismiss()
        dismiss()
    }

    private fun showDialogConfirmationEmailInvitation(emailsAndUserNames: List<String>) {
        val emails = emailsAndUserNames.filter(EmailValidator::valid)
        if (emails.isNotEmpty()) {
            NCInviteFriendDialog(requireActivity()).showDialog(
                inviteList = emails.joinToString(),
                onYesClick = {
                    viewModel.inviteFriend(emails)
                }
            )
        }
    }

    companion object {
        private const val TAG = "AddContactsBottomSheet"
        fun show(fragmentManager: FragmentManager) = AddContactsBottomSheet().apply {
            show(fragmentManager, TAG)
        }
    }

}
