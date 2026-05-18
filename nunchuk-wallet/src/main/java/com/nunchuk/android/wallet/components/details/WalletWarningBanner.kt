package com.nunchuk.android.wallet.components.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.fillBeewax
import com.nunchuk.android.compose.lightGray
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.core.util.RENEW_ACCOUNT_LINK
import com.nunchuk.android.model.BannerState
import com.nunchuk.android.wallet.R

internal sealed class WalletWarning {
    data object ClaimInheritance : WalletWarning()
    data class NeedBackup(val isFreeGroupWallet: Boolean) : WalletWarning()
    data class Banner(val state: BannerState) : WalletWarning()
    data object InactiveAssisted : WalletWarning()
}

private enum class BannerTint { Beeswax, Whisper }

@Composable
internal fun WalletWarningBanner(
    warning: WalletWarning,
    onClaimInheritance: () -> Unit,
    onNeedBackup: () -> Unit,
    onBannerBackupAndRegister: () -> Unit,
    onBannerBackupOnly: () -> Unit,
    onBannerRegisterOnly: () -> Unit,
    onOpenExternalLink: (String) -> Unit,
) {
    when (warning) {
        is WalletWarning.ClaimInheritance -> BannerBase(
            iconRes = R.drawable.ic_claim_warning,
            tint = BannerTint.Beeswax,
            text = stringResource(R.string.nc_inheritance_unlocked_warning),
            actionLabel = stringResource(R.string.nc_do_it_now),
            onClick = onClaimInheritance,
        )

        is WalletWarning.NeedBackup -> BannerBase(
            iconRes = R.drawable.ic_warning_outline,
            tint = BannerTint.Beeswax,
            text = if (warning.isFreeGroupWallet) stringResource(R.string.nc_save_bsms_file_warning)
            else stringResource(R.string.nc_write_down_the_seed_pharse_warning),
            actionLabel = stringResource(R.string.nc_do_it_now),
            onClick = onNeedBackup,
        )

        is WalletWarning.Banner -> when (warning.state) {
            BannerState.BACKUP_AND_REGISTER -> BannerBase(
                iconRes = R.drawable.ic_warning_outline,
                tint = BannerTint.Beeswax,
                text = stringResource(R.string.nc_banner_backup_and_register),
                actionLabel = stringResource(R.string.nc_do_it_now),
                onClick = onBannerBackupAndRegister,
            )

            BannerState.BACKUP_ONLY -> BannerBase(
                iconRes = R.drawable.ic_warning_outline,
                tint = BannerTint.Beeswax,
                text = stringResource(R.string.nc_banner_backup_only),
                actionLabel = stringResource(R.string.nc_do_it_now),
                onClick = onBannerBackupOnly,
            )

            BannerState.REGISTER_ONLY -> BannerBase(
                iconRes = R.drawable.ic_warning_outline,
                tint = BannerTint.Beeswax,
                text = stringResource(R.string.nc_banner_register_only),
                actionLabel = stringResource(R.string.nc_do_it_now),
                onClick = onBannerRegisterOnly,
            )
        }

        is WalletWarning.InactiveAssisted -> BannerBase(
            iconRes = R.drawable.ic_info,
            tint = BannerTint.Whisper,
            text = stringResource(R.string.nc_assisted_wallet_downgrade_hint),
            actionLabel = stringResource(R.string.nc_renew_subscription),
            onClick = { onOpenExternalLink(RENEW_ACCOUNT_LINK) },
        )
    }
}

@Composable
private fun BannerBase(
    iconRes: Int,
    tint: BannerTint,
    text: String,
    actionLabel: String,
    onClick: () -> Unit,
) {
    val bgColor = when (tint) {
        BannerTint.Beeswax -> MaterialTheme.colorScheme.fillBeewax
        BannerTint.Whisper -> MaterialTheme.colorScheme.lightGray
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(color = bgColor, shape = RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.textPrimary,
            modifier = Modifier.size(20.dp),
        )
        val annotated = buildAnnotatedString {
            val cleaned = text.trim().removeSuffix(actionLabel).trim()
            if (cleaned.isNotEmpty()) {
                append(cleaned)
                append(' ')
            }
            withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                append(actionLabel)
            }
        }
        Text(
            text = annotated,
            style = NunchukTheme.typography.titleSmall.copy(
                color = MaterialTheme.colorScheme.textPrimary
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
