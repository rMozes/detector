package org.opencv.samples.tutorial1;

import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by richimozes on 6/2/16.
 */
public class OverlayView extends View implements PreviewHandler {

    private static final String TAG = "OverlayView";

    private Path mRectanglePath;
    private Paint mRectanglePaint;

    private Handler mHandler;
    private boolean mIsVisible = true;
    private ValueAnimator mPointAnimator;
    private Point mPreviewSize;

    private List<Point> mRectangleCoordinate;

    private ContourDetector mDetector;
    private long mLastUpdate;
    private long mSleepTime = 300;

    public OverlayView(Context context) {
        this(context, null);
    }

    public OverlayView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mHandler = new Handler();
        mDetector = new ContourDetector();
        initRectangleProperties();
    }

    private void initRectangleProperties() {
        mRectanglePath = new Path();

        mRectanglePaint = new Paint();
        mRectanglePaint.setAntiAlias(true);
        mRectanglePaint.setStyle(Paint.Style.STROKE);
        mRectanglePaint.setStrokeWidth(convertDpToPixel(4));
        mRectanglePaint.setColor(Color.argb(160, 0, 245, 0));
    }

    private float convertDpToPixel(int _dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, _dp, getResources().getDisplayMetrics());
    }

    @Override
    public void onPreviewData(byte[] _data, Point _size, int _format) {
        if (!isNeedToUpdate())
            return;

        final List<Point> coordinate = orderByPosition(
                mDetector.findLargestRectangleWithLocalTransform(_data, _size, mPreviewSize, _format));
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                setCoordinates(coordinate);
            }
        });
    }

    private boolean isNeedToUpdate() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - mLastUpdate < mSleepTime) {
            return false;
        } else {
            mLastUpdate = currentTime;
            return true;
        }
    }

    private List<Point> orderByPosition(List<Point> points) {
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
                index = 3;
            } else if (pointF.x < centerPoint.x && pointF.y > centerPoint.y) {
                index = 1;
            } else if (pointF.x > centerPoint.x && pointF.y > centerPoint.y) {
                index = 2;
            }
            orderedPoints.put(index, pointF);
        }

        List<Point> pointList = new ArrayList<>();
        for (int i = 0; i < orderedPoints.size(); i++) {
            pointList.add(orderedPoints.get(i));
        }

        return pointList;
    }

    public void setCoordinates(List<Point> _coordinates) {
        if (mRectangleCoordinate == null || mRectangleCoordinate.isEmpty())
            mRectangleCoordinate = _coordinates;

        if (_coordinates == null || _coordinates.isEmpty()) {
            mHandler.postDelayed(mVisible, 1000);
        } else {
            mIsVisible = true;
            mHandler.removeCallbacks(mVisible);
            startAnimation(_coordinates);
        }
    }

    public List<Point> getCoordinates() {
        return mRectangleCoordinate;
    }

    private void startAnimation(List<Point> _coordinate) {
        if (mPointAnimator == null)
            initAnimator(_coordinate);
        else
            updateAnimator(_coordinate);
    }

    private void initAnimator(List<Point> _coordinate) {
        mPointAnimator = ValueAnimator.ofObject(new PointsTypeEvaluator(),
                new ArrayList<>(mRectangleCoordinate), _coordinate);
        mPointAnimator.setDuration(160);
        mPointAnimator.setInterpolator(new LinearInterpolator());
        mPointAnimator.addUpdateListener(mUpdateListener);
        mPointAnimator.start();
    }

    private void updateAnimator(List<Point> _coordinate) {
        if (mPointAnimator.isRunning() || _coordinate.size() != 4)
            return;

        initAnimator(_coordinate);
    }

    private ValueAnimator.AnimatorUpdateListener mUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            mRectangleCoordinate = ((List<Point>) animation.getAnimatedValue());
            invalidate();
        }
    };

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        mRectanglePath.reset();
        drawOverlay(canvas);
    }

    private void drawOverlay(Canvas _canvas) {
        if (!mIsVisible) {
            drawPath(_canvas);
            return;
        }

        if (mRectangleCoordinate == null || mRectangleCoordinate.isEmpty())
            return;


        Point firstPoint = mRectangleCoordinate.get(0);
        mRectanglePath.moveTo(((float) firstPoint.x), ((float) firstPoint.y));

        for (int i = 1; i < mRectangleCoordinate.size(); i++) {
            lineTo(mRectangleCoordinate.get(i));
        }
        lineTo(firstPoint);
        drawPath(_canvas);
    }

    private Runnable mVisible = new Runnable() {
        @Override
        public void run() {
            mIsVisible = false;
            invalidate();
        }
    };

    private void lineTo(Point _point) {
        mRectanglePath.lineTo(((float) _point.x), ((float) _point.y));
    }

    private void drawPath(Canvas _canvas) {
        _canvas.drawPath(mRectanglePath, mRectanglePaint);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mHandler.removeCallbacks(mVisible);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mPreviewSize = new Point(w, h);
    }

    private static final class PointsTypeEvaluator implements TypeEvaluator<List<Point>> {
        @Override
        public List<Point> evaluate(float fraction, List<Point> startValue, List<Point> endValue) {
            List<Point> progressValue = new ArrayList<>();

            for (int i = 0; i < startValue.size(); i++) {
                if (startValue.get(i) == null || endValue.get(i) == null) {
                    return progressValue;
                }

                progressValue.add(transformPoint(fraction, startValue.get(i), endValue.get(i)));
            }

            return progressValue;
        }

        private Point transformPoint(float _fraction, Point _startValue, Point _endValue) {
            double dX = (_endValue.x - _startValue.x) * _fraction;
            double dY = (_endValue.y - _startValue.y) * _fraction;

            double newX = _startValue.x + dX;
            double newY = _startValue.y + dY;

            return new Point(newX, newY);
        }
    }
}
