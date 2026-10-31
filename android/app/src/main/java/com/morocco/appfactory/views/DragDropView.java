package com.morocco.appfactory.views;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.*;
import org.json.*;

public class DragDropView extends View {
    private final Paint paint=new Paint(3);
    private JSONArray houses=new JSONArray();
    private float playerX=.5f,playerY=.5f,vx,vy;
    private int selected=-1;
    private boolean playing,active;
    private String template="Walking Sim";
    private float downX,downY;
    private boolean dragging;
    private final Runnable enableDrag=()->dragging=true;
    private final Runnable frame=new Runnable() { @Override public void run() {
        if(!active) return;
        if(playing) {
            float speed="Driving".equals(template)?.009f:.004f;
            float nx=clamp(playerX+vx*speed,.03f,.97f),ny=clamp(playerY+vy*speed,.04f,.96f);
            boolean collision=false;
            for(int i=0;i<houses.length();i++) { JSONObject h=houses.optJSONObject(i);
                if(h!=null && Math.abs(nx-h.optDouble("x"))<.065 && Math.abs(ny-h.optDouble("y"))<.075) collision=true;
            }
            if(!collision) { playerX=nx; playerY=ny; }
            invalidate();
        }
        postDelayed(this,16);
    }};
    public DragDropView(Context c) { this(c,null); }
    public DragDropView(Context c, AttributeSet a) { super(c,a); }
    private static float clamp(float n,float min,float max) { return Math.max(min,Math.min(max,n)); }
    public void setHouses(JSONArray h) { houses=h==null?new JSONArray():h; invalidate(); }
    public JSONArray getHouses() { return houses; }
    public void setTemplate(String value) { template=value; invalidate(); }
    public void setPlaying(boolean value) { playing=value; selected=-1; invalidate(); }
    public boolean isPlaying() { return playing; }
    public void move(float x,float y) { vx=x; vy=y; }
    public void setActive(boolean value) { active=value; removeCallbacks(frame); if(value) post(frame); else { vx=0; vy=0; } }
    public void addHouse() {
        if(houses.length()>=60)return;
        try { int i=houses.length(); houses.put(new JSONObject().put("x",.18+(i%5)*.14).put("y",.18+(i/5%4)*.17)); invalidate(); }
        catch(JSONException e) { throw new IllegalStateException(e); }
    }
    @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); setActive(true); }
    @Override protected void onDetachedFromWindow() { setActive(false); removeCallbacks(enableDrag); super.onDetachedFromWindow(); }
    @Override protected void onDraw(Canvas c) {
        int w=getWidth(),h=getHeight(); c.drawColor(0xFF202B31);
        paint.setColor(0xFF2C383B); paint.setStrokeWidth(1);
        for(int i=0;i<12;i++) { c.drawLine(i*w/12f,0,i*w/12f,h,paint); c.drawLine(0,i*h/12f,w,i*h/12f,paint); }
        paint.setColor(0xFFB39270); c.drawRect(w*.44f,0,w*.56f,h,paint); c.drawRect(0,h*.44f,w,h*.56f,paint);
        for(int i=0;i<houses.length();i++) { JSONObject house=houses.optJSONObject(i); if(house==null) continue;
            float x=(float)house.optDouble("x",.2)*w,y=(float)house.optDouble("y",.2)*h;
            float rw=w*.055f,rh=h*.06f;
            paint.setColor(0x55000000); c.drawRoundRect(x-rw+4,y-rh+6,x+rw+4,y+rh+6,4,4,paint);
            paint.setColor(i==selected?0xFF6BA7FF:0xFFC18A64); c.drawRoundRect(x-rw,y-rh,x+rw,y+rh,4,4,paint);
            paint.setColor(0xFF7B5144); c.drawRect(x-rw+4,y-rh+4,x+rw-4,y+rh-4,paint);
            paint.setColor(0xFFF5C16C); c.drawRect(x-3,y+rh-10,x+3,y+rh,paint);
        }
        float px=playerX*w,py=playerY*h;
        paint.setColor(0xFF6BA7FF);
        if("Driving".equals(template)) c.drawRoundRect(px-8,py-14,px+8,py+14,5,5,paint);
        else c.drawCircle(px,py,9,paint);
        paint.setColor(0xFFF5C16C); paint.setTextSize(12*getResources().getDisplayMetrics().scaledDensity);
        c.drawText(playing?"PLAY · "+template:"EDIT · "+houses.length()+" houses",16,28,paint);
    }
    @Override public boolean onTouchEvent(MotionEvent e) {
        if(playing) return true;
        float x=e.getX()/Math.max(1,getWidth()),y=e.getY()/Math.max(1,getHeight());
        switch(e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                selected=-1; dragging=false; downX=e.getX();downY=e.getY();
                for(int i=houses.length()-1;i>=0;i--) { JSONObject h=houses.optJSONObject(i);
                    if(h!=null && Math.abs(x-h.optDouble("x"))<.07 && Math.abs(y-h.optDouble("y"))<.08) { selected=i; break; }
                }
                if(selected>=0) { getParent().requestDisallowInterceptTouchEvent(true); postDelayed(enableDrag,250); }
                invalidate(); return true;
            case MotionEvent.ACTION_MOVE:
                if(selected>=0 && dragging) try {
                    houses.getJSONObject(selected).put("x",clamp(x,.06f,.94f)).put("y",clamp(y,.08f,.92f)); invalidate();
                } catch(JSONException ignored) { }
                return true;
            case MotionEvent.ACTION_UP:
                if(selected<0 && houses.length()<60 && "Building".equals(template) && Math.abs(e.getX()-downX)<12 && Math.abs(e.getY()-downY)<12) {
                    try { houses.put(new JSONObject().put("x",clamp(x,.06f,.94f)).put("y",clamp(y,.08f,.92f))); } catch(JSONException ignored) { }
                }
                performClick();
            case MotionEvent.ACTION_CANCEL:
                removeCallbacks(enableDrag); dragging=false; getParent().requestDisallowInterceptTouchEvent(false); invalidate();return true;
            default:return true;
        }
    }
    @Override public boolean performClick() { super.performClick();return true; }
}
