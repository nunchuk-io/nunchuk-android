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

package com.nunchuk.android.messages.components.detail.viewer

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.messages.components.detail.NunchukMedia
import com.nunchuk.android.messages.util.downloadFile
import com.nunchuk.android.messages.util.saveMedia
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoomMediaViewerViewModel @Inject constructor(
    private val sessionHolder: SessionHolder,
    private val application: Application,
) : ViewModel() {
    private val _event = MutableSharedFlow<RoomMediaViewerEvent>()
    val event = _event.asSharedFlow()

    fun download(data: NunchukMedia) {
        val fileService = sessionHolder.getSafeActiveSession()?.fileService() ?: return
        viewModelScope.launch {
            _event.emit(RoomMediaViewerEvent.Loading(true))
            val result = runCatching {
                val file = fileService.downloadFile(data)
                saveMedia(
                    context = application,
                    file = file,
                    title = data.filename,
                    mediaMimeType = data.mimeType,
                )
            }
            if (result.isSuccess) {
                _event.emit(RoomMediaViewerEvent.DownloadFileSuccess)
            } else {
                _event.emit(RoomMediaViewerEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
            }
            _event.emit(RoomMediaViewerEvent.Loading(false))
        }
    }
}

sealed class RoomMediaViewerEvent {
    object DownloadFileSuccess : RoomMediaViewerEvent()
    data class ShowError(val message: String?) : RoomMediaViewerEvent()
    data class Loading(val isLoading: Boolean,) : RoomMediaViewerEvent()
}