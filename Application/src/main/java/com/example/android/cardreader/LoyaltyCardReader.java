/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.cardreader;

import android.app.Service;
import android.content.Context;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.example.android.Utils.Utils;
import com.example.android.common.logger.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.Semaphore;

/**
 * Callback class, invoked when an NFC card is scanned while the device is running in reader mode.
 * <p>
 * Reader mode can be invoked by calling NfcAdapter
 */
public class LoyaltyCardReader implements NfcAdapter.ReaderCallback {
    private static final String TAG = "LoyaltyCardReader";
    // AID for our loyalty card service.
    private static final String SAMPLE_LOYALTY_CARD_AID = "F222222222"; //F222222222
    private static final String SAMPLE_TEST_AID_2 = "F111111111";
    private static final String SAMPLE_TEST_AID = "E000000000"; //E000000000
    // ISO-DEP command HEADER for selecting an AID.
    // Format: [Class | Instruction | Parameter 1 | Parameter 2]
    private static final String SELECT_APDU_HEADER = "00A40400";
    private static final String UPDATE_APDU_HEADER = "00B40400";
    private static final String TEST_APDU_HEADER = "00C40400";
    // "OK" status word sent in response to SELECT AID command (0x9000)
    private static final byte[] SELECT_OK_SW = {(byte) 0x90, (byte) 0x00};
    private static final String TASK_KEY = "TaskTest";

    // Weak reference to prevent retain loop. mAccountCallback is responsible for exiting
    // foreground mode before it becomes invalid (e.g. during onPause() or onStop()).
    private WeakReference<AccountCallback> mAccountCallback;
    private WeakReference<AccountCallback> mTestData;
    private Semaphore semaphore = new Semaphore(1);
    private static SharedPreferences sprf;
    private static Handler handler = new Handler();
    static Vibrator mVibrator;

    public interface AccountCallback {
        void onAccountReceived(String account, int type);
    }

    public LoyaltyCardReader(AccountCallback accountCallback) {
        mAccountCallback = new WeakReference<AccountCallback>(accountCallback);
    }

    public static void setVibrate(Context context) {
        mVibrator = (Vibrator) context.getSystemService(Service.VIBRATOR_SERVICE);
    }


    /**
     * Callback when a new tag is discovered by the system.
     *
     * <p>Communication with the card should take place here.
     *
     * @param tag Discovered tag
     */
    @Override
    public void onTagDiscovered(Tag tag) {
        Log.i(TAG, "New tag discovered: " + Arrays.toString(tag.getTechList()));
        Log.i(TAG, "tag id: " + Utils.byte2hex(tag.getId()));
        // Android's Host-based Card Emulation (HCE) feature implements the ISO-DEP (ISO 14443-4)
        // protocol.
        //
        // In order to communicate with a device using HCE, the discovered tag should be processed
        // using the IsoDep class.
        IsoDep isoDep = IsoDep.get(tag);

        if (isoDep != null) {
            new APDUExecutor(isoDep);
            try {
                // Connect to the remote NFC device
                isoDep.connect();
                if (!isoDep.isConnected()) {
                    Log.w(TAG, "isoDep not connected");
                    return;
                }
                int task = sprf.getInt("TaskTest", -1);
                Log.d(TAG, "task: " + task);
//                switch (task) {
//                    case 0:
//                        callPunchStatusData();
//                        break;
//                    case 1:
//                        callStaffID();
//                        break;
//                    default:
//                        callPunchStatusData();
//                        callStaffID();
//                        break;
//                }
//                if (task == 0) {
//                    callPunchStatusData();
//                } else if(task == 1){
//                    Log.d(TAG,"XDDDDD");
//                    callStaffID();
//                }
                callPunchStatusData();
                callStaffID();
            } catch (IOException e) {
                Log.e(TAG, "Error communicating with card: " + e.toString());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            Log.w(TAG, "ISODep not instance");
        }
    }

    static void getTask(boolean isReset, int task, Context c) {
        if (!isReset) {
            sprf = PreferenceManager.getDefaultSharedPreferences(c);
            sprf.edit().putInt(TASK_KEY, task).apply();
        } else {
            sprf.edit().clear().apply();
        }
    }


    private synchronized void testDisplayResult(byte[] rapduState, byte[] payload, int type) {
        if (Arrays.equals(SELECT_OK_SW, rapduState)) {
            // The remote NFC device will immediately respond with its stored account number
            String payloadData = new String(payload, StandardCharsets.UTF_8);
            Log.i(TAG, "Received: " + payloadData);
            // Inform CardReaderFragment of received account number
            mAccountCallback.get().onAccountReceived(payloadData, type);
        } else {
            Log.d(TAG, "not 0x9000 result: 0x" + Utils.byte2hex(rapduState));
            mAccountCallback.get().onAccountReceived("0x" + Utils.byte2hex(rapduState) + " -- " + new String(payload, StandardCharsets.UTF_8), type);
        }
    }


    private synchronized void callPunchStatusData() throws InterruptedException {

        APDUExecutor.apdu(SAMPLE_TEST_AID, new ApduCallback() {
            @Override
            public void onDone(byte[] result) {
                if (APDUTranslator.rapduResp(result).isEmpty()) {
                    Log.w(TAG, "rapdu no data: ");
                } else {
                    byte[] statusWord = APDUTranslator.rapduResp(result).get(0);
                    byte[] payload = APDUTranslator.rapduResp(result).get(1);
                    testDisplayResult(statusWord, payload, 0);
                    mVibrator.vibrate(300);
                }
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
            }
        });
        // If AID is successfully selected, 0x9000 is returned as the status word (last 2
        // bytes of the result) by convention. Everything before the status word is
        // optional payload, which is used here to hold the account number.

    }

    private synchronized void callStaffID() throws InterruptedException {
        APDUExecutor.apdu(SAMPLE_TEST_AID_2, new ApduCallback() {
            @Override
            public void onDone(byte[] result) {
                if (APDUTranslator.rapduResp(result).isEmpty()) {
                    Log.w(TAG, "rapdu no data: ");
                } else {
                    byte[] statusWord = APDUTranslator.rapduResp(result).get(0);
                    byte[] payload = APDUTranslator.rapduResp(result).get(1);
                    testDisplayResult(statusWord, payload, 1);
                    mVibrator.vibrate(300);
                }
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
            }
        });
    }


    /**
     * Build APDU for SELECT AID command. This command indicates which service a reader is
     * interested in communicating with. See ISO 7816-4.
     *
     * @param aid Application ID (AID) to select
     * @return APDU for SELECT AID command
     */
    public static byte[] BuildSelectApdu(String aid) {
        // Format: [CLASS | INSTRUCTION | PARAMETER 1 | PARAMETER 2 | LENGTH | DATA]
        byte[] apdu = new byte[0];
        Log.d(TAG, "BuildSelectApdu: " + aid);
        if (aid.contains(SAMPLE_TEST_AID)) {
            apdu = Utils.textToByteArray(SELECT_APDU_HEADER + String.format("%02X", aid.length() / 2) + aid);
        } else if (aid.contains(SAMPLE_TEST_AID_2)) {
            apdu = Utils.textToByteArray(TEST_APDU_HEADER + String.format("%02X", aid.length() / 2) + aid);
        }
        return apdu;
    }

}
