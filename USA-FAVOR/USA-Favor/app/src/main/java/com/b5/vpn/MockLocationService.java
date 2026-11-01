package com.b5.vpn;
import android.app.*;import android.content.*;import android.content.pm.PackageManager;import android.os.*;
/** Real Android test provider. Works only with the user's developer-option selection. */
public final class MockLocationService extends Service {
 public static volatile boolean running=false;public static final String INSTRUCTIONS="Go to Settings > Developer Options > Select mock location app > Choose USA-Favor";
 private final Handler handler=new Handler(Looper.getMainLooper());private MockGps gps;
 public static boolean selected(Context c){AppOpsManager ops=c.getSystemService(AppOpsManager.class);try{return ops.checkOpNoThrow(AppOpsManager.OPSTR_MOCK_LOCATION,android.os.Process.myUid(),c.getPackageName())==AppOpsManager.MODE_ALLOWED;}catch(Exception e){return false;}}
 public static boolean permitted(Context c){return selected(c)&&c.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED;}
 public static void startIfPermitted(Context c){if(permitted(c)&&c.getSharedPreferences("b5",0).getBoolean("gps",true))try{c.startForegroundService(new Intent(c,MockLocationService.class));}catch(Exception e){android.util.Log.w("B5","Mock GPS requires visible app/location permission",e);}}
 @Override public void onCreate(){super.onCreate();gps=new MockGps(this);getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel("b5-mock","B5 • Mock location",NotificationManager.IMPORTANCE_LOW));}
 @Override public int onStartCommand(Intent i,int f,int id){if(!B5VpnService.connected||!permitted(this)){stopSelf();return START_NOT_STICKY;}Notification n=new Notification.Builder(this,"b5-mock").setSmallIcon(com.usa.user.R.drawable.ic_b5).setContentTitle("B5 • New York mock GPS").setContentText("40.7128, -74.0060 • Android test provider").setOngoing(true).build();startForeground(5506,n);running=true;handler.removeCallbacks(update);handler.post(update);return START_NOT_STICKY;}
 final Runnable update=new Runnable(){public void run(){if(!B5VpnService.connected||!getSharedPreferences("b5",0).getBoolean("gps",true)){stopSelf();return;}try{gps.update();handler.postDelayed(this,1000);}catch(Exception e){stopSelf();}}};
 @Override public IBinder onBind(Intent i){return null;}
 @Override public void onDestroy(){running=false;stopForeground(STOP_FOREGROUND_REMOVE);handler.removeCallbacksAndMessages(null);gps.stop();super.onDestroy();}
}
