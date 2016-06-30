package org.opencv.samples.tutorial1;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by richimozes on 6/6/16.
 */
public class ContourDetector {

    public ContourDetector() {
    }

    public Bitmap getPerspective(Bitmap _image, List<Point> _points) {
        if (_points == null || _points.size() != 4)
            return _image;

        Map<Integer, Point> pointMap = getOrderedPoints(_points);

        Point topLeft = pointMap.get(0);
        Point topRight = pointMap.get(1);
        Point bottomLeft = pointMap.get(2);
        Point bottomRight = pointMap.get(3);

        int resultWidth = (int)(topRight.x - topLeft.x);
        int bottomWidth = (int)(bottomRight.x - bottomLeft.x);
        if(bottomWidth > resultWidth)
            resultWidth = bottomWidth;

        int resultHeight = (int)(bottomLeft.y - topLeft.y);
        int bottomHeight = (int)(bottomRight.y - topRight.y);
        if(bottomHeight > resultHeight)
            resultHeight = bottomHeight;

        Mat inputMat = new Mat(_image.getHeight(), _image.getHeight(), CvType.CV_8UC1);
        Utils.bitmapToMat(_image, inputMat);
        Mat outputMat = new Mat(resultWidth, resultHeight, CvType.CV_8UC1);

        List<Point> source = new ArrayList<>();
        source.add(topLeft);
        source.add(topRight);
        source.add(bottomLeft);
        source.add(bottomRight);
        Mat startM = Converters.vector_Point2f_to_Mat(source);

        Point ocvPOut1 = new Point(0, 0);
        Point ocvPOut2 = new Point(resultWidth, 0);
        Point ocvPOut3 = new Point(0, resultHeight);
        Point ocvPOut4 = new Point(resultWidth, resultHeight);
        List<Point> dest = new ArrayList<>();
        dest.add(ocvPOut1);
        dest.add(ocvPOut2);
        dest.add(ocvPOut3);
        dest.add(ocvPOut4);
        Mat endM = Converters.vector_Point2f_to_Mat(dest);

        Mat perspectiveTransform = Imgproc.getPerspectiveTransform(startM, endM);

        Imgproc.warpPerspective(inputMat, outputMat, perspectiveTransform, new Size(resultWidth, resultHeight));

        Bitmap output = Bitmap.createBitmap(resultWidth, resultHeight, Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(outputMat, output);

        int i = 7;

        return output;

    }

    private Map<Integer, Point> getOrderedPoints(List<Point> points) {

        Point centerPoint = new Point();
        int size = points.size();
        for (Point pointF : points) {
            centerPoint.x += pointF.x / size;
            centerPoint.y += pointF.y / size;
        }

        Map<Integer, Point> orderedPoints = new HashMap<>();
        for (Point pointF : points) {
            int index = -1;
            if (pointF.x < centerPoint.x && pointF.y < centerPoint.y) {
                index = 0;
            } else if (pointF.x > centerPoint.x && pointF.y < centerPoint.y) {
                index = 1;
            } else if (pointF.x < centerPoint.x && pointF.y > centerPoint.y) {
                index = 2;
            } else if (pointF.x > centerPoint.x && pointF.y > centerPoint.y) {
                index = 3;
            }
            orderedPoints.put(index, pointF);
        }
        return orderedPoints;
    }

    public List<Point> findLargestRectangle(Bitmap _bitmap) {
        List<Point> points = new ArrayList<>();

//        Bitmap bitmap = Bitmap.createScaledBitmap(_bitmap, 640, 480, false);

        Mat imgSource = new Mat();
        Utils.bitmapToMat(_bitmap, imgSource);
        if (imgSource.empty())
            return points;

        List<MatOfPoint> contours = new ArrayList<>();
        findContours(imgSource, contours);

        MatOfPoint2f maxCurve = findLargestContour(contours);
        if (maxCurve == null || maxCurve.empty())
            return points;

        points = convertMatOfPointToPoints(maxCurve);
        Log.d("LargestRectangle", "findLargestRectangle: " + points);

        return points;
    }

    public List<Point> findLargestRectangleWithLocalTransform(byte[] _data, Point _previewSize, Point _screenSize, int _format) {
        List<Point> points = new ArrayList<>();

        Mat imgSource = convertBytesToMat(_data, _previewSize, _format);
        if (imgSource == null)
            return points;

        List<MatOfPoint> contours = new ArrayList<>();
        findContours(imgSource, contours);

        MatOfPoint2f maxCurve = findLargestContour(contours);
        if (maxCurve == null)
            return points;

        points = convertMatOfPointToPointsWithTransform(maxCurve, new Point(imgSource.cols(), imgSource.rows()), _screenSize);

        return points;
    }

    public Mat convertBytesToMat(byte[] _data, Point _previewSize, int _previewFormat) {
        if (_previewFormat == ImageFormat.NV21 || _previewFormat == ImageFormat.YUY2) {
            int width = (int) _previewSize.x;
            int height = (int) _previewSize.y;

            YuvImage yuv = new YuvImage(_data, _previewFormat, width, height, null);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuv.compressToJpeg(new Rect(0, 0, width, height), 100, out);
            _data = out.toByteArray();
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        double size = (_previewSize.x / 640);
        if (size < 1)
            size = 1;

        options.inSampleSize = (int) size;
        final Bitmap bitmap = BitmapFactory.decodeByteArray(_data, 0, _data.length, options);
        if (bitmap == null)
            return null;

        Mat imgSource = new Mat();
        Utils.bitmapToMat(bitmap, imgSource);
        bitmap.recycle();

        return imgSource;
    }

    private void findContours(Mat _imgSource, List<MatOfPoint> _contours) {
        Imgproc.cvtColor(_imgSource, _imgSource, Imgproc.COLOR_BGR2GRAY);
        Imgproc.Canny(_imgSource, _imgSource, 160, 200, 3, true);
        Imgproc.GaussianBlur(_imgSource, _imgSource, new Size(5, 5), 4);
        Imgproc.findContours(_imgSource, _contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);
    }

    private MatOfPoint2f findLargestContour(List<MatOfPoint> _contours) {
        if (_contours.isEmpty())
            return null;

        double maxArea = -1;
        MatOfPoint temp_contour;
        MatOfPoint2f approxCurve = new MatOfPoint2f();
        MatOfPoint2f maxCurve = new MatOfPoint2f();
        for (int idx = 0; idx < _contours.size(); idx++) {
            temp_contour = _contours.get(idx);
            double contourArea = Imgproc.contourArea(temp_contour);
            if (contourArea > maxArea && contourArea > 50000 && !Imgproc.isContourConvex(temp_contour)) {
                MatOfPoint2f new_mat = new MatOfPoint2f(temp_contour.toArray());
                int contourSize = (int) temp_contour.total();
                Imgproc.approxPolyDP(new_mat, approxCurve, contourSize * 0.05, true);
                if (approxCurve.total() == 4) {
                    maxArea = contourArea;
                    maxCurve = approxCurve;
                }
            }
        }

        return maxCurve;
    }

    private List<Point> convertMatOfPointToPoints(MatOfPoint2f _curve) {
        List<Point> points = new ArrayList<>();

        if (_curve.empty())
            return points;

        double[] temDouble = _curve.get(0, 0);
        if (temDouble == null)
            return points;
        Point point1 = new Point(temDouble[0], temDouble[1]);

        temDouble = _curve.get(1, 0);
        if (temDouble == null)
            return points;
        Point point2 = new Point(temDouble[0], temDouble[1]);

        temDouble = _curve.get(2, 0);
        if (temDouble == null)
            return points;
        Point point3 = new Point(temDouble[0], temDouble[1]);

        temDouble = _curve.get(3, 0);
        if (temDouble == null)
            return points;
        Point point4 = new Point(temDouble[0], temDouble[1]);

        points.add(point1);
        points.add(point2);
        points.add(point3);
        points.add(point4);

        return points;
    }

    private List<Point> convertMatOfPointToPointsWithTransform(MatOfPoint2f _curve, Point _previewSize, Point _screenSize) {
        List<Point> points = new ArrayList<>();

        if (_curve.empty())
            return points;

        double[] temDouble = _curve.get(0, 0);
        if (temDouble == null)
            return points;
        Point point1 = transformCoordinate(new Point(temDouble[0], temDouble[1]), _previewSize, _screenSize);

        temDouble = _curve.get(1, 0);
        if (temDouble == null)
            return points;
        Point point2 = transformCoordinate(new Point(temDouble[0], temDouble[1]), _previewSize, _screenSize);

        temDouble = _curve.get(2, 0);
        if (temDouble == null)
            return points;
        Point point3 = transformCoordinate(new Point(temDouble[0], temDouble[1]), _previewSize, _screenSize);

        temDouble = _curve.get(3, 0);
        if (temDouble == null)
            return points;
        Point point4 = transformCoordinate(new Point(temDouble[0], temDouble[1]), _previewSize, _screenSize);

        points.add(point1);
        points.add(point2);
        points.add(point3);
        points.add(point4);

        return points;
    }

    public Point transformCoordinate(Point _point, Point _previewSize, Point _screenSize) {
        Point point = new Point();
        point.x = (_point.x * _screenSize.x) / _previewSize.x;
        point.y = (_point.y * _screenSize.y) / _previewSize.y;

        return point;
    }
}
