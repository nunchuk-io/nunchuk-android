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

package com.nunchuk.android.core.push

import com.nunchuk.android.model.SingleSigner

sealed class PushEvent {
    data class ServerTransactionEvent(val walletId: String, val transactionId: String) : PushEvent()
    data class MessageEvent(val message: String) : PushEvent()
    data object TransactionCreatedEvent : PushEvent()
    data object AddDesktopKeyCompleted : PushEvent()
    data class WalletCreate(val walletId: String, val groupId: String) : PushEvent()
    data class TransactionCancelled(val walletId: String, val transactionId: String) : PushEvent()
    data class DraftResetWallet(val groupId: String) : PushEvent()
    data class GroupMembershipRequestCreated(val groupId: String) : PushEvent()
    data class GroupWalletCreated(val walletId: String) : PushEvent()
    data class GroupEmergencyLockdownStarted(val walletId: String) : PushEvent()
    data object KeyAddedToGroup : PushEvent()
    data object DismissGroupWalletCreatedAlert : PushEvent()
    data class SignedTxSuccess(val txId: String) : PushEvent()
    data class WalletChanged(val walletId: String) : PushEvent()
    data class PrimaryOwnerUpdated(val walletId: String) : PushEvent()
    data class SignedChanged(val xfp: String) : PushEvent()
    data class CoinUpdated(val walletId: String) : PushEvent()
    data class InheritanceEvent(val walletId: String) : PushEvent()
    data class ReplaceKeyChange(val walletId: String) : PushEvent()
    data class WalletReplaced(val newWalletId: String, val newWalletName: String) : PushEvent()
    data class ReloadUsedAddress(val address: String) : PushEvent()

    /**
     * Event for when a new local user signer is added, don't use it for create signer from sync API    
     */
    data class LocalUserSignerAdded(val signer: SingleSigner) : PushEvent()
    data object CloseWalletDetail : PushEvent()
}