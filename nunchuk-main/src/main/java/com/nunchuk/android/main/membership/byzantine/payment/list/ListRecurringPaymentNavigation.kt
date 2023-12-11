package com.nunchuk.android.main.membership.byzantine.payment.list

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.nunchuk.android.model.byzantine.AssistedWalletRole

const val recurringPaymentRoute = "recurring_payment/{groupId}/{walletId}"

fun NavGraphBuilder.recurringPaymentsList(
    onOpenAddRecurringPayment: () -> Unit,
    onOpenRecurringPaymentDetail: (String) -> Unit,
    groupId: String,
    walletId: String,
    myRole: AssistedWalletRole,
) {
    composable(
        route = recurringPaymentRoute,
        arguments = listOf(
            navArgument("groupId") {
                type = NavType.StringType
                defaultValue = groupId
            },
            navArgument("walletId") {
                type = NavType.StringType
                defaultValue = walletId
            },
        )
    ) {
        ListRecurringPaymentRoute(
            onOpenAddRecurringPayment = onOpenAddRecurringPayment,
            onOpenRecurringPaymentDetail = onOpenRecurringPaymentDetail,
            myRole = myRole,
        )
    }
}

fun NavController.navigateToListRecurringPayment(
    navOptions: NavOptions? = null,
    groupId: String,
    walletId: String,
) {
    navigate("recurring_payment/${groupId}/${walletId}", navOptions)
}
