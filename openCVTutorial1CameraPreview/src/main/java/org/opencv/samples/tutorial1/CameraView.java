package org.opencv.samples.tutorial1;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Point;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Svyd on 13.04.2016.
 */

public class CameraView extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = "CameraView";

    private static final String CAMERA_THREAD_NAME = CameraView.class.getSimpleName() + " THREAD";

    private HandlerThread mCameraThread;
    private Handler mCameraHandler;
    private Handler mUIHandler;

    private Camera mCamera;
    private List<Camera.Size> mPreviewSizes = new ArrayList<>();
    private Camera.Size mCurrentPreviewSize;

    private CameraListener mListener;
    private PreviewHandler mPreviewListener;

    private boolean mIsFocusing;
    private boolean mIsRunning;

    private long mLastUpdate;
    private long mSleepTime = 200;

    private Matrix mMatrix;

    private final int FOCUS_AREA_SIZE = 50;

    private BaseLoaderCallback mLoaderCallback;

    public CameraView(Context context) {
        this(context, null);
    }

    public CameraView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mMatrix = new Matrix();
        initLoaderCallback(context);
    }

    private void initLoaderCallback(Context _context) {
        mLoaderCallback = new BaseLoaderCallback(_context) {
            @Override
            public void onManagerConnected(int status) {
                switch (status) {
                    case LoaderCallbackInterface.SUCCESS: {
                        startPreview();
                        Log.i(TAG, "OpenCV loaded successfully");
                        break;
                    }
                    default: {
                        super.onManagerConnected(status);
                    }
                }
            }
        };
    }

    public boolean isRunning() {
        return  mIsRunning;
    }

    public void setPreviewHandler(PreviewHandler _listener) {
        mPreviewListener = _listener;
    }

    private void initHandlers() {
        mCameraThread = new HandlerThread(CAMERA_THREAD_NAME);
        mCameraThread.start();

        mCameraHandler = new Handler(mCameraThread.getLooper());
        mUIHandler = new Handler();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getHolder().addCallback(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        getHolder().removeCallback(this);
        super.onDetachedFromWindow();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        initHandlers();
        mCameraHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mCamera = Camera.open();
                    mPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
                    Log.d(TAG, "onSurfaceCreated");
                } catch (Exception _e) {
                    surfaceDestroy();
                }
            }
        });
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, final int format, final int width, final int height) {
        mCameraHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mCamera == null)
                        return;

                    Log.d(TAG, "surfaceChanged: " + mCurrentPreviewSize);
                    mCurrentPreviewSize = getOptimalPreviewSize(mCamera.getParameters().getSupportedPreviewSizes(), width, height);
                    initMatrix(width, height);
                    setSortedPreviewSize();
                    setPreviewHandler();
                    checkFlash();
                    mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
                } catch (Exception _e) {
                    releaseCamera();
                    releaseHandlers();
                }
            }
        });
    }

    private void initMatrix(int _width, int _height) {
        Matrix matrix = new Matrix();
        matrix.postScale(_width / 2000f, _width / 2000f);
        matrix.postTranslate(_width / 2f, _width / 2f);
        matrix.invert(mMatrix);
    }


    private void checkFlash() {
        final Camera.Parameters parameters = mCamera.getParameters();
        if (mListener != null)
            mUIHandler.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onFlashAvailable(parameters.getFlashMode() != null);
                }
            });
    }

    private void setSortedPreviewSize() {
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewSize(mCurrentPreviewSize.width, mCurrentPreviewSize.height);
        parameters.setPictureSize(mCurrentPreviewSize.width, mCurrentPreviewSize.height);
        mCamera.setParameters(parameters);
    }

    public Point getPictureSize() {
        return new Point(mCurrentPreviewSize.width, mCurrentPreviewSize.height);
    }

    public void startPreview() {
        try {
            mIsRunning = true;
            mCamera.setPreviewDisplay(getHolder());
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
            releaseCamera();
            Log.d(TAG, "startPreview: ", e);
        }
    }

    private void setPreviewHandler() {
        mCamera.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                if (mPreviewListener != null)
                    mPreviewListener.onPreviewData(
                            data,
                            new Point(mCurrentPreviewSize.width, mCurrentPreviewSize.height),
                            camera.getParameters().getPreviewFormat());
            }
        });
    }

    private void releaseHandlers() {
        mCameraThread.quit();
        mCameraHandler = null;
        mCameraThread = null;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mCameraHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "surfaceDestroyed: ");
               surfaceDestroy();
            }
        });
    }

    private void surfaceDestroy() {
        mIsRunning = false;
        if (mCamera == null)
            return;

        mCamera.setPreviewCallback(null);
        releaseCamera();
        releaseHandlers();
    }

    private void releaseCamera() {
        Log.d(TAG, "releaseCamera");
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    public void setFlashMode(String _flashMode) {
        if (mCamera == null)
            return;

        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setFlashMode(_flashMode);
        mCamera.setParameters(parameters);
    }

    public void takePhoto() {
        mCamera.takePicture(new Camera.ShutterCallback() {
            @Override
            public void onShutter() {

            }
        }, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
//                if (mListener != null)
//                    mListener.onPictureTaken(data, camera.getParameters().getPictureFormat());
            }
        }, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                mIsRunning = false;
                if (mListener != null)
                    mListener.onPictureTaken(data, camera.getParameters().getPictureFormat());
            }
        });
    }

    public void setCameraSizeListener(CameraListener _listener) {
        mListener = _listener;
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio=(double)h / w;

        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        focusOnTouch(event);
        return true;
    }

    protected void focusOnTouch(MotionEvent event) {
        if (mCamera != null && !mIsFocusing) {
            mIsFocusing = true;
            mCamera.cancelAutoFocus();
            Rect focusRect = calculateTapArea(event.getX(), event.getY(), 1f);
            Rect meteringRect = calculateTapArea(event.getX(), event.getY(), 1.5f);

            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            List<Camera.Area> areas = new ArrayList<>();
            areas.add(new Camera.Area(focusRect, 1000));
            parameters.setFocusAreas(areas);

            if (meteringAreaSupported()) {
                parameters.setMeteringAreas(areas);
            }

            mCamera.setParameters(parameters);
            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    mIsFocusing = false;
                }
            });
        }
    }

    private Rect calculateTapArea(float x, float y, float coefficient) {
        int areaSize = Float.valueOf(getPixel(FOCUS_AREA_SIZE) * coefficient).intValue();

        int left = clamp((int) x - areaSize / 2, 0, getWidth() - areaSize);
        int top = clamp((int) y - areaSize / 2, 0, getHeight() - areaSize);

        RectF rectF = new RectF(left, top, left + areaSize, top + areaSize);
        mMatrix.mapRect(rectF);

        return new Rect(Math.round(rectF.left), Math.round(rectF.top), Math.round(rectF.right), Math.round(rectF.bottom));
    }

    private float getPixel(float _dimen) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                _dimen, getResources().getDisplayMetrics());
    }

    private int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }

    private boolean meteringAreaSupported() {
        return mCamera.getParameters().getMaxNumMeteringAreas() > 0;
    }

    public interface CameraListener {
        void onFlashAvailable(boolean _available);
        void onPictureTaken(byte[] data, int _format);
    }

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.d("ERROR", "Unable to load OpenCV");
        } else {
            Log.d("SUCCESS", "OpenCV loaded");
        }
    }
}
