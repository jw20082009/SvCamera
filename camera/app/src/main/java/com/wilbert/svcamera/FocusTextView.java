package com.wilbert.svcamera;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

/**
 * @author wilbert
 * @Date 2020/12/26 14:44
 * @email jw20082009@qq.com
 **/
public class FocusTextView extends androidx.appcompat.widget.AppCompatTextView implements View.OnTouchListener {
    private final String TAG = "FocusTextView";
    GestureDetector mGestureDetector;

    public FocusTextView(Context context) {
        super(context);
        init(context);
    }

    public FocusTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public FocusTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context){
        setOnTouchListener(this);
        mGestureDetector = new GestureDetector(context,mGestureListener);
    }

    public void setOnDoubleTabListener(GestureDetector.OnDoubleTapListener listener){
        mGestureDetector.setOnDoubleTapListener(listener);
    }

    GestureDetector.OnGestureListener mGestureListener = new GestureDetector.OnGestureListener() {
        @Override
        public boolean onDown(MotionEvent e) {
            Log.e(TAG,"onDown");
            return true;
        }

        @Override
        public void onShowPress(MotionEvent e) {
            Log.e(TAG,"onShowPress:"+e.getX()+"*"+e.getY());
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            Log.e(TAG,"onSingleTapUp:"+e.getX()+"*"+e.getY());
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            Log.e(TAG,"onScroll:"+distanceX+"*"+distanceY);
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            Log.e(TAG,"onLongPress:"+e.getX()+"*"+e.getY());
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            Log.e(TAG,"onFling:"+velocityX+"*"+velocityY);
            return false;
        }
    };

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
    }
}
