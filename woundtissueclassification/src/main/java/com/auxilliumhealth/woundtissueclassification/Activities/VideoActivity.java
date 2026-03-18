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
package com.auxilliumhealth.woundtissueclassification.Activities;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.auxilliumhealth.woundtissueclassification.databinding.ActivityVideoBinding;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;

public class VideoActivity extends AppCompatActivity {
    ExoPlayer exoPlayer;
    ActivityVideoBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVideoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize ExoPlayer
        exoPlayer = new ExoPlayer.Builder(this).build();
        binding.videoPlayerView.setPlayer(exoPlayer);

        // Show progress bar and hide close button initially
        binding.videoProgressBar.setVisibility(View.VISIBLE);
        binding.closeButton.setVisibility(View.GONE);

        // Get the URI of the video from raw resources
        Uri videoUri = Uri.parse("https://woundteleicon.s3.us-east-1.amazonaws.com/calibrationFlow.mp4");

        // Play Video
        MediaItem mediaItem = MediaItem.fromUri(videoUri);
        exoPlayer.setMediaItem(mediaItem);
        exoPlayer.prepare();
        exoPlayer.play();

        // Listen for buffering state changes
        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) {
                    // Video is ready, hide progress bar and show close button
                    binding.videoProgressBar.setVisibility(View.GONE);
                    binding.videoPlayerView.setVisibility(View.VISIBLE);
                    binding.closeButton.setVisibility(View.VISIBLE);
                }
            }
        });

        // Close the activity when the close button is clicked
        binding.closeButton.setOnClickListener(v -> finish());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
    }
}
