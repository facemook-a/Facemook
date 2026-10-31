package com.morocco.appfactory;

import android.content.*;
import android.os.Bundle;
import android.text.*;
import android.view.*;
import android.widget.*;
import androidx.activity.result.*;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.*;
import androidx.recyclerview.widget.*;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.morocco.appfactory.data.*;
import com.morocco.appfactory.ui.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends AppCompatActivity {
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final List<Project> saved=new ArrayList<>();
    private final List<Row> visible=new ArrayList<>();
    private final String[] titles={"Village Builder","Game Builder","App Builder","Code Editor"};
    private final String[] descriptions={"Create Moroccan 3D villages","Drag, drop, and play your world","Build interfaces. Connect logic blocks.","Java source files and Android project export"};
    private final int[] icons={R.drawable.ic_village,R.drawable.ic_game,R.drawable.ic_blocks,R.drawable.ic_code};
    private final Class<?>[] screens={VillageBuilderActivity.class,GameBuilderActivity.class,AppBuilderNoCodeActivity.class,CodeEditorActivity.class};
    private int tab=1;
    private String query="";
    private RecyclerView list;
    private RowAdapter adapter;
    private final ActivityResultLauncher<Intent> folder=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),result->{
        if(result.getResultCode()==RESULT_OK && result.getData()!=null && result.getData().getData()!=null) {
            try {
                android.net.Uri uri=result.getData().getData();
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                getSharedPreferences("factory",0).edit().putString("workspace_uri",uri.toString()).apply();
                Ui.toast(this,"Workspace selected. Future saves and ZIP exports will also be copied here.");
            }catch(Exception e){Ui.toast(this,Ui.error(e));}
        }
    });
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if(!getSharedPreferences("factory",0).getBoolean("signed_in",false)) {startActivity(new Intent(this,LoginActivity.class));finish();return;}
        setContentView(R.layout.activity_main);
        list=findViewById(R.id.mode_list);list.setLayoutManager(new LinearLayoutManager(this));adapter=new RowAdapter();list.setAdapter(adapter);
        BottomNavigationView nav=findViewById(R.id.bottom_nav);
        nav.getMenu().add(0,1,0,"Create").setIcon(R.drawable.ic_spark);
        nav.getMenu().add(0,2,1,"Projects").setIcon(R.drawable.ic_folder);
        nav.getMenu().add(0,3,2,"Guide").setIcon(R.drawable.ic_book);
        nav.setOnItemSelectedListener(item->{tab=item.getItemId();refresh();return true;});
        ((EditText)findViewById(R.id.search)).addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int st,int count,int after){}
            public void onTextChanged(CharSequence s,int st,int before,int count){query=s.toString().toLowerCase(Locale.ROOT);refresh();}
            public void afterTextChanged(Editable e){}
        });
        FactoryButton profile=findViewById(R.id.profile);profile.setIcon(R.drawable.ic_person);profile.setOnClickListener(this::profile);
        if(state!=null)nav.setSelectedItemId(state.getInt("tab",1));
        refresh();
    }
    @Override protected void onResume(){super.onResume();if(adapter!=null)reload();}
    @Override protected void onSaveInstanceState(Bundle out){out.putInt("tab",tab);super.onSaveInstanceState(out);}
    private void reload(){worker.execute(()->{try {List<Project> all=new ProjectStore(this).list();runOnUiThread(()->{if(isDestroyed())return;saved.clear();saved.addAll(all);refresh();});}catch(Exception e){runOnUiThread(()->Ui.toast(this,Ui.error(e)));}});}
    private void refresh(){
        if(adapter==null)return;visible.clear();
        ((TextView)findViewById(R.id.section_title)).setText(tab==1?"Choose your canvas":tab==2?"Your creations":"The creator’s handbook");
        ((TextView)findViewById(R.id.section_subtitle)).setText(tab==1?"Four ways to turn an idea into something real.":tab==2?"Tap to reopen. Long press a project to delete.":"Everything you need to start making.");
        if(tab==1)for(int i=0;i<4;i++)add(new Row(titles[i],descriptions[i],i==3?"WITH CODE":"NO CODE",icons[i],i,null));
        if(tab==2)for(Project p:saved){int index=Arrays.asList("village","game","app","code").indexOf(p.type);if(index>=0)add(new Row(p.name,titles[index]+" · "+android.text.format.DateFormat.format("dd MMM, HH:mm",p.updatedAt),"SAVED ON DEVICE",icons[index],index,p));}
        if(tab==3){add(new Row("Start with a village","Generate a sample Atlas village. Drag the scene to orbit.","01 · EXPLORE",icons[0],-1,null));add(new Row("Make something interactive","Drag game houses or arrange app components. Add Toast and second-screen actions.","02 · CREATE",icons[1],-1,null));add(new Row("Take it to AndroidIDE","Rewarded test ads unlock source export. Unzip, open in AndroidIDE, and build your APK.","03 · BUILD",icons[3],-1,null));add(new Row("Own your workspace","Profile → Choose storage folder. Select or create MoroccoFactory to mirror future projects and ZIPs.","04 · KEEP",R.drawable.ic_folder,-1,null));}
        adapter.notifyDataSetChanged();TextView empty=findViewById(R.id.empty_state);empty.setVisibility(visible.isEmpty()?View.VISIBLE:View.GONE);empty.setText(query.isEmpty()?"Your next idea belongs here.\nChoose Create to start a project.":"No matching creations. Try another search.");
    }
    private void add(Row row){if((row.title+" "+row.subtitle).toLowerCase(Locale.ROOT).contains(query))visible.add(row);}
    private void profile(View anchor){
        PopupMenu menu=new PopupMenu(this,anchor);menu.getMenu().add("Profile & workspace");menu.getMenu().add("Choose storage folder");menu.getMenu().add("Sign out");
        menu.setOnMenuItemClickListener(item->{String text=item.getTitle().toString();
            if(text.equals("Choose storage folder"))folder.launch(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION));
            else if(text.equals("Sign out")){getSharedPreferences("factory",0).edit().putBoolean("signed_in",false).apply();com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(this,com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN).signOut();startActivity(new Intent(this,LoginActivity.class));finish();}
            else{try{Ui.message(this,"Your studio",getSharedPreferences("factory",0).getString("profile","Guest creator")+"\n\nLocal workspace:\n"+new ProjectStore(this).root()+"\n\nProjects are available to all local profiles on this device. They are not synced to a server. Choose a shared folder to keep copies outside app storage before uninstalling.\n\nTest ads are enabled. No purchase is required.");}catch(Exception e){Ui.toast(this,Ui.error(e));}}
            return true;
        });menu.show();
    }
    private static class Row{final String title,subtitle,badge;final int icon,index;final Project project;Row(String t,String s,String b,int i,int n,Project p){title=t;subtitle=s;badge=b;icon=i;index=n;project=p;}}
    private class RowAdapter extends RecyclerView.Adapter<RowAdapter.Holder>{
        class Holder extends RecyclerView.ViewHolder{Holder(View v){super(v);}}
        @Override public Holder onCreateViewHolder(ViewGroup p,int type){return new Holder(getLayoutInflater().inflate(R.layout.item_mode,p,false));}
        @Override public int getItemCount(){return visible.size();}
        @Override public void onBindViewHolder(Holder h,int position){Row row=visible.get(position);((TextView)h.itemView.findViewById(R.id.mode_title)).setText(row.title);((TextView)h.itemView.findViewById(R.id.mode_subtitle)).setText(row.subtitle);((TextView)h.itemView.findViewById(R.id.mode_badge)).setText(row.badge);((ImageView)h.itemView.findViewById(R.id.mode_icon)).setImageResource(row.icon);Ui.fade(h.itemView,position);
            h.itemView.setOnClickListener(v->{if(row.index<0){Ui.message(MainActivity.this,row.title,row.subtitle);return;}Intent intent=new Intent(MainActivity.this,screens[row.index]);if(row.project!=null)intent.putExtra("project_id",row.project.id);startActivity(intent);});
            h.itemView.setOnLongClickListener(v->{if(row.project==null)return false;Ui.confirm(MainActivity.this,"Delete "+row.title+"?","The local project will be removed. Shared-folder copies are kept.","Delete Project",()->worker.execute(()->{try{new ProjectStore(MainActivity.this).delete(row.project);runOnUiThread(()->reload());}catch(Exception e){runOnUiThread(()->Ui.toast(MainActivity.this,Ui.error(e)));}}));return true;});
        }
    }
    @Override protected void onDestroy(){worker.shutdown();super.onDestroy();}
}
