package com.wilbert.svcamera;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

/**
 * @author wilbert
 * @Date 2020/12/26 14:30
 * @email jiangwang.wilbert@bigo.sg
 **/
public class FocusGLSurfaceView extends GLSurfaceView implements GestureDetector.OnGestureListener {
    private final String TAG = "FocusGLSurfaceView";
    private GestureDetector mGestureDetector = null;

    public FocusGLSurfaceView(Context context) {
        super(context);
        init(context);
    }

    public FocusGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context){
        mGestureDetector = new GestureDetector(context,this);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
    }

    @Override
    public boolean onDown(MotionEvent e) {
        Log.i(TAG,"onDown:"+e.getX()+"*"+e.getY());
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
        Log.i(TAG,"onShowPress:"+e.getX()+"*"+e.getY());
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        Log.i(TAG,"onSingleTapUp:"+e.getX()+"*"+e.getY());
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        Log.i(TAG,"onScroll:"+e1.getX()+"*"+e1.getY());
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        Log.i(TAG,"onLongPress:"+e.getX()+"*"+e.getY());
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        Log.i(TAG,"onFling:"+e1.getX()+"*"+e1.getY());
        return false;
    }
}
