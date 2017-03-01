package com.ashaevy.syncsound;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
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
import com.google.android.exoplayer2.ui.PlaybackControlView;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.FileDataSourceFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.DecoderException;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;

public class MainActivity extends AppCompatActivity implements PlaybackControlView.VisibilityListener {

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
//                try {
//                    //mixSound();
//                    AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100,
//                            AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, 44100, AudioTrack.MODE_STREAM);
//
//                    byte[] output = mix(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/music/moonlight_dance.mp3",
//                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/music/beep.mp3");
//
//                    audioTrack.play();
//
//                    audioTrack.write(output, 0, output.length);
//
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
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

    public static byte[] mix(String path1, String path2) throws Exception {
        byte[] pcm1 = decode(path1, 0, 1000);
        byte[] pcm2 = decode(path2, 0, 1000);
        int len1=pcm1.length;
        int len2=pcm2.length;
        byte[] pcmL;
        byte[] pcmS;
        int lenL; // length of the longest
        int lenS; // length of the shortest
        if (len2>len1) {
            lenL = len1;
            pcmL = pcm1;
            lenS = len2;
            pcmS = pcm2;
        } else {
            lenL = len2;
            pcmL = pcm2;
            lenS = len1;
            pcmS = pcm1;
        }
        for (int idx = 0; idx < lenL; idx++) {
            int sample;
            if (idx >= lenS) {
                sample = pcmL[idx];
            } else {
                sample = pcmL[idx] + pcmS[idx];
            }
            sample=(int)(sample*.71);
            if (sample>127) sample=127;
            if (sample<-128) sample=-128;
            pcmL[idx] = (byte) sample;
        }
        return pcmL;
    }

    private void mixSound() throws IOException {
        AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100,
                AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, 44100, AudioTrack.MODE_STREAM);

        InputStream in1=getResources().openRawResource(R.raw.beep);
        InputStream in2=getResources().openRawResource(R.raw.moonlight_dance);

        byte[] music1 = null;
        music1= new byte[in1.available()];
        music1=convertStreamToByteArray(in1);
        in1.close();


        byte[] music2 = null;
        music2= new byte[in2.available()];
        music2=convertStreamToByteArray(in2);
        in2.close();

        byte[] output = new byte[music1.length];

        audioTrack.play();

        for(int i=0; i < output.length; i++){

            float samplef1 = music1[i] / 128.0f;      //     2^7=128
            float samplef2 = music2[i] / 128.0f;


            float mixed = samplef1 + samplef2;
            // reduce the volume a bit:
            mixed *= 0.8;
            // hard clipping
            if (mixed > 1.0f) mixed = 1.0f;

            if (mixed < -1.0f) mixed = -1.0f;

            byte outputSample = (byte)(mixed * 128.0f);
            output[i] = outputSample;

        }   //for loop
        audioTrack.write(output, 0, output.length);

    }

    public static byte[] convertStreamToByteArray(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buff = new byte[10240];
        int i = Integer.MAX_VALUE;
        while ((i = is.read(buff, 0, buff.length)) > 0) {
            baos.write(buff, 0, i);
        }

        return baos.toByteArray(); // be sure to close InputStream in calling function

    }

    public static byte[] decode(String path, int startMs, int maxMs)
            throws Exception {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream(1024);

        float totalMs = 0;
        boolean seeking = true;

        File file = new File(path);
        InputStream inputStream = new BufferedInputStream(new FileInputStream(file), 8 * 1024);
        try {
            Bitstream bitstream = new Bitstream(inputStream);
            Decoder decoder = new Decoder();

            boolean done = false;
            while (! done) {
                Header frameHeader = bitstream.readFrame();
                if (frameHeader == null) {
                    done = true;
                } else {
                    totalMs += frameHeader.ms_per_frame();

                    if (totalMs >= startMs) {
                        seeking = false;
                    }

                    if (! seeking) {
                        SampleBuffer output = (SampleBuffer) decoder.decodeFrame(frameHeader, bitstream);

                        if (output.getSampleFrequency() != 44100
                                || output.getChannelCount() != 2) {
                            throw new Exception("mono or non-44100 MP3 not supported");
                        }

                        short[] pcm = output.getBuffer();
                        for (short s : pcm) {
                            outStream.write(s & 0xff);
                            outStream.write((s >> 8 ) & 0xff);
                        }
                    }

                    if (totalMs >= (startMs + maxMs)) {
                        done = true;
                    }
                }
                bitstream.closeFrame();
            }

            return outStream.toByteArray();
        } catch (BitstreamException e) {
            throw new IOException("Bitstream error: " + e);
        } catch (DecoderException e) {
            Log.w(TAG, "Decoder error", e);
            throw new Exception(e);
        } finally {
            safeClose(inputStream);
        }
    }

    public static void safeClose(Closeable closeable)
    {
        if (closeable != null)
        {
            try
            {
                closeable.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    private MediaPlayer createMediaPlayer(String fileName) {
        try {
            String completePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + fileName;
            MediaPlayer mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setDataSource(completePath);
            mediaPlayer.prepare();
            mediaPlayer.start();
            return mediaPlayer;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void invertPlayerState(SimpleExoPlayer mediaPlayer) {
        if (!mediaPlayer.getPlayWhenReady()) {
            mediaPlayer.setPlayWhenReady(true);
        } else {
            mediaPlayer.setPlayWhenReady(false);
        }

    }

    @Override
    public void onVisibilityChange(int visibility) {

    }
}
