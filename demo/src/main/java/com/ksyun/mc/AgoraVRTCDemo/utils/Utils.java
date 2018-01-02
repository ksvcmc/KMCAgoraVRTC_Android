package com.ksyun.mc.AgoraVRTCDemo.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by sujia on 2017/8/10.
 */

public class Utils {
    public static String getDeviceID(Context context) {
        if (context == null) {
            return null;
        }
        String result = null;

        if (checkPermission(context, "android.permission.READ_PHONE_STATE")) {
            TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if(manager != null) {
                result = manager.getDeviceId();
            }
            if (result == null){
                result = manager.getSimSerialNumber();
            }
        } else {
            String android_id = Settings.System.getString(context.getContentResolver(), Settings.System.ANDROID_ID);
            result = computeMD5( android.os.Build.SERIAL +  android_id);
        }

        return result;
    }

    private static boolean checkPermission(Context context, String permission)
    {
        if(context == null)
            return false;

        return context.checkCallingOrSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    public static String computeMD5(String string) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] digestBytes = messageDigest.digest(string.getBytes());
            return bytesToHexString(digestBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String bytesToHexString(byte[] bytes) {
        StringBuffer sb = new StringBuffer();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static boolean isNetworkConnectionAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info == null) return false;
        NetworkInfo.State network = info.getState();
        return (network == NetworkInfo.State.CONNECTED || network == NetworkInfo.State.CONNECTING);
    }
}