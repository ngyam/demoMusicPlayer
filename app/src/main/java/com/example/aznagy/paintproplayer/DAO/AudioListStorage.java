package com.example.aznagy.paintproplayer.DAO;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.aznagy.paintproplayer.model.Audio;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class AudioListStorage {

    private final String STORAGE = "com.ngyam.paintproplayer.AUDIOSTORAGE";
    private static final String KEY_AUDIOLIST = "KEY_AUDIOLIST";
    private static final String KEY_AUDOINDEX = "KEY_AUDIOINDEX";

    private SharedPreferences preferences;
    private Context context;

    public AudioListStorage(Context context) {
        this.context = context;
    }

    private static final String LOG_TAG = "AudioListStorage";

    public void storeAudioList(ArrayList<Audio> arrayList) {
        Log.d(LOG_TAG, "storeAudioList");
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = preferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(arrayList);
        editor.putString(KEY_AUDIOLIST, json);
        editor.apply();
    }

    public ArrayList<Audio> loadAudioList() {
        Log.d(LOG_TAG, "loadAudioList");
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = preferences.getString(KEY_AUDIOLIST, null);
        Type type = new TypeToken<ArrayList<Audio>>() {
        }.getType();
        return gson.fromJson(json, type);
    }

    public void storeAudioIndex(int index) {
        Log.d(LOG_TAG, "storeAudioIndex");
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(KEY_AUDOINDEX, index);
        editor.apply();
    }

    public int loadAudioIndex() {
        Log.d(LOG_TAG, "loadAudioIndex");
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        return preferences.getInt(KEY_AUDOINDEX, -1);
    }

    public void clearAudiolist() {
        Log.d(LOG_TAG, "clearAudiolist");
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.commit();
    }
}
