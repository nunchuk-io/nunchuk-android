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

package com.nunchuk.android.widget

import android.app.Activity
import android.app.Dialog
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.Window
import com.nunchuk.android.widget.databinding.NcProgressDialogBinding
import com.nunchuk.android.widget.util.setOnDebounceClickListener
import javax.inject.Inject

class NCProgressDialog @Inject constructor(
    private val activity: Activity
) {
    val binding = NcProgressDialogBinding.inflate(LayoutInflater.from(activity))

    var dialog: Dialog? = null

    fun showDialog(
        title: String = activity.getString(R.string.nc_text_processing),
        btnCancel: String = activity.getString(R.string.nc_text_cancel),
        currentStep: Int,
        totalSteps: Int,
        onCancelClick: () -> Unit = {},
        cancelable: Boolean = false
    ) {

        dialog = Dialog(activity).apply {
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCancelable(cancelable)
            setCanceledOnTouchOutside(cancelable)
            setContentView(binding.root)

            binding.title.text = title
            binding.btnCancel.text = btnCancel
            binding.tvStep.text = "$currentStep/$totalSteps"
            binding.progressBar.max = totalSteps
            binding.progressBar.progress = currentStep
            binding.tvPercentage.text = "${(currentStep * 100) / totalSteps}%"

            binding.btnCancel.setOnDebounceClickListener {
                onCancelClick()
                dismiss()
            }
            show()
            window?.setLayout(MATCH_PARENT, MATCH_PARENT)
        }
    }

    fun updateProgress(currentStep: Int, totalSteps: Int) {
        binding.tvStep.text = "$currentStep/$totalSteps"
        binding.progressBar.progress = currentStep
        binding.tvPercentage.text = "${(currentStep * 100) / totalSteps}%"
    }
}