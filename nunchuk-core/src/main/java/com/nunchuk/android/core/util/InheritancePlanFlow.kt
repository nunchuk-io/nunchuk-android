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

package com.nunchuk.android.core.util

import androidx.annotation.IntDef

object InheritancePlanFlow {

    const val NONE = 0
    const val SETUP = 1
    const val VIEW = 2
    const val CLAIM = 3
    const val SIGN_DUMMY_TX = 4

    @IntDef(
        NONE,
        SETUP,
        VIEW,
        CLAIM,
        SIGN_DUMMY_TX
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class InheritancePlanFlowInfo

}

object InheritanceSourceFlow {
    const val NONE = 0
    const val WIZARD = 1
    const val GROUP_DASHBOARD = 2

    @IntDef(
        NONE,
        WIZARD,
        GROUP_DASHBOARD,
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class InheritanceSourceFlowInfo
}