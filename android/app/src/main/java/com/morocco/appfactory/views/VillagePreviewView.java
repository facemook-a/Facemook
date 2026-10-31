package com.morocco.appfactory.views;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import org.json.*;
import java.util.*;

/** Software-rendered isometric scene: deterministic sample geography, no OSM network claims. */
public class VillagePreviewView extends View {
    private final Paint paint = new Paint(3);
    private JSONArray houses = new JSONArray();
    private float rotation = .65f, downX;
    public VillagePreviewView(Context c) { this(c, null); }
    public VillagePreviewView(Context c, AttributeSet a) { super(c, a); generate("Ait Lumiere"); }

    public static JSONArray sample(String name) {
        Random random = new Random(name.trim().toLowerCase(Locale.ROOT).hashCode());
        JSONArray result = new JSONArray();
        try {
            for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
                if (x == 0 || z == 0 || random.nextFloat() < .18f) continue;
                result.put(new JSONObject().put("x", x * 1.35).put("z", z * 1.35)
                        .put("height", .6 + random.nextDouble() * 1.2));
            }
        } catch (JSONException e) { throw new IllegalStateException(e); }
        return result;
    }
    public void generate(String name) { houses = sample(name); invalidate(); }
    public JSONArray getHouses() { return houses; }
    public void setHouses(JSONArray value) { if (value != null) houses = value; invalidate(); }

    private float[] point(float x, float z, float y) {
        float scale = Math.min(getWidth() / 13f, getHeight() / 10f);
        float sx = (float)(x * Math.cos(rotation) - z * Math.sin(rotation));
        float sz = (float)(x * Math.sin(rotation) + z * Math.cos(rotation));
        return new float[]{getWidth() / 2f + sx * scale, getHeight() * .57f + sz * scale * .45f - y * scale};
    }
    private void polygon(Canvas c, int color, float[]... points) {
        Path path = new Path(); path.moveTo(points[0][0], points[0][1]);
        for (int i = 1; i < points.length; i++) path.lineTo(points[i][0], points[i][1]);
        path.close(); paint.setColor(color); c.drawPath(path, paint);
    }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c); c.drawColor(0xFF171C2D);
        paint.setShader(new RadialGradient(getWidth()*.6f, getHeight()*.3f, Math.max(1,getWidth()),
                new int[]{0xFF283952,0xFF141723}, null, Shader.TileMode.CLAMP));
        c.drawRect(0,0,getWidth(),getHeight(),paint); paint.setShader(null);
        polygon(c,0xFF4D4946,point(-5,-5,0),point(5,-5,0),point(5,5,0),point(-5,5,0));
        polygon(c,0xFFB18D65,point(-.35f,-5,.01f),point(.35f,-5,.01f),point(.35f,5,.01f),point(-.35f,5,.01f));
        polygon(c,0xFFB18D65,point(-5,-.35f,.01f),point(5,-.35f,.01f),point(5,.35f,.01f),point(-5,.35f,.01f));
        List<JSONObject> sorted = new ArrayList<>();
        for (int i=0;i<houses.length();i++) { JSONObject h=houses.optJSONObject(i); if(h!=null) sorted.add(h); }
        Collections.sort(sorted,(a,b)->Float.compare(point((float)a.optDouble("x"),(float)a.optDouble("z"),0)[1],point((float)b.optDouble("x"),(float)b.optDouble("z"),0)[1]));
        for(JSONObject house:sorted) {
            float x=(float)house.optDouble("x"),z=(float)house.optDouble("z"),h=(float)house.optDouble("height",1),w=.47f;
            polygon(c,0xFF785547,point(x-w,z-w,0),point(x+w,z-w,0),point(x+w,z-w,h),point(x-w,z-w,h));
            polygon(c,0xFFAF7A55,point(x+w,z-w,0),point(x+w,z+w,0),point(x+w,z+w,h),point(x+w,z-w,h));
            polygon(c,0xFF95684F,point(x-w,z+w,0),point(x+w,z+w,0),point(x+w,z+w,h),point(x-w,z+w,h));
            polygon(c,0xFFD1AB79,point(x-w,z-w,h),point(x+w,z-w,h),point(x+w,z+w,h),point(x-w,z+w,h));
            polygon(c,0xFFF5C16C,point(x-.12f,z+w,.18f),point(x+.12f,z+w,.18f),point(x+.12f,z+w,h*.65f),point(x-.12f,z+w,h*.65f));
        }
        paint.setColor(0xFFF5C16C); paint.setTextSize(26 * getResources().getDisplayMetrics().scaledDensity / 2);
        c.drawText("ATLAS / SAMPLE TERRAIN",20,getHeight()-20,paint);
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        if(event.getActionMasked()==MotionEvent.ACTION_DOWN) { downX=event.getX(); getParent().requestDisallowInterceptTouchEvent(true); return true; }
        if(event.getActionMasked()==MotionEvent.ACTION_MOVE) { rotation+=(event.getX()-downX)*.008f; downX=event.getX(); invalidate(); return true; }
        if(event.getActionMasked()==MotionEvent.ACTION_UP || event.getActionMasked()==MotionEvent.ACTION_CANCEL) { getParent().requestDisallowInterceptTouchEvent(false); performClick(); return true; }
        return true;
    }
    @Override public boolean performClick() { super.performClick(); return true; }
}
