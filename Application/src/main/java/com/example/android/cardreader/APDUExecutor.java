package com.example.android.cardreader;

import android.nfc.tech.IsoDep;

import com.example.android.Utils.Utils;
import com.example.android.common.logger.Log;

import java.io.IOException;

import static com.example.android.cardreader.LoyaltyCardReader.BuildSelectApdu;

public class APDUExecutor {
    private static final String TAG = "APDUExecutor";
    private static IsoDep isoDep;
    APDUExecutor(IsoDep isoDep){
        APDUExecutor.isoDep = isoDep;
    }

    static synchronized void apdu(String aid, ApduCallback apduCallback) {
        byte[] command = BuildSelectApdu(aid);
        byte[] result = new byte[0];
        // Send command to remote device
        Log.i(TAG, "Sending: " + Utils.byte2hexForLog(command));
        try {
           result = isoDep.transceive(command);
            apduCallback.onDone(result);
        } catch (IOException e) {
            apduCallback.onError(e);
            e.printStackTrace();
        }
    }
}
