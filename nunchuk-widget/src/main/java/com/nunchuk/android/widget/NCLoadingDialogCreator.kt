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

import android.app.Dialog
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.lifecycle.lifecycleScope
import com.nunchuk.android.widget.databinding.NcInfoLoadingDialogBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

class NCLoadingDialogCreator(val activity: AppCompatActivity) {
    val dialog = Dialog(activity)
    val binding = NcInfoLoadingDialogBinding.inflate(LayoutInflater.from(activity))
    var showJob: Job? = null

    init {
        dialog.apply {
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(binding.root)
            window?.attributes?.windowAnimations = R.style.NCAnimatedDialog
            window?.setLayout(MATCH_PARENT, MATCH_PARENT)
        }
    }

    fun showDialog(cancelable: Boolean = true, title: String?, message: String?) {
        showJob?.cancel()
        showJob = activity.lifecycleScope.launch {
            delay(150)
            ensureActive()
            binding.title.text = title
            binding.message.isGone = message.isNullOrEmpty()
            binding.message.text = message
            dialog.setCancelable(cancelable)
            dialog.show()
        }
    }

    fun cancel() {
        showJob?.cancel()
        dialog.cancel()
    }
}
