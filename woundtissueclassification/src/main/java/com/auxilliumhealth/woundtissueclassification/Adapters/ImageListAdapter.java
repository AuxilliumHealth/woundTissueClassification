package com.auxilliumhealth.woundtissueclassification.Adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.auxilliumhealth.woundtissueclassification.Model.WoundListModel;
import com.auxilliumhealth.woundtissueclassification.R;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.gson.Gson;

import java.util.List;

public class ImageListAdapter extends RecyclerView.Adapter<ImageListAdapter.ViewHolder> {
    private Context mContext;
    private List<WoundListModel.ImagingSession> imagingSessions;
    private OnImageClickListener mListener;

    public interface OnImageClickListener {
        void onImageClick(WoundListModel.ImagingSession session);
    }

    public ImageListAdapter(Context context, List<WoundListModel.ImagingSession> imagingSessions, OnImageClickListener listener) {
        this.mContext = context;
        this.imagingSessions = imagingSessions;
        this.mListener = listener;

        Gson gson = new Gson();
        Log.d("ImageListAdapter", "Sessions: " + gson.toJson(imagingSessions));    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.image_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        WoundListModel.ImagingSession session = imagingSessions.get(position);

        // Clean URL (fix spaces)
        String imageUrl = session.getImageUrl().replace(" ", "");




        Glide.with(mContext)
                .load(imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                .centerCrop()
                .placeholder(R.drawable.image_placeholder)
                .into(holder.woundImage);
        holder.itemView.setOnClickListener(v -> {
            if (mListener != null) mListener.onImageClick(session);
        });
    }

    @Override
    public int getItemCount() {
        return imagingSessions != null ? imagingSessions.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView woundImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            woundImage = itemView.findViewById(R.id.woundImage);
        }
    }
}

