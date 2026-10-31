package com.morocco.appfactory.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import com.morocco.appfactory.R;

public final class Ui {
    private Ui() {}
    public static int dp(Context c, float value) { return Math.round(value * c.getResources().getDisplayMetrics().density); }
    public static void toast(Context c, String text) { Toast.makeText(c, text, Toast.LENGTH_LONG).show(); }
    public static void message(Context c, String title, String message) {
        TextView body = new TextView(c); body.setText(message); body.setTextColor(0xFFF6F4FC);
        body.setTextSize(15); body.setPadding(dp(c,24), dp(c,16), dp(c,24), dp(c,16)); body.setTextIsSelectable(true);
        ScrollView scroll = new ScrollView(c); scroll.addView(body);
        LinearLayout content = new LinearLayout(c); content.setOrientation(LinearLayout.VERTICAL); content.addView(scroll,
                new LinearLayout.LayoutParams(-1, dp(c,320)));
        FactoryButton close = new FactoryButton(c); close.setText("Got it");
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(c,56)); lp.setMargins(dp(c,20), dp(c,8), dp(c,20), dp(c,20));
        content.addView(close, lp);
        AlertDialog dialog = new AlertDialog.Builder(c).setTitle(title).setView(content).create();
        close.setOnClickListener(v -> dialog.dismiss()); dialog.show();
    }
    public static void fade(View view, int index) {
        view.setAlpha(0); view.setTranslationY(dp(view.getContext(), 14));
        view.animate().alpha(1).translationY(0).setStartDelay(Math.min(index, 5) * 65L).setDuration(300).start();
    }
    public static int color(String value, int fallback) {
        try { return Color.parseColor(value); } catch (Exception e) { return fallback; }
    }
    public static void confirm(Context c,String title,String message,String positive,Runnable action){
        LinearLayout box=new LinearLayout(c);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(c,24),dp(c,12),dp(c,24),dp(c,24));
        TextView body=new TextView(c);body.setText(message);body.setTextColor(0xFFF6F4FC);body.setTextSize(15);box.addView(body,new LinearLayout.LayoutParams(-1,-2));
        FactoryButton yes=new FactoryButton(c);yes.setText(positive);yes.setIcon(R.drawable.ic_delete);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(c,56));lp.topMargin=dp(c,20);box.addView(yes,lp);
        FactoryButton no=new FactoryButton(c);no.setText("Keep Project");no.setIcon(R.drawable.ic_back);LinearLayout.LayoutParams cancel=new LinearLayout.LayoutParams(-1,dp(c,56));cancel.topMargin=dp(c,12);box.addView(no,cancel);
        AlertDialog dialog=new AlertDialog.Builder(c).setTitle(title).setView(box).create();yes.setOnClickListener(v->{dialog.dismiss();action.run();});no.setOnClickListener(v->dialog.dismiss());dialog.show();
    }
    public static String error(Throwable e) { return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(); }
}
