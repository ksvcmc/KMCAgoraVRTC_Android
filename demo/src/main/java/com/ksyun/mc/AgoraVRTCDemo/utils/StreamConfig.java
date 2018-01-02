package com.ksyun.mc.AgoraVRTCDemo.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.ksyun.media.streamer.framework.AVConst;
import com.ksyun.media.streamer.kit.StreamerConstants;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by sujia on 2017/11/29.
 */

public class StreamConfig {
    public static int[] PREVIEW_RESOLUTION = {
            StreamerConstants.VIDEO_RESOLUTION_360P,
            StreamerConstants.VIDEO_RESOLUTION_480P,
            StreamerConstants.VIDEO_RESOLUTION_720P
    };

    public static int[] TARGET_RESOLUTION = {
            StreamerConstants.VIDEO_RESOLUTION_360P,
            StreamerConstants.VIDEO_RESOLUTION_480P,
            StreamerConstants.VIDEO_RESOLUTION_720P
    };

    public static int[] ENCODE_METHOD = {
            StreamerConstants.ENCODE_METHOD_SOFTWARE,
            StreamerConstants.ENCODE_METHOD_HARDWARE
    };

    public static int[] VCODEC_ID = {
            StreamerConstants.CODEC_ID_AVC,
            StreamerConstants.CODEC_ID_HEVC
    };

    public static int[] ACODEC_PROFILE = {
            AVConst.PROFILE_AAC_LOW,
            AVConst.PROFILE_AAC_HE
    };

    public final static String SHARED_PREF = "shared_pref";
    //key to store shared preferences
    private final static String PREVIEW_RES_IDX = "preview_res_idx";
    private final static String TARGET_RES_IDX = "target_res_idx";
    private final static String FPS = "fps";
    private final static String ENCODE_METHOD_IDX = "encode_method_idx";
    private final static String VCODEC_ID_IDX = "vcodec_id_idx";
    private final static String ACODEC_PROFILE_IDX = "acodec_id_idx";
    private final static String VIDEO_KBITRATE = "video_kbitrate";
    private final static String AUDIO_KBITRATE = "audio_kbitrate";

    public int preview_res_idx = 0;
    public int target_res_idx = 0;
    public int fps = 15;
    public int encode_method_idx = 0;
    public int vcodec_id_idx = 0;
    public int acodec_profile_idx = 0;
    public int video_kbitrate = 800;
    public int audio_kbitrate = 48;

    private Context mContext;

    public StreamConfig(Context context) {
        this.mContext = context;
    }

    public void saveData() {
        SharedPreferences.Editor editor = mContext.getSharedPreferences(SHARED_PREF, MODE_PRIVATE).edit();
        if (preview_res_idx >= 0 && preview_res_idx < PREVIEW_RESOLUTION.length) {
            editor.putInt(PREVIEW_RES_IDX, preview_res_idx);
        }
        if (target_res_idx >= 0 && target_res_idx < TARGET_RESOLUTION.length) {
            editor.putInt(TARGET_RES_IDX, target_res_idx);
        }
        if (encode_method_idx >= 0 && encode_method_idx < ENCODE_METHOD.length) {
            editor.putInt(ENCODE_METHOD_IDX, encode_method_idx);
        }
        if (vcodec_id_idx >= 0 && vcodec_id_idx < ENCODE_METHOD.length) {
            editor.putInt(VCODEC_ID_IDX, vcodec_id_idx);
        }
        if (acodec_profile_idx >= 0 && acodec_profile_idx < ACODEC_PROFILE.length ) {
            editor.putInt(ACODEC_PROFILE_IDX, acodec_profile_idx);
        }

        if (fps > 0 && fps <= 15 ) {
            editor.putInt(FPS, fps);
        }
        if (video_kbitrate > 0 && video_kbitrate <= 800) {
            editor.putInt(VIDEO_KBITRATE, video_kbitrate);
        }
        if (audio_kbitrate > 0 && audio_kbitrate <= 48) {
            editor.putInt(AUDIO_KBITRATE, audio_kbitrate);
        }
        editor.commit();
    }

    public void loadData() {
        SharedPreferences sp = mContext.getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        preview_res_idx = sp.getInt(PREVIEW_RES_IDX, 0);
        target_res_idx = sp.getInt(TARGET_RES_IDX, 0);
        fps = sp.getInt(FPS, 15);
        encode_method_idx = sp.getInt(ENCODE_METHOD_IDX, 0);
        vcodec_id_idx = sp.getInt(VCODEC_ID_IDX, 0);
        acodec_profile_idx = sp.getInt(ACODEC_PROFILE_IDX, 0);
        video_kbitrate = sp.getInt(VIDEO_KBITRATE, 800);
        audio_kbitrate = sp.getInt(AUDIO_KBITRATE, 48);
    }

    public int getPreviewResolution() {
        if (preview_res_idx >= 0 && preview_res_idx < PREVIEW_RESOLUTION.length - 1) {
            return PREVIEW_RESOLUTION[preview_res_idx];
        } else {
            return PREVIEW_RESOLUTION[0];
        }
    }

    public int getTargetResolution() {
        if (target_res_idx >= 0 && target_res_idx < TARGET_RESOLUTION.length) {
            return TARGET_RESOLUTION[target_res_idx];
        } else {
            return TARGET_RESOLUTION[0];
        }
    }

    public int getEncodeMethod() {
        if (encode_method_idx >= 0 && encode_method_idx <ENCODE_METHOD.length) {
            return ENCODE_METHOD[encode_method_idx];
        } else {
            return ENCODE_METHOD[0];
        }
    }

    public int getVcodecId() {
        if (vcodec_id_idx >= 0 && vcodec_id_idx <VCODEC_ID.length) {
            return VCODEC_ID[vcodec_id_idx];
        } else {
            return VCODEC_ID[0];
        }
    }

    public int getAcodecProfile() {
        if (acodec_profile_idx >= 0 && acodec_profile_idx <ACODEC_PROFILE.length) {
            return ACODEC_PROFILE[acodec_profile_idx];
        } else {
            return ACODEC_PROFILE[0];
        }
    }
}
