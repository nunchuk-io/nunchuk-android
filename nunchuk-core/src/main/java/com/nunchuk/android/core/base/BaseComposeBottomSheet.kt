package com.nunchuk.android.core.base

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.nunchuk.android.core.R
import com.nunchuk.android.widget.util.addStateChangedCallback
import com.nunchuk.android.widget.util.expandDialog

abstract class BaseComposeBottomSheet : BottomSheetDialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.NCBottomSheetDialogStyle)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        dialog?.setOnShowListener(DialogInterface::expandDialog)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        addStateChangedCallback(
            onExpanded = ::onExpanded, onCollapsed = ::onCollapsed, onHidden = ::onHidden
        )
    }

    open fun onExpanded() {}

    open fun onCollapsed() {
        dismissAllowingStateLoss()
    }

    open fun onHidden() {
        dismissAllowingStateLoss()
    }
}