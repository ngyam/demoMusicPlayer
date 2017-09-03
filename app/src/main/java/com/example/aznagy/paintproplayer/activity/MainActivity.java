package com.example.aznagy.paintproplayer.activity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.aznagy.paintproplayer.DAO.AudioListStorage;
import com.example.aznagy.paintproplayer.R;
import com.example.aznagy.paintproplayer.adapter.CustomTouchListener;
import com.example.aznagy.paintproplayer.adapter.RecyclerViewAdapter;
import com.example.aznagy.paintproplayer.interfaces.ClickListener;
import com.example.aznagy.paintproplayer.model.Audio;
import com.example.aznagy.paintproplayer.model.AudioProgress;
import com.example.aznagy.paintproplayer.model.PlayList;
import com.example.aznagy.paintproplayer.service.MediaPlayerService;
import com.example.aznagy.paintproplayer.utils.PlayerUtils;


import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static String LOG_TAG = "MainActivity";

    private static final String KEY_SERVICESTATE = "KEY_SERVICESTATE";
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 352;
    public static final String INTENTACTION_MEDIA_INFO_UPDATE = "INTENTACTION_MEDIA_INFO_UPDATE";
    public static final String INTENTACTION_MEDIA_PAUSED = "INTENT_MEDIA_PAUSED";
    public static final String INTENTACTION_MEDIA_RESUMED = "INTENT_MEDIA_RESUMED";
    public static final String INTENTACTION_MEDIA_PLAY_STARTED = "INTENT_MEDIA_PLAY_STARTED";
    public static final String EXTRA_AUDIO_UPDATE = "EXTRA_AUDIO_UPDATE";


    private MediaPlayerService playerService;
    boolean serviceBound = false;

    ArrayList<Audio> audioList;

    private RecyclerViewAdapter adapter;
    private LinearLayoutManager llm;
    private TextView timerStart;
    private TextView timerFinish;
    private TextView songTitle;
    private SeekBar seekBar;
    private boolean seekBarUpdateAllowed = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(com.example.aznagy.paintproplayer.R.layout.activity_main2);
        initViews();
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(LOG_TAG, "onCreate-no permission");

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {

            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            }
        } else {
            loadAudio();
            initRecyclerView();
        }


    }

    private void initViews() {
        this.timerStart = (TextView) findViewById(R.id.songCurrentDurationLabel);
        this.timerFinish = (TextView) findViewById(R.id.songTotalDurationLabel);
        this.songTitle = (TextView) findViewById(R.id.songTitle);
        this.seekBar = (SeekBar) findViewById(R.id.songProgressBar);


        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                seekBarUpdateAllowed = true;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                seekBarUpdateAllowed = false;
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (playerService != null && fromUser) {
                    playerService.seekTo(progress);
                }
                seekBarUpdateAllowed = true;
            }
        });

    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onRestoreInstanceState");
        super.onRestoreInstanceState(savedInstanceState);
        serviceBound = savedInstanceState.getBoolean(KEY_SERVICESTATE);

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.d(LOG_TAG, "onSaveInstanceState");
        outState.putBoolean(KEY_SERVICESTATE, serviceBound);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStart() {
        Log.d(LOG_TAG, "onStart");
        super.onStart();
        initViews();
        registerServiceBroadcastListeners();
        if(serviceBound && adapter != null) {
            PlayList p = playerService.getPlaylist();
            adapter.setSelectedIndex(p.getAudioIndex());
            adapter.notifyDataSetChanged();
            if(playerService.isPlaying()) {
                ((Button)findViewById(R.id.btnPlay)).setText(getString(R.string.pause));
            } else {
                ((Button)findViewById(R.id.btnPlay)).setText(getString(R.string.resume));
            }
        }


    }

    private void registerServiceBroadcastListeners() {
        Log.d(LOG_TAG, "registerServiceBroadcastListeners");
        IntentFilter intentFilter = new IntentFilter(INTENTACTION_MEDIA_INFO_UPDATE);
        intentFilter.addAction(INTENTACTION_MEDIA_PAUSED);
        intentFilter.addAction(INTENTACTION_MEDIA_RESUMED);
        intentFilter.addAction(INTENTACTION_MEDIA_PLAY_STARTED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mediaInfoReceiver, intentFilter);
    }


    private BroadcastReceiver mediaInfoReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(LOG_TAG, "mediaInfoReceiver-onReceive");
            String act = intent.getAction();
            if (INTENTACTION_MEDIA_INFO_UPDATE.equals(act)) {
                if (seekBarUpdateAllowed) {
                    Log.d(LOG_TAG, "mediaInfoReceiver-onReceive-allowed");
                    AudioProgress prog = (AudioProgress) intent.getParcelableExtra(MainActivity.EXTRA_AUDIO_UPDATE);
                    seekBar.setProgress(PlayerUtils.getProgressPercentage(prog.getProgress(), prog.getDuration()));
                    timerStart.setText(PlayerUtils.milliSecondsToTimer(prog.getProgress()));
                    timerFinish.setText(PlayerUtils.milliSecondsToCountdownTimer(prog.getDuration(), prog.getProgress()));
                }
            } else if (INTENTACTION_MEDIA_PAUSED.equals(act)) {
                ((Button) findViewById(R.id.btnPlay)).setText(getString(R.string.resume));
            } else if (INTENTACTION_MEDIA_RESUMED.equals(act)) {
                ((Button) findViewById(R.id.btnPlay)).setText(getString(R.string.pause));
            } else if (INTENTACTION_MEDIA_PLAY_STARTED.equals(act)) {

                AudioProgress prog = (AudioProgress) intent.getParcelableExtra(MainActivity.EXTRA_AUDIO_UPDATE);
                int i = prog.getIndex();
                songTitle.setText(audioList.get(i).getTitle());
                ((Button) findViewById(R.id.btnPlay)).setText(getString(R.string.pause));

                if (adapter != null) {
                    adapter.setSelectedIndex(i);
                    adapter.notifyDataSetChanged();
                }

            }
        }
    };

    @Override
    protected void onStop() {
        Log.d(LOG_TAG, "onStop");
        this.seekBar.setOnSeekBarChangeListener(null);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mediaInfoReceiver);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(LOG_TAG, "onDestroy");
        if (serviceBound) {
            unbindService(serviceConnection);
            playerService.stopSelf();
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(LOG_TAG, "onCreateOptionsMenu");
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(com.example.aznagy.paintproplayer.R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(LOG_TAG, "onOptionsItemSelected");
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == com.example.aznagy.paintproplayer.R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void loadAudio() {
        Log.d(LOG_TAG, "loadAudio");

        audioList = new ArrayList<>();
        ContentResolver contentResolver = getContentResolver();

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        Cursor cursor = contentResolver.query(uri, null, selection, null, sortOrder);

        if (cursor != null && cursor.moveToFirst()) {
            while (cursor.moveToNext()) {
                String data = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                audioList.add(new Audio(data, title, album, artist));
            }
        }
        Log.d(LOG_TAG, "loadAudio: audioListsize: " + audioList.size());
        cursor.close();

        AudioListStorage storage = new AudioListStorage(getApplicationContext());
        storage.storeAudioList(audioList);

    }


    private void initRecyclerView() {

        Log.d(LOG_TAG, "initRecyclerView");
        if (audioList.size() > 0) {
            RecyclerView recyclerView = (RecyclerView) findViewById(com.example.aznagy.paintproplayer.R.id.recyclerview);
            adapter = new RecyclerViewAdapter(audioList, getApplication());
            recyclerView.setAdapter(adapter);
            llm = new LinearLayoutManager(this);
            recyclerView.setLayoutManager(llm);
            recyclerView.addOnItemTouchListener(new CustomTouchListener(this, new ClickListener() {

                @Override
                public void onClick(View view, int index) {
                    Log.d(LOG_TAG, "onClick");
                    playAudio(audioList, index);
                    Button b = (Button) findViewById(R.id.btnPlay);
                    b.setText(getString(R.string.pause));
                }
            }));
            Log.d(LOG_TAG, "initRecyclerView: listener set");
        }

    }


    //Binding this Client to the AudioPlayer Service
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(LOG_TAG, "onServiceConnected");
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
            playerService = binder.getService();
            serviceBound = true;
            //Toast.makeText(MainActivity.this, LOG_TAG, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    private boolean playAudio(ArrayList<Audio> audioList, int audioIndex) {
        Log.d(LOG_TAG, "playAudio");
        //Check is service is active

        if(audioList == null || audioList.isEmpty()) {
            Toast.makeText(this,"No music found",Toast.LENGTH_LONG).show();
            return false;
        }

        if (!serviceBound) {
            Log.d(LOG_TAG, "playAudio-service not bound yet");
            AudioListStorage storage = new AudioListStorage(getApplicationContext());
            storage.storeAudioIndex(audioIndex);

            Thread t = new Thread() {
                public void run() {
                    Intent playerIntent = new Intent(MainActivity.this, MediaPlayerService.class);
                    playerIntent.setAction(MediaPlayerService.INTENT_PLAY_AUDIO);
                    startService(playerIntent);
                    bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
                }
            };
            t.start();

        } else {
            Log.d(LOG_TAG, "playAudio-service bound already");
            AudioListStorage storage = new AudioListStorage(getApplicationContext());
            storage.storeAudioIndex(audioIndex);
            Intent broadcastIntent = new Intent(MediaPlayerService.INTENT_PLAY_AUDIO);
            sendBroadcast(broadcastIntent);
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    loadAudio();
                    initRecyclerView();

                } else {
                    //ToDO
                }
                return;
            }
        }
    }

    public void OnNextClick(View view) {
        Log.d(LOG_TAG, "OnNextClick");
        if (playerService != null) {
            playerService.nextClicked();
        }
    }

    public void OnPlayClick(View view) {
        Log.d(LOG_TAG, "OnPlayClick");
        Button b = (Button) view;
        if (b.getText().equals(getString(R.string.resume))) {
            Log.d(LOG_TAG, "OnPlayClick-resume");
            if (playerService != null && serviceBound) {
                playerService.resumeClicked();
                b.setText(R.string.pause);
            }
        } else if (b.getText().equals(getString(R.string.pause))) {
            Log.d(LOG_TAG, "OnPlayClick-pause");
            if (playerService != null && serviceBound) {
                playerService.pauseClicked();
                b.setText(R.string.resume);
            }
        } else if (b.getText().equals(getString(R.string.play))) {
            Log.d(LOG_TAG, "OnPlayClick-play");
            if(playAudio(audioList, 0)) {
                b.setText(R.string.pause);
            }
        }
    }

    public void OnPreviousClick(View view) {
        Log.d(LOG_TAG, "OnPreviousClick");
        if (playerService != null) {
            playerService.previousClicked();
        }

    }


}
