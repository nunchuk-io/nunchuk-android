package com.nunchuk.android.main.components.tabs.wallet.totalbalance

import android.util.TypedValue
import android.view.View

class TotalBalanceScrollHandler(private val totalBalanceFrame: View) {

    private var totalScrollDistance = 0 // Track the total scroll distance
    private var threshold = 20 // Threshold in dp
    private var isBalanceFrameVisible = true // Track visibility state

    private val thresholdInPx by lazy {
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            threshold.toFloat(),
            totalBalanceFrame.resources.displayMetrics
        ).toInt()
    }

    fun handleScrollChange(scrollY: Int, oldScrollY: Int) {
        val scrollDelta = scrollY - oldScrollY

        if (scrollDelta > 0) {
            // Scrolling down
            totalScrollDistance += scrollDelta
            if (totalScrollDistance > thresholdInPx && isBalanceFrameVisible) {
                hideBalanceFrameWithAnimation()
                isBalanceFrameVisible = false
            }
        } else if (scrollDelta < 0) {
            // Scrolling up
            totalScrollDistance += scrollDelta
            if (totalScrollDistance < -thresholdInPx && !isBalanceFrameVisible) {
                showBalanceFrameWithAnimation()
                isBalanceFrameVisible = true
            }
        }

        // Reset totalScrollDistance when changing scroll direction
        if (scrollDelta > 0 && totalScrollDistance < 0) {
            totalScrollDistance = 0
        } else if (scrollDelta < 0 && totalScrollDistance > 0) {
            totalScrollDistance = 0
        }
    }

    private fun hideBalanceFrameWithAnimation() {
        totalBalanceFrame.animate()
            .translationY(totalBalanceFrame.height.toFloat())
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                totalBalanceFrame.visibility = View.GONE
            }
            .start()
    }

    private fun showBalanceFrameWithAnimation() {
        totalBalanceFrame.visibility = View.VISIBLE
        totalBalanceFrame.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(200)
            .start()
    }

}