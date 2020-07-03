package org.opencv.customlib;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.google.android.material.button.MaterialButton;

import org.opencv.R;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opencv.customlib.helpers.MyConstants;
import org.opencv.customlib.libraries.NativeClass;
import org.opencv.customlib.libraries.PolygonView;
import org.opencv.databinding.ActivityImageCropBinding;

public class ImageCropActivity extends AppCompatActivity {

    FrameLayout holderImageCrop;
    ImageView imageView;
    PolygonView polygonView;
    Bitmap selectedImageBitmap;
    MaterialButton btnImageEnhance;
    ActivityImageCropBinding binding;
    NativeClass nativeClass;
    boolean rotateLeft = true;
    boolean rotateRight = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_image_crop);
        initializeElement();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        /*if (item.getItemId() == R.id.menu_rotate_left) {
            if (rotateLeft) {
                rotateLeft = false;
                rotateImage(90f);
            } else {
                rotateLeft = true;
                rotateImage(180f);
            }
        }
        if (item.getItemId() == R.id.menu_rotate_right) {
            if (rotateRight) {
                rotateRight = false;
                rotateImage(270f);
            } else {
                rotateRight = true;
                rotateImage(0f);
            }
        }*/
        return super.onOptionsItemSelected(item);
    }

    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.crop_image_menu, menu);
        return true;
    }*/

    /**
     * Rotate the image in the crop image view.
     */
    protected void rotateImage(float degrees) {
        imageView.setRotation(degrees);
    }

    private void initializeElement() {
        nativeClass = new NativeClass();
        btnImageEnhance = binding.btnImageEnhance;
        holderImageCrop = binding.holderImageCrop;
        imageView = binding.imageView;
        polygonView = binding.polygonView;

        holderImageCrop.post(new Runnable() {
            @Override
            public void run() {
                initializeCropping();
            }
        });
        btnImageEnhance.setOnClickListener(btnImageEnhanceClick);

    }

    private void initializeCropping() {

        selectedImageBitmap = MyConstants.selectedImageBitmap;
        MyConstants.selectedImageBitmap = null;

        Bitmap scaledBitmap = scaledBitmap(selectedImageBitmap, holderImageCrop.getWidth(), holderImageCrop.getHeight());
        imageView.setImageBitmap(scaledBitmap);

        Bitmap tempBitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
        Map<Integer, PointF> pointFs = getEdgePoints(tempBitmap);

        polygonView.setPoints(pointFs);
        polygonView.setVisibility(View.VISIBLE);

        int padding = (int) getResources().getDimension(R.dimen.scanPadding);

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(tempBitmap.getWidth() + 2 * padding, tempBitmap.getHeight() + 2 * padding);
        layoutParams.gravity = Gravity.CENTER;

        polygonView.setLayoutParams(layoutParams);

    }

    private View.OnClickListener btnImageEnhanceClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            //save selected bitmap to our constants
            //this method will save the image to our device memory
            //so set this variable to null after the image is no longer used
            MyConstants.selectedImageBitmap = getCroppedImage();

           /* //create new intent to start process image
            Intent intent = new Intent(getApplicationContext(), ImageEnhanceActivity.class);
            startActivity(intent);*/
            finishAffinity();

        }
    };

    protected Bitmap getCroppedImage() {

        Map<Integer, PointF> points = polygonView.getPoints();

        float xRatio = (float) selectedImageBitmap.getWidth() / imageView.getWidth();
        float yRatio = (float) selectedImageBitmap.getHeight() / imageView.getHeight();

        float x1 = (points.get(0).x) * xRatio;
        float x2 = (points.get(1).x) * xRatio;
        float x3 = (points.get(2).x) * xRatio;
        float x4 = (points.get(3).x) * xRatio;
        float y1 = (points.get(0).y) * yRatio;
        float y2 = (points.get(1).y) * yRatio;
        float y3 = (points.get(2).y) * yRatio;
        float y4 = (points.get(3).y) * yRatio;

        return nativeClass.getScannedBitmap(selectedImageBitmap, x1, y1, x2, y2, x3, y3, x4, y4);

    }

    private Bitmap scaledBitmap(Bitmap bitmap, int width, int height) {
        Log.v("aashari-tag", "scaledBitmap");
        Log.v("aashari-tag", width + " " + height);
        Matrix m = new Matrix();
        m.setRectToRect(new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight()), new RectF(0, 0, width, height), Matrix.ScaleToFit.CENTER);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
    }

    private Map<Integer, PointF> getEdgePoints(Bitmap tempBitmap) {
        Log.v("aashari-tag", "getEdgePoints");
        List<PointF> pointFs = getContourEdgePoints(tempBitmap);
        Map<Integer, PointF> orderedPoints = orderedValidEdgePoints(tempBitmap, pointFs);
        return orderedPoints;
    }

    private List<PointF> getContourEdgePoints(Bitmap tempBitmap) {
        Log.v("aashari-tag", "getContourEdgePoints");
        List<PointF> result = new ArrayList<>();
        MatOfPoint2f point2f = nativeClass.getPoint(tempBitmap);
        if (point2f != null) {
            List<Point> points = Arrays.asList(point2f.toArray());

            for (int i = 0; i < points.size(); i++) {
                result.add(new PointF(((float) points.get(i).x), ((float) points.get(i).y)));
            }
        }
        return result;

    }

    private Map<Integer, PointF> getOutlinePoints(Bitmap tempBitmap) {
        Log.v("aashari-tag", "getOutlinePoints");
        Map<Integer, PointF> outlinePoints = new HashMap<>();
        outlinePoints.put(0, new PointF(0, 0));
        outlinePoints.put(1, new PointF(tempBitmap.getWidth(), 0));
        outlinePoints.put(2, new PointF(0, tempBitmap.getHeight()));
        outlinePoints.put(3, new PointF(tempBitmap.getWidth(), tempBitmap.getHeight()));
        return outlinePoints;
    }

    private Map<Integer, PointF> orderedValidEdgePoints(Bitmap tempBitmap, List<PointF> pointFs) {
        Log.v("aashari-tag", "orderedValidEdgePoints");
        Map<Integer, PointF> orderedPoints = polygonView.getOrderedPoints(pointFs);
        if (!polygonView.isValidShape(orderedPoints)) {
            orderedPoints = getOutlinePoints(tempBitmap);
        }
        return orderedPoints;
    }

}
