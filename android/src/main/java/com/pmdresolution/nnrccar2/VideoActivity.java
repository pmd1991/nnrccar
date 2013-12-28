package com.pmdresolution.nnrccar2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import java.io.IOException;
import java.util.List;

public class VideoActivity extends Activity {
    private FeatureStreamingCameraPreview mPreview;
    Camera mCamera;
    int numberOfCameras;
    int cameraCurrentlyLocked;

    // The first rear facing camera
    int defaultCameraId;

    private static final String PREFS_NAME = "com.pmdresolution.nnrccar2";

    private FeatureStreamer fs = new FeatureStreamer();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        mPreview = new FeatureStreamingCameraPreview(this, fs);
        setContentView(mPreview);
        final Context activityContext = (Context) this;

        Intent intent = getIntent();
        String addr = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);

        saveIPAddressPref(activityContext, addr);
        fs.connect(addr, 6666);
    }

    static String loadIPAddressPref(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        String prefix = prefs.getString("ipaddr", null);
        if (prefix != null) {
            return prefix;
        } else {
            return "";
        }
    }

    static void saveIPAddressPref(Context context, String text) {
        SharedPreferences.Editor prefs = context
                .getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString("ipaddr", text);
        prefs.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mCamera = Camera.open();
        //cameraCurrentlyLocked = defaultCameraId;
        mPreview.setCamera(mCamera);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mCamera != null) {
            mPreview.setCamera(null);
            mCamera.release();
            mCamera = null;
        }
    }
}

//
//---------------------------------------------------------------------------------
//

class FeatureStreamingCameraPreview extends ViewGroup implements SurfaceHolder.Callback,
        Camera.PreviewCallback {
    private final String TAG = "FSPreview";

    SurfaceView mSurfaceView;
    SurfaceHolder mHolder;
    Camera.Size mPreviewSize;
    List<Camera.Size> mSupportedPreviewSizes;
    Camera mCamera;

    private byte[] pixels = null;
    private float[] accelerometerFeatures = new float[3];
    private FeatureStreamer fs;

    FeatureStreamingCameraPreview(Context context, FeatureStreamer fs) {
        super(context);
        this.fs = fs;
        mSurfaceView = new SurfaceView(context);
        addView(mSurfaceView);

        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void setCamera(Camera camera) {
        mCamera = camera;
        if (mCamera != null) {
            mSupportedPreviewSizes = mCamera.getParameters()
                    .getSupportedPreviewSizes();
            requestLayout();
        }
    }

    public void switchCamera(Camera camera) {
        setCamera(camera);
        try {
            if (true)
                camera.setPreviewDisplay(mHolder);
        } catch (IOException exception) {
            Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
        }
        Camera.Parameters parameters = camera.getParameters();
        parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
        requestLayout();

        camera.setParameters(parameters);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(),
                heightMeasureSpec);

        if (mSupportedPreviewSizes != null) {
            mPreviewSize = getMinimumPreviewSize(mSupportedPreviewSizes, width,
                    height);
            setMeasuredDimension(mPreviewSize.width, mPreviewSize.height);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (changed && getChildCount() > 0) {
            final View child = getChildAt(0);

            final int width = r - l;
            final int height = b - t;

            int previewWidth = width;
            int previewHeight = height;
            if (mPreviewSize != null) {
                previewWidth = mPreviewSize.width;
                previewHeight = mPreviewSize.height;
            }

            // Center the child SurfaceView within the parent.
            if (width * previewHeight > height * previewWidth) {
                final int scaledChildWidth = previewWidth * height / previewHeight;
                child.layout((width - scaledChildWidth) / 2, 0,
                        (width + scaledChildWidth) / 2, height);
            } else {
                final int scaledChildHeight = previewHeight * width / previewWidth;
                child.layout(0, (height - scaledChildHeight) / 2, width,
                        (height + scaledChildHeight) / 2);
            }
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, acquire the camera and tell it where
        // to draw.
        try {
            if (mCamera != null) {
                if (true)
                    mCamera.setPreviewDisplay(holder);
                mCamera.setPreviewCallback(this);
            }
        } catch (IOException exception) {
            Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return, so stop the preview.
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }

    private Camera.Size getMinimumPreviewSize(List<Camera.Size> sizes, int w, int h) {
        if (sizes == null)
            return null;
        int minWidth = Integer.MAX_VALUE;

        Camera.Size optimalSize = null;
        // Try to find the min size
        for (Camera.Size size : sizes) {
            if (size.width < minWidth) {
                optimalSize = size;
                minWidth = size.width;
            }
        }

        return optimalSize;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);

        pixels = new byte[mPreviewSize.width * mPreviewSize.height];
        requestLayout();

        mCamera.setParameters(parameters);
        mCamera.startPreview();
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {

        if (data.length >= mPreviewSize.width * mPreviewSize.height) {
            decodeYUV420SPGrayscale(pixels, data, mPreviewSize.width,
                    mPreviewSize.height);
            synchronized (this) {
                fs.sendFeatures(mPreviewSize.width, mPreviewSize.height, pixels, accelerometerFeatures);
            }
        }

    }

    static public void decodeYUV420SPGrayscale(byte[] rgb, byte[] yuv420sp,
                                               int width, int height) {
        final int frameSize = width * height;

        for (int pix = 0; pix < frameSize; pix++) {
            int pixVal = (0xff & ((int) yuv420sp[pix])) - 16;
            if (pixVal < 0)
                pixVal = 0;
            if (pixVal > 255)
                pixVal = 255;
            rgb[pix] = (byte) pixVal;
        }
    }


}
