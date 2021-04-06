package com.nunchuk.android.main.components.tabs.account

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import javax.inject.Inject

internal class AccountViewModel @Inject constructor(
) : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is account page"
    }
    val text: LiveData<String> = _text
}