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
import android.content.Intent;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.auxilliumhealth.woundtissueclassification.Activities.CameraActivity;
import com.auxilliumhealth.woundtissueclassification.Model.LatestSessionModel;
import com.auxilliumhealth.woundtissueclassification.R;
import com.auxilliumhealth.woundtissueclassification.Repository.Repository;
import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;


public class OldWoundLoctionAdapter extends RecyclerView.Adapter<OldWoundLoctionAdapter.ViewHolder> {



    public static String woundID;
    Context mContext;
    List<LatestSessionModel.Datum> woundLocationModels = new ArrayList<>();
    Repository repository;
    String userId;
    String token;
    String colorHex;
    boolean woundScoreRequired;

    public OldWoundLoctionAdapter(Context context, List<LatestSessionModel.Datum> woundLocationModels, String userId, String token, String colorHex,boolean woundScoreRequired) {
        this.mContext = context;
        this.woundLocationModels = woundLocationModels;
        this.userId = userId;
        this.token = token;
        this.colorHex = colorHex;
        this.woundScoreRequired = woundScoreRequired;

        //woundID = PreferencesHelper.getPreference(mContext, PreferencesHelper.PREF_WOUND_COUNT);
        repository = new Repository(mContext);

    }

    private static String formatText(String inputText) {
        String[] parts = inputText.split("_");

        StringBuilder formattedText = new StringBuilder();
        for (int i = 1; i < parts.length; i++) {
            formattedText.append(parts[i]);
            if (i < parts.length - 1) {
                formattedText.append(" ");
            }
        }

        return formattedText.toString();
    }

    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.old_wound_location_card, parent, false);
        return new ViewHolder(view);

    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {

        LatestSessionModel.Datum dataList = woundLocationModels.get(position);
        String frontBackBodyText = "Side: <font color='gray'>" + formatText(dataList.getWoundLocation()) + "</font>";
        holder.bodyPositionTxt.setText(Html.fromHtml(frontBackBodyText), TextView.BufferType.SPANNABLE);
        Glide.with(mContext).load(dataList.getImageUrl()).into(holder.locationImg);
        holder.oldWoundLocationCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                woundID = dataList.getWoundId();
                Intent i = new Intent(mContext, CameraActivity.class);
                i.putExtra("whereFrom", "Imaging");
                i.putExtra("woundId", dataList.getWoundId());
                i.putExtra("woundLocation", dataList.getWoundLocation());
                i.putExtra("sessionId", dataList.getSessionId());
                i.putExtra("woundScoreRequired", woundScoreRequired);
                i.putExtra("userId", userId);
                i.putExtra("token", token);
                i.putExtra("primaryColor", colorHex);
                mContext.startActivity(i);


            }


        });

    }








    @Override
    public int getItemCount() {
        return woundLocationModels.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        MaterialTextView bodyPositionTxt;
        ImageView locationImg;
        MaterialCardView oldWoundLocationCard;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            bodyPositionTxt = itemView.findViewById(R.id.body_position_txt);
            locationImg = itemView.findViewById(R.id.location_img);
            oldWoundLocationCard = itemView.findViewById(R.id.old_wound_location_card);


        }
    }
}
