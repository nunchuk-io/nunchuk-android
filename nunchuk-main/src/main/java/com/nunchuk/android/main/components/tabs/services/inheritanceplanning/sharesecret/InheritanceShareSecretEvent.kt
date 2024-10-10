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

package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.sharesecret

import androidx.annotation.Keep
import com.nunchuk.android.main.R

sealed class InheritanceShareSecretEvent {
    data class ContinueClick(val type: Int) : InheritanceShareSecretEvent()
}

data class InheritanceShareSecretState(
    val options: List<InheritanceOption> = initOptions()
)

internal fun initOptions(): List<InheritanceOption> {
    val options = mutableListOf<InheritanceOption>().apply {
        add(
            InheritanceOption(
                type = InheritanceShareSecretType.DIRECT.ordinal,
                title = R.string.nc_direct_inheritance,
                desc = R.string.nc_direct_inheritance_desc,
                isSelected = true
            )
        )
        add(
            InheritanceOption(
                type = InheritanceShareSecretType.INDIRECT.ordinal,
                title = R.string.nc_indirect_inheritance,
                desc = R.string.nc_indirect_inheritance_desc,
                isSelected = false
            )
        )
        add(
            InheritanceOption(
                type = InheritanceShareSecretType.JOINT_CONTROL.ordinal,
                title = R.string.nc_joint_control,
                desc = R.string.nc_joint_control_desc,
                isSelected = false
            )
        )
    }
    return options
}

data class InheritanceOption(val type: Int, val title: Int, val desc: Int, val isSelected: Boolean)

@Keep
enum class InheritanceShareSecretType {
    DIRECT, INDIRECT, JOINT_CONTROL
}