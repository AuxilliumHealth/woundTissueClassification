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
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;

public class CameraGridOverlay extends View {
    private Paint paint = new Paint();

    public CameraGridOverlay(Context context) {
        super(context);
        init();
    }

    public CameraGridOverlay(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private int plusColor = Color.WHITE;

    private void init() {
        paint.setStrokeWidth(1.5f);
        paint.setAlpha(100); // Semi-transparent
        paint.setStyle(Paint.Style.STROKE);
    }

    public void setPlusColor(int color) {
        this.plusColor = color;
        invalidate(); // Redraw with the new color
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();

        // 1. Setup Paint
        paint.setColor(plusColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5.0f); // Thinner for a more refined look
        paint.setAlpha(200); // Slight transparency for premium feel
        paint.setStrokeCap(Paint.Cap.ROUND);

        float centerX = width / 2.0f;
        float centerY = height / 2.0f;
        
        // 2. Draw the center crosshair (+)
        float plusSize = 25.0f;
        canvas.drawLine(centerX - plusSize, centerY, centerX + plusSize, centerY, paint);
        canvas.drawLine(centerX, centerY - plusSize, centerX, centerY + plusSize, paint);

        // 3. Draw the corner brackets (The "Box")
        // Responsive box size: 65% of screen width
        float boxHalfSide = width * 0.325f;
        
        float left = centerX - boxHalfSide;
        float right = centerX + boxHalfSide;
        float top = centerY - boxHalfSide;
        float bottom = centerY + boxHalfSide;
        
        float cornerLen = 35.0f;   // Length of the L-shape arms
        float triangleSide = 12.0f; // Size of the filled corner triangle
        
        paint.setStrokeWidth(3.0f);

        // Top-Left
        drawCorner(canvas, left, top, cornerLen, triangleSide, 1, 1);
        // Top-Right
        drawCorner(canvas, right, top, cornerLen, triangleSide, -1, 1);
        // Bottom-Left
        drawCorner(canvas, left, bottom, cornerLen, triangleSide, 1, -1);
        // Bottom-Right
        drawCorner(canvas, right, bottom, cornerLen, triangleSide, -1, -1);
    }

    private void drawCorner(Canvas canvas, float x, float y, float len, float tSize, int dirX, int dirY) {
        paint.setStyle(Paint.Style.STROKE);
        // Draw L-shape lines
        canvas.drawLine(x, y, x + (len * dirX), y, paint);
        canvas.drawLine(x, y, x, y + (len * dirY), paint);
        
        // Draw a small filled triangle at the corner point
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        android.graphics.Path path = new android.graphics.Path();
        path.moveTo(x, y);
        path.lineTo(x + (tSize * dirX), y);
        path.lineTo(x, y + (tSize * dirY));
        path.close();
        canvas.drawPath(path, paint);
    }
}

