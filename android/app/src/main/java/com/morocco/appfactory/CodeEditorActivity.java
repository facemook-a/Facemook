package com.morocco.appfactory;

import android.content.Intent;
import android.os.Bundle;
import android.text.*;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.*;
import com.morocco.appfactory.build.SourceGenerator;
import com.morocco.appfactory.ui.*;
import org.json.*;
import java.util.*;

public class CodeEditorActivity extends BaseBuilderActivity {
    private EditText editor;
    private TextView numbers,terminal;
    private JSONObject files;
    private String current="MainActivity.java";
    private final List<String> names=new ArrayList<>();
    private FileAdapter adapter;
    @Override public void onCreate(Bundle state){super.onCreate(state);if(!setup(state,R.layout.activity_code_editor,"code","Code Editor","Hello Morocco"))return;
        files=project.data.optJSONObject("files");if(files==null){files=new JSONObject();try{files.put(current,SourceGenerator.defaultCode());}catch(JSONException e){throw new IllegalStateException(e);}}
        if(state!=null)current=state.getString("current_file",current);
        editor=findViewById(R.id.code_editor);numbers=findViewById(R.id.line_numbers);terminal=findViewById(R.id.terminal);
        terminal.setMovementMethod(new android.text.method.ScrollingMovementMethod());
        editor.setFilters(new InputFilter[]{new InputFilter.LengthFilter(150000)});editor.addTextChangedListener(new JavaHighlighter(this::updateLines));
        editor.getViewTreeObserver().addOnScrollChangedListener(()->numbers.scrollTo(0,editor.getScrollY()));
        RecyclerView list=findViewById(R.id.file_list);list.setLayoutManager(new LinearLayoutManager(this,RecyclerView.HORIZONTAL,false));adapter=new FileAdapter();list.setAdapter(adapter);refreshFiles();open(current);
        FactoryButton run=findViewById(R.id.run_code);run.setIcon(R.drawable.ic_play);run.setOnClickListener(v->{try{prepare();store.save(project);log("Static text preview: Java executes only after an AndroidIDE build.");startActivity(new Intent(this,PreviewActivity.class).putExtra("project_id",project.id));}catch(Exception e){log("ERROR: "+Ui.error(e));}});
        findViewById(R.id.compile_code).setOnClickListener(v->{try{prepare();Map<String,String> generated=SourceGenerator.generate(project);log("Source contract valid. "+generated.size()+" project files prepared in memory.\nNo Android compiler is embedded. Unlock Export, then open the ZIP in AndroidIDE and run assembleDebug.");}catch(Exception e){log("VALIDATION FAILED: "+Ui.error(e));}});
        findViewById(R.id.new_file).setOnClickListener(v->addFile());
        log("Java workspace ready.\nPackage: com.morocco.generated\nSelect a file above. Save preserves every file.");
    }
    private void updateLines(){String source=editor.getText().toString();int count=1;for(int i=0;i<source.length();i++)if(source.charAt(i)=='\n')count++;StringBuilder out=new StringBuilder();for(int i=1;i<=count;i++)out.append(i).append('\n');numbers.setText(out);}
    private void persistCurrent()throws JSONException{if(editor!=null)files.put(current,editor.getText().toString());}
    private void open(String file){if(!files.has(file))file="MainActivity.java";current=file;editor.setText(files.optString(current));editor.setSelection(0);adapter.notifyDataSetChanged();}
    private void refreshFiles(){names.clear();Iterator<String> it=files.keys();while(it.hasNext())names.add(it.next());Collections.sort(names);adapter.notifyDataSetChanged();}
    private void log(String message){terminal.append("$ "+message+"\n\n");}
    private void addFile(){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(Ui.dp(this,20),Ui.dp(this,12),Ui.dp(this,20),Ui.dp(this,20));
        EditText input=new EditText(this);input.setHint("ClassName (without .java)");input.setSingleLine(true);box.addView(input,new LinearLayout.LayoutParams(-1,Ui.dp(this,56)));
        FactoryButton add=new FactoryButton(this);add.setText("Create Java File");box.addView(add,new LinearLayout.LayoutParams(-1,Ui.dp(this,56)));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("New source file").setView(box).create();
        add.setOnClickListener(v->{String className=input.getText().toString().trim();if(!className.matches("[A-Z][A-Za-z0-9_]{0,63}")){input.setError("Use a Java class name, e.g. Player");return;}String filename=className+".java";if(files.has(filename)){input.setError("File already exists");return;}if(files.length()>=30){input.setError("Maximum 30 files");return;}try{persistCurrent();files.put(filename,"package com.morocco.generated;\n\npublic class "+className+" {\n}\n");refreshFiles();open(filename);dialog.dismiss();}catch(Exception e){log(Ui.error(e));}});dialog.show();
    }
    @Override protected void collect()throws Exception{if(files!=null){persistCurrent();project.data.put("files",files);}}
    @Override protected void onSaveInstanceState(Bundle out){out.putString("current_file",current);super.onSaveInstanceState(out);}
    private class FileAdapter extends RecyclerView.Adapter<FileAdapter.Holder>{class Holder extends RecyclerView.ViewHolder{Holder(View v){super(v);}}
        @Override public Holder onCreateViewHolder(ViewGroup p,int t){TextView view=new TextView(CodeEditorActivity.this);RecyclerView.LayoutParams lp=new RecyclerView.LayoutParams(-2,Ui.dp(CodeEditorActivity.this,56));lp.setMargins(0,6,12,6);view.setLayoutParams(lp);view.setPadding(20,0,20,0);view.setGravity(Gravity.CENTER);view.setTypeface(android.graphics.Typeface.MONOSPACE);view.setTextSize(12);return new Holder(view);}
        @Override public int getItemCount(){return names.size();}
        @Override public void onBindViewHolder(Holder h,int p){String filename=names.get(p);TextView label=(TextView)h.itemView;label.setText(filename);label.setTextColor(filename.equals(current)?0xFFF5C16C:0xFFAAAABE);label.setBackgroundResource(R.drawable.bg_glass_card);label.setOnClickListener(v->{try{persistCurrent();open(filename);}catch(Exception e){log(Ui.error(e));}});}
    }
}
