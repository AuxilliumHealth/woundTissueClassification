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

        // 1. Setup Paint for the "Plus" Point
        paint.setColor(plusColor);
        paint.setStrokeWidth(8.0f); // Thicker stroke for better visibility
        paint.setAlpha(255); // Full opacity for primary color
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND); // Smooth rounded ends for a premium feel

        float centerX = width / 2.0f;
        float centerY = height / 2.0f;
        float plusSize = 35.0f; // Size of the plus arms

        // 2. Draw the "Plus" (+) only
        // Horizontal Line
        canvas.drawLine(centerX - plusSize, centerY, centerX + plusSize, centerY, paint);
        // Vertical Line
        canvas.drawLine(centerX, centerY - plusSize, centerX, centerY + plusSize, paint);
    }
}

