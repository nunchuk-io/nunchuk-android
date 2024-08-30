package com.nunchuk.android.app.referral

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.nunchuk.android.app.referral.address.DEFAULT_ADDRESS
import com.nunchuk.android.app.referral.address.DEFAULT_WALLET_ID
import com.nunchuk.android.app.referral.address.navigateToReferralAddress
import com.nunchuk.android.app.referral.address.referralAddress
import com.nunchuk.android.app.referral.confirmationcode.navigateToReferralConfirmationCode
import com.nunchuk.android.app.referral.confirmationcode.referralConfirmationCode
import com.nunchuk.android.app.referral.invitefriend.referralInviteFriend
import com.nunchuk.android.app.referral.invitefriend.referralInviteFriendRoute
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.base.BaseComposeActivity
import com.nunchuk.android.core.referral.ReferralArgs
import com.nunchuk.android.core.share.IntentSharingController
import com.nunchuk.android.core.util.copyToClipboard
import com.nunchuk.android.widget.NCToastMessage
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ReferralActivity : BaseComposeActivity() {
    private val args by lazy { ReferralArgs.fromBundle(intent.extras!!) }

    private val controller: IntentSharingController by lazy {
        IntentSharingController.from(
            this
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            NunchukTheme {
                val navigationController = rememberNavController()
                val snackState: SnackbarHostState = remember { SnackbarHostState() }

                NavHost(
                    navController = navigationController,
                    startDestination = referralInviteFriendRoute
                ) {
                    referralInviteFriend(args = args,
                        snackState = snackState,
                        navController = navigationController,
                        onCopyToClipboard = {
                            copyToClipboard(label = "Nunchuk", text = it)
                            NCToastMessage(this@ReferralActivity).show("Link copied to clipboard")
                        }, onChangeAddress = { address, walletId, isHasLocalReferralCode ->
                            navigationController.navigateToReferralAddress(
                                address = address.ifBlank { DEFAULT_ADDRESS },
                                walletId = walletId.ifBlank { DEFAULT_WALLET_ID },
                                action = if (isHasLocalReferralCode) ReferralAction.CHANGE.value else ReferralAction.PICK.value)
                        },
                        onViewReferralAddress = {
                            navigationController.navigateToReferralConfirmationCode(
                                action = ReferralAction.VIEW.value,
                            )
                        },
                        onShareLink = { isSms, link ->
                            if (isSms) {
                                controller.shareLinkViaSms(link)
                            } else {
                                controller.shareText(link)
                            }
                        }
                    )

                    referralAddress(
                        navController = navigationController,
                        onSaveChange = {
                            navigationController.navigateToReferralConfirmationCode(
                                action = ReferralAction.CHANGE.value,
                                address = it
                            )
                        })

                    referralConfirmationCode(navigationController, snackState)
                }
            }
        }
    }

    companion object {
        fun buildIntent(activity: Context, args: ReferralArgs): Intent {
            return Intent(activity, ReferralActivity::class.java).apply {
                putExtras(args.toBundle())
            }
        }
    }
}