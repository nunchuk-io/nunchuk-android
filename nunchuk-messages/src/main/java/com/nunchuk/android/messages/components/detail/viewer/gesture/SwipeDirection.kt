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

package com.nunchuk.android.messages.components.detail.viewer.gesture

sealed class SwipeDirection {
    object NotDetected : SwipeDirection()
    object Up : SwipeDirection()
    object Down : SwipeDirection()
    object Left : SwipeDirection()
    object Right : SwipeDirection()

    companion object {
        fun fromAngle(angle: Double): SwipeDirection {
            return when (angle) {
                in 0.0..45.0 -> Right
                in 45.0..135.0 -> Up
                in 135.0..225.0 -> Left
                in 225.0..315.0 -> Down
                in 315.0..360.0 -> Right
                else -> NotDetected
            }
        }
    }
}
