/**
 * ─────────────────────────────────────────────────────────────────────────────────────
 * Created & Developed by:
 * Aravindhan (Full Stack Engineer)
 * Auxilliumhealth LLC
 * GitHub: https://github.com/AravindhanDeveloper
 * ─────────────────────────────────────────────────────────────────────────────────────
 * Copyright (c) 2024. All rights reserved.
 * ─────────────────────────────────────────────────────────────────────────────────────
 */
package com.auxilliumhealth.woundtissueclassification.Utils;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

public class LassoView extends View {
    private Paint paint;
    private Path currentPath;
    private List<Path> pathList = new ArrayList<>();
    private RectF imageBounds = new RectF();
    private ImageView imageView;

    public LassoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void setImageView(ImageView imageView) {
        this.imageView = imageView;
        calculateImageBounds();
    }


    public RectF getImageBounds() {
        return new RectF(imageBounds); // Return a copy for safety
    }

    public void calculateImageBounds() {
        if (imageView == null || imageView.getDrawable() == null) return;

        // Get the image matrix
        Matrix matrix = imageView.getImageMatrix();
        Drawable drawable = imageView.getDrawable();

        // Get the drawable's intrinsic dimensions
        int drawableWidth = drawable.getIntrinsicWidth();
        int drawableHeight = drawable.getIntrinsicHeight();

        // Create a rectangle with the drawable dimensions
        RectF drawableRect = new RectF(0, 0, drawableWidth, drawableHeight);
        RectF viewRect = new RectF(0, 0, imageView.getWidth(), imageView.getHeight());

        // Map the drawable rectangle to the view rectangle
        matrix.mapRect(imageBounds, drawableRect);

        // Adjust for scale type (fitCenter)
        float scale = Math.min(
                viewRect.width() / drawableRect.width(),
                viewRect.height() / drawableRect.height()
        );

        float dx = (viewRect.width() - drawableRect.width() * scale) * 0.5f;
        float dy = (viewRect.height() - drawableRect.height() * scale) * 0.5f;

        imageBounds.set(
                dx,
                dy,
                dx + drawableRect.width() * scale,
                dy + drawableRect.height() * scale
        );
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);
        paint.setAntiAlias(true);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        calculateImageBounds();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (Path path : pathList) {
            canvas.drawPath(path, paint);
        }

        if (currentPath != null) {
            canvas.drawPath(currentPath, paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        float x = event.getX();
        float y = event.getY();

        // Ignore touches outside image bounds
        if (!imageBounds.contains(x, y)) {
            if (event.getAction() == MotionEvent.ACTION_UP && currentPath != null) {
                currentPath = null;
                invalidate();
            }
            return true;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Allow only one path
                if (!pathList.isEmpty()) return true;

                currentPath = new Path();
                currentPath.moveTo(x, y);
                break;

            case MotionEvent.ACTION_MOVE:
                if (currentPath != null) {
                    currentPath.lineTo(x, y);
                }
                break;

            case MotionEvent.ACTION_UP:
                if (currentPath != null) {
                    currentPath.lineTo(x, y);
                    currentPath.close();
                    pathList.clear(); // Only one path allowed
                    pathList.add(currentPath);
                    currentPath = null;
                    notifyListener();
                }
                break;
        }

        invalidate();
        return true;
    }

    public interface OnPathChangedListener {
        void onPathChanged(boolean hasPath);
    }

    private OnPathChangedListener listener;

    public void setOnPathChangedListener(OnPathChangedListener listener) {
        this.listener = listener;
    }

    private void notifyListener() {
        if (listener != null) {
            listener.onPathChanged(!pathList.isEmpty());
        }
    }

    public void undo() {
        if (!pathList.isEmpty()) {
            pathList.clear();
            invalidate();
            notifyListener();
        }
    }

    public void clear() {
        pathList.clear();
        currentPath = null;
        invalidate();
        notifyListener();
    }

    public List<Path> getAllPaths() {
        return pathList;
    }
}

