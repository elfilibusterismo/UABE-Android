package com.elfilibustero.f3d;

import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.annotation.NonNull;

import java.util.Objects;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import app.f3d.F3D.Camera;
import app.f3d.F3D.Engine;
import app.f3d.F3D.Window;

public class MainView extends GLSurfaceView {

    private static final String TAG = "MainView";

    private Engine mEngine;

    private ScaleGestureDetector mScaleDetector;
    private PanGestureDetector mPanDetector;
    private RotateGestureDetector mRotateDetector;
    private GestureDetector mTapDetector;

    private volatile boolean surfaceReady = false;
    private String internalCachePath = "";

    private OnLoadSceneListener mOnLoadSceneListener;

    public MainView(Context context) {
        super(context);
        init(context);
    }

    public MainView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        start();

        this.mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        this.mPanDetector = new PanGestureDetector(new PanListener());
        this.mRotateDetector = new RotateGestureDetector(new RotateListener());
        this.mTapDetector = new GestureDetector(context, new TapListener());
    }


    void start() {
        setEGLConfigChooser(8, 8, 8, 0, 16, 0);
        setEGLContextClientVersion(3);

        setRenderer(new Renderer());
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    private void tryLoadSceneOrDelete(String path) {
        if (mEngine == null || path == null || path.isEmpty()) return;

        try {
            mEngine.getScene().add(path);
            if (mOnLoadSceneListener != null) mOnLoadSceneListener.onLoadScene(path);
            requestRender();
        } catch (RuntimeException e) {
            if (mOnLoadSceneListener != null) mOnLoadSceneListener.onLoadFailed(path, e);
        }
    }

    public void setOnLoadSceneListener(OnLoadSceneListener listener) {
        mOnLoadSceneListener = listener;
    }

    public void updateFilePath(String newFilePath) {
        internalCachePath = newFilePath;

        if (newFilePath == null || newFilePath.isEmpty()) return;

        if (mEngine != null) {
            final String path = newFilePath;
            queueEvent(() -> tryLoadSceneOrDelete(path));
        }
    }

    private void resetCamera() {
        if (!surfaceReady || mEngine == null) return;

        queueEvent(() -> {
            try {
                if (mEngine == null) return;

                Window window = mEngine.getWindow();
                Camera camera = window.getCamera();
                camera.resetToBounds();
            } catch (Throwable t) {
                Log.e(TAG, "resetCamera failed", t);
            }
        });

        requestRender();
    }

    private class TapListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(@NonNull MotionEvent e) {
            resetCamera();
            return true;
        }
    }

    private class Renderer implements GLSurfaceView.Renderer {

        @Override
        public void onDrawFrame(GL10 gl) {
            if (mEngine == null) return;
            mEngine.getWindow().render();
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            if (mEngine == null) return;
            mEngine.getWindow().setSize(width, height);
            requestRender();
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            Engine.autoloadPlugins();

            mEngine = Engine.createExternalEGL();
            mEngine.setCachePath(getContext().getCacheDir().getAbsolutePath());

            mEngine.getOptions().toggle("ui.axis");
            mEngine.getOptions().toggle("render.grid.enable");
            mEngine.getOptions().toggle("render.effect.antialiasing.enable");
            mEngine.getOptions().toggle("render.effect.tone_mapping");
            mEngine.getOptions().toggle("render.hdri.ambient");
            mEngine.getOptions().toggle("render.background.skybox");
            mEngine.getOptions().toggle("ui.filename");
            mEngine.getOptions().toggle("ui.loader_progress");

            surfaceReady = true;

            if (!Objects.equals(internalCachePath, "")) {
                final String path = internalCachePath;
                queueEvent(() -> tryLoadSceneOrDelete(path));
            }
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(@NonNull ScaleGestureDetector detector) {
            if (!surfaceReady || mEngine == null) return true;

            final float scale = detector.getScaleFactor();

            queueEvent(() -> {
                if (mEngine == null) return;
                mEngine.getWindow().getCamera().dolly(scale);
            });

            requestRender();
            return true;
        }
    }

    private class PanListener extends PanGestureDetector.OnPanGestureListener {
        @Override
        public void onPan(PanGestureDetector detector) {
            if (!surfaceReady || mEngine == null) return;

            final float dx = detector.getDistanceX();
            final float dy = detector.getDistanceY();

            queueEvent(() -> {
                if (mEngine == null) return;

                Window window = mEngine.getWindow();
                Camera camera = window.getCamera();

                double[] pos = camera.getPosition();
                double[] focus = camera.getFocalPoint();
                double[] focusDC = window.getDisplayFromWorld(focus);

                double[] shiftDC = {focusDC[0] - dx, focusDC[1] + dy, focusDC[2]};
                double[] shift = window.getWorldFromDisplay(shiftDC);

                double[] motion = {shift[0] - focus[0], shift[1] - focus[1], shift[2] - focus[2]};

                camera.setFocalPoint(new double[]{
                        motion[0] + focus[0],
                        motion[1] + focus[1],
                        motion[2] + focus[2]
                });

                camera.setPosition(new double[]{
                        motion[0] + pos[0],
                        motion[1] + pos[1],
                        motion[2] + pos[2]
                });
            });

            requestRender();
        }
    }

    private class RotateListener extends RotateGestureDetector.OnRotateGestureListener {
        @Override
        public void onRotate(RotateGestureDetector detector) {
            if (!surfaceReady || mEngine == null) return;

            final float dx = detector.getDistanceX();
            final float dy = detector.getDistanceY();

            queueEvent(() -> {
                if (mEngine == null) return;

                Window window = mEngine.getWindow();
                Camera camera = window.getCamera();

                int w = Math.max(1, window.getWidth());
                int h = Math.max(1, window.getHeight());

                double delta_elevation = 200.0 / w;
                double delta_azimuth = -200.0 / h;

                camera.azimuth(dx * delta_azimuth);
                camera.elevation(dy * delta_elevation);
            });

            requestRender();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!surfaceReady || mEngine == null) return true;

        mTapDetector.onTouchEvent(event);
        mPanDetector.onTouchEvent(event);
        mScaleDetector.onTouchEvent(event);
        mRotateDetector.onTouchEvent(event);
        return true;
    }

    public interface OnLoadSceneListener {
        void onLoadScene(String path);

        void onLoadFailed(String path, Throwable t);
    }
}
