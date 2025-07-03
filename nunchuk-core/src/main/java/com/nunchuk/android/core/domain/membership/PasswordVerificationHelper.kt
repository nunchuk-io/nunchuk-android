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

package com.nunchuk.android.core.domain.membership

import android.content.Context
import com.nunchuk.android.core.R
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.widget.NCInputDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PasswordVerificationHelper @Inject constructor(
    private val verifiedPasswordTokenUseCase: VerifiedPasswordTokenUseCase,
    private val accountManager: AccountManager,
    private val requestFederatedTokenUseCase: RequestFederatedTokenUseCase,
    private val verifyFederatedTokenUseCase: VerifyFederatedTokenUseCase
) {

    /**
     * Shows password input dialog and handles verification flow
     * @param context The context to show dialog in
     * @param targetAction The target action for password verification
     * @param coroutineScope The coroutine scope to launch verification in
     * @param onSuccess Callback when password verification succeeds with the token
     * @param onError Callback when password verification fails with error message
     * @param onCancel Optional callback when user cancels the dialog
     */
    fun showPasswordVerificationDialog(
        context: Context,
        targetAction: TargetAction,
        coroutineScope: CoroutineScope,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
        onCancel: () -> Unit = {}
    ) {
        val account = accountManager.getAccount()
        
        if (account.loginTypeOriginal == SignInMode.OPENID.value) {
            // OPENID flow: Request federated token first, then show confirmation code dialog
            showConfirmationCodeFlow(
                context = context,
                targetAction = targetAction,
                coroutineScope = coroutineScope,
                onSuccess = onSuccess,
                onError = onError,
                onCancel = onCancel
            )
        } else {
            // Regular password flow for non-OPENID users
            NCInputDialog(context).showDialog(
                title = context.getString(R.string.nc_re_enter_your_password),
                descMessage = context.getString(R.string.nc_re_enter_your_password_dialog_desc),
                onConfirmed = { password ->
                    verifyPassword(
                        password = password,
                        targetAction = targetAction,
                        coroutineScope = coroutineScope,
                        onSuccess = onSuccess,
                        onError = onError
                    )
                },
                onCanceled = onCancel
            )
        }
    }

    /**
     * Handles the confirmation code flow for OPENID users
     */
    private fun showConfirmationCodeFlow(
        context: Context,
        targetAction: TargetAction,
        coroutineScope: CoroutineScope,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
        onCancel: () -> Unit
    ) {
        coroutineScope.launch {
            val result = requestFederatedTokenUseCase(
                RequestFederatedTokenUseCase.Param(
                    targetAction = targetAction.name
                )
            )
            
            if (result.isSuccess) {
                val email = accountManager.getAccount().email
                showConfirmationCodeDialog(
                    context = context,
                    email = email,
                    coroutineScope = coroutineScope,
                    targetAction = targetAction,
                    onSuccess = onSuccess,
                    onError = onError,
                    onCancel = onCancel
                )
            } else {
                onError(result.exceptionOrNull()?.message.orUnknownError())
            }
        }
    }

    /**
     * Shows the confirmation code input dialog
     */
    private fun showConfirmationCodeDialog(
        context: Context,
        email: String,
        coroutineScope: CoroutineScope,
        targetAction: TargetAction,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
        onCancel: () -> Unit
    ) {
        NCInputDialog(context).showDialog(
            title = context.getString(R.string.nc_enter_confirmation_code),
            descMessage = String.format(
                context.getString(R.string.nc_enter_confirmation_code_desc),
                email
            ),
            inputBoxTitle = context.getString(R.string.nc_confirmation_code),
            clickablePhrases = listOf(
                "Resend code" to {
                    // Resend the federated token
                    coroutineScope.launch {
                        requestFederatedTokenUseCase(
                            RequestFederatedTokenUseCase.Param(
                                targetAction = targetAction.name
                            )
                        )
                    }
                },
            ),
            confirmText = context.getString(R.string.nc_text_continue),
            onConfirmed = { confirmationCode ->
                coroutineScope.launch {
                    try {
                        val result = verifyFederatedTokenUseCase(
                            VerifyFederatedTokenUseCase.Param(
                                targetAction = targetAction.name,
                                token = confirmationCode
                            )
                        )
                        
                        if (result.isSuccess) {
                            onSuccess(result.getOrThrow().orEmpty())
                        } else {
                            onError(result.exceptionOrNull()?.message.orUnknownError())
                        }
                    } catch (e: Exception) {
                        onError(e.message.orUnknownError())
                    }
                }
            },
            onCanceled = onCancel
        )
    }

    /**
     * Verifies password directly without showing dialog
     * @param password The password to verify
     * @param targetAction The target action for password verification
     * @param coroutineScope The coroutine scope to launch verification in
     * @param onSuccess Callback when password verification succeeds with the token
     * @param onError Callback when password verification fails with error message
     */
    fun verifyPassword(
        password: String,
        targetAction: TargetAction,
        coroutineScope: CoroutineScope,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (password.isBlank()) {
            onError("Password cannot be empty")
            return
        }

        coroutineScope.launch {
            val result = verifiedPasswordTokenUseCase(
                VerifiedPasswordTokenUseCase.Param(
                    targetAction = targetAction.name,
                    password = password
                )
            )

            if (result.isSuccess) {
                onSuccess(result.getOrThrow().orEmpty())
            } else {
                onError(result.exceptionOrNull()?.message.orUnknownError())
            }
        }
    }
} 