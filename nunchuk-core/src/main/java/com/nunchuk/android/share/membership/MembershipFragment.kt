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

package com.nunchuk.android.share.membership

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.nunchuk.android.core.R
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.widget.NCInfoDialog
import com.nunchuk.android.widget.NCWarningDialog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
abstract class MembershipFragment : Fragment(), BottomSheetOptionListener {
    @Inject
    lateinit var membershipStepManager: MembershipStepManager

    @Inject
    lateinit var nunchukNavigator: NunchukNavigator

    private val viewModel : MembershipViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isCountdown) {
            membershipStepManager.updateStep(true)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flowObserver(viewModel.event) {
            if (it is MembershipEvent.RestartWizardSuccess) {
                nunchukNavigator.openMembershipActivity(
                    requireActivity(),
                    MembershipStage.NONE
                )
                requireActivity().setResult(Activity.RESULT_OK)
                requireActivity().finish()
            }
        }
    }

    @CallSuper
    override fun onOptionClicked(option: SheetOption) {
        if (option.type == SheetOptionType.TYPE_RESTART_WIZARD) {
            NCWarningDialog(requireActivity()).showDialog(
                title = getString(R.string.nc_confirmation),
                message = getString(R.string.nc_confirm_restart_wizard),
                onYesClick = {
                    resetWizard()
                }
            )
        } else if (option.type == SheetOptionType.TYPE_EXIT_WIZARD) {
            NCInfoDialog(requireActivity()).showDialog(
                message = getString(R.string.nc_resume_wizard_desc),
                onYesClick = {
                    requireActivity().finish()
                }
            )
        }
    }

    protected fun handleShowMore() {
        BottomSheetOption.newInstance(
            listOf(
                SheetOption(
                    type = SheetOptionType.TYPE_RESTART_WIZARD,
                    label = getString(R.string.nc_restart_wizard)
                ),
                SheetOption(
                    type = SheetOptionType.TYPE_EXIT_WIZARD,
                    label = getString(R.string.nc_exit_wizard)
                )
            )
        ).show(childFragmentManager, "BottomSheetOption")
    }

    private fun resetWizard() {
        viewModel.resetWizard(membershipStepManager.plan)
    }

    override fun onDestroy() {
        if (isCountdown) {
            membershipStepManager.updateStep(false)
        }
        super.onDestroy()
    }

    open val isCountdown: Boolean = true
}