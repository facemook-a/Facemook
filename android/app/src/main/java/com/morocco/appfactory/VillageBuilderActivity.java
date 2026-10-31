package com.morocco.appfactory;

import android.os.Bundle;
import android.widget.TextView;
import com.morocco.appfactory.ui.*;
import com.morocco.appfactory.views.VillagePreviewView;

public class VillageBuilderActivity extends BaseBuilderActivity {
    private VillagePreviewView preview;
    @Override public void onCreate(Bundle state){super.onCreate(state);if(!setup(state,R.layout.activity_village,"village","Village Builder","Aït Lumière"))return;
        preview=findViewById(R.id.village_preview);preview.setHouses(project.data.optJSONArray("houses"));
        if(project.data.optJSONArray("houses")==null)preview.generate(project.name);
        findViewById(R.id.generate).setOnClickListener(v->{String value=name.getText().toString().trim();if(value.isEmpty()){name.setError("Name your village");return;}preview.generate(value);stats();Ui.fade(preview,0);});stats();
    }
    private void stats(){((TextView)findViewById(R.id.village_stats)).setText(preview.getHouses().length()+" homes   ·   2 sample roads   ·   Atlas terrain\n\nLocal sample map · no live OpenStreetMap data\nSave the village to return to it later.");}
    @Override protected void collect()throws Exception{if(preview!=null)project.data.put("houses",preview.getHouses()).put("mapSource","deterministic-sample");}
}
