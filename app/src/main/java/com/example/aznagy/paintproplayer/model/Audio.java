package com.example.aznagy.paintproplayer.model;

import java.io.Serializable;

import android.os.Parcel;
import android.os.Parcelable;

public class Audio implements Serializable, Parcelable {

    /**
     *
     */
    private static final long serialVersionUID = -6336199580348994968L;

    private String data;
    private String title;
    private String album;
    private String artist;

    public Audio(String data, String title, String album, String artist) {
        this.data = data;
        this.title = title;
        this.album = album;
        this.artist = artist;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
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

    public static final Parcelable.Creator<Audio> CREATOR = new Parcelable.Creator<Audio>() {

        public Audio createFromParcel(Parcel in) {
            return (Audio) in.readSerializable();
        }

        public Audio[] newArray(int size) {
            return new Audio[size];
        }

    };
}
