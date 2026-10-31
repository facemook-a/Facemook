package com.morocco.appfactory.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.OvershootInterpolator;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import com.morocco.appfactory.R;

public class FactoryButton extends AppCompatButton {
    public FactoryButton(Context context) { this(context, null); }
    public FactoryButton(Context context, AttributeSet attrs) { this(context, attrs, 0); }
    public FactoryButton(Context context, AttributeSet attrs, int style) {
        super(context, attrs, style);
        setBackgroundTintList(null);
        setBackgroundResource(R.drawable.bg_gradient_blue);
        setTextColor(0xFFFFFFFF);
        setTextSize(13);
        setAllCaps(false);
        setGravity(android.view.Gravity.CENTER);
        setTypeface(ResourcesCompat.getFont(context, R.font.poppins_bold));
        setElevation(Ui.dp(context, 12));
        setPadding(Ui.dp(context, 18), 0, Ui.dp(context, 18), 0);
        setCompoundDrawablePadding(Ui.dp(context, 8));
        setMinHeight(Ui.dp(context, 56));
        setIcon(R.drawable.ic_spark);
    }
    public void setIcon(int resource) {
        android.graphics.drawable.Drawable icon = ContextCompat.getDrawable(getContext(), resource);
        if (icon != null) icon.setBounds(0, 0, Ui.dp(getContext(), 20), Ui.dp(getContext(), 20));
        setCompoundDrawablesRelative(icon, null, null, null);
    }
    @Override public boolean performClick() {
        if (!isEnabled()) return false;
        animate().cancel();
        setScaleX(.95f); setScaleY(.95f);
        animate().scaleX(1.05f).scaleY(1.05f).setDuration(150).withEndAction(() ->
                animate().scaleX(1f).scaleY(1f).setDuration(150).setInterpolator(new OvershootInterpolator()).start()).start();
        return super.performClick();
    }
}
