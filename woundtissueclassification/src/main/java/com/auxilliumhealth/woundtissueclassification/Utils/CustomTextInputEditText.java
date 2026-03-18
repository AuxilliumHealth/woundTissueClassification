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
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.appcompat.widget.AppCompatEditText;

public class CustomTextInputEditText extends AppCompatEditText {

    private Drawable clickDrawable;
    private OnDrawableClickListener onDrawableClickListener;

    public CustomTextInputEditText(Context context) {
        super(context);
        init();
    }

    public CustomTextInputEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomTextInputEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Get the drawable at end position (drawableEnd)
        Drawable[] drawables = getCompoundDrawablesRelative();
        clickDrawable = drawables[2]; // Assumes drawableEnd is the last one
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP && clickDrawable != null) {
            // Check if the touch event is within the bounds of the drawable
            if (event.getX() >= (getWidth() - getPaddingEnd() - clickDrawable.getBounds().width())
                    && event.getX() <= (getWidth() - getPaddingEnd())) {
                if (onDrawableClickListener != null) {
                    onDrawableClickListener.onClick();
                    return true;
                }
            }
        }
        return super.onTouchEvent(event);
    }

    // Interface for click listener
    public interface OnDrawableClickListener {
        void onClick();
    }

    // Method to set click listener
    public void setOnDrawableClickListener(OnDrawableClickListener listener) {
        onDrawableClickListener = listener;
    }
}
