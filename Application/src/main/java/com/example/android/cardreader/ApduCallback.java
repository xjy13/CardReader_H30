package com.example.android.cardreader;

public interface ApduCallback {
    void onDone(byte[] result);

    void onError(Exception e);
}
