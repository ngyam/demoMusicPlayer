package com.example.aznagy.paintproplayer.controller;

import java.io.IOException;
import java.util.ArrayList;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.example.aznagy.paintproplayer.DAO.AudioListStorage;
import com.example.aznagy.paintproplayer.activity.MainActivity;
import com.example.aznagy.paintproplayer.model.Audio;
import com.example.aznagy.paintproplayer.model.AudioProgress;
import com.example.aznagy.paintproplayer.model.PlayList;
import com.example.aznagy.paintproplayer.utils.PlayerUtils;

public class MyMediaRemote implements MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnInfoListener,
        MediaPlayer.OnBufferingUpdateListener,
        AudioManager.OnAudioFocusChangeListener,
        PlayList.PlayListChangedListener {


    private static String LOG_TAG = "MyMediaRemote";


    private MediaPlayer mediaPlayer;
    private AudioManager audioManager;
    private Service service;
    //private Handler handly;
    private Thread updaterThread;

    private PlayList playList;

    private int resumePosition;

    public MyMediaRemote(Service mediaPlayerService, PlayList playList) {
        this.playList = playList;
        this.service = mediaPlayerService;
        this.playList.registerListener(this);
        //this.handly = new Handler();
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        Log.d(LOG_TAG, "onBufferingUpdate");
        // TODO Auto-generated method stub

    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        Log.d(LOG_TAG, "onInfo");
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        Log.d(LOG_TAG, "onSeekComplete");
        // TODO Auto-generated method stub

    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.d(LOG_TAG, "onError");
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                Log.d("MediaPlayer Error", "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Log.d("MediaPlayer Error", "MEDIA ERROR SERVER DIED " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Log.d("MediaPlayer Error", "MEDIA ERROR UNKNOWN " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
                Log.d("MediaPlayer Error", "MEDIA ERROR TIMED OUT " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
                Log.d("MediaPlayer Error", "MEDIA ERROR UNSUPPORTED " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_IO:
                Log.d("MediaPlayer Error", "MEDIA ERROR IO " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_MALFORMED:
                Log.d("MediaPlayer Error", "MEDIA ERROR MALFORMED " + extra);
                break;
            default:
                Log.d("MediaPlayer Error", "default:  " + extra);
        }
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d(LOG_TAG, "onPrepared");
        playMedia();

    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d(LOG_TAG, "onCompletion");
        skipToNext();
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        Log.d(LOG_TAG, "onAudioFocusChange");
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                if (mediaPlayer == null) initMediaPlayer();
                else if (!mediaPlayer.isPlaying()) mediaPlayer.start();
                mediaPlayer.setVolume(1.0f, 1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if (mediaPlayer.isPlaying()) mediaPlayer.pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (mediaPlayer.isPlaying()) mediaPlayer.setVolume(0.1f, 0.1f);
                break;
        }
    }

    public void initMediaPlayer() {
        Log.d(LOG_TAG, "initMediaPlayer");
        mediaPlayer = new MediaPlayer();
        //Set up MediaPlayer event listeners
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setOnInfoListener(this);
        mediaPlayer.reset();

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            // Set the data source to the mediaFile location
            mediaPlayer.setDataSource(playList.getActiveAudio().getData());
        } catch (IOException e) {
            e.printStackTrace();
            service.stopSelf();
        }
        mediaPlayer.prepareAsync();
    }


    public boolean requestAudioFocus() {
        Log.d(LOG_TAG, "requestAudioFocus");
        audioManager = (AudioManager) ((Context) service).getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            return true;
        }
        return false;
    }

    public boolean removeAudioFocus() {
        Log.d(LOG_TAG, "removeAudioFocus");
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED ==
                audioManager.abandonAudioFocus(this);
    }

    private void playMedia() {
        Log.d(LOG_TAG, "playMedia");
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
        Intent intent = new Intent(MainActivity.INTENTACTION_MEDIA_PLAY_STARTED);
        intent.putExtra(MainActivity.EXTRA_AUDIO_UPDATE, (Parcelable) new AudioProgress(playList.getAudioIndex(), mediaPlayer.getDuration(), mediaPlayer.getCurrentPosition()));
        LocalBroadcastManager.getInstance(service).sendBroadcast(intent);
        //handly.postDelayed(new UpdateRunnable(),1000);
        startUpdaterThread();
    }

    public void stopMedia() {
        Log.d(LOG_TAG, "stopMedia");
        if (mediaPlayer == null) return;
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
        stopUpdaterThread();
    }

    public void pauseMedia() {
        Log.d(LOG_TAG, "pauseMedia");
        if (mediaPlayer == null) return;

        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            resumePosition = mediaPlayer.getCurrentPosition();
        }

        Intent intent = new Intent(MainActivity.INTENTACTION_MEDIA_PAUSED);
        LocalBroadcastManager.getInstance(service).sendBroadcast(intent);
        stopUpdaterThread();


    }

    public void resumeMedia() {
        Log.d(LOG_TAG, "resumeMedia");
        if (mediaPlayer == null) return;

        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.seekTo(resumePosition);
            mediaPlayer.start();
        }


        Intent intent = new Intent(MainActivity.INTENTACTION_MEDIA_RESUMED);
        LocalBroadcastManager.getInstance(service).sendBroadcast(intent);

        startUpdaterThread();

    }

    public void playSelected() {
        Log.d(LOG_TAG, "playSelected");

        try {
            playList.getActiveAudio();
        } catch (IndexOutOfBoundsException e) {
            Log.e(LOG_TAG, "playThis-audi index out of range");
            service.stopSelf();
        }

        stopMedia();
        if (mediaPlayer != null)
            mediaPlayer.reset();
        initMediaPlayer();

    }

    public void seekTo(int progress) {
        Log.d(LOG_TAG, "seekTo");
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(PlayerUtils.progressToTimer(progress, mediaPlayer.getDuration()));
        }
    }

    public boolean isPlaying() {
        if(mediaPlayer == null) {
            return false;
        }
        return mediaPlayer.isPlaying();
    }


    public void skipToPrevious() {
        Log.d(LOG_TAG, "skipToPrevious");
        if (playList.getAudioIndex() == 0) {
            //if first in playlist
            //set index to the last of audioList
            playList.setAudioIndex(playList.getPlayListSize() - 1);
        } else {
            //get previous in playlist
            playList.setAudioIndex(playList.getAudioIndex() - 1);
        }

        //Update stored index
        new AudioListStorage(service.getApplicationContext()).storeAudioIndex(playList.getAudioIndex());

        stopMedia();
        //reset mediaPlayer
        mediaPlayer.reset();
        initMediaPlayer();
    }

    public void skipToNext() {
        Log.d(LOG_TAG, "skipToNext");

        if (playList.getAudioIndex() == playList.getPlayListSize() - 1) {
            Log.d(LOG_TAG, "skipToNext-last item");
            //if last in playlist
            playList.setAudioIndex(0);
        } else {
            //get next in playlist
            playList.setAudioIndex(playList.getAudioIndex() + 1);
        }

        //Update stored index
        new AudioListStorage(service.getApplicationContext()).storeAudioIndex(playList.getAudioIndex());

        stopMedia();
        //reset mediaPlayer
        mediaPlayer.reset();
        initMediaPlayer();
    }

    public void cleanUp() {
        Log.d(LOG_TAG, "cleanUp");
        /*
        if (handly != null) {
            handly.removeCallbacksAndMessages(null);
        }
        */
        stopUpdaterThread();
        if (mediaPlayer != null) {
            stopMedia();
            mediaPlayer.release();
        }

    }

    @Override
    public void playListChanged(ArrayList<Audio> audioList) {

    }

    @Override
    public void playListIndexChanged(int index) {
    }

    /*
    private class UpdateRunnable implements Runnable {

        @Override
        public void run() {

            if(mediaPlayer != null) {
                Intent intent = new Intent(MainActivity.INTENTACTION_MEDIA_INFO_UPDATE);
                intent.putExtra(MainActivity.EXTRA_AUDIO_UPDATE, (Parcelable) new AudioProgress(playList.getAudioIndex(), mediaPlayer.getDuration(), mediaPlayer.getCurrentPosition()));
                LocalBroadcastManager.getInstance(service).sendBroadcast(intent);
            }

            if((mediaPlayer != null) && mediaPlayer.isPlaying()) {
                handly.postDelayed(this, 1000);
            }

        }
    }
    */


    private void startUpdaterThread() {
        Log.d(LOG_TAG, "startUpdaterThread");
        if (updaterThread == null || updaterThread.isInterrupted() || !updaterThread.isAlive()) {

            updaterThread = new Thread() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            if (mediaPlayer != null) {
                                Intent intent = new Intent(MainActivity.INTENTACTION_MEDIA_INFO_UPDATE);
                                intent.putExtra(MainActivity.EXTRA_AUDIO_UPDATE, (Parcelable) new AudioProgress(playList.getAudioIndex(), mediaPlayer.getDuration(), mediaPlayer.getCurrentPosition()));
                                LocalBroadcastManager.getInstance(service).sendBroadcast(intent);
                                sleep(1000);
                                //handly.post(this);
                            }
                        }
                    } catch (InterruptedException e) {
                        interrupt();
                        e.printStackTrace();
                        return;
                    }
                }
            };
            updaterThread.setName("updaterThread");
            updaterThread.start();
        }
    }

    private void stopUpdaterThread() {
        Log.d(LOG_TAG, "stopUpdaterThread");
        if (updaterThread != null) {
            updaterThread.interrupt();
        }
    }



}
