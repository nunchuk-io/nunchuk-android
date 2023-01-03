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

package com.nunchuk.android.core.util

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.nunchuk.android.widget.NCToastMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector

fun <T> Fragment.flowObserver(flow: Flow<T>, collector: FlowCollector<T>) =
    viewLifecycleOwner.lifecycleScope.launchWhenStarted {
        flow.flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
            .collect(collector)
    }

fun Fragment.showError(message: String?) {
    if (message.isNullOrEmpty().not()) {
        NCToastMessage(requireActivity()).showError(message.orEmpty())
    }
}

fun Fragment.showSuccess(message: String?) {
    if (message.isNullOrEmpty().not()) {
        NCToastMessage(requireActivity()).show(message.orEmpty())
    }
}

fun Fragment.showWarning(message: String?) {
    if (message.isNullOrEmpty().not()) {
        NCToastMessage(requireActivity()).showWarning(message.orEmpty())
    }
}
