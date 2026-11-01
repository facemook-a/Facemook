package com.b5.shared;
import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import static org.junit.Assert.*;import static org.robolectric.Shadows.*;import android.os.Looper;import android.widget.TextView;import android.view.*;import java.util.concurrent.TimeUnit;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28) @org.robolectric.annotation.LooperMode(org.robolectric.annotation.LooperMode.Mode.PAUSED)
public class BrandTest {
 public static class Screen extends BrandActivity {boolean began=false;protected void begin(){began=true;page("Ready","B5 premium");}}
 @Test public void splashWaitsTwoAndHalfSeconds(){try(var controller=Robolectric.buildActivity(Screen.class).create()){Screen a=controller.get();assertFalse("Splash transitioned early",a.began);shadowOf(Looper.getMainLooper()).idleFor(2499,TimeUnit.MILLISECONDS);assertFalse("Splash transitioned early",a.began);shadowOf(Looper.getMainLooper()).idleFor(1,TimeUnit.MILLISECONDS);assertTrue(a.began);}}
 @Test public void missingFirebaseUsesSetupInsteadOfFakeConnection(){android.content.Context c=RuntimeEnvironment.getApplication();c.getSharedPreferences("b5",0).edit().clear().commit();assertFalse(Cloud.configured(c));}
}
