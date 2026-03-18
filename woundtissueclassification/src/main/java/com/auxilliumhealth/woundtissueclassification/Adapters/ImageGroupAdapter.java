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
package com.auxilliumhealth.woundtissueclassification.Adapters;


import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

import com.auxilliumhealth.woundtissueclassification.Model.ResultDataModel;
import com.auxilliumhealth.woundtissueclassification.R;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ImageGroupAdapter extends PagerAdapter {
    private final Context context;
    private List<ResultDataModel> resultList; // Make this non-final so we can update it
    private final ArrayList<String> filePaths;

    public ImageGroupAdapter(Context context, List<ResultDataModel> resultList, ArrayList<String> filePaths) {
        this.context = context;
        this.resultList = new ArrayList<>(resultList); // Create a new list to avoid external modifications
        this.filePaths = filePaths;
    }

    // Add this method to update data
    public void updateData(List<ResultDataModel> newData) {
        this.resultList = new ArrayList<>(newData);
        notifyDataSetChanged();
    }
    // Add this to ImageGroupAdapter
    public List<ResultDataModel> getCurrentList() {
        return new ArrayList<>(resultList); // Return a defensive copy
    }
    @Override
    public int getCount() {
        return resultList.size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        View view = LayoutInflater.from(context).inflate(R.layout.image_group_page, container, false);
        ImageView overlay = view.findViewById(R.id.imageView);

        ResultDataModel resultDataModel = resultList.get(position);
        String filePath = filePaths.get(position);

        // Clear previous image to avoid flickering or wrong images
        overlay.setImageDrawable(null);

        // Show overlay if exists, otherwise show original image
        File overlayFile = resultDataModel.getcontourImage();
        if (overlayFile != null && overlayFile.exists()) {
            Log.d("TAG", "instantiateItem: "+overlayFile);

            Glide.with(context)
                    .load(overlayFile)
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(overlay);
        } else {
            Glide.with(context)
                    .load(filePath)
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(overlay);
        }

        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        // This ensures that when notifyDataSetChanged() is called,
        // all items will be considered as changed and will be recreated
        return POSITION_NONE;
    }

    public ResultDataModel getItem(int position) {
        return resultList.get(position);
    }
}
