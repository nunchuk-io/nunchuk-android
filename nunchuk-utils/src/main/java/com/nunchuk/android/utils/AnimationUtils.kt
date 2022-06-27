package com.nunchuk.android.utils

import android.view.View
import android.view.animation.AccelerateInterpolator
import kotlin.math.max
import kotlin.math.min

private enum class FadeAnimation { FADE_IN, FADE_OUT }

private const val DURATION: Long = 300

fun View.animateVisibility(isVisible: Boolean = true, duration: Long = DURATION) {
    if (isVisible) {
        fadeInAnimation(duration = duration)
    } else {
        fadeOutAnimation(duration = duration)
    }
}

fun View.fadeInAnimation(
    maxAlpha: Float = 1.0f,
    duration: Long = DURATION,
    visibilityDistinct: Boolean = true,
    onComplete: (() -> Unit) = {}
) = fadeInOutAnimation(FadeAnimation.FADE_IN, maxAlpha, duration, visibilityDistinct, onComplete)

fun View.fadeOutAnimation(
    maxAlpha: Float = 0.0f,
    duration: Long = DURATION,
    visibilityDistinct: Boolean = true,
    onComplete: (() -> Unit) = {}
) = fadeInOutAnimation(FadeAnimation.FADE_OUT, maxAlpha, duration, visibilityDistinct, onComplete)

private fun View.fadeInOutAnimation(
    type: FadeAnimation,
    maxAlpha: Float,
    duration: Long,
    visibilityDistinct: Boolean,
    onComplete: (() -> Unit) = {}
) {
    if (FadeAnimation.FADE_IN == type && visibilityDistinct && View.VISIBLE == visibility) return

    if (FadeAnimation.FADE_OUT == type && (View.GONE == visibility || View.INVISIBLE == visibility)) return

    val animator = animate()
    when (type) {
        FadeAnimation.FADE_IN -> {
            visibility = View.VISIBLE
            alpha = 0.0f
            val actualAlpha = min(maxAlpha, 1.0f)
            animator
                .alpha(actualAlpha)
                .setDuration(duration)
                .setInterpolator(AccelerateInterpolator())
                .withEndAction { onComplete() }.start()
        }
        FadeAnimation.FADE_OUT -> {
            val actualAlpha = max(maxAlpha, 0.0f)
            animator
                .alpha(actualAlpha)
                .setDuration(duration)
                .setInterpolator(AccelerateInterpolator())
                .withEndAction {
                    if (0.0f == actualAlpha) {
                        visibility = View.GONE
                    }
                    onComplete()
                }.start()
        }
    }
}
