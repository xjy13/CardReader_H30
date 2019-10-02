package com.example.android.cardreader;

import android.content.Context;

import com.example.android.RAPDUApi.RapduInterface;
import com.example.android.Utils.Utils;
import com.example.android.common.logger.Log;

import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

public class RapduImplement implements RapduInterface {
    private String TAG = "RapduImplement";
    private static final String SAMPLE_TEST_AID = "E000000000";
    private static final String SAMPLE_TEST_AID_2 = "F111111111";
    private static final byte[] SELECT_OK_SW = {(byte) 0x90, (byte) 0x00};
    private static WeakReference<Context> mContext = new WeakReference<>(null);
    private static RapduImplement instance = null;
    private ArrayList<byte[]> list = new ArrayList<>();

    public static RapduImplement getInstance(Context context) {
        if ((mContext.get() == null) || instance == null) {
            instance = new RapduImplement(context);
        }

        return instance;
    }

    private RapduImplement(Context context) {
        mContext = new WeakReference<>(context);
    }

    //  byte[] rapdu = new byte[0];

    @Override
    public ArrayList<byte[]> callPunchStatusData() {
        if (list.size() != 0) {
            list.clear();
        }
        APDUExecutor.apdu(SAMPLE_TEST_AID, new ApduCallback() {
            @Override
            public void onDone(byte[] result) {
                if (result == null) {
                    Log.w(TAG, "rapdu no data: ");
                }
                byte[] statusWord = APDUTranslator.rapduResp(result).get(0);
                byte[] payload = APDUTranslator.rapduResp(result).get(1);

                list.add(statusWord);
                list.add(payload);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
            }
        });
        // If AID is successfully selected, 0x9000 is returned as the status word (last 2
        // bytes of the result) by convention. Everything before the status word is
        // optional payload, which is used here to hold the account number.
        return list;
    }

    @Override
    public ArrayList<byte[]> callStaffID() {
        //  list.clear();
        if (list.size() != 0) {
            list.clear();
        }
        APDUExecutor.apdu(SAMPLE_TEST_AID_2, new ApduCallback() {

            @Override
            public void onDone(byte[] result) {
                if (APDUTranslator.rapduResp(result).isEmpty()) {
                    Log.w(TAG, "rapdu no data: ");
                }
                byte[] statusWord = APDUTranslator.rapduResp(result).get(0);
                byte[] payload = APDUTranslator.rapduResp(result).get(1);
                list.add(statusWord);
                list.add(payload);
//                    testDisplayResult(statusWord, payload, 1);
//                    mVibrator.vibrate(300);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
            }
        });
        return list;
    }

    public synchronized void testDisplayResult(byte[] rapduState, byte[] payload, int type) {
        if (Arrays.equals(SELECT_OK_SW, rapduState)) {
            // The remote NFC device will immediately respond with its stored account number
            String payloadData = new String(payload, StandardCharsets.UTF_8);
            Log.i(TAG, "Received: " + payloadData);
            // Inform CardReaderFragment of received account number
            //  mAccountCallback.get().onAccountReceived(payloadData, type);
        } else {
            Log.d(TAG, "not 0x9000 result: 0x" + Utils.byte2hex(rapduState));
            //    mAccountCallback.get().onAccountReceived("0x" + Utils.byte2hex(rapduState) + " -- " + new String(payload, StandardCharsets.UTF_8), type);
        }
    }
}
