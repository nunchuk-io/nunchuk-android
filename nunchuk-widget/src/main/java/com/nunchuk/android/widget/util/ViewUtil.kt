package com.nunchuk.android.widget.util

import android.text.InputType
import android.view.View
import android.widget.EditText
import com.nunchuk.android.widget.NCEditTextView
import com.nunchuk.android.widget.R
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart

const val DEBOUNCE = 500L

@ExperimentalCoroutinesApi
fun View.clicks() = callbackFlow {
    setOnClickListener {
        offer(Unit)
    }
    awaitClose { setOnClickListener {} }
}

@ExperimentalCoroutinesApi
fun EditText.textChanges() = callbackFlow<CharSequence> {
    val listener = object : SimpleTextWatcher() {
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            offer(s)
        }
    }
    addTextChangedListener(listener)
    awaitClose { removeTextChangedListener(listener) }
}.onStart { emit(text) }

fun NCEditTextView.heightExtended(dimensionPixelSize: Int) {
    findViewById<EditText>(R.id.editText).heightExtended(dimensionPixelSize)
}

fun EditText.heightExtended(dimensionPixelSize: Int) {
    isSingleLine = false
    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
    layoutParams.height = dimensionPixelSize
}