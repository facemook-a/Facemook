package com.morocco.appfactory;

import android.content.*;
import com.morocco.appfactory.build.*;
import com.morocco.appfactory.data.*;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.annotation.*;
import java.io.File;
import java.util.zip.ZipFile;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
@LooperMode(LooperMode.Mode.PAUSED)
public class BuildServiceTest {
    @Test public void serviceWritesCompleteShareableProjectZip()throws Exception{
        Context context=RuntimeEnvironment.getApplication();ProjectStore store=new ProjectStore(context);
        Project project=new Project("code","Service export");project.data.put("files",new JSONObject().put("MainActivity.java",SourceGenerator.defaultCode()));store.save(project);
        SharedPreferences prefs=context.getSharedPreferences("factory",0);String key="last_export_"+project.id;
        prefs.edit().remove("workspace_uri").remove(key).commit();
        ServiceController<ApkBuilderService> controller=Robolectric.buildService(ApkBuilderService.class).create();
        try{
            controller.get().onStartCommand(new Intent(context,ApkBuilderService.class).putExtra("project_id",project.id),0,1);
            long deadline=System.nanoTime()+10_000_000_000L;
            while(!prefs.contains(key)&&System.nanoTime()<deadline)Thread.sleep(20);
            assertTrue("Service did not complete export",prefs.contains(key));File archive=new File(prefs.getString(key,""));assertTrue(archive.isFile());assertTrue(archive.getName().endsWith(".zip"));
            try(ZipFile zip=new ZipFile(archive)){
                String prefix=archive.getName().substring(0,archive.getName().length()-4)+"/";
                assertNotNull(zip.getEntry(prefix+"gradle/wrapper/gradle-wrapper.jar"));
                assertNotNull(zip.getEntry(prefix+"app/src/main/java/com/morocco/generated/MainActivity.java"));
                assertNotNull(zip.getEntry(prefix+"app/src/main/AndroidManifest.xml"));
                assertNotNull(zip.getEntry(prefix+"project.json"));
                assertTrue(zip.getEntry(prefix+"gradle/wrapper/gradle-wrapper.jar").getSize()>10000);
            }
        }finally{controller.destroy();store.delete(project);}
    }
}
