package com.auxilliumhealth.woundtissueclassification.Adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.auxilliumhealth.woundtissueclassification.fragments.CalibrationStepFragment;

public class CalibrationAdapter extends FragmentStateAdapter {

    private final ViewPager2 viewPager;
    private final String sessionId;
    private final String userId;
    private final String woundId;
    private final String token;
    private final String primaryColor;
    private final boolean woundScoreRequired, woundLocationRequired;

    public CalibrationAdapter(@NonNull FragmentActivity fragmentActivity,
                              ViewPager2 viewPager,
                              String sessionId,
                              String userId,
                              String woundId,
                              String token,
                              String primaryColor,
                              boolean woundScoreRequired,
                              boolean woundLocationRequired) {
        super(fragmentActivity);
        this.viewPager = viewPager;
        this.sessionId = sessionId;
        this.userId = userId;
        this.woundId = woundId;
        this.token = token;
        this.primaryColor = primaryColor;
        this.woundScoreRequired = woundScoreRequired;
        this.woundLocationRequired = woundLocationRequired;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // Create fragment with only serializable data
        CalibrationStepFragment fragment =
                CalibrationStepFragment.newInstance(position + 1, sessionId, userId, woundId, token, primaryColor, woundScoreRequired, woundLocationRequired);

        // Pass ViewPager separately via setter
        fragment.setViewPager(viewPager);

        return fragment;
    }

    @Override
    public int getItemCount() {
        return 4; // Total steps
    }
}
