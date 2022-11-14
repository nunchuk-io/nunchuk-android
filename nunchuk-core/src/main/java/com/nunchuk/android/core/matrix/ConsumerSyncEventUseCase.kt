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

package com.nunchuk.android.core.matrix

import com.nunchuk.android.core.repository.SyncEventRepository
import com.nunchuk.android.model.NunchukMatrixEvent
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.utils.CrashlyticsReporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import timber.log.Timber
import javax.inject.Inject

interface ConsumerSyncEventUseCase {
    fun execute(events: List<NunchukMatrixEvent>): Flow<Unit>
}

internal class ConsumerSyncEventUseCaseImpl @Inject constructor(
    private val nativeSdk: NunchukNativeSdk,
    private val repository: SyncEventRepository,
) : ConsumerSyncEventUseCase {

    override fun execute(events: List<NunchukMatrixEvent>) = flow<Unit> {
        events.forEach {
            try {
                if (repository.isSync(it.eventId).not()) {
                    Timber.d("Consume event ${it.eventId}")
                    repository.save(it.eventId)
                    nativeSdk.consumeSyncEvent(it)
                }
            } catch (t: Throwable) {
                CrashlyticsReporter.recordException(t)
            }
        }
    }.flowOn(Dispatchers.IO)

}