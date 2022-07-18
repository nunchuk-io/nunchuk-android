package com.nunchuk.android.core.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.nunchuk.android.arch.R
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.manager.ActivityManager
import com.nunchuk.android.core.network.UnauthorizedEventBus
import com.nunchuk.android.core.network.UnauthorizedException
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.utils.CrashlyticsReporter
import com.nunchuk.android.utils.DisposableManager
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

    fun showLoading(cancelable: Boolean = true, message: String? = null) {
        creator.cancel()
        creator.showDialog(cancelable, message)
    }

    fun hideLoading() {
        creator.cancel()
    }

    fun showOrHideLoading(loading: Boolean, message: String? = null) {
        if (loading) showLoading(message = message) else hideLoading()
    }

    override fun onResume() {
        super.onResume()
        UnauthorizedEventBus.instance().subscribe {
            accountManager.clearUserData()
            navigator.openSignInScreen(this)
            CrashlyticsReporter.recordException(UnauthorizedException())
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

        ActivityManager.instance.add(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        DisposableManager.instance.dispose()
        ActivityManager.instance.remove(this)
        creator.cancel()
    }
}