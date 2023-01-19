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

package com.nunchuk.android.core.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.nunchuk.android.core.R
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.network.UnauthorizedEventBus
import com.nunchuk.android.core.network.UnauthorizedException
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.utils.CrashlyticsReporter
import com.nunchuk.android.widget.NCLoadingDialogCreator
import javax.inject.Inject

abstract class BaseActivity<Binding : ViewBinding> : AppCompatActivity() {

    @Inject
    lateinit var navigator: NunchukNavigator

    @Inject
    lateinit var accountManager: AccountManager

    private val creator: NCLoadingDialogCreator by lazy(LazyThreadSafetyMode.NONE) {
        NCLoadingDialogCreator(this)
    }

    protected lateinit var binding: Binding

    abstract fun initializeBinding(): Binding

    fun showLoading(cancelable: Boolean = true, title: String = getString(R.string.nc_please_wait), message: String? = null) {
        creator.cancel()
        creator.showDialog(cancelable, title = title, message = message)
    }

    fun hideLoading() {
        creator.cancel()
    }

    fun showOrHideLoading(loading: Boolean, title: String = getString(R.string.nc_please_wait), message: String? = null) {
        if (loading) showLoading(title = title, message = message) else hideLoading()
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
    }

    override fun onDestroy() {
        super.onDestroy()
        creator.cancel()
    }
}