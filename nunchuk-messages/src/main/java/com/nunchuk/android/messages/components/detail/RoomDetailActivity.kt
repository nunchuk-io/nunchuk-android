/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
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

package com.nunchuk.android.messages.components.detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.fragment.NavHostFragment
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.constants.RoomAction
import com.nunchuk.android.messages.R
import com.nunchuk.android.messages.components.detail.viewer.RoomMediaViewerFragment
import com.nunchuk.android.messages.databinding.ActivityRoomDetailBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RoomDetailActivity : BaseActivity<ActivityRoomDetailBinding>() {

    private val navHostFragment by lazy(LazyThreadSafetyMode.NONE) {
        supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
    }

    override fun initializeBinding(): ActivityRoomDetailBinding {
        return ActivityRoomDetailBinding.inflate(layoutInflater).also {
            enableEdgeToEdge()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val inflater = navHostFragment.navController.navInflater
        val graph = inflater.inflate(R.navigation.nav_room_detail)
        navHostFragment.navController.setGraph(graph, intent.extras)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val fragment = navHostFragment.childFragmentManager.primaryNavigationFragment
        if (fragment is RoomMediaViewerFragment) {
            return fragment.dispatchTouchEvent(ev)
        }
        return super.dispatchTouchEvent(ev)
    }

    fun superDispatchTouchEvent(ev: MotionEvent) = super.dispatchTouchEvent(ev)

    companion object {
        fun start(
            activityContext: Context,
            roomId: String,
            roomAction: RoomAction = RoomAction.NONE,
            isGroupChat: Boolean = false
        ) {
            activityContext.startActivity(
                Intent(activityContext, RoomDetailActivity::class.java).apply {
                    putExtras(
                        RoomDetailFragmentArgs(
                            roomId = roomId,
                            roomAction = roomAction,
                            isGroupChat = isGroupChat
                        ).toBundle()
                    )
                }
            )
        }
    }
}