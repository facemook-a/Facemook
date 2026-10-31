package com.morocco.appfactory;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.*;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    private final Handler handler=new Handler(Looper.getMainLooper());
    private ObjectAnimator rotation;
    private final Runnable next=()->{
        boolean signed=getSharedPreferences("factory",0).getBoolean("signed_in",false);
        startActivity(new Intent(this,signed?MainActivity.class:LoginActivity.class)); finish();
    };
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);setContentView(R.layout.activity_splash);
        rotation=ObjectAnimator.ofFloat(findViewById(R.id.splash_logo),"rotationY",0,360);
        rotation.setDuration(1400);rotation.setRepeatCount(ObjectAnimator.INFINITE);rotation.start();
    }
    @Override protected void onResume() { super.onResume(); handler.postDelayed(next,1400); }
    @Override protected void onPause() { handler.removeCallbacks(next);super.onPause(); }
    @Override protected void onDestroy() { rotation.cancel();super.onDestroy(); }
}
