package com.example.aznagy.paintproplayer.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

public class AudioProgress implements Parcelable, Serializable {

    private static final long serialVersionUID = -6336199580348994168L;

    private int index;
    private int duration;
    private int progress;


    public AudioProgress(int index, int duration, int progress) {
        this.index = index;
        this.duration = duration;
        this.progress = progress;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    @Override
    public int describeContents() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeSerializable(this);

    }

    public static final Parcelable.Creator<AudioProgress> CREATOR = new Parcelable.Creator<AudioProgress>() {

        public AudioProgress createFromParcel(Parcel in) {
            return (AudioProgress) in.readSerializable();
        }

        public AudioProgress[] newArray(int size) {
            return new AudioProgress[size];
        }

    };


}
