package com.ashaevy.syncsound;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveVideoTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.FileDataSourceFactory;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private SimpleExoPlayer[] mMediaPlayers = new SimpleExoPlayer[3];
    private Handler mMainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mMainHandler = new Handler();

        mMediaPlayers[0] = createExoPlayer("music/moonlight_dance.mp3");
        mMediaPlayers[1] = createExoPlayer("music/hurricane.mp3");
        mMediaPlayers[2] = createExoPlayer("music/beep.mp3");

        findViewById(R.id.start1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                invertPlayerState(mMediaPlayers[0]);
            }
        });

        findViewById(R.id.start2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                invertPlayerState(mMediaPlayers[1]);
            }
        });

        findViewById(R.id.start3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                invertPlayerState(mMediaPlayers[2]);
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (SimpleExoPlayer player: mMediaPlayers) {
            player.release();
        }
    }

    private SimpleExoPlayer createExoPlayer(String fileName) {
        // 1. Create a default TrackSelector
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveVideoTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector =
                new DefaultTrackSelector(videoTrackSelectionFactory);

        // 2. Create a default LoadControl
        LoadControl loadControl = new DefaultLoadControl();

        // 3. Create the player
        SimpleExoPlayer player =
                ExoPlayerFactory.newSimpleInstance(this, trackSelector, loadControl);

        // 4. Create a FileDataSourceFactory
        FileDataSourceFactory fileDataSourceFactory = new FileDataSourceFactory(bandwidthMeter);

        // 5. Create a ExtractorMediaSource
        String completePath = Environment.getExternalStoragePublicDirectory(Environment.
                DIRECTORY_DOWNLOADS) + "/" + fileName;
        ExtractorMediaSource extractorMediaSource = new ExtractorMediaSource(Uri.fromFile(new
                File(completePath)), fileDataSourceFactory, new DefaultExtractorsFactory(),
                mMainHandler, new ExtractorMediaSource.EventListener() {
            @Override
            public void onLoadError(IOException error) {
                Log.e(TAG, "Loading error.", error);
            }
        });

        player.prepare(extractorMediaSource);

        return player;
    }

    private void invertPlayerState(SimpleExoPlayer mediaPlayer) {
        if (!mediaPlayer.getPlayWhenReady()) {
            mediaPlayer.setPlayWhenReady(true);
        } else {
            mediaPlayer.setPlayWhenReady(false);
        }

    }

}
