package com.example.android.Utils;


import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public final class Utils {
    @NonNull
    public static byte[] arraysCopyOf(@NonNull byte[] original, int newLength) {
        return arraysCopyOfRange(original, 0, (original.length > newLength) ? newLength : original.length);
    }

    @NonNull
    public static byte[] arraysCopyOfRange(@NonNull byte[] original, int start, int end) {
        return Arrays.copyOfRange(original, start, end);
    }

    @NonNull
    public static byte[] base64Decode(@NonNull String enc) { // decode throws IllegalArgumentException
        return AndroidBase64.decode(enc, AndroidBase64.DEFAULT);
    }

//    @NonNull
//    public static byte[] base64DecodeForUAF(@NonNull String enc) {
//        return AndroidBase64.decode(enc, AndroidBase64.NO_WRAP | AndroidBase64.NO_PADDING | AndroidBase64.URL_SAFE);
//    }

    @Nullable
    public static String base64Encode(@Nullable byte[] data) {
        if (data == null)
            return null;
        return AndroidBase64.encodeToString(data, AndroidBase64.NO_WRAP);
    }

//    @Nullable
//    public static String base64EncodeForUAF(@Nullable byte[] data) {
//        if (data == null)
//            return null;
//        return AndroidBase64.encodeToString(data,
//                AndroidBase64.NO_WRAP | AndroidBase64.NO_PADDING | AndroidBase64.URL_SAFE);
//    }

    @NonNull
    public static String byte2hex(@NonNull byte[] b) {
        return byte2hex(b, 0, b.length);
    }

    @NonNull
    public static String byte2hex(@Nullable byte[] b, int start, int end) {
        StringBuilder sb = new StringBuilder((end - start) * 2);
        if (b != null) {
            for (int n = start; n < end; n++) {
                sb.append(String.format("%02X", b[n]));
            }
        } else {
            sb.append("");
        }
        return sb.toString();
    }

    @NonNull
    public static String byte2hexForLog(@Nullable byte[] b) {
        return byte2hexForLog(b, 0, b == null ? 0 : b.length);
    }

    @NonNull
    public static String byte2hexForLog(@Nullable byte[] b, int start, int end) {
        StringBuilder sb = new StringBuilder();
        sb.append(byte2hex(b, start, end));
        if (b == null)
            sb.append("(null)");
        else
            sb.append(" (")
                    .append(end - start)
                    .append(")");
        return sb.toString();
    }

    public static int byteArrayToBigEndianInt(@NonNull byte[] byteArray) {
        int length = (byteArray.length > Integer.SIZE / Byte.SIZE) ? Integer.SIZE / Byte.SIZE : byteArray.length;
        int value = 0;
        for (int i = 0; i < length; i++) {
            value <<= Byte.SIZE;
            value |= (byteArray[i] & 0xff);
        }
        return value;
    }

    public static int byteArrayToLittleEndianInt(@NonNull byte[] byteArray) {
        int length = (byteArray.length > Integer.SIZE / Byte.SIZE) ? Integer.SIZE / Byte.SIZE : byteArray.length;
        int value = 0;
        for (int i = 0; i < length; i++) {
            value <<= Byte.SIZE;
            value |= (byteArray[length - 1 - i] & 0xff);
        }
        return value;
    }

//    public static InputStream getImageFromBase64(String b) throws Exception {
//        return InputStreamUtils.fromBytes(base64Decode(b));
//    }

    @NonNull
    public static byte[] intToByteArrayBigEndian(int value) {
        return ByteBuffer.allocate(Integer.SIZE / Byte.SIZE).order(ByteOrder.BIG_ENDIAN).putInt(value).array();
    }

    @NonNull
    public static byte[] intToByteArrayLittleEndian(int value) {
        return ByteBuffer.allocate(Integer.SIZE / Byte.SIZE).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
    }

    @NonNull
    public static byte[] longToByteArrayBigEndian(long value) {
        return ByteBuffer.allocate(Long.SIZE / Byte.SIZE).order(ByteOrder.BIG_ENDIAN).putLong(value).array();
    }

    @NonNull
    public static byte[] longToByteArrayLittleEndian(long value) {
        return ByteBuffer.allocate(Long.SIZE / Byte.SIZE).order(ByteOrder.LITTLE_ENDIAN).putLong(value).array();
    }

    public static boolean isBase64String(@NonNull String s) {
        s = s.trim();
        // fixme with a fake one "abc123=" (padding contains but not enough)
        return s.matches("^[a-zA-Z0-9+/]*={0,3}$") || s.matches("^[a-zA-Z0-9-_]*={0,3}$"); // url
        // safe
    }

    @NonNull
    public static <T> List<T> safeForEach(@Nullable List<T> other) {
        return other == null ? new ArrayList<T>() : other;
    }

    @NonNull
    public static <T> Set<T> safeForEach(@Nullable Set<T> other) {
        return other == null ? new HashSet<T>() : other;
    }

    @NonNull
    public static <T> List<T> safeForEach(@Nullable T[] other) {
        return other == null ? new ArrayList<T>() : Arrays.asList(other);
    }

    // convert string to byte array
    @Nullable
    public static byte[] textToByteArray(String text) {
        if (text == null)
            return new byte[0];

        int length = (text.length() / 2) + (text.length() % 2 == 1 ? 1 : 0);
        byte[] ret = new byte[length];

        if (text.length() % 2 == 1) // error?
        {
            text = "0" + text;
        }

        for (int i = 0; i < text.length(); i += 2) {
            try {
                ret[i / 2] = (byte) (Integer.parseInt(text.substring(i, i + 2), 16) & 0xff);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return null;
            }
        }
        return ret;
    }

    @NonNull
    public static byte[] concat(@NonNull byte[] array1, @NonNull byte[] array2) {
        byte[] c = new byte[array1.length + array2.length];
        if (array1.length != 0)
            System.arraycopy(array1, 0, c, 0, array1.length);
        if (array2.length != 0)
            System.arraycopy(array2, 0, c, array1.length, array2.length);
        return c;
    }

    public static void clearByteArray(@Nullable byte[] b) {
        if (b == null || b.length == 0)
            return;
        for (int i = 0; i < b.length; i++)
            b[i] = 0;
    }

    //below is sample making
    public static byte[] HexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}