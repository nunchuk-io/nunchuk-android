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

package com.nunchuk.android.model.matrix

import android.net.Uri

sealed class BaseRoomMediaType

data class RoomImageType(
    val displayName: String?,
    val size: Long,
    val mimeType: String?,
    val contentUri: Uri,
    val width: Int,
    val height: Int,
    val orientation: Int
) : BaseRoomMediaType()

data class RoomVideoType(
    val displayName: String?,
    val size: Long,
    val mimeType: String?,
    val contentUri: Uri,
    val width: Int,
    val height: Int,
    val orientation: Int,
    val duration: Long
): BaseRoomMediaType()

data class AttachmentType(
    val displayName: String?,
    val size: Long,
    val mimeType: String?,
    val contentUri: Uri,
) : BaseRoomMediaType()