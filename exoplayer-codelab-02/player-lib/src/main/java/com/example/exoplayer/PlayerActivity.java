/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.exoplayer;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.source.ClippingMediaSource;
import com.google.android.exoplayer2.source.LoopingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.dash.DashChunkSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.EventLogger;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

/**
 * A fullscreen activity to play audio or video streams.
 */
public class PlayerActivity extends AppCompatActivity {

    public static final PlaybackParameters HALF_PLAYBACK_PARAMETERS = new PlaybackParameters(0.5F, 1F);
    public static final PlaybackParameters DOUBLE_PLAYBACK_PARAMETERS = new PlaybackParameters(2F, 1F);
    private final String TAG = getClass().getSimpleName();
    // bandwidth meter to measure and estimate bandwidth
    private final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter
            .Builder(this).build();

    private SimpleExoPlayer player;
    private PlayerView playerView;

    private long playbackPosition;
    private int currentWindow;
    private boolean playWhenReady = true;

    private ComponentListener componentListener;
    private MyVideoRendererEventListener videoRendererEventListener;
    private MyAudioRendererEventListener audioRendererEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        playerView = findViewById(R.id.video_view);
        componentListener = new ComponentListener();
        videoRendererEventListener = new MyVideoRendererEventListener();
        audioRendererEventListener = new MyAudioRendererEventListener();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (Util.SDK_INT > 23) {
            initializePlayer();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //hideSystemUi();
        if ((Util.SDK_INT <= 23 || player == null)) {
            initializePlayer();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (Util.SDK_INT <= 23) {
            releasePlayer();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (Util.SDK_INT > 23) {
            releasePlayer();
        }
    }

    private void initializePlayer() {
        if (player == null) {

            player = new SimpleExoPlayer.Builder(this).build();

            playerView.setPlayer(player);
            player.setPlayWhenReady(playWhenReady);
            player.seekTo(currentWindow, playbackPosition);
            player.addListener(componentListener);
            player.addAnalyticsListener(new EventLogger(new DefaultTrackSelector(this)));
            player.addVideoDebugListener(videoRendererEventListener);
            player.addAudioDebugListener(audioRendererEventListener);
        }
        //MediaSource mediaSource = buildMediaSource(Uri.parse(getString(R.string.media_url_dash)));
        //MediaSource mediaSource = buildMP4MediaSource(Uri.parse(getString(R.string.media_url_mp4_another)));
        //MediaSource mediaSource = buildClippingMediaSource(Uri.parse(getString(R.string.media_url_mp4_another)));
        MediaSource mediaSource = buildLoopMediaSource(Uri.parse(getString(R.string.media_url_mp4_another)));
        player.prepare(mediaSource, true, false);
    }

    private void releasePlayer() {
        if (player != null) {
            player.removeListener(componentListener);
            player.removeVideoDebugListener(videoRendererEventListener);
            player.removeAudioDebugListener(audioRendererEventListener);
            playbackPosition = player.getCurrentPosition();
            currentWindow = player.getCurrentWindowIndex();
            playWhenReady = player.getPlayWhenReady();
            player.release();
            player = null;
        }
    }

    /**
     * 播放DASH视频
     *
     * @param uri DASH类型的uri
     * @return
     */
    private MediaSource buildDASHMediaSource(Uri uri) {
        DashChunkSource.Factory dashChunkSourceFactory = new DefaultDashChunkSource.Factory(
                new DefaultHttpDataSourceFactory("ua", BANDWIDTH_METER));
        DataSource.Factory manifestDataSourceFactory = new DefaultHttpDataSourceFactory("ua");
        DashMediaSource dashMediaSource = new DashMediaSource.Factory(dashChunkSourceFactory, manifestDataSourceFactory).
                createMediaSource(uri);
        return dashMediaSource;
    }


    /**
     * @param uri MP3类型uri
     * @return
     */
    /*private MediaSource buildMediaSource(Uri uri) {
        return new ExtractorMediaSource.Factory(
                new DefaultHttpDataSourceFactory("exoplayer-codelab")).
                createMediaSource(uri);
    }*/

    /**
     * @param uri MP4类型uri
     * @return
     */
    private MediaSource buildMP4MediaSource(Uri uri) {

        // Produces DataSource instances through which media data is loaded.
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this,
                Util.getUserAgent(this, "exoplayer-codelab-2"));
        MediaSource videoSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(uri);

        return videoSource;
    }

    /**
     * 只播放视频的一部分 比如从第10秒到30秒的部分
     *
     * @param uri 常规类型媒体文件的uri 比如MP4
     * @return
     */
    private MediaSource buildClippingMediaSource(Uri uri) {
        MediaSource mp4MediaSource = new ProgressiveMediaSource.Factory(
                new DefaultHttpDataSourceFactory(
                        Util.getUserAgent(this, "exoplayer-codelab-2")
                )
        ).createMediaSource(uri);
        //单位是微秒
        ClippingMediaSource clippingMediaSource = new ClippingMediaSource(mp4MediaSource,
                10_000_000, 30_000_000);
        return clippingMediaSource;
    }

    /**
     * 循环播放视频
     * <p>
     * 对于无限循环的情况，请使用ExoPlayer.setRepeatMode而不是LoopingMediaSource。
     *
     * @param uri 常规类型媒体文件的uri 比如MP4
     * @return
     */
    private MediaSource buildLoopMediaSource(Uri uri) {
        MediaSource mp4MediaSource = new ProgressiveMediaSource.Factory(
                new DefaultHttpDataSourceFactory(
                        Util.getUserAgent(this, "exoplayer-codelab-2")
                )
        ).createMediaSource(uri);
        //单位是微秒
        ClippingMediaSource clippingMediaSource = new ClippingMediaSource(mp4MediaSource,
                10_000_000, 30_000_000);

        LoopingMediaSource loopingMediaSource = new LoopingMediaSource(clippingMediaSource, 2);
        return loopingMediaSource;
    }

    @SuppressLint("InlinedApi")
    private void hideSystemUi() {
        playerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    public void adjustSpeed(View view) {
        int id = view.getId();
        if (id == R.id.btn_half_speed) {
            player.setPlaybackParameters(HALF_PLAYBACK_PARAMETERS);
        } else if (id == R.id.btn_normal_speed) {
            player.setPlaybackParameters(PlaybackParameters.DEFAULT);
        } else if (id == R.id.btn_double_speed) {
            player.setPlaybackParameters(DOUBLE_PLAYBACK_PARAMETERS);
        }
    }

    /**
     * 监听播放状态
     */
    private class ComponentListener implements Player.EventListener {

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            String stateString;
            // actually playing media
            if (playWhenReady && playbackState == Player.STATE_READY) {
                Log.d(TAG, "onPlayerStateChanged: actually playing media");
            }
            switch (playbackState) {
                case Player.STATE_IDLE:
                    stateString = "ExoPlayer.STATE_IDLE      -";
                    break;
                case Player.STATE_BUFFERING:
                    stateString = "ExoPlayer.STATE_BUFFERING -";
                    break;
                case Player.STATE_READY:
                    stateString = "ExoPlayer.STATE_READY     -";
                    break;
                case Player.STATE_ENDED:
                    stateString = "ExoPlayer.STATE_ENDED     -";
                    break;
                default:
                    stateString = "UNKNOWN_STATE             -";
                    break;
            }
            Log.d(TAG, "changed state to " + stateString + " playWhenReady: " + playWhenReady);
        }
    }

    private class MyVideoRendererEventListener implements VideoRendererEventListener {
        @Override
        public void onVideoEnabled(DecoderCounters counters) {

        }

        @Override
        public void onVideoDecoderInitialized(String decoderName, long initializedTimestampMs, long initializationDurationMs) {

        }

        @Override
        public void onVideoInputFormatChanged(Format format) {

        }

        @Override
        public void onDroppedFrames(int count, long elapsedMs) {

        }

        @Override
        public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {

        }

        @Override
        public void onRenderedFirstFrame(Surface surface) {

        }

        @Override
        public void onVideoDisabled(DecoderCounters counters) {

        }
    }

    private class MyAudioRendererEventListener implements AudioRendererEventListener {

        @Override
        public void onAudioEnabled(DecoderCounters counters) {

        }

        @Override
        public void onAudioSessionId(int audioSessionId) {

        }

        @Override
        public void onAudioDecoderInitialized(String decoderName, long initializedTimestampMs, long initializationDurationMs) {

        }

        @Override
        public void onAudioInputFormatChanged(Format format) {

        }

        @Override
        public void onAudioSinkUnderrun(int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {

        }

        @Override
        public void onAudioDisabled(DecoderCounters counters) {

        }
    }
}
