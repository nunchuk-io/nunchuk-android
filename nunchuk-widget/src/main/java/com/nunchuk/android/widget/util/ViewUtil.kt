package com.nunchuk.android.widget.util

import android.content.DialogInterface
import android.text.InputFilter
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.annotation.LayoutRes
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
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
    val listener = object : TextWatcherAdapter() {
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            offer(s)
        }
    }
    addTextChangedListener(listener)
    awaitClose { removeTextChangedListener(listener) }
}.onStart { emit(text) }

fun NCEditTextView.heightExtended(dimensionPixelSize: Int) {
    getEditTextView().heightExtended(dimensionPixelSize)
}

fun NCEditTextView.passwordEnabled() {
    val editText = getEditTextView()
    editText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
    editText.transformationMethod = PasswordTransformationMethod.getInstance()
}

fun NCEditTextView.setMaxLength(maxLength: Int) {
    getEditTextView().filters = arrayOf(InputFilter.LengthFilter(maxLength))
}

fun EditText.setMaxLength(maxLength: Int) {
    filters = arrayOf(InputFilter.LengthFilter(maxLength))
}

fun EditText.heightExtended(dimensionPixelSize: Int) {
    isSingleLine = false
    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
    layoutParams.height = dimensionPixelSize
}

fun NCEditTextView.addTextChangedCallback(callback: (String) -> Unit) {
    getEditTextView().addTextChangedCallback(callback)
}

fun EditText.addTextChangedCallback(callback: (String) -> Unit) {
    addTextChangedListener(object : TextWatcherAdapter() {
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            callback("$s")
        }
    })
}

fun ViewGroup.inflate(@LayoutRes resourceId: Int): View {
    return LayoutInflater
        .from(context)
        .inflate(resourceId, this, false)
}

fun EditText.keepCursorLast() {
    setSelection(text.length)
}

fun DialogInterface.expandDialog() {
    val bottomSheetDialog = this as BottomSheetDialog
    val designBottomSheet: View? = bottomSheetDialog.findViewById(R.id.design_bottom_sheet)
    designBottomSheet.run {
        this?.let { BottomSheetBehavior.from(it).state = BottomSheetBehavior.STATE_EXPANDED }
    }
}


