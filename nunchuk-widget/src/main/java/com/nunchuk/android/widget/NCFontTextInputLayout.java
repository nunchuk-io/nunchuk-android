package com.nunchuk.android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.MetricAffectingSpan;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputLayout;

public class NCFontTextInputLayout extends TextInputLayout {

    private String fontName;
    private int fontStyle;

    public NCFontTextInputLayout(final Context context) {
        super(context);
        initFont();
    }

    public NCFontTextInputLayout(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        getFontNameFromAttr(attrs);
        initFont();
    }

    public NCFontTextInputLayout(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        getFontNameFromAttr(attrs);
        initFont();
    }

    @Override
    public void setError(final CharSequence error) {
        if (!TextUtils.isEmpty(error)) {
            final SpannableString ss = new SpannableString(error);
            ss.setSpan(new NCFontSpan(FontStyle.getCurrentTypeface(getContext(), fontStyle, fontName)), 0, ss.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            super.setError(ss);
        } else {
            super.setError(error);
        }
    }

    void initFont() {
        setTypeface(FontStyle.getCurrentTypeface(getContext(), fontStyle, fontName));
    }

    private void getFontNameFromAttr(final AttributeSet attrs) {
        final TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.FontTextView);
        if (typedArray != null) {
            fontName = typedArray.getString(R.styleable.FontTextView_fontTextName);
            fontStyle = typedArray.getInt(R.styleable.FontTextView_fontTextStyle, 0);
            typedArray.recycle();
        }
    }

    @Override
    public void addView(final View child, final int index, final ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
        if (child instanceof EditText) {
            init();
        }
    }

    private void init() {
        final EditText editText = getEditText();
        if (editText == null) {
            return;
        }
        editText.setOnFocusChangeListener((v, hasFocus) -> update(editText, hasFocus));

    }

    private void update(final EditText editText, final boolean hasFocus) {
        final Drawable background = editText.getBackground();
        if (background == null) {
            return;
        }

        final Context context = editText.getContext();
        if (getError() == null) {
            if (hasFocus) {
                background.setColorFilter(ContextCompat.getColor(context, R.color.nc_blue_color), PorterDuff.Mode.SRC_IN);
            } else {
                background.setColorFilter(ContextCompat.getColor(context, R.color.nc_grey_color), PorterDuff.Mode.SRC_IN);
            }
        } else {
            background.setColorFilter(ContextCompat.getColor(context, R.color.nc_red_color), PorterDuff.Mode.SRC_IN
            );
        }
    }

    private static final class NCFontSpan extends MetricAffectingSpan {
        private final Typeface newFont;

        private NCFontSpan(final Typeface newFont) {
            this.newFont = newFont;
        }

        @Override
        public void updateDrawState(final TextPaint ds) {
            ds.setTypeface(newFont);
        }

        @Override
        public void updateMeasureState(final TextPaint paint) {
            paint.setTypeface(newFont);
        }

    }
}
