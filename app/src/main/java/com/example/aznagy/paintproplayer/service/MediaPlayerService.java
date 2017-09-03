package com.example.aznagy.paintproplayer.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.session.MediaSessionManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.example.aznagy.paintproplayer.DAO.AudioListStorage;
import com.example.aznagy.paintproplayer.R;
import com.example.aznagy.paintproplayer.controller.MyMediaRemote;
import com.example.aznagy.paintproplayer.enums.PlaybackStatus;
import com.example.aznagy.paintproplayer.model.Audio;
import com.example.aznagy.paintproplayer.model.PlayList;

import static com.example.aznagy.paintproplayer.R.drawable.image4;


public class MediaPlayerService extends Service {

    private static String LOG_TAG = "MediaPlayerService";

    public static final String INTENT_PLAY_AUDIO = "INTENT_PLAY_AUDIO";

    public static final String ACTION_PLAY = "PLAYER_ACTION_PLAY";
    public static final String ACTION_PAUSE = "PLAYER_ACTION_PAUSE";
    public static final String ACTION_PREVIOUS = "PLAYER_ACTION_PREVIOUS";
    public static final String ACTION_NEXT = "PLAYER_ACTION_NEXT";
    public static final String ACTION_STOP = "PLAYER_ACTION_STOP";


    //MediaSession
    private MediaSessionManager mediaSessionManager;
    private MediaSessionCompat mediaSession;
    private MediaControllerCompat.TransportControls transportControls;


    private static final int NOTIFICATION_ID = 101;

    private MyMediaRemote mediaRemote;

    // Binder given to clients
    private final IBinder iBinder = new LocalBinder();

    //Handle incoming phone calls
    private boolean ongoingCall = false;
    private PhoneStateListener phoneStateListener;
    private TelephonyManager telephonyManager;
    private PlayList playList = null;

    public MediaPlayerService() {

    }

    @Override
    public void onCreate() {
        Log.d(LOG_TAG, "onCreate");
        AudioListStorage storage = new AudioListStorage(getApplicationContext());
        this.playList = new PlayList(storage.loadAudioList(), storage.loadAudioIndex());
        mediaRemote = new MyMediaRemote(this, playList);
        registerCallStateListener();
        registerBecomingNoisyReceiver();
        registerPlayAudioReceiver();
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "onStartCommand");

        if (!mediaRemote.requestAudioFocus())
            stopSelf();

        if (mediaSessionManager == null) {
            try {
                initMediaSession();
            } catch (RemoteException e) {
                e.printStackTrace();
                stopSelf();
            }
        }

        if (intent != null && INTENT_PLAY_AUDIO.equals(intent.getAction())) {
            mediaRemote.playSelected();
            buildNotification(PlaybackStatus.PLAYING);
        }



        handleIncomingTransportControlActions(intent);
        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "onDestroy");
        mediaRemote.cleanUp();
        mediaRemote.removeAudioFocus();
        if (phoneStateListener != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
        removeNotification();
        unregisterReceiver(becomingNoisyReceiver);
        unregisterReceiver(playNewAudio);
        new AudioListStorage(getApplicationContext()).clearAudiolist();
        super.onDestroy();
    }


    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(LOG_TAG, "onUnbind");
        mediaSession.release();
        removeNotification();
        return super.onUnbind(intent);
    }


    @Override
    public void onRebind(Intent intent) {
        Log.d(LOG_TAG, "onRebind");
        // TODO Auto-generated method stub
        super.onRebind(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(LOG_TAG, "onBind");
        return iBinder;
    }


    public void nextClicked() {
        Log.d(LOG_TAG, "nextClicked");
        if (this.mediaRemote != null) {
            this.mediaRemote.skipToNext();
        }
        buildNotification(PlaybackStatus.PLAYING);
        updateMetaData();
    }


    public void pauseClicked() {
        Log.d(LOG_TAG, "pauseClicked");
        if (this.mediaRemote != null) {
            this.mediaRemote.pauseMedia();
        }
        buildNotification(PlaybackStatus.PAUSED);
    }

    public void previousClicked() {
        Log.d(LOG_TAG, "previousClicked");
        if (this.mediaRemote != null) {
            this.mediaRemote.skipToPrevious();
        }
        buildNotification(PlaybackStatus.PLAYING);
        updateMetaData();
    }

    public void resumeClicked() {
        Log.d(LOG_TAG, "resumeClicked");
        if (this.mediaRemote != null) {
            this.mediaRemote.resumeMedia();
        }
        buildNotification(PlaybackStatus.PLAYING);
    }

    public void seekTo(int progress) {
        Log.d(LOG_TAG, "seekTo");
        if (this.mediaRemote != null) {
            this.mediaRemote.seekTo(progress);
        }
        //buildNotification(PlaybackStatus.PLAYING);

    }

    public PlayList getPlaylist() {
        return this.playList;
    }

    public boolean isPlaying() {
        return mediaRemote.isPlaying();
    }

    public class LocalBinder extends Binder {
        public MediaPlayerService getService() {
            Log.d(LOG_TAG, "LocalBinder-getService");
            return MediaPlayerService.this;
        }
    }


    //Becoming noisy
    private BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(LOG_TAG, "becomingNoisyReceiver-onReceive");
            mediaRemote.pauseMedia();
            buildNotification(PlaybackStatus.PAUSED);
        }
    };

    private void registerBecomingNoisyReceiver() {
        Log.d(LOG_TAG, "registerBecomingNoisyReceiver");
        //register after getting audio focus
        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(becomingNoisyReceiver, intentFilter);
    }


    private void registerCallStateListener() {
        Log.d(LOG_TAG, "registerCallStateListener");
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                Log.d(LOG_TAG, "onCallStateChanged");
                switch (state) {
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                    case TelephonyManager.CALL_STATE_RINGING:
                        mediaRemote.pauseMedia();
                        ongoingCall = true;
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        if (ongoingCall) {
                            ongoingCall = false;
                            mediaRemote.resumeMedia();
                        }
                        break;
                }
            }
        };
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }


    private BroadcastReceiver playNewAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(LOG_TAG, "playNewAudio-onReceive");
            playList.setAudioIndex(new AudioListStorage(getApplicationContext()).loadAudioIndex());
            mediaRemote.playSelected();
            updateMetaData();
            buildNotification(PlaybackStatus.PLAYING);
        }
    };

    private void registerPlayAudioReceiver() {
        Log.d(LOG_TAG, "registerPlayAudioReceiver");
        //Register playNewMedia receiver
        IntentFilter filter = new IntentFilter(INTENT_PLAY_AUDIO);
        registerReceiver(playNewAudio, filter);
    }

    public void buildNotification(PlaybackStatus playbackStatus) {
        Log.d(LOG_TAG, "buildNotification");
        int notificationAction = android.R.drawable.ic_media_pause;//needs to be initialized
        PendingIntent playPausePendingIntent = null;

        //Build a new notification according to the current state of the MediaPlayer
        if (playbackStatus == PlaybackStatus.PLAYING) {
            notificationAction = android.R.drawable.ic_media_pause;
            //create the pause action
            playPausePendingIntent = playbackAction(1);
        } else if (playbackStatus == PlaybackStatus.PAUSED) {
            notificationAction = android.R.drawable.ic_media_play;
            //create the play action
            playPausePendingIntent = playbackAction(0);
        }

        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);

        // Create a new Notification
        NotificationCompat.Builder notificationBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                .setShowWhen(false)
                .setStyle(new NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2))
                .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .setLargeIcon(largeIcon)
                .setSmallIcon(android.R.drawable.stat_sys_headset)
                .setContentText(playList.getActiveAudio().getArtist())
                .setContentTitle(playList.getActiveAudio().getTitle())
                .setContentInfo(playList.getActiveAudio().getAlbum())
                .addAction(android.R.drawable.ic_media_previous, getString(R.string.previous), playbackAction(3))
                .addAction(notificationAction, getString(R.string.pause), playPausePendingIntent)
                .addAction(android.R.drawable.ic_media_next, getString(R.string.next), playbackAction(2));

        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    public void removeNotification() {
        Log.d(LOG_TAG, "removeNotification");
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }


    private PendingIntent playbackAction(int actionNumber) {
        Log.d(LOG_TAG, "playbackAction " + actionNumber);
        Intent playbackActionIntent = new Intent(this, MediaPlayerService.class);
        switch (actionNumber) {
            case 0:
                // Play
                playbackActionIntent.setAction(ACTION_PLAY);
                return PendingIntent.getService(this, actionNumber, playbackActionIntent, 0);
            case 1:
                // Pause
                playbackActionIntent.setAction(ACTION_PAUSE);
                return PendingIntent.getService(this, actionNumber, playbackActionIntent, 0);
            case 2:
                // Next track
                playbackActionIntent.setAction(ACTION_NEXT);
                return PendingIntent.getService(this, actionNumber, playbackActionIntent, 0);
            case 3:
                // Previous track
                playbackActionIntent.setAction(ACTION_PREVIOUS);
                return PendingIntent.getService(this, actionNumber, playbackActionIntent, 0);
            default:
                break;
        }
        return null;
    }

    private void handleIncomingTransportControlActions(Intent playbackAction) {
        Log.d(LOG_TAG, "handleIncomingTransportControlActions");
        if (playbackAction == null || playbackAction.getAction() == null) return;

        String actionString = playbackAction.getAction();
        if (actionString.equalsIgnoreCase(ACTION_PLAY)) {
            transportControls.play();
        } else if (actionString.equalsIgnoreCase(ACTION_PAUSE)) {
            transportControls.pause();
        } else if (actionString.equalsIgnoreCase(ACTION_NEXT)) {
            transportControls.skipToNext();
        } else if (actionString.equalsIgnoreCase(ACTION_PREVIOUS)) {
            transportControls.skipToPrevious();
        } else if (actionString.equalsIgnoreCase(ACTION_STOP)) {
            transportControls.stop();
        }
    }

    private void initMediaSession() throws RemoteException {
        Log.d(LOG_TAG, "initMediaSession");
        if (mediaSessionManager != null) return; //mediaSessionManager exists

        mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        mediaSession = new MediaSessionCompat(getApplicationContext(), "AudioPlayer");
        transportControls = mediaSession.getController().getTransportControls();
        //set MediaSession -> ready to receive media commands
        mediaSession.setActive(true);
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        updateMetaData();

        // Attach Callback to receive MediaSession updates
        mediaSession.setCallback(new MediaSessionCompat.Callback() {

            @Override
            public void onPlay() {
                Log.d(LOG_TAG, "mediaSession-onPlay");
                super.onPlay();
                resumeClicked();
            }

            @Override
            public void onPause() {
                Log.d(LOG_TAG, "mediaSession-onPause");
                super.onPause();
                pauseClicked();
            }

            @Override
            public void onSkipToNext() {
                Log.d(LOG_TAG, "mediaSession-onSkipToNext");
                super.onSkipToNext();
                nextClicked();
            }

            @Override
            public void onSkipToPrevious() {
                Log.d(LOG_TAG, "mediaSession-onSkipToPrevious");
                super.onSkipToPrevious();
                previousClicked();
            }

            @Override
            public void onStop() {
                Log.d(LOG_TAG, "mediaSession-onStop");
                super.onStop();
                removeNotification();
                //Stop the service
                stopSelf();
            }

            @Override
            public void onSeekTo(long position) {
                super.onSeekTo(position);
            }
        });
    }

    private void updateMetaData() {
        Log.d(LOG_TAG, "updateMetaData");
        Bitmap albumArt = BitmapFactory.decodeResource(getResources(), R.drawable.image4); //replace with medias albumArt
        // Update the current metadata
        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, playList.getActiveAudio().getArtist())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, playList.getActiveAudio().getAlbum())
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, playList.getActiveAudio().getTitle())
                .build());
    }


}
