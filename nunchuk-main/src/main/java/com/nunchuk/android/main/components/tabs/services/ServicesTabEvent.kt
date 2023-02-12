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

package com.nunchuk.android.main.components.tabs.services

import android.os.Parcelable
import com.nunchuk.android.main.R
import com.nunchuk.android.model.*
import com.nunchuk.android.model.banner.Banner
import com.nunchuk.android.model.banner.BannerPage
import com.nunchuk.android.model.membership.AssistedWalletBrief
import kotlinx.parcelize.Parcelize

sealed class ServicesTabEvent {
    data class ProcessFailure(val message: String) : ServicesTabEvent()
    data class GetServerKeySuccess(
        val signer: SingleSigner,
        val walletId: String,
        val token: String
    ) : ServicesTabEvent()

    data class Loading(val loading: Boolean) : ServicesTabEvent()
    data class CheckPasswordSuccess(val token: String, val walletId: String, val item: ServiceTabRowItem) : ServicesTabEvent()
    data class CreateSupportRoomSuccess(val roomId: String) : ServicesTabEvent()
    data class CheckInheritance(val inheritanceCheck: InheritanceCheck) : ServicesTabEvent()
    object EmailInvalid : ServicesTabEvent()
    data class OnSubmitEmailSuccess(val email: String) : ServicesTabEvent()
}

data class ServicesTabState(
    val isPremiumUser: Boolean? = null,
    val plan: MembershipPlan = MembershipPlan.NONE,
    val assistedWalletIds: List<AssistedWalletBrief> = emptyList(),
    val inheritances: Map<String, Inheritance> = emptyMap(),
    val banner: Banner? = null,
    val bannerPage: BannerPage? = null
) {
    fun initRowItems(): List<Any> {
        val items = mutableListOf<Any>()
        when (plan) {
            MembershipPlan.NONE -> {
                bannerPage?.let { bannerPage ->
                    items.add(NonSubHeader(title = bannerPage.title, desc = bannerPage.desc))
                    bannerPage.items.forEach {
                        items.add(NonSubRow(url = it.url, title = it.title, desc = it.desc))
                    }
                }
            }
            MembershipPlan.IRON_HAND -> {
                items.add(ServiceTabRowCategory.Emergency)
                items.add(ServiceTabRowItem.KeyRecovery)
                items.add(ServiceTabRowCategory.Subscription)
                items.addAll(ServiceTabRowCategory.Subscription.items)
                if (banner != null) {
                    items.add(Banner(banner.id, banner.url, banner.title))
                }
            }
            MembershipPlan.HONEY_BADGER -> {
                items.add(ServiceTabRowCategory.Emergency)
                items.addAll(ServiceTabRowCategory.Emergency.items)
                items.add(ServiceTabRowCategory.Inheritance)
                if (inheritances.isEmpty() || inheritances.values.all { it.status == InheritanceStatus.PENDING_CREATION }) {
                    items.add(ServiceTabRowItem.SetUpInheritancePlan)
                } else {
                    items.add(ServiceTabRowItem.ViewInheritancePlan)
                }
                items.add(ServiceTabRowItem.ClaimInheritance)
                items.add(ServiceTabRowCategory.Subscription)
                items.addAll(ServiceTabRowCategory.Subscription.items)
            }
            else -> {}
        }
        return items
    }
}

internal data class Banner(val id: String, val url: String, val title: String)

internal data class NonSubRow(val url: String, val title: String, val desc: String)

internal data class NonSubHeader(val title: String, val desc: String)

sealed class ServiceTabRowCategory(
    val title: Int,
    val drawableId: Int,
    val items: List<ServiceTabRowItem>
) {
    object Emergency :
        ServiceTabRowCategory(
            R.string.nc_emergency,
            R.drawable.ic_emergency,
            mutableListOf<ServiceTabRowItem>().apply {
                add(ServiceTabRowItem.EmergencyLockdown)
                add(ServiceTabRowItem.KeyRecovery)
            })

    object Inheritance :
        ServiceTabRowCategory(
            R.string.nc_inheritance_planning,
            R.drawable.ic_inheritance_planning,
            mutableListOf<ServiceTabRowItem>().apply {
                add(ServiceTabRowItem.SetUpInheritancePlan)
                add(ServiceTabRowItem.ClaimInheritance)
            })

    object Subscription :
        ServiceTabRowCategory(
            R.string.nc_your_subscription,
            R.drawable.ic_subscription,
            mutableListOf<ServiceTabRowItem>().apply {
                add(ServiceTabRowItem.CoSigningPolicies)
                add(ServiceTabRowItem.OrderNewHardware)
                add(ServiceTabRowItem.RollOverAssistedWallet)
                add(ServiceTabRowItem.ManageSubscription)
            })
}

sealed class ServiceTabRowItem(val category: ServiceTabRowCategory, val title: Int) : Parcelable {
    @Parcelize
    object EmergencyLockdown :
        ServiceTabRowItem(ServiceTabRowCategory.Emergency, R.string.nc_emergency_lockdown)
    @Parcelize
    object KeyRecovery :
        ServiceTabRowItem(ServiceTabRowCategory.Emergency, R.string.nc_key_recovery)
    @Parcelize
    object SetUpInheritancePlan :
        ServiceTabRowItem(ServiceTabRowCategory.Inheritance, R.string.nc_set_up_inheritance_plan)
    @Parcelize
    object ViewInheritancePlan :
        ServiceTabRowItem(ServiceTabRowCategory.Inheritance, R.string.nc_view_inheritance_plan)
    @Parcelize
    object ClaimInheritance :
        ServiceTabRowItem(ServiceTabRowCategory.Inheritance, R.string.nc_claim_an_inheritance)
    @Parcelize
    object CoSigningPolicies :
        ServiceTabRowItem(ServiceTabRowCategory.Subscription, R.string.nc_cosigning_policies)
    @Parcelize
    object OrderNewHardware :
        ServiceTabRowItem(ServiceTabRowCategory.Subscription, R.string.nc_order_new_hardware)
    @Parcelize
    object ManageSubscription :
        ServiceTabRowItem(ServiceTabRowCategory.Subscription, R.string.nc_manage_subscription)
    @Parcelize
    object RollOverAssistedWallet :
        ServiceTabRowItem(ServiceTabRowCategory.Subscription, R.string.nc_roll_over_assisted_wallet)
}