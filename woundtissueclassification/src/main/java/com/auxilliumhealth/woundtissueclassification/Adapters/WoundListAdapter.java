package com.auxilliumhealth.woundtissueclassification.Adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.auxilliumhealth.woundtissueclassification.Model.WoundListModel;
import com.auxilliumhealth.woundtissueclassification.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WoundListAdapter extends RecyclerView.Adapter<WoundListAdapter.WoundViewHolder> {

    private List<WoundListModel.Datum> woundList;
    private OnWoundItemClickListener listener;

    public interface OnWoundItemClickListener {
        void onWoundItemClick(WoundListModel.Datum wound);
        void onSessionItemClick(WoundListModel.ImagingSession session);
    }

    public WoundListAdapter(List<WoundListModel.Datum> woundList, OnWoundItemClickListener listener) {
        this.woundList = woundList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public WoundViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_wound, parent, false);
        return new WoundViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WoundViewHolder holder, int position) {
        WoundListModel.Datum wound = woundList.get(position);
        holder.bind(wound, listener);
    }

    @Override
    public int getItemCount() {
        return woundList.size();
    }

    static class WoundViewHolder extends RecyclerView.ViewHolder {
        private final TextView woundLabel;
        private final RecyclerView recyclerView;

        public WoundViewHolder(@NonNull View itemView) {
            super(itemView);
            woundLabel = itemView.findViewById(R.id.woundLabel);
            recyclerView = itemView.findViewById(R.id.imageRecyclerView);
        }

        public void bind(WoundListModel.Datum wound, OnWoundItemClickListener listener) {
            // Set wound label
            String label = wound.getWoundLocation() != null ? wound.getWoundLocation()
                   : "Wound Image";
            woundLabel.setText(formatText(label));

            // Setup sessions RecyclerView if available
            if (wound.getImagingSessions() != null && !wound.getImagingSessions().isEmpty()) {
                if (recyclerView.getLayoutManager() == null) {
                    // Horizontal scrolling list

                    GridLayoutManager layoutManager = new GridLayoutManager(itemView.getContext(), 3);

                    recyclerView.setLayoutManager(layoutManager);
                }

                ImageListAdapter adapter = new ImageListAdapter(
                        itemView.getContext(),
                        wound.getImagingSessions(),
                        session -> {
                            if (listener != null) {
                                listener.onSessionItemClick(session);
                            }
                        }
                );
                recyclerView.setAdapter(adapter);
                recyclerView.setVisibility(View.VISIBLE);

                Log.d("WoundListAdapter", "Setting up recycler with " + wound.getImagingSessions().size() + " images");
            } else {
                recyclerView.setVisibility(View.GONE);
                Log.d("WoundListAdapter", "No imaging sessions available");
            }

            // Set click listener for the whole item
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onWoundItemClick(wound);
            });
        }
        private static String formatText(String inputText) {
            String[] parts = inputText.split("_");

            StringBuilder formattedText = new StringBuilder();
            for (int i = 1; i < parts.length; i++) {
                formattedText.append(parts[i]);
                if (i < parts.length - 1) {
                    formattedText.append(" "); // Add space between words
                }
            }

            return formattedText.toString();
        }

        // Optional: Get the latest session based on date
        private WoundListModel.ImagingSession getLatestSession(List<WoundListModel.ImagingSession> sessions) {
            if (sessions.isEmpty()) return null;

            sessions.sort((s1, s2) -> {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                    Date d1 = sdf.parse(s1.getDateTime());
                    Date d2 = sdf.parse(s2.getDateTime());
                    return d2.compareTo(d1); // Descending: latest first
                } catch (ParseException e) {
                    return 0;
                }
            });

            return sessions.get(0);
        }
    }
}
