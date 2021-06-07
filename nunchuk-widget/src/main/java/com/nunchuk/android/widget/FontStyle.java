package com.nunchuk.android.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class FontStyle {
    public static final int STYLE_NORMAL = 0;
    public static final int STYLE_ITALIC = 1;
    public static final int STYLE_SEMI_BOLD = 2;
    public static final int STYLE_SEMI_BOLD_ITALIC = 3;
    public static final int STYLE_LIGHT = 4;
    public static final int STYLE_BOLD = 5;

    private static final Map<String, Typeface> FONTS = new HashMap<>();

    public static Typeface getTypeFace(final Context context, final String assetFontName) {
        synchronized (FONTS) {
            if (FONTS.containsKey(assetFontName)) {
                return FONTS.get(assetFontName);
            } else {
                try {
                    final Typeface typeface = Typeface.createFromAsset(context.getAssets(), assetFontName);
                    FONTS.put(assetFontName, typeface);
                    return typeface;
                } catch (final Throwable ex) {
                    Log.e("TAG", Log.getStackTraceString(ex));
                    return Typeface.DEFAULT;
                }
            }
        }
    }

    public static Typeface getCurrentTypeface(final Context context, final int fontStyle, String fontName) {
        fontName = getFontName(context, fontStyle, fontName);
        return getTypeFace(context, fontName);
    }


    public static String getFontName(final Context context, final int fontStyle, String fontName) {
        if (TextUtils.isEmpty(fontName)) {
            try {
                fontName = getFontWithStyle(context, fontStyle);
            } catch (final Resources.NotFoundException e) {
                fontName = context.getString(R.string.default_font_regular);
            }
        }
        return fontName;
    }

    public static Typeface getCurrentTypeface(final Context context, final int fontStyle) {
        return getCurrentTypeface(context, fontStyle, null);
    }

    private static String getFontWithStyle(final Context context, final int mFontStyle) throws Resources.NotFoundException {
        switch (mFontStyle) {
            case STYLE_NORMAL:
                return context.getString(R.string.default_font_regular);
            case STYLE_ITALIC:
                return context.getString(R.string.default_font_italic);
            case STYLE_SEMI_BOLD:
                return context.getString(R.string.default_font_semibold);
            case STYLE_SEMI_BOLD_ITALIC:
                return context.getString(R.string.default_font_semibold_italic);
            case STYLE_BOLD:
                return context.getString(R.string.default_font_bold);
            case STYLE_LIGHT:
                return context.getString(R.string.default_font_light);
            default:
                return context.getString(R.string.default_font_regular);
        }
    }
}
