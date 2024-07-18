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

package com.nunchuk.android.auth.components.verify

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.ForegroundColorSpan
import android.text.style.MetricAffectingSpan
import android.text.style.UnderlineSpan
import android.view.MotionEvent
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.content.res.ResourcesCompat
import com.nunchuk.android.auth.R
import com.nunchuk.android.auth.components.verify.VerifyNewDeviceEvent.ProcessErrorEvent
import com.nunchuk.android.auth.components.verify.VerifyNewDeviceEvent.ProcessingEvent
import com.nunchuk.android.auth.components.verify.VerifyNewDeviceEvent.SignInSuccessEvent
import com.nunchuk.android.auth.databinding.ActivityVerifyNewDeviceBinding
import com.nunchuk.android.auth.util.getTextTrimmed
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.utils.NotificationUtils
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setTransparentStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class VerifyNewDeviceActivity : BaseActivity<ActivityVerifyNewDeviceBinding>() {

    private val viewModel: VerifyNewDeviceViewModel by viewModels()

    private val email
        get() = intent.getStringExtra(EXTRAS_EMAIL)
    private val loginHalfToken
        get() = intent.getStringExtra(EXTRAS_LOGIN_HALF_TOKEN)
    private val deviceId
        get() = intent.getStringExtra(EXTRAS_DEVICE_ID)
    private val staySignedIn
        get() = intent.getBooleanExtra(EXTRAS_STAY_SIGNED_IN, false)

    private var startResendText = 0
    private var endResendText = 0

    override fun initializeBinding() = ActivityVerifyNewDeviceBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTransparentStatusBar(false)
        setupViews()
        observeEvent()
    }

    private fun showToolbarBackButton() {
        setSupportActionBar(binding.toolbarVerifyScreen)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    private fun observeEvent() {
        viewModel.event.observe(this) {
            when (it) {
                is ProcessErrorEvent -> onHandleError(it.message)
                is SignInSuccessEvent -> {
                    openMainScreen(it.token, it.encryptedDeviceId)
                }

                is ProcessingEvent -> showLoading()
                is VerifyNewDeviceEvent.ResendVerifyCodeSuccessEvent -> {
                    binding.tvConfirmInstruction.text = getConfirmInstructionText()
                    hideLoading()
                }
            }
        }
    }

    private fun onHandleError(message: String) {
        hideLoading()
        NCToastMessage(this).showError(message)
    }

    private fun openMainScreen(token: String, deviceId: String) {
        hideLoading()
        setResult(RESULT_OK)
        finish()
        if (NotificationUtils.areNotificationsEnabled(this).not()) {
            navigator.openTurnNotificationScreen(this)
        } else {
            navigator.openMainScreen(
                activityContext = this,
                isClearTask = true
            )
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupViews() {
        binding.btnContinue.setOnClickListener { onVerifyNewDeviceClick() }
        showToolbarBackButton()
        binding.tvConfirmInstruction.text = getConfirmInstructionText()
        binding.tvConfirmInstruction.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val x = event.x.toInt()
                val y = event.y.toInt()

                val layout = (v as TextView).layout
                val line = layout.getLineForVertical(y)
                val offset = layout.getOffsetForHorizontal(line, x.toFloat())

                if (offset in startResendText until endResendText) {
                    viewModel.handleResendVerifyNewDeviceCode(
                        email = email.orEmpty(),
                        loginHalfToken = loginHalfToken.orEmpty(),
                        deviceId = deviceId.orEmpty(),
                    )
                    return@setOnTouchListener true
                }
            }
            return@setOnTouchListener false
        }
    }

    private fun getConfirmInstructionText(): SpannableString {
        val resendText = getString(R.string.nc_text_resend_code)
        val confirmInstructionText =
            "${getString(R.string.nc_text_verify_instruction, email)} $resendText"
        val start = confirmInstructionText.length - resendText.length
        val end = confirmInstructionText.length
        startResendText = start
        endResendText = end
        val textSpannable = SpannableString(confirmInstructionText).apply {
            setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(
                ForegroundColorSpan(getColor(R.color.nc_primary_color)),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            runCatching {
                val typefaceSpan =
                    Typeface.create(
                        ResourcesCompat.getFont(
                            this@VerifyNewDeviceActivity,
                            R.font.lato_bold
                        ), Typeface.NORMAL
                    )
                val customTypefaceSpan = CustomTypefaceSpan(typefaceSpan)
                setSpan(customTypefaceSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        return textSpannable
    }

    private class CustomTypefaceSpan(private val typeface: Typeface) : MetricAffectingSpan() {
        override fun updateMeasureState(p: TextPaint) {
            p.typeface = typeface
            p.flags = p.flags or Paint.SUBPIXEL_TEXT_FLAG
        }

        override fun updateDrawState(tp: TextPaint) {
            tp.typeface = typeface
            tp.flags = tp.flags or Paint.SUBPIXEL_TEXT_FLAG
        }
    }

    private fun onVerifyNewDeviceClick() {
        viewModel.handleVerifyNewDevice(
            email = email.orEmpty(),
            loginHalfToken = loginHalfToken.orEmpty(),
            pin = binding.edtConfirmCode.getTextTrimmed(),
            deviceId = deviceId.orEmpty(),
            staySignedIn = staySignedIn
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    // FIXME extract/wrap serialize/deserialize logic with ActivityArgs
    companion object {
        const val EXTRAS_EMAIL = "EXTRAS_EMAIL"
        const val EXTRAS_LOGIN_HALF_TOKEN = "EXTRAS_LOGIN_HALF_TOKEN"
        const val EXTRAS_DEVICE_ID = "EXTRAS_DEVICE_ID"
        const val EXTRAS_STAY_SIGNED_IN = "EXTRAS_STAY_SIGNED_IN"

        fun buildIntent(
            activityContext: Context,
            email: String,
            loginHalfToken: String,
            deviceId: String,
            staySignedIn: Boolean
        ): Intent {
            return Intent(activityContext, VerifyNewDeviceActivity::class.java).apply {
                putExtra(EXTRAS_EMAIL, email)
                putExtra(EXTRAS_LOGIN_HALF_TOKEN, loginHalfToken)
                putExtra(EXTRAS_DEVICE_ID, deviceId)
                putExtra(EXTRAS_STAY_SIGNED_IN, staySignedIn)
            }
        }
    }

}