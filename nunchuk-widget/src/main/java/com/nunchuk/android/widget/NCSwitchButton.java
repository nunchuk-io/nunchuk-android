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

package com.nunchuk.android.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Checkable;

import androidx.core.content.ContextCompat;

public class NCSwitchButton extends View implements Checkable {
    private static final int DEFAULT_WIDTH = dp2pxInt(58);
    private static final int DEFAULT_HEIGHT = dp2pxInt(36);

    private static final int ANIMATE_STATE_NONE = 0;
    private static final int ANIMATE_STATE_PENDING_DRAG = 1;
    private static final int ANIMATE_STATE_DRAGING = 2;
    private static final int ANIMATE_STATE_PENDING_RESET = 3;
    private static final int ANIMATE_STATE_PENDING_SETTLE = 4;
    private static final int ANIMATE_STATE_SWITCH = 5;
    private final android.animation.ArgbEvaluator argbEvaluator = new android.animation.ArgbEvaluator();
    private int shadowRadius;
    private int shadowOffset;
    private int shadowColor;
    private float viewRadius;
    private float buttonRadius;
    private float height;
    private float left;
    private float top;
    private float right;
    private float bottom;
    private float centerY;
    private int background;
    private int uncheckColor;
    private int checkedColor;
    private int borderWidth;
    private int checkLineColor;
    private int checkLineWidth;
    private float checkLineLength;
    private int uncheckCircleColor;
    private int uncheckCircleWidth;
    private float uncheckCircleOffsetX;
    private float uncheckCircleRadius;
    private float checkedLineOffsetX;
    private float checkedLineOffsetY;
    private int uncheckButtonColor;
    private int checkedButtonColor;
    private float buttonMinX;
    private float buttonMaxX;
    private Paint buttonPaint;
    private Paint paint;
    private ViewState viewState;
    private ViewState beforeState;
    private ViewState afterState;
    private int animateState = ANIMATE_STATE_NONE;
    private final ValueAnimator.AnimatorUpdateListener animatorUpdateListener = new ValueAnimator.AnimatorUpdateListener() {

        @Override
        public void onAnimationUpdate(final ValueAnimator animation) {
            final float value = (Float) animation.getAnimatedValue();
            switch (animateState) {
                case ANIMATE_STATE_PENDING_RESET: {
                }
                case ANIMATE_STATE_PENDING_DRAG: {
                    viewState.checkedLineColor = (int) argbEvaluator.evaluate(
                            value,
                            beforeState.checkedLineColor,
                            afterState.checkedLineColor
                    );

                    viewState.radius = beforeState.radius + (afterState.radius - beforeState.radius) * value;

                    if (animateState != ANIMATE_STATE_PENDING_DRAG) {
                        viewState.buttonX = beforeState.buttonX + (afterState.buttonX - beforeState.buttonX) * value;
                    }

                    viewState.checkStateColor = (int) argbEvaluator.evaluate(
                            value,
                            beforeState.checkStateColor,
                            afterState.checkStateColor
                    );

                    break;
                }
                case ANIMATE_STATE_SWITCH: {
                    viewState.buttonX = beforeState.buttonX + (afterState.buttonX - beforeState.buttonX) * value;

                    final float fraction = (viewState.buttonX - buttonMinX) / (buttonMaxX - buttonMinX);

                    viewState.checkStateColor = (int) argbEvaluator.evaluate(fraction, uncheckColor, checkedColor);

                    viewState.radius = fraction * viewRadius;
                    viewState.checkedLineColor = (int) argbEvaluator.evaluate(fraction, Color.TRANSPARENT, checkLineColor);
                    break;
                }
                default:
                case ANIMATE_STATE_DRAGING: {
                }
                case ANIMATE_STATE_NONE: {
                    break;
                }
            }
            postInvalidate();
        }
    };
    private ValueAnimator valueAnimator;
    private boolean isChecked;
    private boolean enableEffect;
    private boolean shadowEffect;
    private boolean showIndicator;
    private boolean isTouchingDown;
    private boolean enableAnimate;
    private final Runnable postPendingDrag = () -> {
        if (!isInAnimating()) {
            pendingDragState();
        }
    };
    private boolean isUiInited;
    private boolean isEventBroadcast;
    private OnCheckedChangeListener onCheckedChangeListener;
    private final Animator.AnimatorListener animatorListener = new AnimatorListenerAdapter() {

        @Override
        public void onAnimationEnd(final Animator animation) {
            switch (animateState) {
                case ANIMATE_STATE_PENDING_DRAG: {
                    animateState = ANIMATE_STATE_DRAGING;
                    viewState.checkedLineColor = Color.TRANSPARENT;
                    viewState.radius = viewRadius;

                    postInvalidate();
                    break;
                }
                case ANIMATE_STATE_PENDING_RESET: {
                    animateState = ANIMATE_STATE_NONE;
                    postInvalidate();
                    break;
                }
                case ANIMATE_STATE_PENDING_SETTLE: {
                    animateState = ANIMATE_STATE_NONE;
                    postInvalidate();
                    broadcastEvent();
                    break;
                }
                case ANIMATE_STATE_SWITCH: {
                    isChecked = !isChecked;
                    animateState = ANIMATE_STATE_NONE;
                    postInvalidate();
                    broadcastEvent();
                    break;
                }
                default:
                case ANIMATE_STATE_NONE: {
                    break;
                }
            }
        }

    };
    private long touchDownTime;

    public NCSwitchButton(final Context context) {
        super(context);
        init(context, null);
    }

    public NCSwitchButton(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public NCSwitchButton(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public NCSwitchButton(final Context context, final AttributeSet attrs, final int defStyleAttr, final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private static float dp2px(final float dp) {
        final Resources r = Resources.getSystem();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
    }

    private static int dp2pxInt(final float dp) {
        return (int) dp2px(dp);
    }

    private static int optInt(final TypedArray typedArray, final int index, final int def) {
        if (typedArray == null) {
            return def;
        }
        return typedArray.getInt(index, def);
    }

    private static float optPixelSize(final TypedArray typedArray, final int index, final float def) {
        if (typedArray == null) {
            return def;
        }
        return typedArray.getDimension(index, def);
    }

    private static int optPixelSize(final TypedArray typedArray, final int index, final int def) {
        if (typedArray == null) {
            return def;
        }
        return typedArray.getDimensionPixelOffset(index, def);
    }

    private static int optColor(final TypedArray typedArray, final int index, final int def) {
        if (typedArray == null) {
            return def;
        }
        return typedArray.getColor(index, def);
    }

    private static boolean optBoolean(final TypedArray typedArray, final int index, final boolean def) {
        if (typedArray == null) {
            return def;
        }
        return typedArray.getBoolean(index, def);
    }

    @Override
    public final void setPadding(final int left, final int top, final int right, final int bottom) {
        super.setPadding(0, 0, 0, 0);
    }

    private void init(final Context context, final AttributeSet attrs) {
        TypedArray typedArray = null;
        if (attrs != null) {
            typedArray = context.obtainStyledAttributes(attrs, R.styleable.SwitchButton);
        }

        shadowEffect = optBoolean(typedArray, R.styleable.SwitchButton_sb_shadow_effect, true);

        uncheckCircleColor = optColor(typedArray, R.styleable.SwitchButton_sb_uncheckcircle_color, 0XffAAAAAA);//0XffAAAAAA;

        uncheckCircleWidth = optPixelSize(typedArray, R.styleable.SwitchButton_sb_uncheckcircle_width, dp2pxInt(1.5f));//dp2pxInt(1.5f);

        uncheckCircleOffsetX = dp2px(10);

        uncheckCircleRadius = optPixelSize(typedArray, R.styleable.SwitchButton_sb_uncheckcircle_radius, dp2px(4));//dp2px(4);

        checkedLineOffsetX = dp2px(4);
        checkedLineOffsetY = dp2px(4);

        shadowRadius = optPixelSize(typedArray, R.styleable.SwitchButton_sb_shadow_radius, dp2pxInt(2.5f));//dp2pxInt(2.5f);

        shadowOffset = optPixelSize(typedArray, R.styleable.SwitchButton_sb_shadow_offset, dp2pxInt(1.5f));//dp2pxInt(1.5f);

        shadowColor = optColor(typedArray, R.styleable.SwitchButton_sb_shadow_color, 0X33000000);//0X33000000;

        uncheckColor = optColor(typedArray, R.styleable.SwitchButton_sb_uncheck_color, 0XffDDDDDD);//0XffDDDDDD;

        checkedColor = ContextCompat.getColor(context, R.color.nc_control_state_activated);

        borderWidth = optPixelSize(typedArray, R.styleable.SwitchButton_sb_border_width, dp2pxInt(1));//dp2pxInt(1);

        checkLineColor = optColor(typedArray, R.styleable.SwitchButton_sb_checkline_color, Color.WHITE);//Color.WHITE;

        checkLineWidth = optPixelSize(typedArray, R.styleable.SwitchButton_sb_checkline_width, dp2pxInt(1f));//dp2pxInt(1.0f);

        checkLineLength = dp2px(6);

        final int buttonColor = optColor(typedArray, R.styleable.SwitchButton_sb_button_color, Color.WHITE);//Color.WHITE;

        uncheckButtonColor = optColor(typedArray, R.styleable.SwitchButton_sb_uncheckbutton_color, buttonColor);

        checkedButtonColor = optColor(typedArray, R.styleable.SwitchButton_sb_checkedbutton_color, buttonColor);

        final int effectDuration = optInt(typedArray, R.styleable.SwitchButton_sb_effect_duration, 300);//300;

        isChecked = optBoolean(typedArray, R.styleable.SwitchButton_sb_checked, false);

        showIndicator = optBoolean(typedArray, R.styleable.SwitchButton_sb_show_indicator, true);

        background = optColor(typedArray, R.styleable.SwitchButton_sb_background, Color.WHITE);

        enableEffect = optBoolean(typedArray, R.styleable.SwitchButton_sb_enable_effect, true);

        enableAnimate = optBoolean(typedArray, R.styleable.SwitchButton_sb_enable_animate, false);

        if (typedArray != null) {
            typedArray.recycle();
        }

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        buttonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        buttonPaint.setColor(buttonColor);

        if (shadowEffect) {
            buttonPaint.setShadowLayer(shadowRadius, 0, shadowOffset, shadowColor);
        }

        viewState = new ViewState();
        beforeState = new ViewState();
        afterState = new ViewState();

        valueAnimator = ValueAnimator.ofFloat(0f, 1f);
        valueAnimator.setDuration(effectDuration);
        valueAnimator.setRepeatCount(0);

        valueAnimator.addUpdateListener(animatorUpdateListener);
        valueAnimator.addListener(animatorListener);

        super.setClickable(true);
        this.setPadding(0, 0, 0, 0);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        if (widthMode == MeasureSpec.UNSPECIFIED || widthMode == MeasureSpec.AT_MOST) {
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(DEFAULT_WIDTH, MeasureSpec.EXACTLY);
        }
        if (heightMode == MeasureSpec.UNSPECIFIED || heightMode == MeasureSpec.AT_MOST) {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(DEFAULT_HEIGHT, MeasureSpec.EXACTLY);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        final float viewPadding = Math.max(shadowRadius + shadowOffset, borderWidth);

        height = h - viewPadding - viewPadding;

        viewRadius = height * .5f;
        buttonRadius = viewRadius - borderWidth;

        left = viewPadding;
        top = viewPadding;
        right = w - viewPadding;
        bottom = h - viewPadding;

        centerY = (top + bottom) * .5f;

        buttonMinX = left + viewRadius;
        buttonMaxX = right - viewRadius;

        if (isChecked) {
            setCheckedViewState(viewState);
        } else {
            setUncheckViewState(viewState);
        }

        isUiInited = true;

        postInvalidate();
    }

    private void setUncheckViewState(final ViewState viewState) {
        viewState.radius = 0;
        viewState.checkStateColor = uncheckColor;
        viewState.checkedLineColor = Color.TRANSPARENT;
        viewState.buttonX = buttonMinX;
        buttonPaint.setColor(uncheckButtonColor);
    }

    private void setCheckedViewState(final ViewState viewState) {
        viewState.radius = viewRadius;
        viewState.checkStateColor = checkedColor;
        viewState.checkedLineColor = checkLineColor;
        viewState.buttonX = buttonMaxX;
        buttonPaint.setColor(checkedButtonColor);
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);

        paint.setStrokeWidth(borderWidth);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(background);
        drawRoundRect(canvas, left, top, right, bottom, viewRadius, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(uncheckColor);
        drawRoundRect(canvas, left, top, right, bottom, viewRadius, paint);

        if (showIndicator) {
            drawUncheckIndicator(canvas);
        }

        final float des = viewState.radius * .5f;
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(viewState.checkStateColor);
        paint.setStrokeWidth(borderWidth + des * 2f);
        drawRoundRect(canvas, left + des, top + des, right - des, bottom - des, viewRadius, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(1);
        drawArc(canvas, left, top, left + 2 * viewRadius, top + 2 * viewRadius, 90, 180, paint);
        canvas.drawRect(left + viewRadius, top, viewState.buttonX, top + 2 * viewRadius, paint);

        if (showIndicator) {
            drawCheckedIndicator(canvas);
        }

        drawButton(canvas, viewState.buttonX, centerY);
    }

    protected void drawCheckedIndicator(final Canvas canvas) {
        drawCheckedIndicator(canvas,
                viewState.checkedLineColor,
                checkLineWidth,
                left + viewRadius - checkedLineOffsetX, centerY - checkLineLength,
                left + viewRadius - checkedLineOffsetY, centerY + checkLineLength,
                paint);
    }

    protected void drawCheckedIndicator(final Canvas canvas, final int color, final float lineWidth, final float sx, final float sy, final float ex, final float ey, final Paint paint) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(color);
        paint.setStrokeWidth(lineWidth);
        canvas.drawLine(sx, sy, ex, ey, paint);
    }

    private void drawUncheckIndicator(final Canvas canvas) {
        drawUncheckIndicator(canvas, uncheckCircleColor, uncheckCircleWidth, right - uncheckCircleOffsetX, centerY, uncheckCircleRadius, paint);
    }

    protected void drawUncheckIndicator(final Canvas canvas,
                                        final int color,
                                        final float lineWidth,
                                        final float centerX, final float centerY,
                                        final float radius,
                                        final Paint paint) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(color);
        paint.setStrokeWidth(lineWidth);
        canvas.drawCircle(centerX, centerY, radius, paint);
    }

    private void drawArc(final Canvas canvas,
                         final float left, final float top,
                         final float right, final float bottom,
                         final float startAngle, final float sweepAngle,
                         final Paint paint) {
        canvas.drawArc(left, top, right, bottom, startAngle, sweepAngle, true, paint);
    }

    private void drawRoundRect(final Canvas canvas,
                               final float left, final float top,
                               final float right, final float bottom,
                               final float backgroundRadius,
                               final Paint paint) {
        canvas.drawRoundRect(left, top, right, bottom, backgroundRadius, backgroundRadius, paint);
    }

    private void drawButton(final Canvas canvas, final float x, final float y) {
        canvas.drawCircle(x, y, buttonRadius, buttonPaint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);
        paint.setColor(0XffDDDDDD);
        canvas.drawCircle(x, y, buttonRadius, paint);
    }

    @Override
    public boolean isChecked() {
        return isChecked;
    }

    @Override
    public void setChecked(final boolean checked) {
        if (checked == isChecked) {
            postInvalidate();
            return;
        }
        toggle(enableEffect, false);
    }

    @Override
    public void toggle() {
        toggle(true);
    }

    public void toggle(final boolean animate) {
        toggle(animate, true);
    }

    private void toggle(final boolean animate, final boolean broadcast) {
        if (!isEnabled()) {
            return;
        }

        if (isEventBroadcast) {
            throw new RuntimeException("should NOT switch the state in method: [onCheckedChanged]!");
        }
        if (!isUiInited) {
            isChecked = !isChecked;
            if (broadcast) {
                broadcastEvent();
            }
            return;
        }

        if (valueAnimator.isRunning()) {
            valueAnimator.cancel();
        }

        final var isNeedAnimate = animate && enableAnimate;

        if (!enableEffect || !isNeedAnimate) {
            isChecked = !isChecked;
            if (isChecked) {
                setCheckedViewState(viewState);
            } else {
                setUncheckViewState(viewState);
            }
            postInvalidate();
            if (broadcast) {
                broadcastEvent();
            }
            return;
        }

        animateState = ANIMATE_STATE_SWITCH;
        beforeState.copy(viewState);

        if (isChecked) {
            setUncheckViewState(afterState);
        } else {
            setCheckedViewState(afterState);
        }
        valueAnimator.start();
    }

    private void broadcastEvent() {
        if (onCheckedChangeListener != null) {
            isEventBroadcast = true;
            onCheckedChangeListener.onCheckedChanged(this, isChecked);
        }
        isEventBroadcast = false;
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }
        final int actionMasked = event.getActionMasked();

        switch (actionMasked) {
            case MotionEvent.ACTION_DOWN: {
                isTouchingDown = true;
                touchDownTime = System.currentTimeMillis();
                removeCallbacks(postPendingDrag);
                postDelayed(postPendingDrag, 100);
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                final float eventX = event.getX();
                if (isPendingDragState()) {
                    float fraction = eventX / getWidth();
                    fraction = Math.max(0f, Math.min(1f, fraction));

                    viewState.buttonX = buttonMinX + (buttonMaxX - buttonMinX) * fraction;

                } else if (isDragState()) {
                    float fraction = eventX / getWidth();
                    fraction = Math.max(0f, Math.min(1f, fraction));

                    viewState.buttonX = buttonMinX + (buttonMaxX - buttonMinX) * fraction;

                    viewState.checkStateColor = (int) argbEvaluator.evaluate(fraction, uncheckColor, checkedColor);
                    postInvalidate();

                }
                break;
            }
            case MotionEvent.ACTION_UP: {
                isTouchingDown = false;
                removeCallbacks(postPendingDrag);

                if (System.currentTimeMillis() - touchDownTime <= 300) {
                    toggle();
                } else if (isDragState()) {
                    final float eventX = event.getX();
                    float fraction = eventX / getWidth();
                    fraction = Math.max(0f, Math.min(1f, fraction));
                    final boolean newCheck = fraction > .5f;
                    if (newCheck == isChecked) {
                        pendingCancelDragState();
                    } else {
                        isChecked = newCheck;
                        pendingSettleState();
                    }
                } else if (isPendingDragState()) {
                    pendingCancelDragState();
                }
                break;
            }
            case MotionEvent.ACTION_CANCEL: {
                isTouchingDown = false;

                removeCallbacks(postPendingDrag);

                if (isPendingDragState() || isDragState()) {
                    pendingCancelDragState();
                }
                break;
            }
        }
        return true;
    }

    private boolean isInAnimating() {
        return animateState != ANIMATE_STATE_NONE;
    }

    private boolean isPendingDragState() {
        return animateState == ANIMATE_STATE_PENDING_DRAG || animateState == ANIMATE_STATE_PENDING_RESET;
    }

    private boolean isDragState() {
        return animateState == ANIMATE_STATE_DRAGING;
    }

    public void setShadowEffect(final boolean shadowEffect) {
        if (this.shadowEffect == shadowEffect) {
            return;
        }
        this.shadowEffect = shadowEffect;

        if (this.shadowEffect) {
            buttonPaint.setShadowLayer(shadowRadius, 0, shadowOffset, shadowColor);
        } else {
            buttonPaint.setShadowLayer(0, 0, 0, 0);
        }
    }

    public void setEnableEffect(final boolean enable) {
        this.enableEffect = enable;
    }

    private void pendingDragState() {
        if (isInAnimating()) {
            return;
        }
        if (!isTouchingDown) {
            return;
        }

        if (valueAnimator.isRunning()) {
            valueAnimator.cancel();
        }

        animateState = ANIMATE_STATE_PENDING_DRAG;

        beforeState.copy(viewState);
        afterState.copy(viewState);

        if (isChecked) {
            afterState.checkStateColor = checkedColor;
            afterState.buttonX = buttonMaxX;
            afterState.checkedLineColor = checkedColor;
        } else {
            afterState.checkStateColor = uncheckColor;
            afterState.buttonX = buttonMinX;
            afterState.radius = viewRadius;
        }

        valueAnimator.start();
    }

    private void pendingCancelDragState() {
        if (isDragState() || isPendingDragState()) {
            if (valueAnimator.isRunning()) {
                valueAnimator.cancel();
            }

            animateState = ANIMATE_STATE_PENDING_RESET;
            beforeState.copy(viewState);

            if (isChecked) {
                setCheckedViewState(afterState);
            } else {
                setUncheckViewState(afterState);
            }
            valueAnimator.start();
        }
    }

    private void pendingSettleState() {
        if (valueAnimator.isRunning()) {
            valueAnimator.cancel();
        }

        animateState = ANIMATE_STATE_PENDING_SETTLE;
        beforeState.copy(viewState);

        if (isChecked) {
            setCheckedViewState(afterState);
        } else {
            setUncheckViewState(afterState);
        }
        valueAnimator.start();
    }

    @Override
    public final void setOnClickListener(final OnClickListener l) {
    }

    @Override
    public final void setOnLongClickListener(final OnLongClickListener l) {
    }

    public void setOnCheckedChangeListener(final OnCheckedChangeListener l) {
        onCheckedChangeListener = l;
    }

    public interface OnCheckedChangeListener {
        void onCheckedChanged(NCSwitchButton view, boolean isChecked);
    }

    private static class ViewState {
        float buttonX;

        int checkStateColor;

        int checkedLineColor;

        float radius;

        ViewState() {
        }

        private void copy(final ViewState source) {
            this.buttonX = source.buttonX;
            this.checkStateColor = source.checkStateColor;
            this.checkedLineColor = source.checkedLineColor;
            this.radius = source.radius;
        }
    }

}