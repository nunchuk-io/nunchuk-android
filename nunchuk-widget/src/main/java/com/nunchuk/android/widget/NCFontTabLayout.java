package com.nunchuk.android.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

public class NCFontTabLayout extends TabLayout {
    public NCFontTabLayout(final Context context) {
        super(context);
    }

    public NCFontTabLayout(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public NCFontTabLayout(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setupWithViewPager(final ViewPager viewPager) {
        super.setupWithViewPager(viewPager);
        customizeTabs(viewPager);
    }

    public void customizeTabs(@NonNull final ViewPager viewPager) {
        removeAllTabs();

        final ViewGroup slidingTabStrip = (ViewGroup) getChildAt(0);
        final PagerAdapter adapter = viewPager.getAdapter();

        final int space = (int) getContext().getResources().getDimension(R.dimen.nc_padding_8);
        for (int i = 0, count = adapter.getCount(); i < count; i++) {
            final Tab tab = newTab();
            addTab(tab.setText(adapter.getPageTitle(i)));
            final AppCompatTextView view = (AppCompatTextView) ((ViewGroup) slidingTabStrip.getChildAt(i)).getChildAt(1);
            view.setTypeface(FontStyle.getCurrentTypeface(view.getContext(), FontStyle.STYLE_SEMI_BOLD));
            view.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
            view.setLayoutParams(new LinearLayout.LayoutParams(view.getMeasuredWidth() + space, LayoutParams.WRAP_CONTENT));
        }
    }

}