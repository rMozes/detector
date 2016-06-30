package org.opencv.samples.tutorial1;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.scanlibrary.PolygonView;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by richimozes on 6/10/16.
 */
public class ResizeActivity extends Activity {

    public static final String EXTRA_PATH = "extra_path";

    public static Intent newIntent(String _path, Context _context) {
        Intent intent = new Intent(_context, ResizeActivity.class);
        intent.putExtra(EXTRA_PATH, _path);

        return intent;
    }

    private PolygonView pvPolygon;
    private ImageView ivImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resize);

        findUI();
        ivImage.post(new Runnable() {
            @Override
            public void run() {
                setUpUI();
            }
        });
    }

    private void findUI() {
        pvPolygon = ((PolygonView) findViewById(R.id.pvPolygon));
        ivImage = ((ImageView) findViewById(R.id.ivImage));
    }

    private void setUpUI() {
        Bitmap bitmap = scaledBitmap(getBitmap(), ivImage.getWidth(), ivImage.getHeight());
        if (bitmap == null)
            return;

        ivImage.setImageBitmap(bitmap);
        pvPolygon.setPoints(getOutlinePoints(bitmap));
        int padding = (int) getResources().getDimension(R.dimen.scanPadding);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(bitmap.getWidth() + 2 * padding, bitmap.getHeight() + 2 * padding);
        layoutParams.gravity = Gravity.CENTER;
        pvPolygon.setLayoutParams(layoutParams);
    }

    private Bitmap scaledBitmap(Bitmap bitmap, int width, int height) {
        Matrix m = new Matrix();
        m.setRectToRect(new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight()), new RectF(0, 0, width, height), Matrix.ScaleToFit.CENTER);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
    }

    private Bitmap getBitmap() {
        String path = getPath();
        return BitmapFactory.decodeFile(path);
    }

    private String getPath() {
        return getIntent().getStringExtra(EXTRA_PATH);
    }

    private Map<Integer, PointF> getOutlinePoints(Bitmap tempBitmap) {
        Map<Integer, PointF> outlinePoints = new HashMap<>();
        outlinePoints.put(0, new PointF(0, 0));
        outlinePoints.put(1, new PointF(tempBitmap.getWidth(), 0));
        outlinePoints.put(2, new PointF(0, tempBitmap.getHeight()));
        outlinePoints.put(3, new PointF(tempBitmap.getWidth(), tempBitmap.getHeight()));
        return outlinePoints;
    }
}
