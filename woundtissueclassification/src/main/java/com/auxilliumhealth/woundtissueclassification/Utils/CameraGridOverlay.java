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

    private void init() {
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(1.5f);
        paint.setAlpha(100); // Semi-transparent
        paint.setStyle(Paint.Style.STROKE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();

        // Vertical lines
        canvas.drawLine(width / 3.0f, 0, width / 3.0f, height, paint);
        canvas.drawLine(2 * width / 3.0f, 0, 2 * width / 3.0f, height, paint);

        // Horizontal lines
        canvas.drawLine(0, height / 3.0f, width, height / 3.0f, paint);
        canvas.drawLine(0, 2 * height / 3.0f, width, 2 * height / 3.0f, paint);

        // Center crosshair
        float centerX = width / 2.0f;
        float centerY = height / 2.0f;
        float crossLength = 30.0f;
        canvas.drawLine(centerX - crossLength, centerY, centerX + crossLength, centerY, paint);
        canvas.drawLine(centerX, centerY - crossLength, centerX, centerY + crossLength, paint);
    }
}

