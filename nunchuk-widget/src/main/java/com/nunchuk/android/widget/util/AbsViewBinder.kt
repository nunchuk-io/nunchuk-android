/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.widget.util

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding

abstract class AbsViewBinder<in Model, out Binding : ViewBinding> protected constructor(
    protected val container: ViewGroup,
    private val models: List<Model>
) {

    protected abstract fun initializeBinding(): Binding

    protected val context: Context = container.context

    protected val inflater: LayoutInflater = LayoutInflater.from(context)

    protected abstract fun bindItem(position: Int, model: Model)

    fun bindItems() {
        val size = models.size
        val childCount = container.childCount
        if (childCount == 0 || childCount < size) {
            addChildViews(container, size - childCount)
        } else if (childCount > size) {
            container.removeViews(0, childCount - size)
        }
        for (i in 0 until size) {
            bindItem(i, models[i])
        }
    }

    private fun addChildViews(container: ViewGroup, size: Int) {
        for (i in 0 until size) {
            container.addView(initializeBinding().root)
        }
    }

}

