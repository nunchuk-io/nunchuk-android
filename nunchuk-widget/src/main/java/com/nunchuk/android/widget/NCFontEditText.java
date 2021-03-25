package com.nunchuk.android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatEditText;

public class NCFontEditText extends AppCompatEditText {

    private String fontName;
    private int fontStyle;

    public NCFontEditText(final Context context) {
        super(context);
        initFont();
    }

    public NCFontEditText(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        getFontNameFromAttr(attrs);
        initFont();
    }

    public NCFontEditText(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        getFontNameFromAttr(attrs);
        initFont();
    }

    void initFont() {
        this.setTypeface(FontStyle.getCurrentTypeface(getContext(), fontStyle, fontName));
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
