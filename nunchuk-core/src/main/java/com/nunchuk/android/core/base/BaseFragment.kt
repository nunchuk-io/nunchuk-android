package com.nunchuk.android.core.base

import com.nunchuk.android.nav.NunchukNavigator
import dagger.android.support.DaggerFragment
import javax.inject.Inject

abstract class BaseFragment : DaggerFragment() {

    @Inject
    protected lateinit var navigator: NunchukNavigator
}