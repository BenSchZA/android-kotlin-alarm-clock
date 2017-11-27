package com.roostermornings.android.realm;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.Gson;

import io.realm.RealmObject;

/**
 * Created by bscholtz on 2017/11/27.
 */

public class RoosterMediaItem extends RealmObject {

    public String jsonParcel = "";

    public String getJsonParcel() {
        return jsonParcel;
    }

    public void setJsonParcel(String jsonParcel) {
        this.jsonParcel = jsonParcel;
    }
}
