package com.auxilliumhealth.woundtissueclassification.Adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.auxilliumhealth.woundtissueclassification.Model.AiModelData;
import com.auxilliumhealth.woundtissueclassification.Model.AnalysisImage;
import com.auxilliumhealth.woundtissueclassification.R;
import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textview.MaterialTextView;
import com.google.gson.Gson;

import java.util.ArrayList;


public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {
    Context mContext;

    ArrayList<AnalysisImage> analysisImageList;

    public ImageAdapter(Context context, ArrayList<AnalysisImage> analysisImageList) {
        this.mContext = context;
        this.analysisImageList = analysisImageList;

    }

    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.image_card, parent, false);
        return new ViewHolder(view);

    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        AnalysisImage result = analysisImageList.get(position);
        Glide.with(mContext).load(result.getImageUrl()).thumbnail(Glide.with(mContext).load(R.drawable.image_error)).into(holder.woundImage);
        holder.imageTitle.setText(result.getTitle());

    }


    @Override
    public int getItemCount() {
        return analysisImageList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {


        MaterialTextView imageTitle;
        ImageView woundImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            woundImage = itemView.findViewById(R.id.wound_image);
            imageTitle = itemView.findViewById(R.id.title_txt);


        }
    }
}

