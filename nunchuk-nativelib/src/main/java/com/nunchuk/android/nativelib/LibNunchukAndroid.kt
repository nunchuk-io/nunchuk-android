package com.nunchuk.android.nativelib

/**
 * Jni external methods here
 */
internal class LibNunchukAndroid {

    external fun retrieveData()

    companion object {
        init {
            System.loadLibrary("nunchuk-android")
        }
    }

}