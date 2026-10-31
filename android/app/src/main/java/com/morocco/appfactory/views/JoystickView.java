package com.morocco.appfactory.views;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.*;

public class JoystickView extends View {
    public interface Listener { void onMove(float xPercent, float yPercent); }
    private Listener listener;
    private final Paint paint = new Paint(3);
    private float xPercent, yPercent;
    public JoystickView(Context c) { this(c,null); }
    public JoystickView(Context c, AttributeSet a) { super(c,a); setFocusable(true); }
    public void setListener(Listener listener) { this.listener=listener; }
    public float getXPercent() { return xPercent; }
    public float getYPercent() { return yPercent; }
    @Override protected void onDraw(Canvas c) {
        float x=getWidth()/2f,y=getHeight()/2f,r=Math.min(x,y)*.92f;
        paint.setShader(null); paint.setColor(0x33FFFFFF); c.drawCircle(x,y,r,paint);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(2); paint.setColor(0x55FFFFFF); c.drawCircle(x,y,r,paint); paint.setStyle(Paint.Style.FILL);
        float cx=x+xPercent*r*.55f,cy=y+yPercent*r*.55f;
        paint.setShader(new LinearGradient(cx-r*.4f,cy-r*.4f,cx+r*.4f,cy+r*.4f,0xFFF5C16C,0xFF3A7BD5,Shader.TileMode.CLAMP));
        c.drawCircle(cx,cy,r*.4f,paint); paint.setShader(null);
    }
    @Override public boolean onTouchEvent(MotionEvent e) {
        switch(e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: getParent().requestDisallowInterceptTouchEvent(true);
            case MotionEvent.ACTION_MOVE:
                float r=Math.min(getWidth(),getHeight())*.46f;
                float x=(e.getX()-getWidth()/2f)/r,y=(e.getY()-getHeight()/2f)/r;
                float length=(float)Math.sqrt(x*x+y*y); if(length>1) { x/=length; y/=length; }
                xPercent=x; yPercent=y; break;
            case MotionEvent.ACTION_UP: case MotionEvent.ACTION_CANCEL:
                xPercent=0; yPercent=0; getParent().requestDisallowInterceptTouchEvent(false); performClick(); break;
            default: return false;
        }
        if(listener!=null) listener.onMove(xPercent,yPercent); invalidate(); return true;
    }
    @Override public boolean performClick() { super.performClick(); return true; }
}
