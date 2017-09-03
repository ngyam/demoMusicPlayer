package com.example.aznagy.paintproplayer.model;

import java.util.ArrayList;

public class PlayList {

    private ArrayList<Audio> audioList;
    private int audioIndex = -1;
    private PlayListChangedListener listener;

    public interface PlayListChangedListener {
        void playListChanged(ArrayList<Audio> audioList);

        void playListIndexChanged(int index);
    }

    public PlayList(ArrayList<Audio> audioList, int audioIndex) {
        this.audioList = audioList;
        this.audioIndex = audioIndex;
    }

    public ArrayList<Audio> getAudioList() {
        return audioList;
    }

    public void setAudioList(ArrayList<Audio> audioList) {
        this.audioList = audioList;
        if (listener != null) {
            listener.playListChanged(this.audioList);
        }
    }

    public int getAudioIndex() {
        return audioIndex;
    }

    public void setAudioIndex(int audioIndex) {
        this.audioIndex = audioIndex;
        if (listener != null) {
            listener.playListIndexChanged(this.audioIndex);
        }
    }

    public Audio getActiveAudio() throws IndexOutOfBoundsException {
        return audioList.get(this.audioIndex);
    }

    public int getPlayListSize() {
        return this.audioList.size();
    }

    public void registerListener(PlayListChangedListener listener) {
        this.listener = listener;
    }

    public void removeListener() {
        this.listener = null;
    }


}
