package com.b5.vpn;
import android.app.*;import android.location.*;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.*;import org.robolectric.shadows.ShadowLocationManager;import static org.junit.Assert.*;import static org.robolectric.Shadows.*;
/** Robolectric does not implement test providers; capture those platform calls explicitly. */
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,shadows=MockLocationTest.TestProviderShadow.class)
public class MockLocationTest {
 @Implements(LocationManager.class) public static class TestProviderShadow extends ShadowLocationManager {
  public Location published;public boolean enabled,removed;public int added;
  @Implementation protected void addTestProvider(String n,boolean a,boolean b,boolean c,boolean d,boolean e,boolean f,boolean g,int power,int accuracy){assertEquals(LocationManager.GPS_PROVIDER,n);added++;}
  @Implementation protected void setTestProviderEnabled(String n,boolean on){enabled=on;}
  @Implementation protected void setTestProviderLocation(String n,Location location){if(!enabled)throw new IllegalStateException("Provider not enabled");published=new Location(location);}
  @Implementation protected void removeTestProvider(String n){removed=true;enabled=false;}
 }
 @Test public void providerPublishesAccurateFreshNewYorkAndCleansUp(){Application app=RuntimeEnvironment.getApplication();MockGps gps=new MockGps(app);gps.update();LocationManager lm=app.getSystemService(LocationManager.class);TestProviderShadow s=(TestProviderShadow)org.robolectric.shadow.api.Shadow.extract(lm);Location l=s.published;assertNotNull(l);assertEquals(40.7128,l.getLatitude(),0.00001);assertEquals(-74.0060,l.getLongitude(),0.00001);assertEquals(3,l.getAccuracy(),0.01);gps.update();assertEquals(1,s.added);gps.stop();assertTrue(s.removed);assertFalse(s.enabled);}
 @Test public void missingMockSelectionIsDetected(){Application app=RuntimeEnvironment.getApplication();AppOpsManager ops=app.getSystemService(AppOpsManager.class);shadowOf(ops).setMode(AppOpsManager.OPSTR_MOCK_LOCATION,android.os.Process.myUid(),app.getPackageName(),AppOpsManager.MODE_ERRORED);assertFalse(MockLocationService.selected(app));shadowOf(ops).setMode(AppOpsManager.OPSTR_MOCK_LOCATION,android.os.Process.myUid(),app.getPackageName(),AppOpsManager.MODE_ALLOWED);assertTrue(MockLocationService.selected(app));}
}
