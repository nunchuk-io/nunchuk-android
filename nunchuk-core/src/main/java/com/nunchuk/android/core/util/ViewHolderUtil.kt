package com.nunchuk.android.core.util

import android.text.Html
import android.text.Spanned
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView

fun RecyclerView.ViewHolder.getString(
    @StringRes resId: Int
) = itemView.context.getString(resId)

fun RecyclerView.ViewHolder.getString(
    @StringRes resId: Int,
    arg0: String,
    arg1: String
): Spanned = Html.fromHtml(itemView.context.getString(resId, arg0, arg1))

fun RecyclerView.ViewHolder.getString(
    @StringRes resId: Int,
    arg: String
): Spanned = Html.fromHtml(itemView.context.getString(resId, arg))