package com.auxilliumhealth.woundtissueclassification.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.res.ColorStateList;
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
import com.auxilliumhealth.woundtissueclassification.databinding.FragmentWoundLocationBinding;

public class WoundLocationFragment extends Fragment implements View.OnClickListener {

    private FragmentWoundLocationBinding binding;
    private String frontBackBody;
    private boolean isFrontImage = true;

    private String userId;
    private String token;
    private String primaryColor;
    private String woundId;
    private boolean woundScoreRequired;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentWoundLocationBinding.inflate(inflater, container, false);
        getArgumentsData();
        initView();
        return binding.getRoot();
    }

    private void getArgumentsData() {
        Bundle args = getArguments();
        if (args != null) {
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
    }

    private void initView() {
        binding.woundAreaTxt.setTextColor(Color.parseColor(primaryColor));
        binding.rotateButton.setColorFilter(Color.parseColor(primaryColor));
        int circleColor = Color.parseColor(primaryColor);
        binding.nextImg.setSupportBackgroundTintList(ColorStateList.valueOf(circleColor));

        // Change FAB icon color
        binding.nextImg.setImageTintList(ColorStateList.valueOf(Color.WHITE));

        binding.bodyImg.setOnClickListener(this);
        binding.backImg.setOnClickListener(this);
        binding.rotateButton.setOnClickListener(this);
        binding.nextImg.setOnClickListener(this);

        if (frontBackBody != null) {
            switch (frontBackBody) {
                case "Front":
                    binding.bodyImg.setImageDrawable(getActivity().getDrawable(R.drawable.front));
                    binding.woundAreaTxt.setText("Front");
                    isFrontImage = true;
                    break;
                case "Back":
                    binding.bodyImg.setImageDrawable(getActivity().getDrawable(R.drawable.back));
                    binding.woundAreaTxt.setText("Back");
                    isFrontImage = false;
                    break;
            }
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.back_img) {
            getActivity().finish();
        }
        else if (view.getId() == R.id.rotate_button) {
            rotateImage();
        }
        else if (view.getId() == R.id.next_img || view.getId() == R.id.body_img) {
            if (isFrontImage) {
                loadFragment(new WoundFrontFragment(), "Front");
            } else {
                loadFragment(new WoundBackFragment(), "Back");
            }
        }
    }

    public void loadFragment(Fragment fragment, String frontBackBody) {
        Bundle bundle = new Bundle();
        bundle.putString("frontBackBody", frontBackBody);
        bundle.putString("userId", userId);
        bundle.putString("token", token);
        bundle.putString("primaryColor", primaryColor);
        bundle.putString("woundId", woundId);
        bundle.putBoolean("woundScoreRequired", woundScoreRequired);
        fragment.setArguments(bundle);

        FragmentManager fm = getActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fm.beginTransaction();
        fragmentTransaction.replace(R.id.container, fragment);
        fragmentTransaction.commit();
    }

    private void rotateImage() {
        ObjectAnimator rotateAnimator = ObjectAnimator.ofFloat(binding.bodyImg, "rotationY", 0f, 90f);
        rotateAnimator.setDuration(250);

        rotateAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (isFrontImage) {
                    binding.bodyImg.setImageDrawable(getActivity().getDrawable(R.drawable.back));
                    binding.woundAreaTxt.setText("Back");
                } else {
                    binding.bodyImg.setImageDrawable(getActivity().getDrawable(R.drawable.front));
                    binding.woundAreaTxt.setText("Front");
                }
                isFrontImage = !isFrontImage;

                ObjectAnimator rotateBackAnimator = ObjectAnimator.ofFloat(binding.bodyImg, "rotationY", 90f, 180f);
                rotateBackAnimator.setDuration(250);
                rotateBackAnimator.start();
            }
        });

        rotateAnimator.start();
    }
}
