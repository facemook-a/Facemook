package com.morocco.appfactory;

import android.os.Bundle;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.morocco.appfactory.data.*;
import com.morocco.appfactory.ui.*;
import org.json.*;
import java.util.regex.*;

public class PreviewActivity extends AppCompatActivity {
    private FrameLayout canvas;
    private Project project;
    @Override public void onCreate(Bundle state){super.onCreate(state);
        try{project=new ProjectStore(this).load(getIntent().getStringExtra("project_id"));}catch(Exception e){Ui.toast(this,Ui.error(e));finish();return;}
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(0xFF0F0F1A);
        Toolbar toolbar=new Toolbar(this);toolbar.setTitle("Preview · "+project.name);toolbar.setTitleTextColor(Color.WHITE);toolbar.setSubtitle("code".equals(project.type)?"STATIC TEXT PREVIEW · NOT JAVA EXECUTION":"LIVE NO-CODE PREVIEW");toolbar.setSubtitleTextColor(0xFFF5C16C);toolbar.setNavigationIcon(R.drawable.ic_back);toolbar.setNavigationOnClickListener(v->finish());root.addView(toolbar,new LinearLayout.LayoutParams(-1,Ui.dp(this,80)));
        canvas=new FrameLayout(this);root.addView(canvas,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);canvas.post(()->{if("code".equals(project.type))renderCode();else renderApp();});
    }
    private void renderCode(){
        String source=project.data.optJSONObject("files").optString("MainActivity.java");
        Matcher match=Pattern.compile("\\.setText\\(\\s*(\"(?:\\\\.|[^\"\\\\])*\")\\s*\\)").matcher(source);
        String title="No literal setText() found. Export this source project and run it in AndroidIDE.";
        if(match.find())try{title=new JSONArray("["+match.group(1)+"]").getString(0);}catch(JSONException e){title="This Java string needs a full AndroidIDE build to preview.";}
        TextView text=new TextView(this);text.setText(title);text.setTextSize(28);text.setTextColor(Color.WHITE);text.setGravity(Gravity.CENTER);text.setPadding(24,24,24,24);canvas.addView(text,new FrameLayout.LayoutParams(-1,-1));
    }
    private void renderApp(){
        canvas.removeAllViews();JSONArray items=project.data.optJSONArray("components");if(items==null||items.length()==0){TextView empty=new TextView(this);empty.setText("Add a component in the designer to begin.");empty.setTextColor(Color.WHITE);empty.setGravity(Gravity.CENTER);canvas.addView(empty,new FrameLayout.LayoutParams(-1,-1));return;}
        for(int i=0;i<items.length();i++){
            JSONObject item=items.optJSONObject(i);if(item==null)continue;String type=item.optString("type");View v;
            if("ImageView".equals(type)){ImageView image=new ImageView(this);image.setImageResource(R.drawable.ic_image);image.setScaleType(ImageView.ScaleType.CENTER_INSIDE);image.setContentDescription(item.optString("text"));v=image;}
            else{TextView label="EditText".equals(type)?new EditText(this):new TextView(this);label.setText(item.optString("text"));label.setTextColor(Color.WHITE);label.setTextSize(15);label.setGravity(Gravity.CENTER);label.setPadding(12,0,12,0);v=label;}
            GradientDrawable bg=new GradientDrawable();bg.setColor(Ui.color(item.optString("color"),0xFF3A7BD5));bg.setCornerRadius(Ui.dp(this,"Button".equals(type)?28:12));v.setBackground(bg);
            int width=Math.min(Ui.dp(this,item.optInt("width",180)),canvas.getWidth());canvas.addView(v,new FrameLayout.LayoutParams(width,Ui.dp(this,56)));
            v.setX(Math.max(0,Math.min(canvas.getWidth()-width,(float)item.optDouble("x")*canvas.getWidth())));v.setY(Math.max(0,Math.min(canvas.getHeight()-Ui.dp(this,56),(float)item.optDouble("y")*canvas.getHeight())));
            String action=item.optString("action");if("Show Toast".equals(action))v.setOnClickListener(view->Ui.toast(this,item.optString("value")));
            if("Go to Screen".equals(action))v.setOnClickListener(view->secondScreen(item.optString("value","Second Screen")));
        }
    }
    private void secondScreen(String title){canvas.removeAllViews();LinearLayout screen=new LinearLayout(this);screen.setGravity(Gravity.CENTER);screen.setOrientation(LinearLayout.VERTICAL);screen.setPadding(24,24,24,24);TextView text=new TextView(this);text.setText(title);text.setTextColor(Color.WHITE);text.setTextSize(28);text.setGravity(Gravity.CENTER);screen.addView(text,new LinearLayout.LayoutParams(-1,-2));FactoryButton back=new FactoryButton(this);back.setText("Back to First Screen");back.setIcon(R.drawable.ic_back);screen.addView(back,new LinearLayout.LayoutParams(-1,Ui.dp(this,56)));back.setOnClickListener(v->renderApp());canvas.addView(screen,new FrameLayout.LayoutParams(-1,-1));}
}
