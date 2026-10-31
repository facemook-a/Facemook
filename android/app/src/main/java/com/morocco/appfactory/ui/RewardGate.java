package com.morocco.appfactory.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.ads.*;
import com.google.android.gms.ads.rewarded.*;
import com.morocco.appfactory.R;

/** Unlocks only on Google's earned-reward callback; failures never simulate a reward. */
public final class RewardGate {
    private final AppCompatActivity activity;
    private final FactoryButton watch;
    private final Runnable earned;
    private boolean loading,closed;
    private static boolean initialized;
    public RewardGate(AppCompatActivity a,FactoryButton button,Runnable onEarned){activity=a;watch=button;earned=onEarned;}
    public void watch(){
        if(loading||closed)return;
        loading=true;watch.setEnabled(false);watch.setText("Loading rewarded test ad…");
        if(!initialized){MobileAds.initialize(activity.getApplicationContext(),status->{});initialized=true;}
        RewardedAd.load(activity,activity.getString(R.string.rewarded_ad_unit_id),new AdRequest.Builder().build(),new RewardedAdLoadCallback(){
            @Override public void onAdFailedToLoad(@NonNull LoadAdError error){
                if(closed)return;reset();Ui.message(activity,"Ad unavailable","No reward was granted. Check your connection and try again.\n\nGoogle test-ad response: "+error.getMessage()+" ("+error.getCode()+")");
            }
            @Override public void onAdLoaded(@NonNull RewardedAd ad){
                if(closed||activity.isFinishing()||activity.isDestroyed())return;
                if(!activity.getLifecycle().getCurrentState().isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)){reset();Ui.toast(activity,"Return to the builder and tap Watch Ad again");return;}
                ad.setFullScreenContentCallback(new FullScreenContentCallback(){
                    @Override public void onAdDismissedFullScreenContent(){if(!closed)reset();}
                    @Override public void onAdFailedToShowFullScreenContent(@NonNull AdError error){if(!closed){reset();Ui.toast(activity,"Ad could not open: "+error.getMessage());}}
                });
                ad.show(activity,reward->{if(!closed){earned.run();watch.setVisibility(android.view.View.GONE);}});
            }
        });
    }
    private void reset(){loading=false;watch.setEnabled(true);watch.setText("Watch Ad to Unlock Export");}
    public void close(){closed=true;}
}
