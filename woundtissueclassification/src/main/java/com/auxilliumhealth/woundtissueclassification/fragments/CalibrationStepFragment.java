package com.auxilliumhealth.woundtissueclassification.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;


import com.auxilliumhealth.woundtissueclassification.Activities.CameraActivity;
import com.auxilliumhealth.woundtissueclassification.Activities.VideoActivity;
import com.auxilliumhealth.woundtissueclassification.R;
import com.auxilliumhealth.woundtissueclassification.ViewModel.CalibrationViewModel;
import com.auxilliumhealth.woundtissueclassification.databinding.FragmentCalifrationStep1Binding;
import com.auxilliumhealth.woundtissueclassification.databinding.FragmentCalifrationStep2Binding;
import com.auxilliumhealth.woundtissueclassification.databinding.FragmentCalifrationStep3Binding;
import com.auxilliumhealth.woundtissueclassification.databinding.FragmentCalifrationStep4Binding;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textview.MaterialTextView;

import java.util.Locale;

public class CalibrationStepFragment extends Fragment {

    private static final String ARG_STEP = "step";
    private static final String ARG_SESSION_ID = "sessionId";
    private static final String ARG_USER_ID = "userId";
    private static final String ARG_WOUND_ID = "woundId";
    private static final String ARG_TOKEN = "token";
    private static final String ARG_PRIMARY_COLOR = "primaryColor";

    private int step;
    private String sessionId, userId, woundId, token, primaryColor;

    private ViewPager2 viewPager;
    private CalibrationViewModel calibrationViewModel;

    // Bindings
    private FragmentCalifrationStep1Binding binding1;
    private FragmentCalifrationStep2Binding binding2;
    private FragmentCalifrationStep3Binding binding3;
    private FragmentCalifrationStep4Binding binding4;

    // Step 1
    private String selectedCoinName = null;
    private MaterialCardView selectedCard = null;
    private int selectedCoinPosition = 1;

    public CalibrationStepFragment() {
        // Required empty public constructor
    }

    public static CalibrationStepFragment newInstance(int step, String sessionId, String userId, String woundId, String token, String primaryColor) {
        CalibrationStepFragment fragment = new CalibrationStepFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_STEP, step);
        args.putString(ARG_SESSION_ID, sessionId);
        args.putString(ARG_USER_ID, userId);
        args.putString(ARG_WOUND_ID, woundId);
        args.putString(ARG_TOKEN, token);
        args.putString(ARG_PRIMARY_COLOR, primaryColor);
        fragment.setArguments(args);
        return fragment;
    }

    public void setViewPager(ViewPager2 viewPager) {
        this.viewPager = viewPager;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            step = getArguments().getInt(ARG_STEP);
            sessionId = getArguments().getString(ARG_SESSION_ID);
            userId = getArguments().getString(ARG_USER_ID);
            woundId = getArguments().getString(ARG_WOUND_ID);
            token = getArguments().getString(ARG_TOKEN);
            primaryColor = getArguments().getString(ARG_PRIMARY_COLOR);
        }
        calibrationViewModel = new ViewModelProvider(requireActivity()).get(CalibrationViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        switch (step) {
            case 1:
                binding1 = FragmentCalifrationStep1Binding.inflate(inflater, container, false);
                return binding1.getRoot();
            case 2:
                binding2 = FragmentCalifrationStep2Binding.inflate(inflater, container, false);
                return binding2.getRoot();
            case 3:
                binding3 = FragmentCalifrationStep3Binding.inflate(inflater, container, false);
                return binding3.getRoot();
            case 4:
                binding4 = FragmentCalifrationStep4Binding.inflate(inflater, container, false);
                return binding4.getRoot();
            default:
                binding1 = FragmentCalifrationStep1Binding.inflate(inflater, container, false);
                return binding1.getRoot();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        switch (step) {
            case 1:
                handleStep1();
                break;
            case 2:
                handleStep2();
                break;
            case 3:
                handleStep3();
                break;
            case 4:
                handleStep4();
                break;
        }
    }

    private void handleStep1() {
        binding1.helpTxt.setTextColor(Color.parseColor(primaryColor));
        setupCountrySpinner();
        setupUSCoinSelection();
        setupIndianCoinSelection();

        binding1.helpTxt.setOnClickListener(v -> startActivity(new Intent(getActivity(), VideoActivity.class)));
    }

    private void setupCountrySpinner() {
        // Create country list
        String[] countries = {"United States", "India"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, countries);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding1.countrySpinner.setAdapter(adapter);

        // Detect default region
        String defaultCountry = detectRegion();
        int defaultPosition = getCountryPosition(defaultCountry);
        binding1.countrySpinner.setSelection(defaultPosition);

        // Show appropriate currency layout based on default selection
        showCurrencyLayout(defaultPosition);

        // Set spinner selection listener
        binding1.countrySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                showCurrencyLayout(position);
                resetCoinSelection();
                TextView selectedText = (TextView) binding1.countrySpinner.getSelectedView();
                if (selectedText != null) {
                    selectedText.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private String detectRegion() {
        String countryCode = Locale.getDefault().getCountry();
        switch (countryCode) {
            case "US":
                return "United States";
            case "IN":
                return "India";
            default:
                return "United States"; // Default to US
        }
    }

    private int getCountryPosition(String country) {
        switch (country) {
            case "United States":
                return 0;
            case "India":
                return 1;
            default:
                return 0;
        }
    }

    private void showCurrencyLayout(int countryPosition) {
        switch (countryPosition) {
            case 0: // United States
                binding1.usCurrencyLayout.setVisibility(View.VISIBLE);
                binding1.indianCurrencyLayout.setVisibility(View.GONE);
                break;
            case 1: // India
                binding1.usCurrencyLayout.setVisibility(View.GONE);
                binding1.indianCurrencyLayout.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void setupUSCoinSelection() {
        MaterialCardView[] usCards = {binding1.cardPenny, binding1.cardNickel, binding1.cardDime, binding1.cardQuarter};
        String[] usCoinIds = {"coin_name_penny", "coin_name_nickel", "coin_name_dime", "coin_name_quarter"};
        int[] usCoinPositions = {1, 2, 3, 4}; // Positions for US coins

        for (int i = 0; i < usCards.length; i++) {
            MaterialCardView card = usCards[i];
            int position = usCoinPositions[i];
            int textViewId = getResources().getIdentifier(usCoinIds[i], "id", requireContext().getPackageName());

            if (textViewId == 0) continue;

            MaterialTextView coinTextView = card.findViewById(textViewId);

            card.setOnClickListener(v -> {
                // Only process click if US layout is visible
                if (binding1.usCurrencyLayout.getVisibility() == View.VISIBLE) {
                    handleCoinSelection(card, coinTextView, position);
                }
            });
        }
    }

    private void setupIndianCoinSelection() {
        MaterialCardView[] indianCards = {binding1.cardRupees};
        String[] indianCoinIds = {"coin_name_rupees"};
        int[] indianCoinPositions = {5}; // Position for Indian coin

        for (int i = 0; i < indianCards.length; i++) {
            MaterialCardView card = indianCards[i];
            int position = indianCoinPositions[i];
            int textViewId = getResources().getIdentifier(indianCoinIds[i], "id", requireContext().getPackageName());

            if (textViewId == 0) continue;

            MaterialTextView coinTextView = card.findViewById(textViewId);

            card.setOnClickListener(v -> {
                // Only process click if Indian layout is visible
                if (binding1.indianCurrencyLayout.getVisibility() == View.VISIBLE) {
                    handleCoinSelection(card, coinTextView, position);
                }
            });
        }
    }

    private void handleCoinSelection(MaterialCardView card, MaterialTextView coinTextView, int position) {
        // Reset previous selection
        if (selectedCard != null) {
            selectedCard.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.white));
        }

        // Set new selection
        card.setStrokeColor(Color.parseColor(primaryColor));
        selectedCard = card;

        if (coinTextView != null) {
            selectedCoinName = coinTextView.getText().toString();
            calibrationViewModel.setSelectedCoin(selectedCoinName, position);
        }

        // Auto-navigate to next step
        if (viewPager != null && viewPager.getAdapter() != null && viewPager.getCurrentItem() < viewPager.getAdapter().getItemCount() - 1) {
            viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
        }
    }

    private void resetCoinSelection() {
        if (selectedCard != null) {
            selectedCard.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.white));
            selectedCard = null;
        }
        selectedCoinName = null;
        selectedCoinPosition = 1; // Reset to default position
        calibrationViewModel.setSelectedCoin(null, 1);
    }

    private void handleStep2() {
        binding2.helpTxt.setTextColor(Color.parseColor(primaryColor));

        calibrationViewModel.getSelectedPosition().observe(getViewLifecycleOwner(), position -> {
            if (position == null) return;
            selectedCoinPosition = position;
            int imgResId;
            switch (position) {
                case 1:
                    imgResId = R.drawable.penny_coin;
                    break;
                case 2:
                    imgResId = R.drawable.nickel_coin;
                    break;
                case 3:
                    imgResId = R.drawable.dime_coin;
                    break;
                case 4:
                    imgResId = R.drawable.quarter_coin;
                    break;
                case 5:
                    imgResId = R.drawable.ten_rupees;
                    break;
                default:
                    imgResId = R.drawable.penny_coin;
            }
            binding2.coinOneImg.setImageResource(imgResId);
            binding2.coinTwoImg.setImageResource(imgResId);
        });

        binding2.nextBtn.setOnClickListener(v -> {
            if (viewPager != null && viewPager.getCurrentItem() < 3) {
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
            }
        });
        binding2.nextBtn.setBackgroundColor(Color.parseColor(primaryColor));
        binding2.helpTxt.setOnClickListener(v -> startActivity(new Intent(getActivity(), VideoActivity.class)));
    }

    private void handleStep3() {
        binding3.helpTxt.setTextColor(Color.parseColor(primaryColor));

// Example: Change ALL colors to green


        binding3.nextBtn.setOnClickListener(v -> {
            if (viewPager != null && viewPager.getCurrentItem() < 3) {
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
            }
        });
        binding3.nextBtn.setBackgroundColor(Color.parseColor(primaryColor));
        binding3.helpTxt.setOnClickListener(v -> startActivity(new Intent(getActivity(), VideoActivity.class)));
    }

    private void handleStep4() {
        binding4.helpTxt.setTextColor(Color.parseColor(primaryColor));

        calibrationViewModel.getSelectedPosition().observe(getViewLifecycleOwner(), position -> {
            if (position != null) {
                selectedCoinPosition = position;
            }
        });
        binding4.nextBtn.setBackgroundColor(Color.parseColor(primaryColor));

        binding4.nextBtn.setOnClickListener(v -> {
            Intent i = new Intent(getActivity(), CameraActivity.class);
            i.putExtra("whereFrom", "calibrate");
            i.putExtra("coinType", String.valueOf(selectedCoinPosition));
            i.putExtra("sessionId", sessionId);
            i.putExtra("userId", userId);
            i.putExtra("woundId", woundId);
            i.putExtra("token", token);
            i.putExtra("primaryColor", primaryColor);
            startActivity(i);
            getActivity().finish();
        });

        binding4.helpTxt.setOnClickListener(v -> startActivity(new Intent(getActivity(), VideoActivity.class)));
    }
}