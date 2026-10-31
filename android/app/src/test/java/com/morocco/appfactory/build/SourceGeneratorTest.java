package com.morocco.appfactory.build;

import com.morocco.appfactory.data.Project;
import org.json.*;
import org.junit.Test;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import static org.junit.Assert.*;

public class SourceGeneratorTest {
    private Project fixture(String type)throws Exception{
        Project p=new Project(type,"Atlas ' نور \"Studio\" <&> @?");
        if(type.equals("code"))p.data.put("files",new JSONObject().put("MainActivity.java",SourceGenerator.defaultCode()).put("Player.java","package com.morocco.generated; public class Player {}"));
        if(type.equals("app")){
            JSONArray components=new JSONArray();
            for(String t:Arrays.asList("Button","TextView","ImageView","EditText"))components.put(new JSONObject().put("type",t).put("text","@? He said \"Salam\" & <مرحبا>\nIt's ready").put("color","#3A7BD5").put("width",200).put("x",.1).put("y",components.length()*.13).put("action",components.length()%2==0?"Show Toast":"Go to Screen").put("value","مرحبا \"world\"\nSecond line"));
            p.data.put("components",components);
        }
        if(type.equals("village"))p.data.put("houses",new JSONArray().put(new JSONObject().put("x",1).put("z",2).put("height",1.2)));
        if(type.equals("game"))p.data.put("template","Driving").put("objects",new JSONArray().put(new JSONObject().put("x",.2).put("y",.2)));
        return p;
    }
    @Test public void allModesProduceAndroidProjects()throws Exception{
        String dest=System.getProperty("mf.fixtureDir");
        for(String type:Arrays.asList("village","game","app","code")){
            Project p=fixture(type);Map<String,String> files=SourceGenerator.generate(p);
            assertTrue(files.containsKey("app/src/main/java/com/morocco/generated/MainActivity.java"));
            assertTrue(files.get("app/build.gradle").contains("targetSdk 33"));
            assertTrue(files.get("app/src/main/AndroidManifest.xml").contains("android:exported=\"true\""));
            assertEquals(p.id,Project.parse(files.get("project.json")).id);
            for(Map.Entry<String,String> entry:files.entrySet()){
                assertFalse(entry.getKey().contains(".."));assertFalse(entry.getKey().endsWith(".apk"));
                if(dest!=null){File out=new File(new File(dest,type),entry.getKey());assertTrue(out.getParentFile().isDirectory()||out.getParentFile().mkdirs());Files.write(out.toPath(),entry.getValue().getBytes(StandardCharsets.UTF_8));}
            }
        }
    }
    @Test public void javaStringsPreserveQuotesSlashesAndUnicode()throws Exception{
        String value="quote\" \\ newline\n tab\t\r null\0 مرحبا";
        assertEquals(value,new JSONArray("["+SourceGenerator.javaString(value)+"]").getString(0));
    }
    @Test public void namesCannotEscapeExportDirectory(){
        for(String value:Arrays.asList("../unsafe","../../","/etc/passwd","مرحبا","","a\nb"))assertTrue(SourceGenerator.safeName(value).matches("[a-zA-Z0-9_-]{1,48}"));
    }
    @Test public void rejectsPathTraversalJavaFiles()throws Exception{
        JSONObject files=new JSONObject().put("MainActivity.java",SourceGenerator.defaultCode()).put("../../Escape.java","package com.morocco.generated;");
        try{SourceGenerator.validateCodeFiles(files);fail("Traversal accepted");}catch(JSONException expected){assertTrue(expected.getMessage().contains("filename"));}
    }
    @Test public void rejectsWrongPackageAndMissingEntryPoint()throws Exception{
        try{SourceGenerator.validateCodeFiles(new JSONObject());fail();}catch(JSONException expected){assertTrue(expected.getMessage().contains("MainActivity"));}
        try{SourceGenerator.validateCodeFiles(new JSONObject().put("MainActivity.java","package example; public class MainActivity {}"));fail();}catch(JSONException expected){assertTrue(expected.getMessage().contains("package"));}
    }
    @Test public void componentsCannotInjectXmlOrJava()throws Exception{
        Map<String,String> files=SourceGenerator.generate(fixture("app"));String xml=files.get("app/src/main/res/layout/activity_main.xml");
        assertTrue(xml.contains("&lt;مرحبا&gt;"));assertTrue(xml.contains("&amp;"));assertFalse(xml.contains("<مرحبا>"));
        assertTrue(files.get("app/src/main/java/com/morocco/generated/MainActivity.java").contains("Toast.makeText"));
        assertTrue(files.get("app/src/main/java/com/morocco/generated/MainActivity.java").contains("setOnClickListener"));
    }
    @Test public void projectIdsAreValidated()throws Exception{
        Project p=fixture("game");JSONObject json=p.toJson();json.put("id","../../private");
        try{Project.parse(json.toString());fail();}catch(JSONException expected){assertTrue(expected.getMessage().contains("ID"));}
    }
}
