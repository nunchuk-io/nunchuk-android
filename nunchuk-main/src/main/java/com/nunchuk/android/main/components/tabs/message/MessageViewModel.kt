package com.nunchuk.android.main.components.tabs.message

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import javax.inject.Inject

internal class MessageViewModel @Inject constructor(
) : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is message page"
    }
    val text: LiveData<String> = _text
}