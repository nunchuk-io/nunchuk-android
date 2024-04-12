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

package com.nunchuk.android.main.components.tabs.wallet

import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.ByzantineGroup
import com.nunchuk.android.model.ByzantineMember
import com.nunchuk.android.model.MembershipPlan
import com.nunchuk.android.model.MembershipStepInfo
import com.nunchuk.android.model.SatsCardStatus
import com.nunchuk.android.model.TapSignerStatus
import com.nunchuk.android.model.WalletExtended
import com.nunchuk.android.model.banner.Banner
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.KeyHealthStatus
import com.nunchuk.android.model.membership.AssistedWalletBrief
import com.nunchuk.android.model.setting.WalletSecuritySetting
import com.nunchuk.android.type.Chain
import com.nunchuk.android.type.ConnectionStatus

internal data class WalletsState(
    val wallets: List<WalletExtended> = emptyList(),
    val signers: List<SignerModel> = emptyList(),
    val connectionStatus: ConnectionStatus? = null,
    val chain: Chain = Chain.MAIN,
    val plans: List<MembershipPlan>? = null,
    val remainingTime: Int = 0,
    val assistedWallets: List<AssistedWalletBrief> = emptyList(),
    val isSetupInheritance: Boolean = false,
    val isHideUpsellBanner: Boolean = false,
    val banner: Banner? = null,
    val walletSecuritySetting: WalletSecuritySetting = WalletSecuritySetting(),
    val currentWalletPin: String = "",
    val allGroups: List<ByzantineGroup> = emptyList(),
    val groupWalletUis: List<GroupWalletUi> = emptyList(),
    val alerts: Map<String, Int> = emptyMap(),
    val keyHealthStatus: Map<String, List<KeyHealthStatus>> = emptyMap(),
    val useLargeFont: Boolean = false,
    val personalSteps: List<MembershipStepInfo> = emptyList()
)

internal sealed class WalletsEvent {
    data class Loading(val loading: Boolean) : WalletsEvent()
    data class ShowErrorEvent(val e: Throwable?) : WalletsEvent()
    data object AddWalletEvent : WalletsEvent()
    data object ShowSignerIntroEvent : WalletsEvent()
    data object WalletEmptySignerEvent : WalletsEvent()
    class NeedSetupSatsCard(val status: SatsCardStatus) : WalletsEvent()
    class NfcLoading(val loading: Boolean) : WalletsEvent()
    class GoToSatsCardScreen(val status: SatsCardStatus) : WalletsEvent()
    class GetTapSignerStatusSuccess(val status: TapSignerStatus) : WalletsEvent()
    class SatsCardUsedUp(val numberOfSlot: Int) : WalletsEvent()
    data object DenyWalletInvitationSuccess : WalletsEvent()
    data object None : WalletsEvent()
    data class AcceptWalletInvitationSuccess(val walletId: String?, val groupId: String) : WalletsEvent()
}

internal data class GroupWalletUi(
    val wallet: WalletExtended? = null,
    val group: ByzantineGroup? = null,
    val role: String = AssistedWalletRole.NONE.name,
    val inviterName: String = "",
    val isAssistedWallet: Boolean = false,
    val badgeCount: Int = 0,
    val primaryOwnerMember: ByzantineMember?= null,
    val keyStatus: Map<String, KeyHealthStatus> = emptyMap(),
    val signers: List<SignerModel> = emptyList(),
)