package com.example.android.cardreader;

import com.example.android.Utils.Utils;
import com.example.android.common.logger.Log;

import java.util.ArrayList;
import java.util.Arrays;

public class APDUTranslator {
    final static String TAG = "APDUTranslator";
    private static ArrayList<byte[]> feedbackDataSet = new ArrayList<>();

    protected static ArrayList<byte[]> rapduResp(byte[] rapdu) {
        byte[] state;
        byte[] payload;
        feedbackDataSet.clear();
        Log.d(TAG, "hsuj result: " + Utils.byte2hex(rapdu));
        // If AID is successfully selected, 0x9000 is returned as the status word (last 2
        // bytes of the result) by convention. Everything before the status word is
        // optional payload, which is used here to hold the account number.
        if(rapdu.length <= 0){
            return feedbackDataSet;
        }
        int resultLength = rapdu.length;
        state = new byte[]{rapdu[resultLength - 2], rapdu[resultLength - 1]};
        payload = Arrays.copyOf(rapdu, resultLength - 2);
        feedbackDataSet.add(state);
        if(payload != null){
            feedbackDataSet.add(payload);
        }
        return feedbackDataSet;
    }


}
