package com.elfilibustero.f3d;

import android.view.MotionEvent;

/**
 * Provides a gesture detector to detect panning.
 * Panning is considered to be a two finger swipe.
 */
public class PanGestureDetector {
    private static final int INVALID_POINTER_ID = -1;

    private Line mPreviousLine;
    private int mPointerId1, mPointerId2;
    private float mXDistance, mYDistance;

    private final OnPanGestureListener mGestureListener;

    public PanGestureDetector(final OnPanGestureListener listener) {
        mGestureListener = listener;
        mPointerId1 = INVALID_POINTER_ID;
        mPointerId2 = INVALID_POINTER_ID;
        mXDistance = 0f;
        mYDistance = 0f;
        mPreviousLine = new Line();
    }

    public float getDistanceX() {
        return mXDistance;
    }

    public float getDistanceY() {
        return mYDistance;
    }

    public void onTouchEvent(final MotionEvent event) {
        switch (event.getActionMasked()) {

            case MotionEvent.ACTION_DOWN: {
                mPointerId1 = event.getPointerId(event.getActionIndex());
                mPointerId2 = INVALID_POINTER_ID;
                mXDistance = 0f;
                mYDistance = 0f;
                break;
            }

            case MotionEvent.ACTION_POINTER_DOWN: {
                if (event.getPointerCount() >= 2) {
                    // Pick two pointers by index (stable)
                    mPointerId1 = event.getPointerId(0);
                    mPointerId2 = event.getPointerId(1);

                    unpackLinePosition(event, mPreviousLine);
                    mXDistance = 0f;
                    mYDistance = 0f;
                }
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (event.getPointerCount() >= 2
                        && mPointerId1 != INVALID_POINTER_ID
                        && mPointerId2 != INVALID_POINTER_ID) {

                    Line currentLine = new Line();
                    unpackLinePosition(event, currentLine);

                    updateDistanceBetweenLines(mPreviousLine, currentLine);

                    mGestureListener.onPan(this);

                    mPreviousLine = currentLine;
                } else {
                    mXDistance = 0f;
                    mYDistance = 0f;
                }
                break;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                // After pointer up, we may still have >=2 pointers remaining.
                int remaining = event.getPointerCount() - 1;

                if (remaining >= 2) {
                    int upIndex = event.getActionIndex();

                    int firstIndex = (upIndex == 0) ? 1 : 0;
                    int secondIndex = -1;

                    for (int i = 0; i < event.getPointerCount(); i++) {
                        if (i == upIndex) continue;
                        if (i == firstIndex) continue;
                        secondIndex = i;
                        break;
                    }

                    if (secondIndex != -1) {
                        mPointerId1 = event.getPointerId(firstIndex);
                        mPointerId2 = event.getPointerId(secondIndex);

                        unpackLinePosition(event, mPreviousLine);
                        mXDistance = 0f;
                        mYDistance = 0f;
                    } else {
                        mPointerId1 = INVALID_POINTER_ID;
                        mPointerId2 = INVALID_POINTER_ID;
                        mXDistance = 0f;
                        mYDistance = 0f;
                    }
                } else {
                    mPointerId1 = INVALID_POINTER_ID;
                    mPointerId2 = INVALID_POINTER_ID;
                    mXDistance = 0f;
                    mYDistance = 0f;
                }
                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                mPointerId1 = INVALID_POINTER_ID;
                mPointerId2 = INVALID_POINTER_ID;
                mXDistance = 0f;
                mYDistance = 0f;
                break;
            }
        }
    }

    private void unpackLinePosition(final MotionEvent event, final Line line) {
        int index1 = event.findPointerIndex(mPointerId1);
        int index2 = event.findPointerIndex(mPointerId2);

        if (index1 >= 0) {
            line.setX1(event.getX(index1));
            line.setY1(event.getY(index1));
        }

        if (index2 >= 0) {
            line.setX2(event.getX(index2));
            line.setY2(event.getY(index2));
        }
    }

    private void updateDistanceBetweenLines(final Line line1, final Line line2) {
        Point center1 = line1.getCenter();
        Point center2 = line2.getCenter();

        mXDistance = center2.getX() - center1.getX();
        mYDistance = center2.getY() - center1.getY();
    }

    public static class OnPanGestureListener {
        void onPan(PanGestureDetector detector) {
            throw new RuntimeException("Not implemented!");
        }
    }
}
