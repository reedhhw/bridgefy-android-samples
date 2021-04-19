package com.bridgefy.samples.alerts.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public class Alert implements Parcelable {

    private String id;
    private int count;
    private String name;
    private long date;


    public Alert(String id, int count, String name, long date) {
        this.id = id;
        this.count = count;
        this.name = name;
        this.date = date;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeInt(this.count);
        dest.writeString(this.name);
        dest.writeLong(this.date);
    }

    protected Alert(Parcel in) {
        this.id = in.readString();
        this.count = in.readInt();
        this.name = in.readString();
        this.date = in.readLong();
    }


    public static final Parcelable.Creator<Alert> CREATOR = new Parcelable.Creator<Alert>() {
        @Override
        public Alert createFromParcel(Parcel source) {
            return new Alert(source);
        }

        @Override
        public Alert[] newArray(int size) {
            return new Alert[size];
        }
    };

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return id + " " + count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Alert)) return false;
        Alert alert = (Alert) o;
        return getDate() == alert.getDate();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDate());
    }
}

