package com.auxilliumhealth.woundtissueclassification.Utils;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Window;
import android.widget.TextView;

import com.airbnb.lottie.LottieAnimationView;
import com.auxilliumhealth.woundtissueclassification.R;

public class LoadingDialog extends Dialog {

    private String title = "Please Wait";
    private String message = "AI is processing your image\nWait for result...";
    private LottieAnimationView lottieAnimationView;
    private TextView tvTitle, tvMessage;
    private boolean isCancelable = false;

    public LoadingDialog(Context context) {
        super(context);
    }

    public LoadingDialog(Context context, String title, String message) {
        super(context);
        this.title = title;
        this.message = message;
    }

    public LoadingDialog(Context context, String title, String message, boolean isCancelable) {
        super(context);
        this.title = title;
        this.message = message;
        this.isCancelable = isCancelable;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_loading);

        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        setCancelable(isCancelable);
        setCanceledOnTouchOutside(false);

        initializeViews();
        setupContent();
    }

    private void initializeViews() {
        lottieAnimationView = findViewById(R.id.lottieAnimationView);
        tvTitle = findViewById(R.id.tvTitle);
        tvMessage = findViewById(R.id.tvMessage);
    }

    private void setupContent() {
        if (tvTitle != null) tvTitle.setText(title);
        if (tvMessage != null) tvMessage.setText(message);
    }

    public void updateMessage(String newMessage) {
        if (tvMessage != null && isShowing()) {
            tvMessage.setText(newMessage);
        }
    }

    @Override
    public void dismiss() {
        try {
            if (isShowing()) {
                super.dismiss();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}