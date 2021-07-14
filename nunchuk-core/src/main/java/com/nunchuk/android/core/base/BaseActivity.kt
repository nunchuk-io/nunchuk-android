package com.nunchuk.android.core.base

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.nunchuk.android.arch.R
import com.nunchuk.android.core.manager.ActivityManager
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.utils.DisposableManager
import com.nunchuk.android.widget.NCLoadingDialogCreator
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import javax.inject.Inject

abstract class BaseActivity : AppCompatActivity(), HasAndroidInjector {

    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    @Inject
    lateinit var navigator: NunchukNavigator

    private lateinit var creator: NCLoadingDialogCreator

    override fun androidInjector(): AndroidInjector<Any> = androidInjector

    private var dialog: Dialog? = null

    fun showLoading() {
        dialog?.cancel()
        dialog = creator.showDialog()
    }

    fun hideLoading() {
        dialog?.cancel()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        overridePendingTransition(R.anim.enter, R.anim.exit)
        ActivityManager.instance.add(this)
        creator = NCLoadingDialogCreator(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        DisposableManager.instance.dispose()
        ActivityManager.instance.remove(this)
        dialog?.cancel()
    }

}