package com.nunchuk.android.nativelib

import javax.inject.Inject

internal class LibNunchukFacade @Inject constructor() {

    private val impl = LibNunchukAndroid()

    fun retrieveData() {
        impl.retrieveData()
    }

}