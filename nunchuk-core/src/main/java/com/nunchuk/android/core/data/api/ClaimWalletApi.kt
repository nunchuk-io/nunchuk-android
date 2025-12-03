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

package com.nunchuk.android.core.data.api

import com.nunchuk.android.core.data.model.UpdateWalletPayload
import com.nunchuk.android.core.data.model.membership.CreateOrUpdateServerTransactionRequest
import com.nunchuk.android.core.data.model.membership.CreateOrUpdateWalletResponse
import com.nunchuk.android.core.data.model.membership.GetWalletsResponse
import com.nunchuk.android.core.data.model.membership.TransactionResponse
import com.nunchuk.android.core.data.model.membership.TransactionsResponse
import com.nunchuk.android.core.network.Data
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

internal interface ClaimWalletApi {
    @GET("/v1.1/user-wallets/claiming-wallets")
    suspend fun getClaimingWallets(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
        @Query("statuses") statuses: List<String>,
    ): Data<GetWalletsResponse>

    @GET("/v1.1/user-wallets/claiming-wallets/{local_id}")
    suspend fun getClaimingWallet(
        @Path("local_id") localId: String
    ): Data<CreateOrUpdateWalletResponse>

    @PUT("/v1.1/user-wallets/claiming-wallets/{local_id}")
    suspend fun updateClaimingWallet(
        @Path("local_id") localId: String,
        @Body payload: UpdateWalletPayload
    ): Data<CreateOrUpdateWalletResponse>

    @DELETE("/v1.1/user-wallets/claiming-wallets/{local_id}")
    suspend fun deleteClaimingWallet(
        @Path("local_id") localId: String
    ): Data<Unit>

    @GET("/v1.1/user-wallets/claiming-wallets/{local_id}/transactions")
    suspend fun getClaimingWalletTransactions(
        @Path("local_id") localId: String,
        @Query("offset") offset: Int,
        @Query("limit") limit: Int = TRANSACTION_PAGE_COUNT,
    ): Data<TransactionsResponse>

    @GET("/v1.1/user-wallets/claiming-wallets/{local_id}/transactions/{transaction_id}")
    suspend fun getClaimingWalletTransaction(
        @Path("local_id") localId: String,
        @Path("transaction_id") transactionId: String,
    ): Data<TransactionResponse>

    @POST("/v1.1/user-wallets/claiming-wallets/{local_id}/transactions")
    suspend fun createClaimingWalletTransaction(
        @Path("local_id") localId: String,
        @Body payload: CreateOrUpdateServerTransactionRequest
    ): Data<TransactionResponse>

    @PUT("/v1.1/user-wallets/claiming-wallets/{local_id}/transactions/{transaction_id}/rbf")
    suspend fun replaceClaimingWalletTransaction(
        @Path("local_id") localId: String,
        @Path("transaction_id") transactionId: String,
        @Body payload: CreateOrUpdateServerTransactionRequest
    ): Data<TransactionResponse>
}

