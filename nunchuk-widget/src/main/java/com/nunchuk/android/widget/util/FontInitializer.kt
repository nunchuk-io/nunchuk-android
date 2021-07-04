package com.nunchuk.android.widget.util

import android.content.Context
import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.Typeface
import android.text.TextUtils
import android.util.AttributeSet
import android.widget.TextView
import com.nunchuk.android.widget.R
import java.util.*

class FontInitializer(private val context: Context) {

    fun <T : TextView> initTypeface(t: T, attrs: AttributeSet? = null) {
        when {
            attrs != null && !t.isInEditMode -> t.typeface = getCurrentTypeface(attrs)
            else -> t.typeface = getCurrentTypeface(STYLE_NORMAL)
        }
    }

    private fun getCurrentTypeface(attrs: AttributeSet): Typeface {
        val typedArray: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.FontTextView)
        val fontName = typedArray.getString(R.styleable.FontTextView_fontTextName)
        val fontStyle = typedArray.getInt(R.styleable.FontTextView_fontTextStyle, 0)
        typedArray.recycle()
        return getCurrentTypeface(fontStyle, fontName)
    }

    fun getCurrentTypeface(fontStyle: Int, fontName: String? = null) = getTypeFace(getFontName(fontStyle, fontName))

    private fun getTypeFace(fontName: String?): Typeface = synchronized(FONTS) {
        when {
            fontName == null -> Typeface.DEFAULT
            FONTS.containsKey(fontName) -> FONTS[fontName]!!
            else -> try {
                val typeface = Typeface.createFromAsset(context.assets, fontName)
                FONTS[fontName] = typeface
                typeface
            } catch (ex: Throwable) {
                Typeface.DEFAULT
            }
        }
    }

    private fun getFontName(fontStyle: Int, fontName: String? = null): String? {
        if (TextUtils.isEmpty(fontName)) {
            return try {
                getFontWithStyle(fontStyle)
            } catch (e: Resources.NotFoundException) {
                context.getString(R.string.default_font_regular)
            }
        }
        return fontName
    }

    @Throws(Resources.NotFoundException::class)
    private fun getFontWithStyle(fontStyle: Int) = when (fontStyle) {
        STYLE_NORMAL -> context.getString(R.string.default_font_regular)
        STYLE_ITALIC -> context.getString(R.string.default_font_italic)
        STYLE_SEMI_BOLD -> context.getString(R.string.default_font_semibold)
        STYLE_SEMI_BOLD_ITALIC -> context.getString(R.string.default_font_semibold_italic)
        STYLE_BOLD -> context.getString(R.string.default_font_bold)
        STYLE_LIGHT -> context.getString(R.string.default_font_light)
        else -> context.getString(R.string.default_font_regular)
    }

    companion object {
        const val STYLE_NORMAL = 0
        const val STYLE_ITALIC = 1
        const val STYLE_SEMI_BOLD = 2
        const val STYLE_SEMI_BOLD_ITALIC = 3
        const val STYLE_LIGHT = 4
        const val STYLE_BOLD = 5
        private val FONTS: MutableMap<String, Typeface> = HashMap()
    }
}