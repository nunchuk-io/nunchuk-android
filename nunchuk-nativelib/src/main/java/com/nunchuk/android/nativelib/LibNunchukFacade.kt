package com.nunchuk.android.nativelib

class LibNunchukFacade {

    private val impl = LibNunchukAndroid()

    fun retrieveData() {
        impl.retrieveData()
    }

}