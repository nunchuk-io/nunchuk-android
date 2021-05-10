package com.nunchuk.android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatButton;

public class NCFontButton extends AppCompatButton {

    private String fontName;
    private int fontStyle;

    public NCFontButton(final Context context) {
        super(context);
        initTypeface();
    }

    public NCFontButton(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        getFontNameFromAttr(attrs);
        initTypeface();
    }

    public NCFontButton(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        getFontNameFromAttr(attrs);
        initTypeface();
    }

    private void initTypeface() {
        if (isInEditMode()) return;

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

}