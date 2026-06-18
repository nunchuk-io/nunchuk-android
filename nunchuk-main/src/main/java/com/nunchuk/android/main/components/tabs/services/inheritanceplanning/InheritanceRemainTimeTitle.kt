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

package com.nunchuk.android.main.components.tabs.services.inheritanceplanning

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.res.stringResource
import com.nunchuk.android.core.util.InheritancePlanFlow
import com.nunchuk.android.main.R

/**
 * The inheritance plan flow ([InheritancePlanFlow]) the current navigation graph is running in.
 * Provided once at the graph root so any screen can adapt its UI without threading the flow through
 * every screen/navigation signature. Defaults to [InheritancePlanFlow.NONE] so screens rendered
 * outside the graph (e.g. previews) keep their default behavior.
 */
val LocalInheritancePlanFlow = staticCompositionLocalOf { InheritancePlanFlow.NONE }

/**
 * Title for inheritance flow top bars. The "Est. time remaining" estimate is only meaningful while
 * setting up a plan, so it is hidden when viewing an existing plan ([InheritancePlanFlow.VIEW]).
 */
@Composable
fun estimateRemainTimeTitle(remainTime: Int): String {
    return if (LocalInheritancePlanFlow.current == InheritancePlanFlow.VIEW) {
        ""
    } else {
        stringResource(id = R.string.nc_estimate_remain_time, remainTime)
    }
}
