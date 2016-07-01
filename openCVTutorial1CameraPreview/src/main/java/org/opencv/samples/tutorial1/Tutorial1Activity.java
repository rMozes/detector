//package org.opencv.samples.tutorial1;
//
//import org.opencv.android.BaseLoaderCallback;
//import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
//import org.opencv.android.LoaderCallbackInterface;
//import org.opencv.android.OpenCVLoader;
//import org.opencv.core.Mat;
//import org.opencv.android.CameraBridgeViewBase;
//import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
//import org.opencv.core.MatOfPoint;
//import org.opencv.core.MatOfPoint2f;
//import org.opencv.core.Point;
//import org.opencv.core.Scalar;
//import org.opencv.core.Size;
//import org.opencv.imgproc.Imgproc;
//
//import android.app.Activity;
//import android.os.Bundle;
//import android.util.Log;
//import android.view.Menu;
//import android.view.MenuItem;
//import android.view.SurfaceView;
//import android.view.WindowManager;
//import android.widget.Toast;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class Tutorial1Activity extends Activity implements CvCameraViewListener2 {
//    private static final String TAG = "OCVSample::Activity";
//
//    private CameraBridgeViewBase mOpenCvCameraView;
//    private boolean              mIsJavaCamera = true;
//    private MenuItem             mItemSwitchCamera = null;
//
//    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
//        @Override
//        public void onManagerConnected(int status) {
//            switch (status) {
//                case LoaderCallbackInterface.SUCCESS:
//                {
//                    Log.i(TAG, "OpenCV loaded successfully");
//                    mOpenCvCameraView.enableView();
//                } break;
//                default:
//                {
//                    super.onManagerConnected(status);
//                } break;
//            }
//        }
//    };
//
//    public Tutorial1Activity() {
//        Log.i(TAG, "Instantiated new " + this.getClass());
//    }
//
//    /** Called when the activity is first created. */
//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        Log.i(TAG, "called onCreate");
//        super.onCreate(savedInstanceState);
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//
//        setContentView(R.layout.tutorial1_surface_view);
//
//        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_java_surface_view);
//
//        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
//
//        mOpenCvCameraView.setCvCameraViewListener(this);
//    }
//
//    @Override
//    public void onPause()
//    {
//        super.onPause();
//        if (mOpenCvCameraView != null)
//            mOpenCvCameraView.disableView();
//    }
//
//    @Override
//    public void onResume()
//    {
//        super.onResume();
//        if (!OpenCVLoader.initDebug()) {
//            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
//            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
//        } else {
//            Log.d(TAG, "OpenCV library found inside package. Using it!");
//            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
//        }
//    }
//
//    public void onDestroy() {
//        super.onDestroy();
//        if (mOpenCvCameraView != null)
//            mOpenCvCameraView.disableView();
//    }
//
//    public void onCameraViewStarted(int width, int height) {
//    }
//
//    public void onCameraViewStopped() {
//    }
//
//    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
//        return findLargestRectangleWithLocalTransform(inputFrame.rgba());
//    }
//
//    private static Mat findLargestRectangleWithLocalTransform(Mat original_image) {
//        Mat imgSource = original_image.clone();
//
//        Imgproc.cvtColor(imgSource, imgSource, Imgproc.COLOR_BGR2GRAY);
//
//        //convert the image to black and white does (8 bit)
//        Imgproc.Canny(imgSource, imgSource, 80, 100);
//        Imgproc.GaussianBlur(imgSource, imgSource, new Size(5, 5), 0);
//
////        Imgproc.adaptiveThreshold(imgSource, imgSource, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 3, 1);
//
//
//        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
//
//        Imgproc.findContours(imgSource, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);
//
//        double maxArea = -1;
//        int maxAreaIdx = -1;
//        if (contours.isEmpty())
//            return original_image;
//
//        Log.d(TAG, "not empty");
//        MatOfPoint temp_contour = contours.get(0);
//        MatOfPoint2f approxCurve = new MatOfPoint2f();
//        MatOfPoint2f maxCurve = new MatOfPoint2f();
//        Mat largest_contour = contours.get(0);
//        List<MatOfPoint> largest_contours = new ArrayList<MatOfPoint>();
//        for (int idx = 0; idx < contours.size(); idx++) {
//            temp_contour = contours.get(idx);
//            double contourarea = Imgproc.contourArea(temp_contour);
//            if (contourarea > maxArea && contourarea > 10000) {
//                MatOfPoint2f new_mat = new MatOfPoint2f( temp_contour.toArray() );
//                int contourSize = (int)temp_contour.total();
//                Imgproc.approxPolyDP(new_mat, approxCurve, contourSize * 0.05, true);
//                if (approxCurve.total() == 4) {
//                    maxArea = contourarea;
//                    maxCurve = approxCurve;
//                    maxAreaIdx = idx;
//                    Log.d(TAG, "findLargestRect: " + idx);
//                    largest_contours.add(temp_contour);
//                    largest_contour = temp_contour;
//                }
//            }
//        }
//        if (largest_contours.isEmpty())
//            return original_image;
//        MatOfPoint temp_largest = largest_contours.get(largest_contours.size()-1);
//        largest_contours = new ArrayList<MatOfPoint>();
//        largest_contours.add(temp_largest);
//
//        Imgproc.cvtColor(imgSource, imgSource, Imgproc.COLOR_BayerBG2RGB);
//        Imgproc.drawContours(imgSource, largest_contours, -1, new Scalar(0, 255, 0), 1);
//
////        return imgSource;
//        double[] temDouble = maxCurve.get(0, 0);
//        Point point1 = new Point(temDouble[0], temDouble[1]); //RGB
//        Imgproc.circle(original_image, point1, 20, new Scalar(255, 0, 0), 5);
//
//        temDouble = maxCurve.get(1, 0);
//        Point point2 = new Point(temDouble[0], temDouble[1]);
//        Imgproc.circle(original_image, point2, 20, new Scalar(0, 255, 0), 5);
//
//        temDouble = maxCurve.get(2, 0);
//        if (temDouble == null)
//            return original_image;
//        Point point3 = new Point(temDouble[0], temDouble[1]);
//        Imgproc.circle(original_image, point3, 20, new Scalar(0, 0, 255), 5);
//
//        temDouble = maxCurve.get(3, 0);
//        if (temDouble == null)
//            return original_image;
//        Point point4 = new Point(temDouble[0], temDouble[1]);
//        Imgproc.circle(original_image, point4, 20, new Scalar(255, 255, 255), 5);
//
//        return original_image;
//    }
//}


package org.opencv.samples.tutorial1;

import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class Tutorial1Activity extends Activity implements CameraView.CameraListener {
    private static final String TAG = "OCVSample::Activity";

    private ContourDetector mDetector;
    private CameraView cvCameraView;
    private OverlayView ovOverlay;
    private Button btnTakePhoto;
    private LinearLayout llImageContainer;
    private ImageView ivImage;
    private  Bitmap mBitmap;
    private Button btnResize;

    public Tutorial1Activity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.tutorial1_surface_view);

        mDetector = new ContourDetector();

        cvCameraView = ((CameraView) findViewById(R.id.cvCameraView));
        cvCameraView.setCameraSizeListener(this);

        ovOverlay = ((OverlayView) findViewById(R.id.ovOverlay));

        cvCameraView.setPreviewHandler(ovOverlay);

        btnTakePhoto = ((Button) findViewById(R.id.btnTakePhoto));

        llImageContainer = ((LinearLayout) findViewById(R.id.llImageContainer));
        ivImage = ((ImageView) findViewById(R.id.ivImage));

        btnTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cvCameraView.takePhoto();
            }
        });
//        btnResize.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                try {
//                    String path = getPath();
//                    Intent intent = ResizeActivity.newIntent(path, getApplicationContext());
//                    startActivity(intent);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        });
    }

    private String getPath() throws Exception {
        OutputStream outputStream = null;
        try {
            File cache = getApplicationContext().getCacheDir();
            File bitmapPath = new File(cache.getPath(), "Test.png");
            outputStream = new FileOutputStream(bitmapPath);
            mBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);

            return bitmapPath.getPath();
        } catch (Exception _e) {
            return "";
        } finally {
            if (outputStream != null)
                outputStream.close();
        }
    }

    @Override
    public void onFlashAvailable(boolean _available) {

    }

    @Override
    public void onPictureTaken(byte[] data, int _format) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        Bitmap test = BitmapFactory.decodeByteArray(data, 0, data.length);

        options = new BitmapFactory.Options();
        double size = (test.getWidth() / 640);
        if (size < 1)
            size = 1;

        options.inSampleSize = (int) size;
        final Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
        if (bitmap == null)
            return;

        Mat imgSource = new Mat();
        Utils.bitmapToMat(bitmap, imgSource);

        List<Point> points = mDetector.findLargestRectangle(bitmap);

        mBitmap = mDetector.getPerspective(bitmap, points);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                llImageContainer.setVisibility(View.VISIBLE);
                ivImage.setImageBitmap(mBitmap);
            }
        });
    }

    @Override
    public void onBackPressed() {
        boolean isRunning = llImageContainer.getVisibility() == View.GONE;
        if (isRunning) {
            super.onBackPressed();
        } else {
            llImageContainer.setVisibility(View.GONE);
            cvCameraView.startPreview();
        }
    }
}
