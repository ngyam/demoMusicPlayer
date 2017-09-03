package com.example.aznagy.paintproplayer.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.example.aznagy.paintproplayer.interfaces.ClickListener;

public class CustomTouchListener implements RecyclerView.OnItemTouchListener {

    private static String LOG_TAG = "CustomTouchListener";

    //Gesture detector to intercept the touch events
    GestureDetector gestureDetector;
    private ClickListener clickListener;

    public CustomTouchListener(Context context, final ClickListener clickListener) {
        Log.d(LOG_TAG, "CustomTouchListener");
        this.clickListener = clickListener;
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                Log.d(LOG_TAG, "onSingleTapUp");
                return true;
            }
        });
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView recyclerView, MotionEvent e) {
        Log.d(LOG_TAG, "onInterceptTouchEvent");
        View child = recyclerView.findChildViewUnder(e.getX(), e.getY());
        if (child != null && clickListener != null && gestureDetector.onTouchEvent(e)) {
            clickListener.onClick(child, recyclerView.getChildLayoutPosition(child));
        }
        return false;
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent e) {
        Log.d(LOG_TAG, "onTouchEvent");
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        Log.d(LOG_TAG, "onRequestDisallowInterceptTouchEvent");
    }
}
