package com.morocco.appfactory.build;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import com.morocco.appfactory.data.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;

public class ApkBuilderService extends Service {
    public static final String ACTION="com.morocco.appfactory.BUILD_EVENT";
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    @Override public IBinder onBind(Intent intent){return null;}
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        String id=intent==null?null:intent.getStringExtra("project_id");
        if(id==null){stopSelf(startId);return START_NOT_STICKY;}
        worker.execute(()->{
            File staging=null;
            try{
                ProjectStore store=new ProjectStore(this);Project project=store.load(id);
                progress(id,5,"Generating Android source…",null,null);
                Map<String,String> files=SourceGenerator.generate(project);
                File exports=new File(store.root(),"Export");
                if(!exports.isDirectory()&&!exports.mkdirs())throw new IOException("Cannot create export folder");
                String dirName=SourceGenerator.safeName(project.name)+"_"+id.substring(0,8)+"_"+System.currentTimeMillis();
                File output=new File(exports,dirName);staging=output;
                if(!output.mkdirs())throw new IOException("Cannot create project directory");
                int completed=0;
                for(Map.Entry<String,String> file:files.entrySet()){
                    File target=new File(output,file.getKey());
                    if(!target.getCanonicalPath().startsWith(output.getCanonicalPath()+File.separator))throw new IOException("Invalid output path");
                    File parent=target.getParentFile();if(!parent.isDirectory()&&!parent.mkdirs())throw new IOException("Cannot create source folder");
                    try(OutputStream out=new FileOutputStream(target)){out.write(file.getValue().getBytes(StandardCharsets.UTF_8));}
                    progress(id,10+(++completed*65/files.size()),"Writing "+file.getKey(),null,null);
                }
                try(InputStream in=getAssets().open("gradle-wrapper.jar");OutputStream out=new FileOutputStream(new File(output,"gradle/wrapper/gradle-wrapper.jar"))){copy(in,out);}
                new File(output,"gradlew").setExecutable(true);
                progress(id,85,"Packaging AndroidIDE project ZIP…",null,null);
                File archive=new File(exports,dirName+".zip");
                try(ZipOutputStream zip=new ZipOutputStream(new FileOutputStream(archive))){zip(output,output,zip);}
                // Store the durable result before notifying a screen that may have been recreated.
                getSharedPreferences("factory",0).edit().putString("last_export_"+id,archive.getAbsolutePath()).apply();
                String mirrorError=null;
                try{store.mirror("Export",archive.getName(),readBytes(archive),"application/zip");}
                catch(IOException e){mirrorError=" Local ZIP is safe. Shared copy failed: "+e.getMessage();}
                progress(id,100,"Android project ready. Build APK in AndroidIDE."+(mirrorError==null?"":mirrorError),archive.getAbsolutePath(),null);
            }catch(Exception e){
                if(staging!=null)deleteTree(staging);
                progress(id,0,"Export failed",null,e.getMessage()==null?e.toString():e.getMessage());
            }finally{stopSelf(startId);}
        });return START_NOT_STICKY;
    }
    private void progress(String id,int progress,String message,String path,String error){
        Intent event=new Intent(ACTION).setPackage(getPackageName()).putExtra("project_id",id).putExtra("progress",progress).putExtra("message",message);
        if(path!=null)event.putExtra("path",path);if(error!=null)event.putExtra("error",error);
        sendBroadcast(event,getPackageName()+".permission.BUILD_EVENT");
    }
    private static void zip(File root,File current,ZipOutputStream zip)throws IOException{
        File[] children=current.listFiles();if(children==null)throw new IOException("Cannot read generated files");
        Arrays.sort(children,(a,b)->a.getName().compareTo(b.getName()));
        for(File child:children){if(child.isDirectory())zip(root,child,zip);else{String relative=child.getAbsolutePath().substring(root.getAbsolutePath().length()+1).replace(File.separatorChar,'/');zip.putNextEntry(new ZipEntry(root.getName()+"/"+relative));try(InputStream in=new FileInputStream(child)){copy(in,zip);}zip.closeEntry();}}
    }
    private static void copy(InputStream in,OutputStream out)throws IOException{byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}
    private static byte[] readBytes(File file)throws IOException{try(InputStream in=new FileInputStream(file);ByteArrayOutputStream out=new ByteArrayOutputStream()){copy(in,out);return out.toByteArray();}}
    private static void deleteTree(File file){File[] children=file.listFiles();if(children!=null)for(File child:children)deleteTree(child);if(!file.delete())android.util.Log.w("ApkBuilder","Could not clean "+file.getName());}
    @Override public void onDestroy(){worker.shutdown();super.onDestroy();}
}
