package com.example.android.RAPDUApi;

import android.content.Context;

import com.example.android.cardreader.RapduImplement;

import java.util.ArrayList;

public interface RapduInterface {
    class Factory {
        public static RapduInterface create(Context context) {
            return RapduImplement.getInstance(context);
        }
    }

    ArrayList<byte[]> callPunchStatusData();

    ArrayList<byte[]> callStaffID();
}
