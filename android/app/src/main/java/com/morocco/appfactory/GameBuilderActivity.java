package com.morocco.appfactory;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import com.morocco.appfactory.views.*;
import com.morocco.appfactory.ui.*;

public class GameBuilderActivity extends BaseBuilderActivity {
    private DragDropView canvas;
    private Spinner templates;
    private final String[] options={"Walking Sim","Driving","Building"};
    @Override public void onCreate(Bundle state){super.onCreate(state);if(!setup(state,R.layout.activity_game_builder,"game","Game Builder","Atlas Adventures"))return;
        canvas=findViewById(R.id.game_canvas);canvas.setHouses(project.data.optJSONArray("objects"));if(!project.data.has("objects"))for(int i=0;i<5;i++)canvas.addHouse();
        templates=findViewById(R.id.template_spinner);templates.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,options));
        for(int i=0;i<options.length;i++)if(options[i].equals(project.data.optString("template")))templates.setSelection(i);
        templates.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int pos,long id){canvas.setTemplate(options[pos]);}});
        ((JoystickView)findViewById(R.id.joystick)).setListener(canvas::move);
        findViewById(R.id.add_house).setOnClickListener(v->{if(canvas.isPlaying()){Ui.toast(this,"Switch to Edit to place houses");return;}if(canvas.getHouses().length()>=60){Ui.toast(this,"Maximum 60 houses");return;}canvas.addHouse();stats();});
        FactoryButton play=findViewById(R.id.play_game);play.setIcon(R.drawable.ic_play);play.setOnClickListener(v->{canvas.setPlaying(!canvas.isPlaying());play.setText(canvas.isPlaying()?"Return to Edit":"Play / Edit");stats();});stats();
    }
    private void stats(){((TextView)findViewById(R.id.game_stats)).setText(canvas.getHouses().length()+" houses · "+(canvas.isPlaying()?"Joystick active · houses block movement":"Edit mode · Building template also supports tap-to-place"));}
    @Override protected void collect()throws Exception{if(canvas!=null)project.data.put("objects",canvas.getHouses()).put("template",templates.getSelectedItem().toString()).put("controls","joystick").put("collision",true);}
    @Override protected void onResume(){super.onResume();if(canvas!=null)canvas.setActive(true);}
    @Override protected void onPause(){if(canvas!=null)canvas.setActive(false);super.onPause();}
}
