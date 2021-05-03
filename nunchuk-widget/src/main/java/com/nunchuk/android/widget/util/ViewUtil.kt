package com.nunchuk.android.widget.util

import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.annotation.LayoutRes
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

fun NCEditTextView.passwordEnabled() {
    val editText = findViewById<EditText>(R.id.editText)
    editText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
    editText.transformationMethod = PasswordTransformationMethod.getInstance()
}

fun EditText.heightExtended(dimensionPixelSize: Int) {
    isSingleLine = false
    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
    layoutParams.height = dimensionPixelSize
}

fun ViewGroup.inflate(@LayoutRes resourceId: Int): View {
    return LayoutInflater
        .from(context)
        .inflate(resourceId, this, false)
}