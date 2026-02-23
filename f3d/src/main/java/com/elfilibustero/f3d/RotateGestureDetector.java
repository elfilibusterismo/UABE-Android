package com.elfilibustero.f3d;

import android.view.MotionEvent;

/**
 * Provides a gesture detector to detect rotation (1-finger swipe).
 * Rotation is disabled while 2+ fingers are down.
 */
public class RotateGestureDetector {

    private static final int INVALID_POINTER_ID = -1;

    private int mPointerId;
    private float mLastTouchX, mLastTouchY;
    private float mDistanceX, mDistanceY;

    private final OnRotateGestureListener mGestureListener;

    public RotateGestureDetector(final OnRotateGestureListener listener) {
        mGestureListener = listener;
        reset();
    }

    public float getDistanceX() {
        return mDistanceX;
    }

    public float getDistanceY() {
        return mDistanceY;
    }

    private void reset() {
        mPointerId = INVALID_POINTER_ID;
        mLastTouchX = 0f;
        mLastTouchY = 0f;
        mDistanceX = 0f;
        mDistanceY = 0f;
    }

    public void onTouchEvent(final MotionEvent event) {
        switch (event.getActionMasked()) {

            case MotionEvent.ACTION_DOWN: {
                mPointerId = event.getPointerId(event.getActionIndex());

                int idx = event.findPointerIndex(mPointerId);
                if (idx >= 0) {
                    mLastTouchX = event.getX(idx);
                    mLastTouchY = event.getY(idx);
                } else {
                    reset();
                }

                mDistanceX = 0f;
                mDistanceY = 0f;
                break;
            }

            case MotionEvent.ACTION_POINTER_DOWN: {
                // second finger down => disable rotation
                reset();
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (mPointerId == INVALID_POINTER_ID) break;

                // keep this strictly 1-finger rotation
                if (event.getPointerCount() != 1) {
                    reset();
                    break;
                }

                int idx = event.findPointerIndex(mPointerId);
                if (idx < 0) {
                    reset();
                    break;
                }

                float x = event.getX(idx);
                float y = event.getY(idx);

                mDistanceX = x - mLastTouchX;
                mDistanceY = y - mLastTouchY;

                mLastTouchX = x;
                mLastTouchY = y;

                mGestureListener.onRotate(this);
                break;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                // rotation stays disabled until a new ACTION_DOWN
                reset();
                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                reset();
                break;
            }
        }
    }

    public static class OnRotateGestureListener {
        void onRotate(RotateGestureDetector detector) {
            throw new RuntimeException("Not implemented!");
        }
    }
}
