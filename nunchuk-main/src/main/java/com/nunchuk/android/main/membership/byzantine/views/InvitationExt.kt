package com.nunchuk.android.main.membership.byzantine.views

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.nunchuk.android.main.R
import com.nunchuk.android.model.wallet.Invitation

@Composable
internal fun Invitation.toInviteMemberMessage(): String {
    return when {
        inviterName.isNotBlank() && inviterEmail.isNotBlank() -> {
            stringResource(
                R.string.nc_free_group_wallet_invitation_message_with_name_and_email,
                inviterName,
                inviterEmail
            )
        }

        inviterName.isNotBlank() -> {
            stringResource(
                R.string.nc_pending_wallet_invite_member,
                inviterName
            )
        }

        inviterEmail.isNotBlank() -> {
            stringResource(
                R.string.nc_free_group_wallet_invitation_message_with_email_only,
                inviterEmail
            )
        }

        else -> {
            stringResource(R.string.nc_free_group_wallet_invitation_message_default)
        }
    }
}
