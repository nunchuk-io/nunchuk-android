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

package com.nunchuk.android.core.base

import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.nunchuk.android.core.R
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.network.UnauthorizedEventBus
import com.nunchuk.android.core.network.UnauthorizedException
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.utils.CrashlyticsReporter
import com.nunchuk.android.widget.NCLoadingDialogCreator
import com.nunchuk.android.widget.NCToastMessage
import kotlinx.coroutines.flow.filterIsInstance
import java.util.Locale
import javax.inject.Inject

abstract class BaseActivity<Binding : ViewBinding> : AppCompatActivity(), LoadingDialog {

    @Inject
    lateinit var navigator: NunchukNavigator

    @Inject
    lateinit var accountManager: AccountManager

    @Inject
    lateinit var pushEventManager: PushEventManager

    private val creator: NCLoadingDialogCreator by lazy(LazyThreadSafetyMode.NONE) {
        NCLoadingDialogCreator(this)
    }

    protected lateinit var binding: Binding

    abstract fun initializeBinding(): Binding

    override fun showLoading(
        cancelable: Boolean,
        title: String,
        message: String?
    ) {
        creator.cancel()
        creator.showDialog(cancelable, title = title, message = message)
    }

    override fun hideLoading() {
        creator.cancel()
    }

    override fun showOrHideLoading(
        loading: Boolean,
        title: String,
        message: String?
    ) {
        if (loading) showLoading(title = title, message = message) else hideLoading()
    }

    override fun attachBaseContext(newBase: Context) {
        newBase.resources.configuration.apply {
            setLocale(Locale.US)
        }
        Locale.setDefault(Locale.US)
        super.attachBaseContext(newBase)
    }

    override fun onResume() {
        super.onResume()
        UnauthorizedEventBus.instance().subscribe {
            val loginType = accountManager.loginType()
            if (loginType == SignInMode.EMAIL.value || loginType == SignInMode.PRIMARY_KEY.value) {
                accountManager.clearUserData()
                navigator.openSignInScreen(this)
                CrashlyticsReporter.recordException(UnauthorizedException())
            }
        }
    }

    override fun onPause() {
        super.onPause()
        UnauthorizedEventBus.instance().unsubscribe()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = initializeBinding()
        setContentView(binding.root)
        overridePendingTransition(R.anim.enter, R.anim.exit)

        flowObserver(pushEventManager.event.filterIsInstance<PushEvent.MessageEvent>()) {
            if (it.message.isNotEmpty()) NCToastMessage(this@BaseActivity).showError(message = it.message)
        }
        flowObserver(pushEventManager.event.filterIsInstance<PushEvent.WalletReplaced>()) {
            showTransferFundDialog(
                navigator = navigator,
                newWalletId = it.newWalletId,
                newWalletName = it.newWalletName
            )
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        creator.cancel()
    }
}