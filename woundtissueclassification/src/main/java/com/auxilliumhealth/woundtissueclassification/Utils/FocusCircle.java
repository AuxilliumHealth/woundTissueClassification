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

import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import androidx.annotation.Nullable;

public class FocusCircle extends View {
    private Paint oPaint = new Paint();
    private Paint iPaint = new Paint();
    private float x, y; // Changed from height, width to x, y for clarity
    private float iRadius = 0f;
    private float oRadius = 0f;
    private static final float MAX_OUTER_RADIUS = 80f;
    private static final float MAX_INNER_RADIUS = 50f;
    private static final int ANIMATION_DURATION = 300;

    public FocusCircle(Context context) {
        super(context);
        init();
    }

    public FocusCircle(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        oPaint.setStrokeWidth(3.0f);
        oPaint.setAntiAlias(true);
        oPaint.setStyle(Paint.Style.STROKE);
        oPaint.setColor(Color.WHITE);

        iPaint.setStyle(Paint.Style.FILL);
        iPaint.setColor(0x33FFFFFF); // Semi-transparent white for inner circle
        iPaint.setAntiAlias(true);
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
        animateFocus();
    }

    private void animateFocus() {
        PropertyValuesHolder outerRadiusHolder = PropertyValuesHolder.ofFloat("oRadius", MAX_OUTER_RADIUS * 1.2f, MAX_OUTER_RADIUS * 0.8f);
        PropertyValuesHolder innerRadiusHolder = PropertyValuesHolder.ofFloat("iRadius", 0f, MAX_INNER_RADIUS);

        ValueAnimator animator = ValueAnimator.ofPropertyValuesHolder(outerRadiusHolder, innerRadiusHolder);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.setDuration(ANIMATION_DURATION);
        animator.addUpdateListener(animation -> {
            oRadius = (float) animation.getAnimatedValue("oRadius");
            iRadius = (float) animation.getAnimatedValue("iRadius");
            invalidate();
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                oRadius = 0f;
                iRadius = 0f;
                setVisibility(View.GONE);
                invalidate();
            }
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (oRadius > 0) {
            canvas.drawCircle(x, y, iRadius, iPaint); // Inner circle
            canvas.drawCircle(x, y, oRadius, oPaint); // Outer circle
        }
    }
}