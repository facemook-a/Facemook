package com.morocco.appfactory.build;

import com.morocco.appfactory.data.Project;
import org.json.*;
import java.util.*;
import java.util.regex.Pattern;

/** Pure Java generator, shared by the service and JVM tests. Emits buildable projects, never fake APKs. */
public final class SourceGenerator {
    public static final String PACKAGE = "com.morocco.generated";
    private static final String JAVA_PATH = "app/src/main/java/com/morocco/generated/";
    private SourceGenerator() {}

    public static String safeName(String name) {
        String result = name.replaceAll("[^a-zA-Z0-9_-]", "_").replaceAll("_+", "_");
        if (result.length() > 48) result = result.substring(0,48);
        return result.isEmpty() || result.matches("_+") ? "MoroccoProject" : result;
    }
    public static String javaString(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '\\': out.append("\\\\"); break;
                case '"': out.append("\\\""); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default: if(c<32) out.append(String.format(Locale.ROOT,"\\u%04x",(int)c)); else out.append(c);
            }
        }
        return out.append('"').toString();
    }
    public static String xml(String value) {
        return value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;");
    }
    private static String resourceText(String value) {
        return xml(value.replace("\\","\\\\").replace("'","\\'").replace("\"","\\\"").replace("\n","\\n").replace("@","\\@").replace("?","\\?"));
    }
    public static Map<String,String> generate(Project project) throws JSONException {
        Map<String,String> files = new LinkedHashMap<>();
        files.put("settings.gradle", "pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }\ndependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { google(); mavenCentral() } }\nrootProject.name = '"+safeName(project.name)+"'\ninclude ':app'\n");
        files.put("build.gradle", "plugins { id 'com.android.application' version '8.2.2' apply false }\n");
        files.put("gradle.properties", "org.gradle.jvmargs=-Xmx1536m -Dfile.encoding=UTF-8\norg.gradle.workers.max=2\n");
        files.put("app/build.gradle", "plugins { id 'com.android.application' }\nandroid { namespace '"+PACKAGE+"'; compileSdk 33\n defaultConfig { applicationId '"+PACKAGE+"'; minSdk 21; targetSdk 33; versionCode 1; versionName '1.0' }\n compileOptions { sourceCompatibility JavaVersion.VERSION_1_8; targetCompatibility JavaVersion.VERSION_1_8 }\n}\n");
        files.put("app/src/main/AndroidManifest.xml", "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"><application android:theme=\"@android:style/Theme.Material.NoActionBar\" android:label=\"@string/app_name\" android:allowBackup=\"false\"><activity android:name=\".MainActivity\" android:exported=\"true\"><intent-filter><action android:name=\"android.intent.action.MAIN\"/><category android:name=\"android.intent.category.LAUNCHER\"/></intent-filter></activity></application></manifest>\n");
        files.put("app/src/main/res/values/strings.xml", "<resources><string name=\"app_name\" formatted=\"false\">"+resourceText(project.name)+"</string></resources>\n");
        files.put("app/src/main/res/layout/activity_main.xml", layout(project));
        if ("code".equals(project.type)) {
            JSONObject source=project.data.optJSONObject("files");
            validateCodeFiles(source);
            Iterator<String> keys=source.keys();
            while(keys.hasNext()) { String file=keys.next(); files.put(JAVA_PATH+file,source.getString(file)); }
        } else if("app".equals(project.type)) files.put(JAVA_PATH+"MainActivity.java", noCodeJava(project));
        else if("village".equals(project.type)||"game".equals(project.type)) {
            files.put(JAVA_PATH+"MainActivity.java", "package "+PACKAGE+";\nimport android.app.Activity; import android.os.Bundle;\npublic class MainActivity extends Activity { @Override public void onCreate(Bundle b) { super.onCreate(b); setContentView(new FactoryScene(this)); } }\n");
            files.put(JAVA_PATH+"FactoryScene.java", sceneJava(project));
        } else throw new JSONException("Unknown project type");
        files.put("project.json",project.toJson().toString(2));
        files.put("gradle/wrapper/gradle-wrapper.properties", "distributionBase=GRADLE_USER_HOME\ndistributionPath=wrapper/dists\ndistributionUrl=https\\://services.gradle.org/distributions/gradle-8.2.1-bin.zip\ndistributionSha256Sum=03ec176d388f2aa99defcadc3ac6adf8dd2bce5145a129659537c0874dea5ad1\nzipStoreBase=GRADLE_USER_HOME\nzipStorePath=wrapper/dists\n");
        files.put("gradlew", "#!/bin/sh\nAPP_HOME=$(CDPATH= cd -- \"$(dirname -- \"$0\")\" && pwd)\nif [ -n \"$JAVA_HOME\" ]; then JAVACMD=\"$JAVA_HOME/bin/java\"; else JAVACMD=java; fi\nexec \"$JAVACMD\" -classpath \"$APP_HOME/gradle/wrapper/gradle-wrapper.jar\" org.gradle.wrapper.GradleWrapperMain \"$@\"\n");
        files.put("gradlew.bat", "@echo off\r\nif defined JAVA_HOME (\"%JAVA_HOME%\\bin\\java.exe\" -classpath \"%~dp0gradle\\wrapper\\gradle-wrapper.jar\" org.gradle.wrapper.GradleWrapperMain %*) else (java -classpath \"%~dp0gradle\\wrapper\\gradle-wrapper.jar\" org.gradle.wrapper.GradleWrapperMain %*)\r\n");
        files.put("README.md", "# "+project.name.replace("\n"," ")+"\n\nGenerated by Morocco Factory. This folder contains Android source code, not a compiled APK.\nOpen this directory in AndroidIDE with JDK 17 and Android SDK 33/build-tools 34.0.0. Run `chmod +x gradlew` then `./gradlew assembleDebug`. The APK is written to app/build/outputs/apk/debug/app-debug.apk.\n\nVillage geography is a deterministic sample, not live OpenStreetMap. Generated projects use no advertising or login SDKs. The game template supports joystick movement and house collision. No-code actions support Toast and a second screen.\n");
        return files;
    }

    public static void validateCodeFiles(JSONObject files) throws JSONException {
        if(files==null || !files.has("MainActivity.java")) throw new JSONException("MainActivity.java is required");
        if(files.length()>30) throw new JSONException("Maximum 30 Java files");
        Iterator<String> keys=files.keys();
        while(keys.hasNext()) {
            String name=keys.next();
            if(!name.matches("[A-Za-z_$][A-Za-z0-9_$]*\\.java")) throw new JSONException("Invalid Java filename: "+name);
            String code=files.getString(name);
            if(code.length()>150000) throw new JSONException("File exceeds 150,000 characters: "+name);
            if(!Pattern.compile("(?m)^\\s*package\\s+com\\.morocco\\.generated\\s*;").matcher(code).find()) throw new JSONException("Use package "+PACKAGE+" in "+name);
        }
    }
    public static String defaultCode() {
        return "package "+PACKAGE+";\n\nimport android.app.Activity;\nimport android.os.Bundle;\nimport android.widget.TextView;\nimport android.graphics.Color;\n\npublic class MainActivity extends Activity {\n    @Override\n    public void onCreate(Bundle savedInstanceState) {\n        super.onCreate(savedInstanceState);\n        TextView title = new TextView(this);\n        title.setText(\"Hello from Morocco!\");\n        title.setTextSize(28);\n        title.setTextColor(Color.WHITE);\n        title.setBackgroundColor(Color.rgb(15, 15, 26));\n        title.setGravity(17);\n        setContentView(title);\n    }\n}\n";
    }
    private static String layout(Project project) {
        if(!"app".equals(project.type)) return "<FrameLayout xmlns:android=\"http://schemas.android.com/apk/res/android\" android:layout_width=\"match_parent\" android:layout_height=\"match_parent\"/>\n";
        StringBuilder out=new StringBuilder("<FrameLayout xmlns:android=\"http://schemas.android.com/apk/res/android\" android:layout_width=\"match_parent\" android:layout_height=\"match_parent\" android:background=\"#0F0F1A\">\n");
        JSONArray components=project.data.optJSONArray("components");
        if(components!=null) for(int i=0;i<components.length();i++) {
            JSONObject c=components.optJSONObject(i); if(c==null) continue;
            String type=componentType(c.optString("type"));
            String text=c.optString("text",type).replace("\\","\\\\").replace("'","\\'").replace("\"","\\\"").replace("\n","\\n").replace("@","\\@").replace("?","\\?");
            out.append('<').append(type).append(" android:id=\"@+id/component_").append(i).append("\" android:layout_width=\"").append(width(c)).append("dp\" android:layout_height=\"56dp\" android:layout_marginLeft=\"").append((int)(fraction(c,"x")*360)).append("dp\" android:layout_marginTop=\"").append((int)(fraction(c,"y")*600)).append("dp\"");
            if("ImageView".equals(type)) out.append(" android:src=\"@android:drawable/ic_menu_gallery\" android:contentDescription=\"").append(xml(text)).append('"');
            else out.append(" android:text=\"").append(xml(text)).append("\" android:textColor=\"#FFFFFF\"");
            out.append(" android:background=\"").append(color(c)).append("\"/>\n");
        }
        return out.append("</FrameLayout>\n").toString();
    }
    public static String componentType(String type) {
        return Arrays.asList("Button","TextView","ImageView","EditText").contains(type)?type:"TextView";
    }
    private static int width(JSONObject c) { return Math.max(48,Math.min(320,c.optInt("width",180))); }
    private static String color(JSONObject c) { String value=c.optString("color","#3A7BD5");return value.matches("#[a-fA-F0-9]{6}([a-fA-F0-9]{2})?")?value:"#3A7BD5"; }
    private static double fraction(JSONObject c,String key) { double n=c.optDouble(key,0);return !Double.isNaN(n)&&!Double.isInfinite(n)?Math.max(0,Math.min(.9,n)):0; }

    private static String noCodeJava(Project project) {
        StringBuilder out=new StringBuilder("package "+PACKAGE+";\nimport android.app.Activity; import android.os.Bundle; import android.view.*; import android.widget.*; import android.graphics.Color;\npublic class MainActivity extends Activity {\n private int dp(float v) { return Math.round(v*getResources().getDisplayMetrics().density); }\n @Override public void onCreate(Bundle b) { super.onCreate(b); setContentView(R.layout.activity_main); final android.view.ViewGroup root=(android.view.ViewGroup)findViewById(android.R.id.content);\n");
        JSONArray components=project.data.optJSONArray("components");
        if(components!=null) for(int i=0;i<components.length();i++) {
            JSONObject c=components.optJSONObject(i);if(c==null)continue;
            out.append("final View v").append(i).append("=findViewById(R.id.component_").append(i).append(");\n");
            out.append("root.post(() -> { android.view.ViewGroup.LayoutParams raw=v").append(i).append(".getLayoutParams(); raw.width=Math.min(dp(").append(width(c)).append("),root.getWidth()); v").append(i).append(".setLayoutParams(raw); v").append(i).append(".setX((float)Math.min(root.getWidth()-raw.width,root.getWidth()*").append(fraction(c,"x")).append(")); v").append(i).append(".setY((float)Math.min(root.getHeight()-dp(56),root.getHeight()*").append(fraction(c,"y")).append(")); });\n");
            String action=c.optString("action","None"),value=javaString(c.optString("value","Hello from Morocco!"));
            if("Show Toast".equals(action)) out.append("v").append(i).append(".setOnClickListener(v -> Toast.makeText(this,").append(value).append(",Toast.LENGTH_SHORT).show());\n");
            if("Go to Screen".equals(action)) out.append("v").append(i).append(".setOnClickListener(v -> { TextView screen=new TextView(this); screen.setText(").append(value).append("); screen.setTextSize(28); screen.setTextColor(Color.WHITE); screen.setBackgroundColor(0xFF0F0F1A); screen.setGravity(17); screen.setOnClickListener(back -> recreate()); setContentView(screen); });\n");
        }
        return out.append("}\n}\n").toString();
    }
    private static String sceneJava(Project project) {
        boolean village="village".equals(project.type);
        JSONArray items=project.data.optJSONArray(village?"houses":"objects");if(items==null)items=new JSONArray();
        String title=javaString(project.name+(village?" · Sample village":" · "+project.data.optString("template","Walking Sim")));
        String json=javaString(items.toString());
        return "package "+PACKAGE+";\nimport android.content.Context; import android.graphics.*; import android.view.*; import org.json.*;\n"+
            "public class FactoryScene extends View {\n final Paint p=new Paint(3); JSONArray houses; float x=.5f,y=.7f,vx,vy; boolean alive; final boolean village="+village+"; final float speed="+("Driving".equals(project.data.optString("template"))?".011f":".005f")+";\n"+
            " public FactoryScene(Context c) { super(c); try { houses=new JSONArray("+json+"); } catch(Exception e) { houses=new JSONArray(); } }\n"+
            " final Runnable tick=new Runnable(){ public void run(){ if(!alive)return; float nx=Math.max(.03f,Math.min(.97f,x+vx*speed)),ny=Math.max(.06f,Math.min(.94f,y+vy*speed)); boolean hit=false; if(!village)for(int i=0;i<houses.length();i++){JSONObject h=houses.optJSONObject(i); if(h!=null&&Math.abs(nx-h.optDouble(\"x\"))<.065&&Math.abs(ny-h.optDouble(\"y\"))<.075)hit=true;} if(!hit){x=nx;y=ny;} invalidate();postDelayed(this,16); }};\n"+
            " protected void onAttachedToWindow(){super.onAttachedToWindow();alive=true;post(tick);} protected void onDetachedFromWindow(){alive=false;removeCallbacks(tick);super.onDetachedFromWindow();}\n"+
            " protected void onDraw(Canvas c){int w=getWidth(),h=getHeight();c.drawColor(0xFF171C2D);p.setColor(0xFF524940);c.drawRect(w*.44f,0,w*.56f,h,p);for(int i=0;i<houses.length();i++){JSONObject o=houses.optJSONObject(i);if(o==null)continue;float hx,hy;if(village){hx=w*.5f+(float)(o.optDouble(\"x\")-o.optDouble(\"z\"))*w*.065f;hy=h*.5f+(float)(o.optDouble(\"x\")+o.optDouble(\"z\"))*h*.026f;}else{hx=(float)o.optDouble(\"x\")*w;hy=(float)o.optDouble(\"y\")*h;}float size=w*.05f,roof=village?(float)o.optDouble(\"height\",1)*size:size;p.setColor(0xFFC18A64);c.drawRect(hx-size,hy-roof,hx+size,hy+size,p);p.setColor(0xFF8C5D48);Path a=new Path();a.moveTo(hx-size,hy-roof);a.lineTo(hx,hy-roof-size*.5f);a.lineTo(hx+size,hy-roof);a.close();c.drawPath(a,p);p.setColor(0xFFF5C16C);c.drawRect(hx-3,hy,hx+3,hy+size,p);}p.setColor(0xFFF5C16C);p.setTextSize(18*getResources().getDisplayMetrics().scaledDensity);c.drawText("+title+",20,60,p);if(!village){p.setColor(0xFF6BA7FF);c.drawCircle(x*w,y*h,12,p);p.setColor(0x33FFFFFF);c.drawCircle(w*.18f,h*.85f,w*.13f,p);p.setShader(new LinearGradient(0,0,w*.35f,h,0xFFF5C16C,0xFF3A7BD5,Shader.TileMode.CLAMP));c.drawCircle(w*.18f+vx*w*.07f,h*.85f+vy*w*.07f,w*.05f,p);p.setShader(null);}}\n"+
            " public boolean onTouchEvent(MotionEvent e){if(village)return true;if(e.getActionMasked()==MotionEvent.ACTION_UP||e.getActionMasked()==MotionEvent.ACTION_CANCEL){vx=vy=0;performClick();}else{float dx=(e.getX()-getWidth()*.18f)/(getWidth()*.13f),dy=(e.getY()-getHeight()*.85f)/(getWidth()*.13f);float len=(float)Math.sqrt(dx*dx+dy*dy);vx=dx/Math.max(1,len);vy=dy/Math.max(1,len);}return true;}public boolean performClick(){super.performClick();return true;}\n}\n";
    }
}
