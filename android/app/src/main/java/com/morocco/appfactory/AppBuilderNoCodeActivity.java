package com.morocco.appfactory;

import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.*;
import com.morocco.appfactory.build.SourceGenerator;
import com.morocco.appfactory.ui.*;
import org.json.*;

public class AppBuilderNoCodeActivity extends BaseBuilderActivity {
    private final String[] types={"Button","TextView","ImageView","EditText"};
    private final String[] actions={"None","Show Toast","Go to Screen"};
    private JSONArray components;
    private FrameLayout canvas;
    private EditText text,color,width,value;
    private Spinner action;
    private int selected=-1;
    @Override public void onCreate(Bundle state){super.onCreate(state);if(!setup(state,R.layout.activity_app_builder_nocode,"app","App Builder","My Moroccan App"))return;
        components=project.data.optJSONArray("components");if(components==null)components=new JSONArray();
        canvas=findViewById(R.id.design_canvas);text=findViewById(R.id.component_text);color=findViewById(R.id.component_color);width=findViewById(R.id.component_width);value=findViewById(R.id.logic_value);action=findViewById(R.id.logic_action);
        width.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);color.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        action.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,actions));
        RecyclerView palette=findViewById(R.id.palette);palette.setLayoutManager(new LinearLayoutManager(this,RecyclerView.HORIZONTAL,false));palette.setAdapter(new PaletteAdapter());
        if(getResources().getConfiguration().screenWidthDp>=700){
            LinearLayout panels=findViewById(R.id.designer_panels);panels.setOrientation(LinearLayout.HORIZONTAL);
            palette.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(this,140),Ui.dp(this,440)));palette.setLayoutManager(new LinearLayoutManager(this));
            canvas.setLayoutParams(new LinearLayout.LayoutParams(0,Ui.dp(this,440),1));
            View properties=findViewById(R.id.properties_panel);properties.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(this,225),-2));
        }
        canvas.setOnDragListener((v,event)->{
            if(event.getAction()==DragEvent.ACTION_DRAG_STARTED)return event.getClipDescription()!=null&&event.getClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN);
            if(event.getAction()==DragEvent.ACTION_DROP){String type=event.getClipData().getItemAt(0).getText().toString();for(String known:types)if(known.equals(type))add(type,event.getX()/canvas.getWidth(),event.getY()/canvas.getHeight());return true;}return true;
        });
        findViewById(R.id.apply_properties).setOnClickListener(v->{try{apply();render();Ui.toast(this,"Component updated");}catch(Exception e){Ui.toast(this,Ui.error(e));}});
        findViewById(R.id.delete_component).setOnClickListener(v->{if(selected>=0){components.remove(selected);selected=-1;render();clearProperties();}});
        findViewById(R.id.preview_app).setOnClickListener(v->{try{prepare();store.save(project);startActivity(new Intent(this,PreviewActivity.class).putExtra("project_id",project.id));}catch(Exception e){Ui.toast(this,Ui.error(e));}});
        canvas.post(()->{render();if(components.length()>0)select(0);else clearProperties();});
    }
    private void add(String type,float x,float y){
        if(components.length()>=40){Ui.toast(this,"Maximum 40 components per screen");return;}
        try{components.put(new JSONObject().put("type",type).put("text",type.equals("Button")?"Made in Morocco":type)
            .put("color","#3A7BD5").put("width",180).put("x",Math.max(0,Math.min(.65,x))).put("y",Math.max(0,Math.min(.85,y))).put("action","None").put("value","Hello from Morocco!"));selected=components.length()-1;render();select(selected);}catch(Exception e){Ui.toast(this,Ui.error(e));}
    }
    private void render(){
        canvas.removeAllViews();
        for(int i=0;i<components.length();i++){
            JSONObject item=components.optJSONObject(i);if(item==null)continue;String type=SourceGenerator.componentType(item.optString("type"));
            View view;
            if(type.equals("ImageView")){ImageView image=new ImageView(this);image.setImageResource(R.drawable.ic_image);image.setScaleType(ImageView.ScaleType.CENTER_INSIDE);image.setContentDescription(item.optString("text"));view=image;}
            else{TextView label=new TextView(this);label.setText(item.optString("text",type));label.setTextColor(Color.WHITE);label.setTextSize(14);label.setGravity(Gravity.CENTER);label.setPadding(8,0,8,0);view=label;}
            GradientDrawable bg=new GradientDrawable();bg.setColor(Ui.color(item.optString("color"),0xFF3A7BD5));bg.setCornerRadius(Ui.dp(this,type.equals("Button")?28:12));if(i==selected)bg.setStroke(Ui.dp(this,2),0xFFF5C16C);view.setBackground(bg);
            int w=Math.min(Math.max(Ui.dp(this,48),Ui.dp(this,item.optInt("width",180))),Math.max(1,canvas.getWidth()));
            FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(w,Ui.dp(this,56));canvas.addView(view,lp);
            view.setX(Math.min(Math.max(0,canvas.getWidth()-w),(float)item.optDouble("x",0)*canvas.getWidth()));view.setY(Math.min(Math.max(0,canvas.getHeight()-Ui.dp(this,56)),(float)item.optDouble("y",0)*canvas.getHeight()));
            int index=i;view.setOnTouchListener(new View.OnTouchListener(){float offsetX,offsetY,rawX,rawY;boolean moved;
                @Override public boolean onTouch(View v,android.view.MotionEvent e){
                    if(e.getActionMasked()==MotionEvent.ACTION_DOWN){selected=index;offsetX=v.getX();offsetY=v.getY();rawX=e.getRawX();rawY=e.getRawY();moved=false;canvas.getParent().requestDisallowInterceptTouchEvent(true);select(index);return true;}
                    if(e.getActionMasked()==MotionEvent.ACTION_MOVE){float dx=e.getRawX()-rawX,dy=e.getRawY()-rawY;moved|=Math.abs(dx)+Math.abs(dy)>8;v.setX(Math.max(0,Math.min(canvas.getWidth()-v.getWidth(),offsetX+dx)));v.setY(Math.max(0,Math.min(canvas.getHeight()-v.getHeight(),offsetY+dy)));return true;}
                    if(e.getActionMasked()==MotionEvent.ACTION_UP||e.getActionMasked()==MotionEvent.ACTION_CANCEL){try{item.put("x",v.getX()/Math.max(1,canvas.getWidth())).put("y",v.getY()/Math.max(1,canvas.getHeight()));}catch(JSONException ignored){}canvas.getParent().requestDisallowInterceptTouchEvent(false);v.performClick();render();return true;}return false;
                }
            });
        }
    }
    private void select(int index){
        selected=index;JSONObject item=components.optJSONObject(index);if(item==null)return;
        setPropertiesEnabled(true);text.setText(item.optString("text"));color.setText(item.optString("color","#3A7BD5"));width.setText(String.valueOf(item.optInt("width",180)));value.setText(item.optString("value","Hello from Morocco!"));
        for(int i=0;i<actions.length;i++)if(actions[i].equals(item.optString("action","None")))action.setSelection(i);
    }
    private void setPropertiesEnabled(boolean enabled){text.setEnabled(enabled);color.setEnabled(enabled);width.setEnabled(enabled);value.setEnabled(enabled);action.setEnabled(enabled);findViewById(R.id.apply_properties).setEnabled(enabled);findViewById(R.id.delete_component).setEnabled(enabled);}
    private void clearProperties(){text.setText("");color.setText("");width.setText("");value.setText("");setPropertiesEnabled(false);}
    private void apply()throws Exception{
        if(selected<0)return;JSONObject item=components.getJSONObject(selected);
        String colorValue=color.getText().toString().trim();if(!colorValue.matches("#[a-fA-F0-9]{6}([a-fA-F0-9]{2})?")){color.setError("Use #RRGGBB or #AARRGGBB");throw new IllegalArgumentException("Enter a valid color");}
        int size;try{size=Integer.parseInt(width.getText().toString());}catch(NumberFormatException e){throw new IllegalArgumentException("Width must be a number (48–320)");}if(size<48||size>320)throw new IllegalArgumentException("Width must be 48–320 dp");
        if(text.length()>2000||value.length()>2000)throw new IllegalArgumentException("Text limit: 2,000 characters");
        item.put("text",text.getText().toString()).put("color",colorValue).put("width",size).put("action",action.getSelectedItem().toString()).put("value",value.getText().toString());
    }
    @Override protected void collect()throws Exception{if(components!=null){apply();project.data.put("components",components);}}
    private class PaletteAdapter extends RecyclerView.Adapter<PaletteAdapter.Holder>{
        class Holder extends RecyclerView.ViewHolder{Holder(View v){super(v);}}
        @Override public Holder onCreateViewHolder(ViewGroup p,int t){TextView label=new TextView(AppBuilderNoCodeActivity.this);RecyclerView.LayoutParams lp=new RecyclerView.LayoutParams(Ui.dp(AppBuilderNoCodeActivity.this,130),Ui.dp(AppBuilderNoCodeActivity.this,78));lp.setMargins(0,4,10,4);label.setLayoutParams(lp);label.setGravity(Gravity.CENTER);label.setTextColor(0xFFFFFFFF);label.setTextSize(13);label.setBackgroundResource(R.drawable.bg_glass_card);return new Holder(label);}
        @Override public int getItemCount(){return types.length;}
        @Override public void onBindViewHolder(Holder h,int p){String type=types[p];((TextView)h.itemView).setText((p==0?"◉ ":p==1?"T ":p==2?"▧ ":"✎ ")+type);h.itemView.setOnClickListener(v->add(type,.08f,(components.length()%6)*.12f));h.itemView.setOnLongClickListener(v->{ClipData clip=ClipData.newPlainText("component",type);if(Build.VERSION.SDK_INT>=24)v.startDragAndDrop(clip,new View.DragShadowBuilder(v),null,0);else v.startDrag(clip,new View.DragShadowBuilder(v),null,0);return true;});}
    }
}
