package com.nunchuk.android.main.membership.byzantine.payment.detail

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.nunchuk.android.model.byzantine.DummyTransactionPayload

const val recurringPaymentDetailRoute =
    "recurring_payment_detail/{groupId}/{walletId}/{recurringPaymentId}"

fun NavGraphBuilder.addRecurringPaymentDetail(
    onOpenDummyTransaction: (DummyTransactionPayload) -> Unit,
) {
    composable(
        recurringPaymentDetailRoute,
        arguments = listOf(
            navArgument("groupId") {
                type = NavType.StringType
            },
            navArgument("walletId") {
                type = NavType.StringType
            },
            navArgument("recurringPaymentId") {
                type = NavType.StringType
            },
        )
    ) {
        RecurringPaymentDetailRoute(onOpenDummyTransaction = onOpenDummyTransaction)
    }
}

fun NavController.navigateToRecurringPaymentDetail(
    groupId: String,
    walletId: String,
    recurringPaymentId: String,
) {
    navigate(
        "recurring_payment_detail/${groupId}/${walletId}/${recurringPaymentId}"
    )
}