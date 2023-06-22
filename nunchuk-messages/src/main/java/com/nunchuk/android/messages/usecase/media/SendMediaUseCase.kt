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

package com.nunchuk.android.messages.usecase.media

import android.app.Application
import android.net.Uri
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.messages.components.detail.media.getSelectedMediaFiles
import com.nunchuk.android.messages.components.detail.media.toContentAttachmentData
import com.nunchuk.android.usecase.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import org.matrix.android.sdk.api.session.room.Room
import javax.inject.Inject

class SendMediaUseCase @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val application: Application,
) : UseCase<SendMediaUseCase.Data, Unit>(dispatcher) {

    override suspend fun execute(parameters: Data) {
        parameters.room.sendService().sendMedias(
            parameters.content.getSelectedMediaFiles(application).map { content -> content.toContentAttachmentData() },
            false,
            emptySet(),
        )
    }

    data class Data(
        val room: Room,
        val content: List<Uri>,
    )
}