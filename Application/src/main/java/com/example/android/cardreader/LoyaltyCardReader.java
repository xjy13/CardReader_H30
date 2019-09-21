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

import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;

import com.example.android.Utils.Utils;
import com.example.android.common.logger.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Callback class, invoked when an NFC card is scanned while the device is running in reader mode.
 *
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
    private static final String TEST_APDU_HEADER =   "00C40400";
    // "OK" status word sent in response to SELECT AID command (0x9000)
    private static final byte[] SELECT_OK_SW = {(byte) 0x90, (byte) 0x00};

    // Weak reference to prevent retain loop. mAccountCallback is responsible for exiting
    // foreground mode before it becomes invalid (e.g. during onPause() or onStop()).
    private WeakReference<AccountCallback> mAccountCallback;

    public interface AccountCallback {
        void onAccountReceived(String account);
    }

    public LoyaltyCardReader(AccountCallback accountCallback) {
        mAccountCallback = new WeakReference<AccountCallback>(accountCallback);
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
        Log.i(TAG, "New tag discovered: "+ Arrays.toString(tag.getTechList()));
        Log.i(TAG,"tag id: "+Utils.byte2hex(tag.getId()));
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
                if(!isoDep.isConnected()){
                    Log.w(TAG,"isoDep not connected");
                    return;
                }
                // Build SELECT AID command for our loyalty card service.
                // This command tells the remote device which service we wish to communicate with.
                Log.i(TAG, "Requesting remote AID: " + SAMPLE_TEST_AID);
                // Send command to remote device
                byte[] result = APDUExecutor.apdu(SAMPLE_TEST_AID);
                // If AID is successfully selected, 0x9000 is returned as the status word (last 2
                // bytes of the result) by convention. Everything before the status word is
                // optional payload, which is used here to hold the account number.
                byte[] statusWord =  APDUTranslator.rapduResp(result).get(0);
                byte[] payload = APDUTranslator.rapduResp(result).get(1);
                Log.d(TAG,"payload: "+Utils.byte2hexForLog(payload));


                byte[] result_2 = APDUExecutor.apdu(SAMPLE_TEST_AID_2);
                byte[] payload_2 = APDUTranslator.rapduResp(result_2).get(0);
                Log.i(TAG, "GET_STRING_1: " + Utils.byte2hex(result_2));
                Log.i(TAG, "GET STRING_2: " + new String(APDUTranslator.rapduResp(result_2).get(1), StandardCharsets.UTF_8));
                testDisplayResult(statusWord,payload);
            } catch (IOException e) {
                Log.e(TAG, "Error communicating with card: " + e.toString());
            }
        }
    }


    private synchronized void testDisplayResult(byte[] rapduState, byte[] payload){
        if (Arrays.equals(SELECT_OK_SW, rapduState)) {
            // The remote NFC device will immediately respond with its stored account number
            String accountNumber = new String(payload, StandardCharsets.UTF_8);
            Log.i(TAG, "Received: " + accountNumber);
            // Inform CardReaderFragment of received account number
            mAccountCallback.get().onAccountReceived(accountNumber);
        } else {
            Log.d(TAG, "not 0x9000 result: " + Utils.byte2hex(rapduState));
            mAccountCallback.get().onAccountReceived(Utils.byte2hex(rapduState)+" -- "+new String(payload, StandardCharsets.UTF_8));
        }
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
        Log.d(TAG,"BuildSelectApdu: "+aid);
        if(aid.contains(SAMPLE_TEST_AID)){
            apdu = Utils.textToByteArray(SELECT_APDU_HEADER + String.format("%02X", aid.length() / 2) + aid);
        }else if(aid.contains(SAMPLE_TEST_AID_2)){
            apdu = Utils.textToByteArray(TEST_APDU_HEADER + String.format("%02X", aid.length() / 2) + aid);
        }
        return apdu;
    }

    /**
     * Utility class to convert a byte array to a hexadecimal string.
     *
     * @param bytes Bytes to convert
     * @return String, containing hexadecimal representation.
     */
    public static String ByteArrayToHexString(byte[] bytes) {
        final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for ( int j = 0; j < bytes.length; j++ ) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * Utility class to convert a hexadecimal string to a byte string.
     *
     * <p>Behavior with input strings containing non-hexadecimal characters is undefined.
     *
     * @param s String containing hexadecimal characters to convert
     * @return Byte array generated from input
     */


}
