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

package com.nunchuk.android.core.matrix

import com.nunchuk.android.core.network.UnauthorizedException
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.messages.components.list.isServerNotices
import com.nunchuk.android.usecase.contact.GetContactByChatIdUseCase
import com.nunchuk.android.utils.CrashlyticsReporter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.failure.GlobalError
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.statistics.StatisticEvent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionListener @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val getContactByChatIdUseCase: GetContactByChatIdUseCase,
) : Session.Listener {
    override fun onGlobalError(session: Session, globalError: GlobalError) {
        if (globalError is GlobalError.InvalidToken || globalError === GlobalError.ExpiredAccount) {
            CrashlyticsReporter.recordException(UnauthorizedException("Matrix unauthorized"))
        }
    }

    override fun onNewInvitedRoom(session: Session, roomId: String) {
        session.coroutineScope.launch(dispatcher) {
            val summary = session.roomService().getRoom(roomId)?.roomSummary() ?: return@launch
            if (summary.isServerNotices()) {
                runCatching {
                    session.roomService().joinRoom(roomId)
                }
                return@launch
            }
            val chatId = summary.inviterId ?: return@launch
            val result = getContactByChatIdUseCase(chatId)
            if (result.isSuccess && result.getOrThrow() != null) {
                runCatching {
                    session.roomService().joinRoom(roomId)
                }
            }
        }
    }

    override fun onStatisticsEvent(session: Session, statisticEvent: StatisticEvent) {}

    override fun onSessionStopped(session: Session) {
        session.coroutineScope.coroutineContext.cancelChildren()
    }

    override fun onClearCache(session: Session) {
        session.coroutineScope.coroutineContext.cancelChildren()
    }
}
