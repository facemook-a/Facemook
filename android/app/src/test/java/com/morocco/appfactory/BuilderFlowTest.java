package com.morocco.appfactory;

import android.content.*;
import android.widget.*;
import com.morocco.appfactory.data.*;
import com.morocco.appfactory.ui.JavaHighlighter;
import com.morocco.appfactory.views.*;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.*;
import android.os.Looper;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import java.io.File;
import java.io.FileOutputStream;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
@LooperMode(LooperMode.Mode.PAUSED)
public class BuilderFlowTest {
    @Before public void session(){RuntimeEnvironment.getApplication().getSharedPreferences("factory",0).edit().clear().putBoolean("signed_in",true).putBoolean("isGuest",true).commit();}
    @Test public void guestLoginOpensDashboard(){
        try(ActivityController<LoginActivity> c=Robolectric.buildActivity(LoginActivity.class).setup()){
            c.get().findViewById(R.id.guest).performClick();
            assertTrue(c.get().getSharedPreferences("factory",0).getBoolean("isGuest",false));
            assertEquals(MainActivity.class.getName(),shadowOf(c.get()).getNextStartedActivity().getComponent().getClassName());
        }
    }
    @Test public void villageGenerationChangesSceneAndExportStartsLocked(){
        try(ActivityController<VillageBuilderActivity> c=Robolectric.buildActivity(VillageBuilderActivity.class).setup()){
            VillageBuilderActivity a=c.get();((EditText)a.findViewById(R.id.project_name)).setText("Chefchaouen");a.findViewById(R.id.generate).performClick();
            assertTrue(((VillagePreviewView)a.findViewById(R.id.village_preview)).getHouses().length()>10);
            assertFalse(a.findViewById(R.id.export_project).isEnabled());assertTrue(a.findViewById(R.id.watch_ad).isShown());
        }
    }
    @Test public void gameAddsHouseAndSwitchesToPlay(){
        try(ActivityController<GameBuilderActivity> c=Robolectric.buildActivity(GameBuilderActivity.class).setup()){
            GameBuilderActivity a=c.get();DragDropView canvas=a.findViewById(R.id.game_canvas);int count=canvas.getHouses().length();a.findViewById(R.id.add_house).performClick();assertEquals(count+1,canvas.getHouses().length());a.findViewById(R.id.play_game).performClick();assertTrue(canvas.isPlaying());
        }
    }
    @Test public void projectSaveAndReopenPreserveBuilderData()throws Exception{
        Context context=RuntimeEnvironment.getApplication();Project p=new Project("game","Stored Atlas");p.data.put("template","Driving").put("objects",new JSONArray().put(new JSONObject().put("x",.2).put("y",.3)));ProjectStore store=new ProjectStore(context);store.save(p);
        Intent intent=new Intent(context,GameBuilderActivity.class).putExtra("project_id",p.id);
        try(ActivityController<GameBuilderActivity> c=Robolectric.buildActivity(GameBuilderActivity.class,intent).setup()){
            assertEquals("Stored Atlas",((EditText)c.get().findViewById(R.id.project_name)).getText().toString());assertEquals(1,((DragDropView)c.get().findViewById(R.id.game_canvas)).getHouses().length());assertEquals("Driving",((Spinner)c.get().findViewById(R.id.template_spinner)).getSelectedItem());
        }
        store.delete(p);
    }
    @Test public void codeEditorHasRealSourceAndLineNumbers(){
        try(ActivityController<CodeEditorActivity> c=Robolectric.buildActivity(CodeEditorActivity.class).setup()){
            CodeEditorActivity a=c.get();assertTrue(((EditText)a.findViewById(R.id.code_editor)).getText().toString().contains("class MainActivity"));assertTrue(((TextView)a.findViewById(R.id.line_numbers)).getText().toString().contains("10\n"));a.findViewById(R.id.compile_code).performClick();assertTrue(((TextView)a.findViewById(R.id.terminal)).getText().toString().contains("Source contract valid"));
        }
    }
    @Test public void noCodePropertiesAndLogicSurviveSave()throws Exception{
        Context context=RuntimeEnvironment.getApplication();Project p=new Project("app","My UI");
        p.data.put("components",new JSONArray().put(new JSONObject().put("type","Button").put("text","First").put("color","#3A7BD5").put("width",180).put("x",.1).put("y",.1).put("action","Show Toast").put("value","Hello")));
        ProjectStore store=new ProjectStore(context);store.save(p);
        try(ActivityController<AppBuilderNoCodeActivity> c=Robolectric.buildActivity(AppBuilderNoCodeActivity.class,new Intent(context,AppBuilderNoCodeActivity.class).putExtra("project_id",p.id)).setup()){
            shadowOf(Looper.getMainLooper()).idle();AppBuilderNoCodeActivity a=c.get();
            ((EditText)a.findViewById(R.id.component_text)).setText("Salam, maker!");((EditText)a.findViewById(R.id.logic_value)).setText("It works");a.findViewById(R.id.apply_properties).performClick();a.prepare();store.save(a.project);
            JSONObject saved=store.load(p.id).data.getJSONArray("components").getJSONObject(0);assertEquals("Salam, maker!",saved.getString("text"));assertEquals("Show Toast",saved.getString("action"));assertEquals("It works",saved.getString("value"));
        }store.delete(p);
    }
    @Test @Config(sdk=21) public void minimumSdkCanInflateEveryBuilder(){
        try(ActivityController<VillageBuilderActivity> c=Robolectric.buildActivity(VillageBuilderActivity.class).setup()){assertNotNull(c.get().findViewById(R.id.village_preview));}
        try(ActivityController<GameBuilderActivity> c=Robolectric.buildActivity(GameBuilderActivity.class).setup()){assertNotNull(c.get().findViewById(R.id.joystick));}
        try(ActivityController<AppBuilderNoCodeActivity> c=Robolectric.buildActivity(AppBuilderNoCodeActivity.class).setup()){assertNotNull(c.get().findViewById(R.id.design_canvas));}
        try(ActivityController<CodeEditorActivity> c=Robolectric.buildActivity(CodeEditorActivity.class).setup()){assertNotNull(c.get().findViewById(R.id.code_editor));}
    }
    @Test public void incompleteJavaDraftSurvivesActivityRecreation(){
        android.os.Bundle state=new android.os.Bundle();
        try(ActivityController<CodeEditorActivity> c=Robolectric.buildActivity(CodeEditorActivity.class).setup()){
            ((EditText)c.get().findViewById(R.id.code_editor)).setText("// still writing my class\npublic class");c.pause().saveInstanceState(state);
            assertFalse(state.containsKey("project"));assertTrue(state.containsKey("draft"));
        }
        try(ActivityController<CodeEditorActivity> c=Robolectric.buildActivity(CodeEditorActivity.class).setup(state)){
            assertEquals("// still writing my class\npublic class",((EditText)c.get().findViewById(R.id.code_editor)).getText().toString());
        }
    }
    @Test @GraphicsMode(GraphicsMode.Mode.NATIVE) @Config(qualifiers="w411dp-h891dp-xxhdpi")
    public void dashboardRendersAndSearchFiltersModes()throws Exception{
        try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).setup()){
            MainActivity a=c.get();androidx.recyclerview.widget.RecyclerView modes=a.findViewById(R.id.mode_list);assertEquals(4,modes.getAdapter().getItemCount());
            View content=a.findViewById(android.R.id.content);int w=1233,h=2460;content.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));content.layout(0,0,w,h);
            Bitmap bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);content.draw(new Canvas(bitmap));File image=new File("build/reports/ui/dashboard.png");assertTrue(image.getParentFile().isDirectory()||image.getParentFile().mkdirs());try(FileOutputStream out=new FileOutputStream(image)){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}bitmap.recycle();
            ((EditText)a.findViewById(R.id.search)).setText("village");assertEquals(1,modes.getAdapter().getItemCount());
        }
    }
    @Test public void highlighterColorsKeywordsStringsAndComments(){
        android.text.Editable source=new android.text.SpannableStringBuilder("public class Test { String x = \"hello\"; // note\n}");new JavaHighlighter(()->{}).afterTextChanged(source);android.text.style.ForegroundColorSpan[] spans=source.getSpans(0,source.length(),android.text.style.ForegroundColorSpan.class);assertTrue(spans.length>=4);
    }
}
