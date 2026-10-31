package com.morocco.appfactory;

import android.content.*;
import android.os.*;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import com.morocco.appfactory.build.ApkBuilderService;
import com.morocco.appfactory.data.*;
import com.morocco.appfactory.ui.*;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public abstract class BaseBuilderActivity extends AppCompatActivity {
    protected Project project;
    protected ProjectStore store;
    protected EditText name;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private RewardGate rewardGate;
    private FactoryButton export,watch,share;
    private ProgressBar progress;
    private TextView status;
    private File lastExport;
    private boolean registered,building;
    protected abstract void collect() throws Exception;
    private final BroadcastReceiver events=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent intent){
        if(project==null||!project.id.equals(intent.getStringExtra("project_id")))return;
        progress.setVisibility(View.VISIBLE);progress.setProgress(intent.getIntExtra("progress",0));
        String error=intent.getStringExtra("error");String path=intent.getStringExtra("path");
        status.setText(error==null?intent.getStringExtra("message"):"Export failed: "+error);
        if(error!=null||path!=null){building=false;refreshGate();}
        if(path!=null){lastExport=new File(path);share.setVisibility(View.VISIBLE);status.append("\n"+path);}
    }};
    protected boolean setup(Bundle state,int layout,String type,String title,String defaultName){
        setContentView(layout);store=new ProjectStore(this);
        try{
            if(state!=null&&state.containsKey("draft")){
                String draft=state.getString("draft","");
                if(!draft.matches("builder-[a-zA-Z0-9-]+\\.json"))throw new IllegalArgumentException("Invalid draft");
                project=Project.parse(ProjectStore.read(new File(getCacheDir(),draft)));
            }
            else if(getIntent().hasExtra("project_id"))project=store.load(getIntent().getStringExtra("project_id"));
            else project=new Project(type,defaultName);
            if(!type.equals(project.type))throw new IllegalArgumentException("Project belongs to a different builder");
        }catch(Exception e){Ui.toast(this,"Could not open project: "+Ui.error(e));finish();return false;}
        Toolbar toolbar=findViewById(R.id.toolbar);toolbar.setTitle(title);toolbar.setSubtitle("MOROCCO FACTORY / STUDIO");toolbar.setNavigationOnClickListener(v->getOnBackPressedDispatcher().onBackPressed());
        name=findViewById(R.id.project_name);name.setText(project.name);name.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(80)});
        export=findViewById(R.id.export_project);watch=findViewById(R.id.watch_ad);share=findViewById(R.id.share_export);progress=findViewById(R.id.build_progress);status=findViewById(R.id.build_status);
        export.setIcon(R.drawable.ic_export);watch.setIcon(R.drawable.ic_lock);watch.setBackgroundResource(R.drawable.bg_gradient_gold);share.setIcon(R.drawable.ic_share);share.setVisibility(View.GONE);
        rewardGate=new RewardGate(this,watch,()->{getSharedPreferences("factory",0).edit().putBoolean("reward_"+project.id,true).apply();refreshGate();Ui.toast(this,"Export unlocked for this project");});
        watch.setOnClickListener(v->rewardGate.watch());export.setOnClickListener(v->exportProject());share.setOnClickListener(v->share());
        FactoryButton save=findViewById(R.id.save_project);save.setIcon(R.drawable.ic_save);save.setOnClickListener(v->saveProject(false));
        refreshGate();return true;
    }
    protected void prepare() throws Exception {
        String value=name.getText().toString().trim();if(value.isEmpty()){name.setError("Name your project");throw new IllegalArgumentException("Enter a project name");}
        project.name=value;collect();
        if(project.toJson().toString().length()>600000)throw new IllegalArgumentException("Project exceeds 600 KB. Reduce the source files.");
    }
    protected void saveProject(boolean forExport){
        try{prepare();Project snapshot=Project.parse(project.toJson().toString());
            if(forExport){building=true;refreshGate();status.setText("Saving project…");}
            worker.execute(()->{
                try{store.save(snapshot);String warning=null;
                    try{store.mirror("Projects",snapshot.id+".json",snapshot.toJson().toString(2).getBytes(StandardCharsets.UTF_8),"application/json");}catch(Exception e){warning="Saved locally. Shared copy failed: "+Ui.error(e);}
                    String result=warning;
                    runOnUiThread(()->{if(isDestroyed())return;if(forExport){try{startService(new Intent(this,ApkBuilderService.class).putExtra("project_id",snapshot.id));}catch(Exception e){building=false;refreshGate();Ui.toast(this,Ui.error(e));}}else Ui.toast(this,result==null?"Project saved":result);});
                }catch(Exception e){runOnUiThread(()->{if(isDestroyed())return;building=false;refreshGate();Ui.toast(this,"Save failed: "+Ui.error(e));});}
            });
        }catch(Exception e){Ui.toast(this,Ui.error(e));}
    }
    protected void exportProject(){
        if(building)return;
        if(!getSharedPreferences("factory",0).getBoolean("reward_"+project.id,false)){Ui.toast(this,"Watch a rewarded test ad to unlock this project’s export");return;}
        saveProject(true);
    }
    private void refreshGate(){
        boolean unlocked=getSharedPreferences("factory",0).getBoolean("reward_"+project.id,false);
        export.setEnabled(unlocked&&!building);export.setBackgroundResource(unlocked?R.drawable.bg_gradient_blue:R.drawable.bg_gradient_gray);export.setText(building?"Preparing source project…":"EXPORT APK");watch.setVisibility(unlocked?View.GONE:View.VISIBLE);
    }
    private void share(){if(lastExport==null||!lastExport.isFile()){Ui.toast(this,"Export a project first");return;}
        try{android.net.Uri uri=FileProvider.getUriForFile(this,getPackageName()+".files",lastExport);Intent send=new Intent(Intent.ACTION_SEND).setType("application/zip").putExtra(Intent.EXTRA_STREAM,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);send.setClipData(ClipData.newRawUri("Android project",uri));startActivity(Intent.createChooser(send,"Share AndroidIDE source project"));}catch(Exception e){Ui.toast(this,"No sharing app available: "+Ui.error(e));}}
    @Override protected void onStart(){super.onStart();if(project==null)return;
        IntentFilter filter=new IntentFilter(ApkBuilderService.ACTION);
        if(Build.VERSION.SDK_INT>=33)registerReceiver(events,filter,getPackageName()+".permission.BUILD_EVENT",null,Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(events,filter,getPackageName()+".permission.BUILD_EVENT",null);
        registered=true;
        String path=getSharedPreferences("factory",0).getString("last_export_"+project.id,"");if(!path.isEmpty()&&new File(path).isFile()){lastExport=new File(path);share.setVisibility(View.VISIBLE);status.setText("Last Android project ZIP:\n"+path);}
    }
    @Override protected void onStop(){if(registered){unregisterReceiver(events);registered=false;}super.onStop();}
    @Override protected void onSaveInstanceState(Bundle out){
        if(project!=null)try{
            project.name=name.getText().toString();collect();
            String filename="builder-"+project.id+".json";
            try(FileOutputStream stream=new FileOutputStream(new File(getCacheDir(),filename))){stream.write(project.toJson().toString().getBytes(StandardCharsets.UTF_8));}
            out.putString("draft",filename);
        }catch(Exception e){android.util.Log.w("Builder","Could not retain editor draft",e);}
        super.onSaveInstanceState(out);
    }
    @Override protected void onDestroy(){if(rewardGate!=null)rewardGate.close();worker.shutdown();super.onDestroy();}
}
