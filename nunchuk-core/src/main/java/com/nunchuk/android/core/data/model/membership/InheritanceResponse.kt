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

package com.nunchuk.android.core.data.model.membership

import com.google.gson.annotations.SerializedName
import com.nunchuk.android.core.data.model.byzantine.DummyTransactionDto

class InheritanceResponse(
    @SerializedName("inheritance")
    val inheritance: InheritanceDto? = null,
    @SerializedName("dummy_transaction")
    val dummyTransaction: DummyTransactionDto? = null
)

class InheritanceDto(
    @SerializedName("wallet_id")
    val walletId: String? = null,
    @SerializedName("wallet_local_id")
    val walletLocalId: String? = null,
    @SerializedName("magic")
    val magic: String? = null,
    @SerializedName("note")
    val note: String? = null,
    @SerializedName("notification_emails")
    val notificationEmails: List<String>? = null,
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("activation_time_milis")
    val activationTimeMilis: Long? = null,
    @SerializedName("created_time_milis")
    val createdTimeMilis: Long? = null,
    @SerializedName("last_modified_time_milis")
    val lastModifiedTimeMilis: Long? = null,
    @SerializedName("buffer_period")
    val bufferPeriod: PeriodResponse.Data? = null,
    @SerializedName("owner_id")
    val ownerId: String? = null,
)