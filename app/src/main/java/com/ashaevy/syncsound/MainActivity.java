package com.ashaevy.syncsound;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

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
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    public static final String FILES_FOLDER_UNDER_DOWNLOADS = "/music/";

    private int mPlayersCount;
    private SongPlayer[] mMediaPlayers;
    private Handler mMainHandler;

    private long mGlobalClock;

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private Runnable updateTimeLine = new Runnable() {
        @Override
        public void run() {
            if (MainActivity.this.isFinishing()) {
                return;
            }

            updateGlobalClock();
            long millis = mGlobalClock;
            String hms = String.format(Locale.getDefault(), "%02d:%02d:%02d",
                    TimeUnit.MILLISECONDS.toHours(millis),
                    TimeUnit.MILLISECONDS.toMinutes(millis) -
                            TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
                    TimeUnit.MILLISECONDS.toSeconds(millis) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
            ((TextView) findViewById(R.id.clock)).setText(hms);
            mMainHandler.postDelayed(updateTimeLine, 1000L);
        }
    };
    public static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();

    private void updateGlobalClock() {
        int playingCount = 0;
        for (int i = 0; i < mMediaPlayers.length; i++) {
            long currentPosition = mMediaPlayers[i].mPlayer.getCurrentPosition();
            if (mMediaPlayers[i].mPlayer.getPlayWhenReady() && currentPosition > 0) {
                mGlobalClock = currentPosition;
                playingCount++;
            }
        }
        ((TextView) findViewById(R.id.active_count)).
                setText(getString(R.string.playing_text, playingCount));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mMainHandler = new Handler();

        // TODO check permissions
        String downloadsPath = Environment.getExternalStoragePublicDirectory(Environment.
                DIRECTORY_DOWNLOADS) + FILES_FOLDER_UNDER_DOWNLOADS;

        File downloads = new File(downloadsPath);
        File[] files = downloads.listFiles();
        mPlayersCount = files.length;

        mMediaPlayers = new SongPlayer[mPlayersCount];

        // TODO do on background
        for (int i = 0; i < mPlayersCount; i++) {
            try {
                mMediaPlayers[i] = new SongPlayer(files[i].getName(),
                        createExoPlayer(files[i].getCanonicalPath()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // specify an adapter (see also next example)
        mAdapter = new MyAdapter(mMediaPlayers);
        mRecyclerView.setAdapter(mAdapter);

        findViewById(R.id.play_all).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mGlobalClock = 0;
                for (int i = 0; i < mPlayersCount; i++) {
                    start(i);
                }
            }
        });

        findViewById(R.id.stop_all).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mGlobalClock = 0;
                for (int i = 0; i < mPlayersCount; i++) {
                    stop(i);
                }
            }
        });

        mMainHandler.postDelayed(updateTimeLine, 1000L);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (SongPlayer player: mMediaPlayers) {
            player.mPlayer.release();
        }
    }

    private SimpleExoPlayer createExoPlayer(String fileName) {
        // 1. Create a default TrackSelector
        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveVideoTrackSelection.Factory(BANDWIDTH_METER);
        TrackSelector trackSelector =
                new DefaultTrackSelector(videoTrackSelectionFactory);

        // 2. Create a default LoadControl
        LoadControl loadControl = new DefaultLoadControl();

        // 3. Create the player
        SimpleExoPlayer player =
                ExoPlayerFactory.newSimpleInstance(this, trackSelector, loadControl);
        ExtractorMediaSource extractorMediaSource = createMediaSource(fileName);

        player.prepare(extractorMediaSource);

        return player;
    }

    @NonNull
    private ExtractorMediaSource createMediaSource(String fileName) {
        // 4. Create a FileDataSourceFactory
        FileDataSourceFactory fileDataSourceFactory = new FileDataSourceFactory(BANDWIDTH_METER);

        return new ExtractorMediaSource(Uri.fromFile(new
                File(fileName)), fileDataSourceFactory, new DefaultExtractorsFactory(),
                mMainHandler, new ExtractorMediaSource.EventListener() {
            @Override
            public void onLoadError(IOException error) {
                Log.e(TAG, "Loading error.", error);
            }
        });
    }

    private void start(int id) {
        SimpleExoPlayer mediaPlayer = mMediaPlayers[id].mPlayer;

        if (!mediaPlayer.getPlayWhenReady()) {
            mediaPlayer.setPlayWhenReady(true);
        }

        if (mediaPlayer.getCurrentPosition() != 0) {
            mediaPlayer.seekTo(0);
        }
    }

    private void stop(int id) {
        SimpleExoPlayer mediaPlayer = mMediaPlayers[id].mPlayer;

        if (mediaPlayer.getPlayWhenReady()) {
            mediaPlayer.setPlayWhenReady(false);
        }
    }

    public static class SongPlayer {
        public String mSongName;
        public SimpleExoPlayer mPlayer;

        public SongPlayer(String mSongName, SimpleExoPlayer mPlayer) {
            this.mSongName = mSongName;
            this.mPlayer = mPlayer;
        }
    }

    public static class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
        private SongPlayer[] mSongPlayers;

        public static class ViewHolder extends RecyclerView.ViewHolder {
            public TextView mSongNameView;
            public Button mPlayButton;
            public Button mPauseButton;

            public ViewHolder(LinearLayout linearLayout) {
                super(linearLayout);
            }
        }

        // Provide a suitable constructor (depends on the kind of dataset)
        public MyAdapter(SongPlayer[] songPlayers) {
            mSongPlayers = songPlayers;
        }

        // Create new views (invoked by the layout manager)
        @Override
        public MyAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                       int viewType) {
            // create a new view
            LinearLayout linearLayout = (LinearLayout) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.song_layout, parent, false);
            final ViewHolder vh = new ViewHolder(linearLayout);
            vh.mSongNameView = ((TextView) linearLayout.findViewById(R.id.song_name));
            vh.mPlayButton = ((Button) linearLayout.findViewById(R.id.play_button));
            vh.mPauseButton = ((Button) linearLayout.findViewById(R.id.pause_button));

            vh.mPlayButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    play(vh.getAdapterPosition());
                }
            });
            vh.mPauseButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    pause(vh.getAdapterPosition());
                }
            });

            return vh;
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.mSongNameView.setText(mSongPlayers[position].mSongName);
            float volume = mSongPlayers[position].mPlayer.getVolume();
            if (volume > 0f) {
                holder.mPlayButton.setEnabled(false);
                holder.mPauseButton.setEnabled(true);
            } else {
                holder.mPlayButton.setEnabled(true);
                holder.mPauseButton.setEnabled(false);
            }
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return mSongPlayers.length;
        }

        private void play(int id) {
            SimpleExoPlayer mediaPlayer = mSongPlayers[id].mPlayer;
            mediaPlayer.setVolume(1.0f);
            notifyItemChanged(id);
        }

        private void pause(int id) {
            SimpleExoPlayer mediaPlayer = mSongPlayers[id].mPlayer;
            mediaPlayer.setVolume(.0f);
            notifyItemChanged(id);
        }

    }

}
