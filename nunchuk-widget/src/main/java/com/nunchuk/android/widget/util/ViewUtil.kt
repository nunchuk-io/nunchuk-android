package com.nunchuk.android.widget.util

import android.content.DialogInterface
import android.text.InputFilter
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.annotation.LayoutRes
import com.google.android.material.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.nunchuk.android.widget.NCEditTextView

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

fun DialogInterface.expandDialog() {
    val bottomSheetDialog = this as BottomSheetDialog
    val designBottomSheet: View? = bottomSheetDialog.findViewById(R.id.design_bottom_sheet)
    designBottomSheet?.let {
        BottomSheetBehavior.from(it).state = BottomSheetBehavior.STATE_EXPANDED
    }
}

fun BottomSheetDialogFragment.addStateChangedCallback(
    onExpanded: () -> Unit = {},
    onCollapsed: () -> Unit = {},
    onHidden: () -> Unit = {}
) {
    (dialog?.findViewById(R.id.design_bottom_sheet) as ViewGroup?)?.let {
        val bottomSheetBehavior = BottomSheetBehavior.from(it)
        bottomSheetBehavior.peekHeight = BottomSheetBehavior.PEEK_HEIGHT_AUTO
        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(view: View, i: Int) {
                if (BottomSheetBehavior.STATE_EXPANDED == i) {
                    onExpanded()
                }
                if (BottomSheetBehavior.STATE_COLLAPSED == i) {
                    onCollapsed()
                }
                if (BottomSheetBehavior.STATE_HIDDEN == i) {
                    dismissAllowingStateLoss()
                    onHidden()
                }
            }

            override fun onSlide(view: View, v: Float) {}
        })
    }

}

fun EditText.setOnEnterOrSpaceListener(callback: () -> Unit) {
    setOnKeyListener(object : View.OnKeyListener {
        override fun onKey(v: View, keyCode: Int, event: KeyEvent): Boolean {
            if (event.action == KeyEvent.ACTION_DOWN && (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_SPACE)) {
                callback()
                return true
            }
            return false
        }
    })
}

fun EditText.setOnEnterListener(callback: () -> Unit) {
    setOnKeyListener(object : View.OnKeyListener {
        override fun onKey(v: View, keyCode: Int, event: KeyEvent): Boolean {
            if (event.action == KeyEvent.ACTION_DOWN && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                callback()
                return true
            }
            return false
        }
    })
}



