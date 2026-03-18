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
package com.auxilliumhealth.woundtissueclassification.fragments;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.auxilliumhealth.woundtissueclassification.R;
import com.auxilliumhealth.woundtissueclassification.databinding.FragmentWoundBackUpperBinding;

import java.io.ByteArrayOutputStream;

public class WoundBackFragment extends Fragment implements View.OnClickListener {

    FragmentWoundBackUpperBinding binding;
    String upperLowerbody, frontBackBody, userId, token, primaryColor, woundId;
    boolean woundScoreRequired;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentWoundBackUpperBinding.inflate(inflater, container, false);
        initView();
        return binding.getRoot();
    }

    private void initView() {
        Bundle args = getArguments();
        if (args != null) {
            upperLowerbody = args.getString("upperLowerbody");
            frontBackBody = args.getString("frontBackBody");
            userId = args.getString("userId");
            if (userId == null) userId = args.getString("patientId");
            if (userId == null) userId = args.getString("user_id");

            token = args.getString("token");

            woundId = args.getString("woundId");
            if (woundId == null) woundId = args.getString("wound_id");

            primaryColor = args.getString("primaryColor");
            woundScoreRequired = args.getBoolean("woundScoreRequired");

        }

        binding.bodyPartTxt.setTextColor(Color.parseColor(primaryColor));
        binding.leftTxt.setTextColor(Color.parseColor(primaryColor));
        binding.rightTxt.setTextColor(Color.parseColor(primaryColor));

        setupClickListeners();
    }

    private void setupClickListeners() {
        binding.backImg.setOnClickListener(this);
        binding.leftFaceCard.setOnClickListener(this);
        binding.rightFaceCard.setOnClickListener(this);
        binding.leftNeckCard.setOnClickListener(this);
        binding.rightNeckCard.setOnClickListener(this);
        binding.leftBackCard.setOnClickListener(this);
        binding.rightBackCard.setOnClickListener(this);
        binding.leftHipCard.setOnClickListener(this);
        binding.rightHipCard.setOnClickListener(this);
        binding.leftSholderCard.setOnClickListener(this);
        binding.rightSholderCard.setOnClickListener(this);
        binding.leftFingerCard.setOnClickListener(this);
        binding.rightFingerCard.setOnClickListener(this);
        binding.leftWristCard.setOnClickListener(this);
        binding.rightWristCard.setOnClickListener(this);
        binding.leftHiptokneeCard.setOnClickListener(this);
        binding.leftHealCard.setOnClickListener(this);
        binding.leftKneetoankleCard.setOnClickListener(this);
        binding.rightHealCard.setOnClickListener(this);
        binding.rightHiptokneeCard.setOnClickListener(this);
        binding.rightKneetoankleCard.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.back_img) {
            goBack();
            return;
        }

        // Handle all body part clicks
        handleBodyPartClick(view);
    }

    private void handleBodyPartClick(View view) {
        String partSideBody = "";
        String woundPartBody = "";
        int imageResId = 0;
        boolean isUpperBody = true;

        // Determine body part details based on clicked view
        if (view.getId() == R.id.left_face_card) {
            partSideBody = "Left"; woundPartBody = "Head";
            imageResId = R.id.left_head_img;
        }
        else if (view.getId() == R.id.right_face_card) {
            partSideBody = "Right"; woundPartBody = "Head";
            imageResId = R.id.right_head_img;
        }
        else if (view.getId() == R.id.left_neck_card) {
            partSideBody = "Left"; woundPartBody = "Neck";
            imageResId = R.id.left_neck_img;
        }
        else if (view.getId() == R.id.right_neck_card) {
            partSideBody = "Right"; woundPartBody = "Neck";
            imageResId = R.id.right_neck_img;
        }
        else if (view.getId() == R.id.left_back_card) {
            partSideBody = "Left"; woundPartBody = "Back";
            imageResId = R.id.left_back_img;
        }
        else if (view.getId() == R.id.right_back_card) {
            partSideBody = "Right"; woundPartBody = "Back";
            imageResId = R.id.right_back_img;
        }
        else if (view.getId() == R.id.left_hip_card) {
            partSideBody = "Left"; woundPartBody = "Hip and Waist";
            imageResId = R.id.left_hip_and_waist_img;
        }
        else if (view.getId() == R.id.right_hip_card) {
            partSideBody = "Right"; woundPartBody = "Hip and Waist";
            imageResId = R.id.right_hip_and_waist_img;
        }
        else if (view.getId() == R.id.left_sholder_card) {
            partSideBody = "Left"; woundPartBody = "Shoulder to Elbow";
            imageResId = R.id.left_shoulder_img;
        }
        else if (view.getId() == R.id.right_sholder_card) {
            partSideBody = "Right"; woundPartBody = "Shoulder to Elbow";
            imageResId = R.id.right_shoulder_img;
        }
        else if (view.getId() == R.id.left_wrist_card) {
            partSideBody = "Left"; woundPartBody = "Elbow to Fingers";
            imageResId = R.id.left_elbow_img;
        }
        else if (view.getId() == R.id.right_wrist_card) {
            partSideBody = "Right"; woundPartBody = "Elbow to Fingers";
            imageResId = R.id.right_elbow_img;
        }
        else if (view.getId() == R.id.left_finger_card) {
            partSideBody = "Left"; woundPartBody = "Knuckle and Fingers";
            imageResId = R.id.left_finger_img;
        }
        else if (view.getId() == R.id.right_finger_card) {
            partSideBody = "Right"; woundPartBody = "Knuckle and Fingers";
            imageResId = R.id.right_finger_img;
        }
        // Lower body parts
        else if (view.getId() == R.id.left_hiptoknee_card) {
            partSideBody = "Left"; woundPartBody = "Hip to Knee";
            imageResId = R.id.left_hip_knee_img; isUpperBody = false;
        }
        else if (view.getId() == R.id.right_hiptoknee_card) {
            partSideBody = "Right"; woundPartBody = "Hip to Knee";
            imageResId = R.id.right_hip_knee_img; isUpperBody = false;
        }
        else if (view.getId() == R.id.left_kneetoankle_card) {
            partSideBody = "Left"; woundPartBody = "Knee to Ankle";
            imageResId = R.id.left_knee_ankle_img; isUpperBody = false;
        }
        else if (view.getId() == R.id.right_kneetoankle_card) {
            partSideBody = "Right"; woundPartBody = "Knee to Ankle";
            imageResId = R.id.right_knee_ankle_img; isUpperBody = false;
        }
        else if (view.getId() == R.id.left_heal_card) {
            partSideBody = "Left"; woundPartBody = "Heel";
            imageResId = R.id.left_heel_img; isUpperBody = false;
        }
        else if (view.getId() == R.id.right_heal_card) {
            partSideBody = "Right"; woundPartBody = "Heel";
            imageResId = R.id.right_heel_img; isUpperBody = false;
        }
        else {
            return;
        }

        // Navigate to WoundSummeryFragment
        navigateToWoundSummary(partSideBody, woundPartBody, imageResId, isUpperBody);
    }

    private void navigateToWoundSummary(String partSideBody, String woundPartBody, int imageResId, boolean isUpperBody) {
        try {
            // Get the image view and bitmap
            View imageView = binding.getRoot().findViewById(imageResId);
            if (imageView != null) {
                imageView.setDrawingCacheEnabled(true);
                Bitmap bitmap = Bitmap.createBitmap(imageView.getDrawingCache());
                imageView.setDrawingCacheEnabled(false);

                byte[] byteArray = bitmapToByteArray(bitmap);

                // Create and setup WoundSummeryFragment
                WoundSummeryFragment fragment = new WoundSummeryFragment();
                Bundle args = new Bundle();
                args.putString("frontBackBody", frontBackBody);
                args.putString("upperLowerbody", isUpperBody ? "Upper" : "Lower");
                args.putString("partSideBody", partSideBody);
                args.putString("woundPartBody", woundPartBody);
                args.putBoolean("woundScoreRequired", woundScoreRequired);
                args.putByteArray("woundPartImg", byteArray);
                args.putString("userId", userId);
                args.putString("token", token);
                args.putString("woundId", woundId);
                args.putString("primaryColor", primaryColor);
                fragment.setArguments(args);

                // Navigate to fragment
                FragmentManager fragmentManager = getParentFragmentManager();
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.replace(R.id.container, fragment);
                transaction.addToBackStack("woundSummary");
                transaction.commit();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void goBack() {
        FragmentManager fragmentManager = getParentFragmentManager();
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
        } else {
            // Load previous fragment (WoundLocationFragment)
            WoundLocationFragment fragment = new WoundLocationFragment();
            Bundle bundle = new Bundle();
            bundle.putString("frontBackBody", frontBackBody);
            bundle.putString("userId", userId);
            bundle.putString("token", token);
            bundle.putString("woundId", woundId);
            bundle.putString("primaryColor", primaryColor);
            bundle.putBoolean("woundScoreRequired", woundScoreRequired);
            fragment.setArguments(bundle);

            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.container, fragment);
            transaction.commit();
        }
    }

    private byte[] bitmapToByteArray(Bitmap bitmap) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            return stream.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }
}