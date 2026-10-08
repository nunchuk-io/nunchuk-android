package com.nunchuk.android.core.base

interface LoadingDialog {
    fun showLoading(
        cancelable: Boolean = true,
        title: String? = null,
        message: String? = null
    )

    fun hideLoading()

    fun showOrHideLoading(
        loading: Boolean,
        title: String? = null,
        message: String? = null
    )
}