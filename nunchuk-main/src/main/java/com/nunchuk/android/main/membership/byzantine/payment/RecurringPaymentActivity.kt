package com.nunchuk.android.main.membership.byzantine.payment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.nunchuk.android.main.membership.byzantine.payment.amount.addPaymentAmount
import com.nunchuk.android.main.membership.byzantine.payment.amount.navigateToPaymentAmount
import com.nunchuk.android.main.membership.byzantine.payment.list.recurringPaymentRoute
import com.nunchuk.android.main.membership.byzantine.payment.list.recurringPaymentsList
import com.nunchuk.android.main.membership.byzantine.payment.name.addPaymentName
import com.nunchuk.android.main.membership.byzantine.payment.name.navigateToPaymentName
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RecurringPaymentActivity : AppCompatActivity() {

    private val viewModel: RecurringPaymentViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            ComposeView(this).apply {
                setContent {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = recurringPaymentRoute
                    ) {
                        recurringPaymentsList(
                            onOpenAddRecurringPayment = {
                                navController.navigateToPaymentName()
                            },
                        )
                        addPaymentName(
                            recurringPaymentViewModel = viewModel,
                            openPaymentAmountScreen = {
                                navController.navigateToPaymentAmount()
                            },
                        )
                        addPaymentAmount(
                            recurringPaymentViewModel = viewModel,
                            openCalculateScreen = { }
                        )
                    }
                }
            }
        )
    }

    companion object {
        private const val GROUP_ID = "group_id"
        private const val WALLET_ID = "wallet_id"

        fun navigate(
            activity: Context,
            groupId: String,
            walletId: String?,
        ) {
            val intent = Intent(activity, RecurringPaymentActivity::class.java).apply {
                putExtra(GROUP_ID, groupId)
                putExtra(WALLET_ID, walletId)
            }
            activity.startActivity(intent)
        }
    }
}