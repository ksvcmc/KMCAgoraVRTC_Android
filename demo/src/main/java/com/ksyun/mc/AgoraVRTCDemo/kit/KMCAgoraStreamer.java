package com.ksyun.mc.AgoraVRTCDemo.kit;

import android.bluetooth.BluetoothHeadset;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;

import com.ksyun.mc.agoravrtc.AgoraErrorCode;
import com.ksyun.mc.agoravrtc.AgoraVideoProfile;
import com.ksyun.mc.agoravrtc.KMCAgoraEventListener;
import com.ksyun.mc.agoravrtc.KMCAuthResultListener;
import com.ksyun.media.streamer.filter.imgbuf.ImgBufScaleFilter;
import com.ksyun.media.streamer.filter.imgtex.ImgTexMixer;
import com.ksyun.media.streamer.filter.imgtex.ImgTexScaleFilter;
import com.ksyun.media.streamer.framework.ImgBufFormat;
import com.ksyun.media.streamer.kit.KSYStreamer;

import java.util.Arrays;

/**
 * Created by sujia on 2017/7/17.
 */
public class KMCAgoraStreamer extends KSYStreamer {
    private static final String TAG = "KMCAgoraStreamer";
    private static final boolean DEBUG = false;
    private static final String VERSION = "1.0.4.0";

    protected int mIdxBgPicture = 0;
    private static final int mIdxVideoSub = 4;
    private static final int mIdxAudioRemote = 2;

    private KMCAgoraVRTCClient mRTCClient;
    private ImgTexScaleFilter mRTCRemoteImgTexScaleFilter;
    private ImgTexScaleFilter mRTCImgTexScaleFilter;
    private ImgBufScaleFilter mImgBufScale;

    public static final int RTC_MAIN_SCREEN_CAMERA = 1;
    public static final int RTC_MAIN_SCREEN_REMOTE = 2;
    private int mRTCMainScreen = RTC_MAIN_SCREEN_CAMERA;

    public static final int SCALING_MODE_FULL_FILL = ImgTexMixer.SCALING_MODE_FULL_FILL;
    public static final int SCALING_MODE_BEST_FIT = ImgTexMixer.SCALING_MODE_BEST_FIT;
    public static final int SCALING_MODE_CENTER_CROP = ImgTexMixer.SCALING_MODE_CENTER_CROP;

    private MusicIntentReceiver mHeadSetReceiver;
    private boolean mHeadSetPlugged = false;
    private boolean mIsCalling = false;
    private boolean mIsRemoteConnected = false;

    private float mPresetSubLeft = 0.65f;
    private float mPresetSubTop = 0.f;
    private float mPresetSubWidth = 0.35f;
    private float mPresetSubHeight = 0.35f;
    private int mPresetSubMode = SCALING_MODE_CENTER_CROP;

    private float mPresetMainLeft = 0.f;
    private float mPresetMainTop = 0.f;
    private float mPresetMainWidth = 1.f;
    private float mPresetMainHeight = 1.f;
    private int mPresetMainMode = SCALING_MODE_CENTER_CROP;

    private boolean mMuteAudio;
    private PictureCapture mPictureCapture;

    public KMCAgoraStreamer(Context context) {
        super(context.getApplicationContext());
    }


    /**
     * set rtc main Screen
     *
     * @param mainScreenType
     */
    public void setRTCMainScreen(int mainScreenType) {
        if (mainScreenType < RTC_MAIN_SCREEN_CAMERA
                || mainScreenType > RTC_MAIN_SCREEN_REMOTE) {
            throw new IllegalArgumentException("Invalid rtc main screen type");
        }
        mRTCMainScreen = mainScreenType;
    }

    /**
     * the sub screen position
     * must be set before registerRTC
     *
     * @param width  0~1 default value 0.35f
     * @param height 0~1 default value 0.3f
     * @param left   0~1 default value 0.65f
     * @param top    0~1 default value 0.f
     * @param mode   scaling mode
     */
    public void setRTCSubScreenRect(float left, float top, float width, float height, int mode) {
        mPresetSubLeft = left;
        mPresetSubTop = top;
        mPresetSubWidth = width;
        mPresetSubHeight = height;
        mPresetSubMode = mode;

        mImgTexMixer.setRenderRect(mIdxVideoSub, left, top, width, height, 1.0f);
        mImgTexMixer.setScalingMode(mIdxVideoSub, mode);

        mImgTexPreviewMixer.setRenderRect(mIdxVideoSub, left, top, width, height, 1.0f);
        mImgTexPreviewMixer.setScalingMode(mIdxVideoSub, mode);
    }

    /**
     * set camera screen rect when rtc connected
     * @param left 0~1 default value 0.f
     * @param top 0~1 default value 0.f
     * @param width 0~1 default value 1.f
     * @param height 0~1 default value 1.f
     * @param mode scaling mode
     */
    public void setRTCMainScreenRect(float left, float top, float width, float height, int mode) {
        mPresetMainLeft = left;
        mPresetMainTop = top;
        mPresetMainWidth = width;
        mPresetMainHeight = height;
        mPresetMainMode = mode;

        mImgTexMixer.setScalingMode(mIdxCamera, mode);
        mImgTexPreviewMixer.setScalingMode(mIdxCamera, mode);

        if (isRemoteConnected()) {
            mImgTexMixer.setRenderRect(mIdxCamera, left, top, width, height, 1.0f);
            mImgTexPreviewMixer.setRenderRect(mIdxCamera, left, top, width, height, 1.0f);
        }
    }



    public RectF getSubScreenRect() {
        return mImgTexMixer.getRenderRect(mIdxVideoSub);
    }

    public RectF getMainScreenRect() {
        return mImgTexMixer.getRenderRect(mIdxCamera);
    }

    public void switchMainScreen() {
        if (mRTCMainScreen == RTC_MAIN_SCREEN_REMOTE) {
            mRTCMainScreen = RTC_MAIN_SCREEN_CAMERA;
        } else if (mRTCMainScreen == RTC_MAIN_SCREEN_CAMERA) {
            mRTCMainScreen = RTC_MAIN_SCREEN_REMOTE;
        }

        setRTCMainScreen(mRTCMainScreen);
        updateRTCConnect(mRTCMainScreen);
    }

    public int getRTCMainScreen() {
        return mRTCMainScreen;
    }


    public boolean isRemoteConnected() {
        return mIsRemoteConnected;
    }

    @Override
    protected void initModules() {
        mHeadSetPlugged = false;
        mIsCalling = false;
        super.initModules();

        mAudioCapture.setSampleRate(16000);
        //rtc remote image
        mRTCClient = new KMCAgoraVRTCClient(mGLRender, mContext);

        mRTCRemoteImgTexScaleFilter = new ImgTexScaleFilter(mGLRender);
        mRTCRemoteImgTexScaleFilter.setReuseFbo(false);

        mRTCImgTexScaleFilter = new ImgTexScaleFilter(mGLRender);
        mImgTexFilterMgt.getSrcPin().connect(mRTCImgTexScaleFilter.getSinkPin());
        mRTCImgTexScaleFilter.getSrcPin().connect(mRTCClient.getImgTexSinkPin());

        mImgBufScale = new ImgBufScaleFilter();
        mImgBufScale.setOutputFormat(ImgBufFormat.FMT_I420);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            mCameraCapture.mImgBufSrcPin.connect(mImgBufScale.getSinkPin());
            mImgBufScale.getSrcPin().connect(mRTCClient.getVideoSinkPin());
        }

        registerHeadsetPlugReceiver();

        mRTCClient.registerEventListener(new KMCAgoraEventListener() {
            @Override
            public void onEvent(int event, Object... data) {
                switch (event) {
                    case KMCAgoraEventListener.USER_JOINED: {
                        int uid = (int) data[0];
                        Log.i("KMCStreamer","USER_JOINED uid: " + uid);
                        mRTCClient.startReceiveRemoteData();
                        break;
                    }

                    case KMCAgoraEventListener.JOIN_CHANNEL_RESULT: {
                        mRTCClient.startReceiveRemoteData();
                        break;
                    }

                    case KMCAgoraEventListener.FIRST_FRAME_DECODED: {
                        setAudioMode(mHeadSetPlugged ? AudioManager.MODE_IN_COMMUNICATION :
                                AudioManager.MODE_NORMAL);
                        mIsRemoteConnected = true;
                        Log.d(TAG, "onFirstRemoteVideoDecoded " + Arrays.toString(data));
                        updateRTCConnect(mRTCMainScreen);
                        break;
                    }

                    case KMCAgoraEventListener.LEAVE_CHANNEL: {
                        // temporarily only one remote stream supported, so reset uid here
                        mIsRemoteConnected = false;
                        mRTCClient.stopReceiveRemoteData();
                        updateRTCConnect(RTC_MAIN_SCREEN_CAMERA);
                        if(!mIsCalling) {
                            if ((mIsRecording || mIsFileRecording) &&
                                    !mAudioCapture.isRecordingState()) {
                                mAudioCapture.start();
                            }
                        }
                        break;
                    }

                    case KMCAgoraEventListener.USER_OFFLINE: {
                        mIsRemoteConnected = false;
                        updateRTCConnect(RTC_MAIN_SCREEN_CAMERA);
                        break;
                    }

                    case KMCAgoraEventListener.ERROR: {
                        int errorCode = (Integer) data[0];
                        if (errorCode == AgoraErrorCode.ERR_INVALID_APP_ID) {
                            Log.e(TAG, "invalid app id");
                        }
                    }
                }
            }
        });

        mIdxCamera = 1;
        mIdxWmLogo = 2;
        mIdxWmTime = 3;
        updateRTCConnect(RTC_MAIN_SCREEN_CAMERA);
        // create pip modules
        mPictureCapture = new PictureCapture(mGLRender);
        // pip connection
        mPictureCapture.getSrcPin().connect(getImgTexPreviewMixer().getSinkPin(mIdxBgPicture));
        getImgTexPreviewMixer().setScalingMode(mIdxBgPicture, ImgTexMixer.SCALING_MODE_CENTER_CROP);
        mPictureCapture.getSrcPin().connect(getImgTexMixer().getSinkPin(mIdxBgPicture));
        getImgTexMixer().setScalingMode(mIdxBgPicture, ImgTexMixer.SCALING_MODE_CENTER_CROP);
    }

    @Override
    public void setRotateDegrees(int degrees) {
        boolean isLastLandscape = (mRotateDegrees % 180) != 0;
        boolean isLandscape = (degrees % 180) != 0;
        if (isLastLandscape != isLandscape) {
            if(mIsRemoteConnected) {
                setRTCSubScreenRect(mPresetSubLeft, mPresetSubTop, mPresetSubHeight,
                        mPresetSubWidth, mPresetSubMode);
            }
        }
        super.setRotateDegrees(degrees);
        if(mIsRemoteConnected) {
            updateRTCConnect(mRTCMainScreen);
        }
    }

    @Override
    public void setMuteAudio(boolean isMute) {
        super.setMuteAudio(isMute);
        //对所有远端用户进行静音与否
//        mRTCClient.getRTCWrapper().muteAllRemoteAudioStreams(isMute);
        //允许/禁止往网络发送本地音频流
        mRTCClient.getRTCWrapper().muteLocalAudioStream(isMute);

        mMuteAudio = isMute;
    }

    @Override
    public void setFrontCameraMirror(boolean mirror) {
        super.setFrontCameraMirror(mirror);
        if (mImgBufScale != null) {
            mImgBufScale.setMirror(mirror);
        }
    }

    @Override
    protected void setPreviewParams() {
        super.setPreviewParams();
        //转换成agora支持的编码分辨率
        switch (mTargetWidth) {
            case 360:
                if (mRTCImgTexScaleFilter != null) {
                    mRTCImgTexScaleFilter.setTargetSize(360, 640);
                }
                if (mImgBufScale != null) {
                    mImgBufScale.setTargetSize(360, 640);
                }
                mRTCClient.setVideoProfile(AgoraVideoProfile.VIDEO_PROFILE_360P, true);
                break;
            case 480:
                if (mRTCImgTexScaleFilter != null) {
                    mRTCImgTexScaleFilter.setTargetSize(480, 848);
                }
                if (mImgBufScale != null) {
                    mImgBufScale.setTargetSize(480, 848);
                }
                mRTCClient.setVideoProfile(AgoraVideoProfile.VIDEO_PROFILE_480P_8, true);
                break;
            case 720:
                if (mRTCImgTexScaleFilter != null) {
                    mRTCImgTexScaleFilter.setTargetSize(720, 1280);
                }
                if (mImgBufScale != null) {
                    mImgBufScale.setTargetSize(720, 1280);
                }
                mRTCClient.setVideoProfile(AgoraVideoProfile.VIDEO_PROFILE_720P, true);
                break;
            default:
                //just demo for rtc img,see following for details(setVideoProfile)
                // https://docs.agora.io/cn/user_guide/API/android_api_live.html
                boolean isLandscape = (mRotateDegrees % 180) != 0;
                if(isLandscape) {
                    if (mRTCImgTexScaleFilter != null) {
                        mRTCImgTexScaleFilter.setTargetSize(640, 360);
                    }
                    if (mImgBufScale != null) {
                        mImgBufScale.setTargetSize(640, 360);
                    }
                    mRTCClient.setVideoProfile(AgoraVideoProfile.VIDEO_PROFILE_360P, false);
                } else {
                    if (mRTCImgTexScaleFilter != null) {
                        mRTCImgTexScaleFilter.setTargetSize(360, 640);
                    }
                    if (mImgBufScale != null) {
                        mImgBufScale.setTargetSize(360, 640);
                    }
                    mRTCClient.setVideoProfile(AgoraVideoProfile.VIDEO_PROFILE_360P, true);
                }

                break;

        }
    }

    public void startRTC(String channel) {
        if (mIsCalling) {
            return;
        }
        mIsCalling = true;

        setAudioParams();
        mAudioCapture.getSrcPin().disconnect(false);
        if (mAudioCapture.isRecordingState()) {
            mAudioCapture.stop();
        }

        //join rtc
        mRTCClient.joinChannel(channel, 0);

        //connect rtc audio
        mRTCClient.getLocalAudioSrcPin().connect(mAudioFilterMgt.getSinkPin());
        mRTCClient.getRemoteAudioSrcPin().connect(mAudioMixer.getSinkPin(mIdxAudioRemote));

        //对所有远端用户进行静音与否
//        mRTCClient.getRTCWrapper().muteAllRemoteAudioStreams(mMuteAudio);
        //允许/禁止往网络发送本地音频流
        mRTCClient.getRTCWrapper().muteLocalAudioStream(mMuteAudio);
    }
    @Override
    protected void startAudioCapture() {
        if(!mIsCalling) { //在开启连麦时，不允许在启动推流中的音频
            super.startAudioCapture();
        }
    }

    public void stopRTC() {
        if (!mIsCalling) {
            return;
        }
        mIsCalling = false;

        //leave rtc
        mRTCClient.stopReceiveRemoteData();
        setRTCMainScreen(RTC_MAIN_SCREEN_CAMERA);
        mRTCClient.leaveChannel();

        //disconnect rtc audio
        mRTCClient.getLocalAudioSrcPin().disconnect(false);
        mRTCClient.getRemoteAudioSrcPin().disconnect(false);
        //connect audio capture
        mAudioCapture.getSrcPin().connect(mAudioFilterMgt.getSinkPin());
    }

    /**
     * Set if start audio preview.<br/>
     * Should start only when headset plugged.
     *
     * @param enable true to start, false to stop.
     */
    @Override
    public void setEnableAudioPreview(boolean enable) {
        if (mIsRemoteConnected) {
            setAudioMode(mHeadSetPlugged == true ?
                    AudioManager.MODE_IN_COMMUNICATION : AudioManager.MODE_NORMAL);
        }

        super.setEnableAudioPreview(enable);
    }

    /**
     * Release all resources used by KMCAgoraStreamer.
     */
    @Override
    public void release() {
        super.release();
        getImgTexFilterMgt().release();
        setOnErrorListener(null);
        setOnInfoListener(null);
        setOnLogEventListener(null);

        if (mImgBufScale != null) {
            mImgBufScale.release();
            mImgBufScale = null;
        }
        mRTCClient.release();
        unregisterHeadsetPlugReceiver();
    }


    private void registerHeadsetPlugReceiver() {
        mHeadSetReceiver = new MusicIntentReceiver();
        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        filter.addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED);
        filter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        mContext.registerReceiver(mHeadSetReceiver, filter);
    }

    private void unregisterHeadsetPlugReceiver() {
        if (mHeadSetReceiver != null) {
            mContext.unregisterReceiver(mHeadSetReceiver);
        }
    }

    private class MusicIntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean headsetConnected = false;

            String action = intent.getAction();
            int state = BluetoothHeadset.STATE_DISCONNECTED;

            if (action.equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)) {
                state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE,
                        BluetoothHeadset.STATE_DISCONNECTED);
                if (state == BluetoothHeadset.STATE_CONNECTED) {
                    headsetConnected = true;
                } else if (state == BluetoothHeadset.STATE_DISCONNECTED) {
                    headsetConnected = false;
                }
            } else if (action.equals(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED))// audio
            {
                state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE,
                        BluetoothHeadset.STATE_AUDIO_DISCONNECTED);
                if (state == BluetoothHeadset.STATE_AUDIO_CONNECTED) {
                    headsetConnected = true;
                } else if (state == BluetoothHeadset.STATE_AUDIO_DISCONNECTED) {
                    headsetConnected = false;
                }
            } else if (action.equals(Intent.ACTION_HEADSET_PLUG)) {
                state = intent.getIntExtra("state", -1);

                switch (state) {
                    case 0:
                        Log.d(TAG, "Headset is unplugged");
                        headsetConnected = false;
                        break;
                    case 1:
                        Log.d(TAG, "Headset is plugged");
                        headsetConnected = true;
                        break;
                    default:
                        Log.d(TAG, "I have no idea what the headset state is");
                }
            }

            mHeadSetPlugged = headsetConnected;
            if(mIsRemoteConnected) {
                setAudioMode(mHeadSetPlugged == true ?
                        AudioManager.MODE_IN_COMMUNICATION : AudioManager.MODE_NORMAL);
            }
        }
    }

    public RectF getRTCSubScreenRect() {
        return mImgTexMixer.getRenderRect(mIdxVideoSub);
    }

    public static String getAgoraRTCVersion() {
        return VERSION;
    }

    private void updateRTCConnect(int rtcMainScreen) {
        mImgTexFilterMgt.getSrcPin().disconnect(false);
        mRTCRemoteImgTexScaleFilter.getSrcPin().disconnect(false);
        mRTCClient.getImgTexSrcPin().disconnect(false);

        boolean needScale = false;
        if (mRTCClient.getImgTexFormat() != null) {
            boolean isLandscape = (mRotateDegrees % 180) != 0;

            if ((isLandscape && mRTCClient.getImgTexFormat().width <
                    mRTCClient.getImgTexFormat().height) ||
                    (!isLandscape && mRTCClient.getImgTexFormat().width >
                            mRTCClient.getImgTexFormat().height)) {
                needScale = false;
            }
        }

        mImgTexFilterMgt.getSrcPin().connect(mRTCImgTexScaleFilter.getSinkPin());

        if (rtcMainScreen == RTC_MAIN_SCREEN_REMOTE) {
            mImgTexFilterMgt.getSrcPin().connect(mImgTexMixer.getSinkPin(mIdxVideoSub));
            mImgTexFilterMgt.getSrcPin().connect(mImgTexPreviewMixer.getSinkPin(mIdxVideoSub));

            if (needScale) {
                mRTCRemoteImgTexScaleFilter.setTargetSize(mRTCClient.getImgTexFormat().height,
                        mRTCClient.getImgTexFormat().width);
                mRTCClient.getImgTexSrcPin().connect(mRTCRemoteImgTexScaleFilter.getSinkPin());
                mRTCRemoteImgTexScaleFilter.getSrcPin().connect(mImgTexMixer.getSinkPin(mIdxCamera));
                mRTCRemoteImgTexScaleFilter.getSrcPin().connect(mImgTexPreviewMixer.getSinkPin(mIdxCamera));

            } else {
                mRTCClient.getImgTexSrcPin().connect(mImgTexMixer.getSinkPin(mIdxCamera));
                mRTCClient.getImgTexSrcPin().connect(mImgTexPreviewMixer.getSinkPin(mIdxCamera));
            }
            mImgTexMixer.setMainSinkPinIndex(mIdxVideoSub);
            mImgTexPreviewMixer.setMainSinkPinIndex(mIdxVideoSub);
        } else {
            mImgTexFilterMgt.getSrcPin().connect(mImgTexMixer.getSinkPin(mIdxCamera));
            mImgTexFilterMgt.getSrcPin().connect(mImgTexPreviewMixer.getSinkPin(mIdxCamera));

            if(mIsRemoteConnected) {
                if (needScale) {
                    mRTCRemoteImgTexScaleFilter.setTargetSize(mRTCClient.getImgTexFormat().height,
                            mRTCClient.getImgTexFormat().width);
                    mRTCClient.getImgTexSrcPin().connect(mRTCRemoteImgTexScaleFilter.getSinkPin());
                    mRTCRemoteImgTexScaleFilter.getSrcPin().connect(mImgTexMixer.getSinkPin(mIdxVideoSub));
                    mRTCRemoteImgTexScaleFilter.getSrcPin().connect(mImgTexPreviewMixer.getSinkPin(mIdxVideoSub));
                } else {
                    mRTCClient.getImgTexSrcPin().connect(mImgTexMixer.getSinkPin(mIdxVideoSub));
                    mRTCClient.getImgTexSrcPin().connect(mImgTexPreviewMixer.getSinkPin(mIdxVideoSub));
                }
            }
            mImgTexMixer.setMainSinkPinIndex(mIdxCamera);
            mImgTexPreviewMixer.setMainSinkPinIndex(mIdxCamera);
        }

        updateRemoteSize(rtcMainScreen);
    }

    public void setAudioMode(int mode) {
        if (mode != AudioManager.MODE_NORMAL && mode != AudioManager.MODE_IN_COMMUNICATION) {
            return;
        }
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        if (mode == AudioManager.MODE_NORMAL) {
            audioManager.setSpeakerphoneOn(true);//打开扬声器
        } else if (mode == AudioManager.MODE_IN_COMMUNICATION) {
            audioManager.setSpeakerphoneOn(false);//关闭扬声器
        }
        audioManager.setMode(mode);
    }

    private void updateRemoteSize(int rtcMainScreen) {
        if (!mIsRemoteConnected) {
            mImgTexMixer.setRenderRect(mIdxCamera, 0.f, 0.f, 1.0f, 1.0f, 1.0f);
            mImgTexPreviewMixer.setRenderRect(mIdxCamera, 0.f, 0.f, 1.0f, 1.0f, 1.0f);
            return;
        }

        boolean isLandscape = (mRotateDegrees % 180) != 0;
        boolean tIsLandscape = mRTCClient.getImgTexFormat().width > mRTCClient.getImgTexFormat()
                .height;

        boolean needScale = false;

        int tWidth = mRTCClient.getImgTexFormat().width;
        int tHeight = mRTCClient.getImgTexFormat().height;

        //本地预览显示和remote的横竖屏不一致时，需要resize
        if ((isLandscape && tWidth < tHeight) ||
                (!isLandscape && tWidth > tHeight)) {
            needScale = true;
        }

        if (needScale) {
            if (rtcMainScreen == RTC_MAIN_SCREEN_REMOTE) {
                float left, top, w_new, h_new;
                left =  mPresetSubLeft;
                top = mPresetSubTop;
                w_new = mPresetSubWidth;
                h_new = mPresetSubHeight;

                if (!isLandscape && tIsLandscape) {
                    left = 0;
                    w_new = (float) 1.0;
//                    h_new = (float) (tHeight * mScreenRenderWidth) / (mScreenRenderHeight * tWidth);
                    h_new = (float) (tHeight * mPreviewWidth) / (mPreviewHeight * tWidth);
                    top = (float) (1.0 - h_new) / (float) 2.0;
                } else if (isLandscape && !tIsLandscape) {
                    top = 0;
                    h_new = (float) 1.0;
//                  w_new = (float) (tWidth * mScreenRenderHeight) / (mScreenRenderWidth * tHeight);
                    w_new = (float) (tWidth * mPreviewHeight) / (mPreviewWidth * tHeight);
                    left = (float) (1.0 - w_new) / (float) 2.0;
                }

                mImgTexMixer.setRenderRect(mIdxCamera, left, top, w_new,
                        h_new, 1.0f);
                mImgTexPreviewMixer.setRenderRect(mIdxCamera, left, top, w_new,
                        h_new, 1.0f);

            } else if (rtcMainScreen == RTC_MAIN_SCREEN_CAMERA) {
                RectF rect = getRTCSubScreenRect();
                float w = rect.width();
                float h = rect.height();

                float w_new;
                float h_new;
//              w_new = (float) mScreenRenderHeight * h / (float) mScreenRenderWidth;
//              h_new = (float) mScreenRenderWidth * w / (float) mScreenRenderHeight;
                w_new = (float) mPreviewHeight * h / (float) mPreviewWidth;
                h_new = (float) mPreviewWidth * w / (float) mPreviewHeight;

                float left = (float) (1.0 - w_new - 0.10);
                float top = (float) 0.05;
                mImgTexMixer.setRenderRect(mIdxVideoSub, left, top, w_new,
                        h_new, 1.0f);
                mImgTexPreviewMixer.setRenderRect(mIdxVideoSub, left, top, w_new,
                        h_new, 1.0f);
            }
        } else {
            mImgTexMixer.setRenderRect(mIdxCamera, mPresetMainLeft, mPresetMainTop,
                    mPresetMainWidth, mPresetMainHeight, 1.0f);
            mImgTexMixer.setRenderRect(mIdxVideoSub, mPresetSubLeft, mPresetSubTop,
                    mPresetSubWidth, mPresetSubHeight, 1.0f);
            mImgTexPreviewMixer.setRenderRect(mIdxCamera, mPresetMainLeft, mPresetMainTop,
                    mPresetMainWidth, mPresetMainHeight, 1.0f);
            mImgTexPreviewMixer.setRenderRect(mIdxVideoSub, mPresetSubLeft, mPresetSubTop,
                    mPresetSubWidth, mPresetSubHeight, 1.0f);
        }
    }

    public void authorize(String token, KMCAuthResultListener listener) {
        if (mRTCClient != null) {
            mRTCClient.authorize(token, listener);
        }
    }

    public void setBgPictureRect(float x, float y, float w, float h,
                                 float alpha, int mode) {
        getImgTexPreviewMixer().setRenderRect(mIdxBgPicture, x, y, w, h, alpha);
        getImgTexMixer().setRenderRect(mIdxBgPicture, x, y, w, h, alpha);

        getImgTexPreviewMixer().setScalingMode(mIdxBgPicture, mode);
        getImgTexMixer().setScalingMode(mIdxBgPicture, mode);
    }

    public void showBgPicture(Bitmap bitmap) {
        mPictureCapture.start(bitmap);
    }

    public void hideBgPicture() {
        mPictureCapture.stop();
    }
}
