package com.nunchuk.android.main.components.tabs.services

import com.nunchuk.android.main.R

data class ServicesTabState(
    val rowItems: List<Any> = initRowItems()
)

private fun initRowItems(): List<Any> {
    val items = mutableListOf<Any>()
    items.add(ServiceTabRowCategory.Emergency)
    items.addAll(ServiceTabRowCategory.Emergency.items)
    items.add(ServiceTabRowCategory.Inheritance)
    items.addAll(ServiceTabRowCategory.Inheritance.items)
    items.add(ServiceTabRowCategory.Subscription)
    items.addAll(ServiceTabRowCategory.Subscription.items)
    return items
}

sealed class ServiceTabRowCategory(val title: Int, val drawableId: Int, val items: List<ServiceTabRowItem>) {
    object Emergency :
        ServiceTabRowCategory(R.string.nc_emergency, R.drawable.ic_emergency, mutableListOf<ServiceTabRowItem>().apply {
            add(ServiceTabRowItem.EmergencyLockdown)
            add(ServiceTabRowItem.KeyRecovery)
        })

    object Inheritance :
        ServiceTabRowCategory(R.string.nc_inheritance_planning, R.drawable.ic_inheritance_planning, mutableListOf<ServiceTabRowItem>().apply {
            add(ServiceTabRowItem.SetUpInheritancePlan)
            add(ServiceTabRowItem.ClaimInheritance)
        })

    object Subscription :
        ServiceTabRowCategory(R.string.nc_your_subscription, R.drawable.ic_subscription, mutableListOf<ServiceTabRowItem>().apply {
            add(ServiceTabRowItem.CoSigningPolicies)
            add(ServiceTabRowItem.OrderNewHardware)
            add(ServiceTabRowItem.ManageSubscription)
            add(ServiceTabRowItem.RollOverAssistedWallet)
        })
}

sealed class ServiceTabRowItem(val category: ServiceTabRowCategory, val title: Int) {
    object EmergencyLockdown : ServiceTabRowItem(ServiceTabRowCategory.Emergency, R.string.nc_emergency_lockdown)
    object KeyRecovery : ServiceTabRowItem(ServiceTabRowCategory.Emergency, R.string.nc_key_recovery)
    object SetUpInheritancePlan : ServiceTabRowItem(ServiceTabRowCategory.Inheritance, R.string.nc_set_up_inheritance_plan)
    object ClaimInheritance : ServiceTabRowItem(ServiceTabRowCategory.Inheritance, R.string.nc_claim_inheritance)
    object CoSigningPolicies : ServiceTabRowItem(ServiceTabRowCategory.Subscription, R.string.nc_co_signing_policies)
    object OrderNewHardware : ServiceTabRowItem(ServiceTabRowCategory.Subscription, R.string.nc_order_new_hardware)
    object ManageSubscription : ServiceTabRowItem(ServiceTabRowCategory.Subscription, R.string.nc_manage_subscription)
    object RollOverAssistedWallet : ServiceTabRowItem(ServiceTabRowCategory.Subscription, R.string.nc_roll_over_assisted_wallet)
}