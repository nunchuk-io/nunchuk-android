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

package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.notifypref

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.contact.components.add.EmailWithState
import com.nunchuk.android.contact.components.add.EmailsViewBinder
import com.nunchuk.android.core.base.BaseFragment
import com.nunchuk.android.core.util.InheritancePlanFlow
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningViewModel
import com.nunchuk.android.main.databinding.FragmentInheritanceNotifyPrefBinding
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.utils.EmailValidator
import com.nunchuk.android.widget.util.setOnEnterOrSpaceListener
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class InheritanceNotifyPrefFragment : BaseFragment<FragmentInheritanceNotifyPrefBinding>() {

    @Inject
    lateinit var membershipStepManager: MembershipStepManager

    private val viewModel: InheritanceNotifyPrefViewModel by viewModels()
    private val args: InheritanceNotifyPrefFragmentArgs by navArgs()
    private val inheritanceViewModel: InheritancePlanningViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        membershipStepManager.updateStep(true)
    }

    override fun initializeBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentInheritanceNotifyPrefBinding =
        FragmentInheritanceNotifyPrefBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        viewModel.init(inheritanceViewModel.setupOrReviewParam)
        flowObserver(viewModel.event) { event ->
            when (event) {
                is InheritanceNotifyPrefEvent.ContinueClick -> openReviewPlanScreen(
                    isDiscard = false,
                    event.emails,
                    event.isNotify
                )
                InheritanceNotifyPrefEvent.AllEmailValidEvent -> showErrorMessage(show = false)
                InheritanceNotifyPrefEvent.InvalidEmailEvent -> showErrorMessage(show = true, isEmptyError = false)
                InheritanceNotifyPrefEvent.EmptyEmailError -> showErrorMessage(show = true, isEmptyError = true)
            }
        }
        flowObserver(viewModel.state) { state ->
            bindEmailList(state.emails)
            binding.notifyCheck.isChecked = state.isNotify
        }
    }

    private fun openReviewPlanScreen(isDiscard: Boolean, emails: List<String>, isNotify: Boolean) {
        inheritanceViewModel.setOrUpdate(inheritanceViewModel.setupOrReviewParam.copy(isNotify = isNotify, emails = emails))
        if (args.isUpdateRequest || inheritanceViewModel.setupOrReviewParam.planFlow == InheritancePlanFlow.VIEW) {
            if (isDiscard.not()) {
                setFragmentResult(
                    REQUEST_KEY,
                    bundleOf(
                        EXTRA_IS_NOTIFY to isNotify,
                        EXTRA_EMAILS to emails
                    )
                )
            }
            findNavController().popBackStack()
        } else {
            findNavController().navigate(
                InheritanceNotifyPrefFragmentDirections.actionInheritanceNotifyPrefFragmentToInheritanceReviewPlanFragment()
            )
        }
    }

    private fun showErrorMessage(show: Boolean, isEmptyError: Boolean = false) {
        binding.tvError.isVisible = show
        if (isEmptyError) {
            binding.inputLayout.background = ResourcesCompat.getDrawable(resources, R.drawable.nc_edit_text_error_bg, null)
            binding.tvError.text = getString(R.string.nc_please_provide_valid_email_address)
        }else {
            binding.inputLayout.background = ResourcesCompat.getDrawable(resources, R.drawable.view_add_contact_background, null)
            binding.tvError.text = getString(R.string.nc_contact_valid_email_address)
        }
    }

    private fun setupViews() {
        binding.toolbarTitle.text = if (isSetupFlow()) String.format(
            getString(R.string.nc_estimate_remain_time),
            viewModel.remainTime.value
        ) else ""
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
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
        binding.notifyCheck.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateIsNotify(isChecked)
        }
        binding.btnNotification.setOnClickListener {
            openReviewPlanScreen(isDiscard = true, emptyList(), false)
        }
        val continueBtnText =
            if (isSetupFlow()) getText(R.string.nc_text_continue) else getText(R.string.nc_update_notification_preferences)
        binding.btnContinue.text = continueBtnText
        binding.btnContinue.setOnClickListener {
            val currentText = binding.input.text.toString().trim()
            if (currentText.isNotEmpty()) {
                viewModel.handleAddEmail(currentText)
                binding.input.setText("")
            }
            viewModel.onContinueClicked()
        }
    }

    private fun isSetupFlow() =
        inheritanceViewModel.setupOrReviewParam.planFlow == InheritancePlanFlow.SETUP && args.isUpdateRequest.not()

    private fun bindEmailList(emails: List<EmailWithState>) {
        if (emails.isEmpty()) {
            binding.emails.removeAllViews()
        } else {
            EmailsViewBinder(binding.emails, emails, viewModel::handleRemove).bindItems()
        }
        val isError = emails.any { it.valid.not() || EmailValidator.valid(it.email).not() }
        showErrorMessage(show = isError, isEmptyError = false)
    }

    override fun onDestroy() {
        membershipStepManager.updateStep(false)
        super.onDestroy()
    }

    companion object {
        const val REQUEST_KEY = "InheritanceNotifyPrefFragment"
        const val EXTRA_IS_NOTIFY = "EXTRA_IS_NOTIFY"
        const val EXTRA_EMAILS = "EXTRA_EMAILS"
    }
}
