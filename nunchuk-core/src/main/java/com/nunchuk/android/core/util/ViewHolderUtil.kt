package com.nunchuk.android.core.util

import android.text.Spanned
import androidx.annotation.StringRes
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView

fun RecyclerView.ViewHolder.getString(
    @StringRes resId: Int
) = itemView.context.getString(resId)

fun RecyclerView.ViewHolder.getHtmlString(
    @StringRes resId: Int
): Spanned = HtmlCompat.fromHtml(itemView.context.getString(resId), HtmlCompat.FROM_HTML_MODE_COMPACT)

fun RecyclerView.ViewHolder.getHtmlString(
    @StringRes resId: Int,
    arg0: String,
    arg1: String
): Spanned = HtmlCompat.fromHtml(itemView.context.getString(resId, arg0, arg1), HtmlCompat.FROM_HTML_MODE_COMPACT)

fun RecyclerView.ViewHolder.getHtmlString(
    @StringRes resId: Int,
    arg: String
): Spanned = itemView.context.getHtmlText(resId, arg)