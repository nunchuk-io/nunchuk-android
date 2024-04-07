package com.nunchuk.android.main.util

import android.content.Context
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.CheckWalletPinUseCase
import com.nunchuk.android.core.domain.GetWalletPinUseCase
import com.nunchuk.android.core.domain.membership.TargetAction
import com.nunchuk.android.core.domain.membership.VerifiedPKeyTokenUseCase
import com.nunchuk.android.core.domain.membership.VerifiedPasswordTokenUseCase
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.main.R
import com.nunchuk.android.model.setting.WalletSecuritySetting
import com.nunchuk.android.usecase.GetWalletSecuritySettingUseCase
import com.nunchuk.android.widget.NCInputDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class WalletSecuritySettingsManager @Inject constructor(
    private val getWalletSecuritySettingUseCase: GetWalletSecuritySettingUseCase,
    private val checkWalletPinUseCase: CheckWalletPinUseCase,
    private val verifiedPasswordTokenUseCase: VerifiedPasswordTokenUseCase,
    private val verifiedPKeyTokenUseCase: VerifiedPKeyTokenUseCase,
    private val getWalletPinUseCase: GetWalletPinUseCase,
    private val accountManager: AccountManager
) {
    private var walletSecuritySetting: WalletSecuritySetting = WalletSecuritySetting()
    private var currentWalletPin: String = ""
    private lateinit var coroutineScope: CoroutineScope
    var onError: (Throwable?) -> Unit = {}
    var openWalletDetailsScreen: (String) -> Unit = { _ -> }

    fun init(coroutineScope: CoroutineScope) {
        this.coroutineScope = coroutineScope
        coroutineScope.launch {
            getWalletSecuritySettingUseCase(Unit)
                .collect {
                    walletSecuritySetting = it.getOrNull() ?: WalletSecuritySetting()
                }
        }
        coroutineScope.launch {
            getWalletPinUseCase(Unit).collect {
                currentWalletPin = it.getOrDefault("")
            }
        }
    }

    private fun isWalletPinEnable() =
        walletSecuritySetting.protectWalletPin

    private fun isWalletPasswordEnable() =
        accountManager.loginType() == SignInMode.EMAIL.value && walletSecuritySetting.protectWalletPassword

    private fun isWalletPassphraseEnable() =
        accountManager.loginType() == SignInMode.PRIMARY_KEY.value && walletSecuritySetting.protectWalletPassphrase

    private fun checkWalletPin(context: Context, input: String, walletId: String) =
        coroutineScope.launch {
            val match = checkWalletPinUseCase(input).getOrDefault(false)
            if (match) {
                openWalletDetailsScreen(walletId)
            } else {
                onError(Throwable(context.getString(R.string.nc_incorrect_current_pin)))
            }
        }

    private fun enterPasswordDialog(context: Context, walletId: String) {
        NCInputDialog(context).showDialog(
            title = context.getString(R.string.nc_re_enter_your_password),
            descMessage = context.getString(R.string.nc_re_enter_your_password_dialog_desc),
            onConfirmed = {
                confirmPassword(context, it, walletId)
            }
        )
    }

    private fun enterPassphraseDialog(context: Context, walletId: String) {
        NCInputDialog(context).showDialog(
            title = context.getString(R.string.nc_re_enter_your_passphrase),
            descMessage = context.getString(R.string.nc_re_enter_your_passphrase_dialog_desc),
            onConfirmed = {
                confirmPassphrase(context, it, walletId)
            }
        )
    }

    private fun confirmPassword(context: Context, password: String, walletId: String) =
        coroutineScope.launch {
            if (password.isBlank()) {
                return@launch
            }
            val result = verifiedPasswordTokenUseCase(
                VerifiedPasswordTokenUseCase.Param(
                    password = password, targetAction = TargetAction.PROTECT_WALLET.name
                )
            )
            if (result.isSuccess) {
                actionAfterCheckingPasswordOrPassphrase(context, walletId)
            } else {
                onError(result.exceptionOrNull())
            }
        }

    fun checkWalletSecurity(context: Context, walletId: String) {
        if (isWalletPasswordEnable()) {
            enterPasswordDialog(context, walletId)
        } else if (isWalletPassphraseEnable()) {
            enterPassphraseDialog(context, walletId)
        } else if (isWalletPinEnable()) {
            showInputPinDialog(context, walletId)
        } else {
            openWalletDetailsScreen(walletId)
        }
    }

    private fun confirmPassphrase(context: Context, passphrase: String, walletId: String) =
        coroutineScope.launch {
            if (passphrase.isBlank()) {
                return@launch
            }
            val result = verifiedPKeyTokenUseCase(passphrase)
            if (result.isSuccess) {
                actionAfterCheckingPasswordOrPassphrase(context, walletId)
            } else {
                onError(result.exceptionOrNull())
            }
        }

    private fun actionAfterCheckingPasswordOrPassphrase(context: Context, walletId: String) {
        if (isWalletPinEnable()) {
            showInputPinDialog(context, walletId)
        } else {
            openWalletDetailsScreen(walletId)
        }
    }

    private fun showInputPinDialog(context: Context, walletId: String) {
        NCInputDialog(context).showDialog(
            title = context.getString(com.nunchuk.android.settings.R.string.nc_enter_your_pin),
            onConfirmed = {
                checkWalletPin(context, it, walletId)
            }
        )
    }

}