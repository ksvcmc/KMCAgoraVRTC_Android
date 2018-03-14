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
import com.ksyun.mc.agoravrtc.Constants;
import com.ksyun.mc.agoravrtc.KMCAgoraEventListener;
import com.ksyun.mc.agoravrtc.KMCAuthResultListener;
import com.ksyun.mc.agoravrtc.stats.OnLogEventListener;
import com.ksyun.media.streamer.capture.ImgTexSrcPin;
import com.ksyun.media.streamer.filter.imgbuf.ImgBufScaleFilter;
import com.ksyun.media.streamer.filter.imgtex.ImgTexMixer;
import com.ksyun.media.streamer.filter.imgtex.ImgTexScaleFilter;
import com.ksyun.media.streamer.framework.ImgBufFormat;
import com.ksyun.media.streamer.kit.KSYStreamer;

import java.util.List;

/**
 * Created by sujia on 2017/7/17.
 */
public class KMCMultiUserRTCStreamer extends KSYStreamer {
    private static final String TAG = "KMCMultiUserRTCStreamer";
    private static final boolean DEBUG = false;

    protected int mIdxBgPicture = 0;
    private static final int mIdxAudioRemote = 2;

    private KMCMultiUserRTCClient mRTCClient;
    private ImgTexScaleFilter mRTCRemoteImgTexScaleFilter;
    private ImgTexScaleFilter mRTCImgTexScaleFilter;
    private ImgBufScaleFilter mImgBufScale;

    public static final int RTC_MAIN_SCREEN_CAMERA = 1;
    public static final int RTC_MAIN_SCREEN_REMOTE = 2;
    private int mRTCMainScreen = RTC_MAIN_SCREEN_REMOTE;

    public static final int SCALING_MODE_FULL_FILL = ImgTexMixer.SCALING_MODE_FULL_FILL;
    public static final int SCALING_MODE_BEST_FIT = ImgTexMixer.SCALING_MODE_BEST_FIT;
    public static final int SCALING_MODE_CENTER_CROP = ImgTexMixer.SCALING_MODE_CENTER_CROP;

    private MusicIntentReceiver mHeadSetReceiver;
    private boolean mHeadSetPlugged = false;
    private boolean mIsCalling = false;
    private boolean mIsRemoteConnected = false;

    private boolean mMuteAudio;
    private PictureCapture mPictureCapture;

    public KMCMultiUserRTCStreamer(Context context) {
        super(context.getApplicationContext());
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
        mRTCClient = new KMCMultiUserRTCClient(mGLRender, mContext);

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
                        Log.i(TAG, "USER_JOINED uid: " + uid);

                        mRTCClient.startReceiveRemoteData();
                        mRTCClient.addUser(uid);
                        break;
                    }

                    case KMCAgoraEventListener.JOIN_CHANNEL_RESULT: {
                        boolean success = (Boolean) data[0];
                        if (success) {
                            Log.d(TAG, "join channel success");
                        }
                        break;
                    }

                    case KMCAgoraEventListener.FIRST_FRAME_DECODED: {
                        int uid = (int) data[0];
                        int weight = (int) data[1];
                        int height = (int) data[2];
                        Log.d(TAG, "onFirstRemoteVideoDecoded, uid:" + uid + " , weight:" + weight
                        + " height:" + height);

                        setAudioMode(mHeadSetPlugged ? AudioManager.MODE_IN_COMMUNICATION :
                                AudioManager.MODE_NORMAL);
                        mIsRemoteConnected = true;
                        updateRTCScreen();
                        break;
                    }

                    case KMCAgoraEventListener.LEAVE_CHANNEL: {
                        mIsRemoteConnected = false;
                        Log.d(TAG, "leave channel");

                        mRTCClient.stopReceiveRemoteData();
                        mRTCClient.clearSrcPins();
                        updateRTCScreen();

                        if(!mIsCalling) {
                            if ((mIsRecording || mIsFileRecording) &&
                                    !mAudioCapture.isRecordingState()) {
                                mAudioCapture.start();
                            }
                        }
                        break;
                    }

                    case KMCAgoraEventListener.USER_OFFLINE: {
                        int uid = (int) data[0];
                        Log.i(TAG, "USER_OFFLINE uid: " + uid);

                        mRTCClient.removeUser(uid);
                        if (!mRTCClient.hasRemoteConnected()) {
                            mIsRemoteConnected = false;
                        }
                        updateRTCScreen();
                        break;
                    }

                    case KMCAgoraEventListener.ERROR: {
                        int errorCode = (Integer) data[0];
                        Log.i(TAG, "error: " + errorCode);
                        if (errorCode == AgoraErrorCode.ERR_INVALID_APP_ID) {
                            Log.e(TAG, "invalid app id");
                        }
                    }
                }
            }
        });

        //pin 0-4 用做背景图以及rtc画面
        mIdxWmLogo = 5;
        mIdxWmTime = 6;
        updateRTCScreen();

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
        super.setRotateDegrees(degrees);
    }

    @Override
    public void setMuteAudio(boolean isMute) {
        super.setMuteAudio(isMute);
        //对所有远端用户进行静音与否
        mRTCClient.muteAllRemoteAudioStreams(isMute);
        //允许/禁止往网络发送本地音频流
        mRTCClient.muteLocalAudioStream(isMute);

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
//        mRTCClient.muteAllRemoteAudioStreams(mMuteAudio);
        //允许/禁止往网络发送本地音频流
        mRTCClient.muteLocalAudioStream(mMuteAudio);
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
     * Release all resources used by KMCMultiUserRTCStreamer.
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

    public static String getAgoraRTCVersion() {
        return Constants.VERSION;
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

    public void authorize(String token, KMCAuthResultListener listener) {
        if (mRTCClient != null) {
            mRTCClient.authorize(token, listener);
        }
    }

    public void setOnRTCEventListener(KMCAgoraEventListener listener) {
        mRTCClient.registerEventListener(listener);
    }

    public void updateRTCScreen() {
        List<Integer> userList = mRTCClient.getUserList();
        if ( userList != null) {
            //远端用户人数
            int remote_user_count = userList.size();
            //默认先连接remote user，camera作为左后一个index
            int camera_index = remote_user_count < KMCMultiUserRTCClient.MAX_REMOTE_USER ?
                    remote_user_count + 1 : KMCMultiUserRTCClient.MAX_REMOTE_USER + 1;
            int remote_user_start_idx = 1;

            //当以camera作为主屏幕（大窗口）显示时，将camera index设置为背景图（0）后第一个index
            if (remote_user_count == 1 &&
                    mRTCMainScreen == RTC_MAIN_SCREEN_CAMERA) {
                camera_index = 1;
                remote_user_start_idx = 2;
            }

            //顺序连接remote user和mixer的对应管脚
            for (int i = 0; i < remote_user_count &&
                    i < KMCMultiUserRTCClient.MAX_REMOTE_USER; i++) {
                int uid = userList.get(i);
                ImgTexSrcPin srcPin = mRTCClient.getImgTexSrcPin(uid);
                srcPin.disconnect(false);
                int index = i + remote_user_start_idx;
                srcPin.connect(mImgTexMixer.getSinkPin(index));
                srcPin.connect(mImgTexPreviewMixer.getSinkPin(index));
            }

            mImgTexFilterMgt.getSrcPin().disconnect(false);
            mImgTexFilterMgt.getSrcPin().connect(mRTCImgTexScaleFilter.getSinkPin());
            mImgTexFilterMgt.getSrcPin().connect(mImgTexMixer.getSinkPin(camera_index));
            mImgTexFilterMgt.getSrcPin().connect(mImgTexPreviewMixer.getSinkPin(camera_index));
            mImgTexMixer.setMainSinkPinIndex(camera_index);
            mImgTexPreviewMixer.setMainSinkPinIndex(camera_index);

            int mode  = SCALING_MODE_CENTER_CROP;
            if (remote_user_count == 0) {
                //只有自己，camera全屏
                setMixerRect(camera_index, 0.f, 0.f, 1.f, 1.f, mode);
            } else if (remote_user_count == 1) {
                //两人连麦的窗口位置及大小根据设置进行
                if (mRTCMainScreen == RTC_MAIN_SCREEN_REMOTE) {
                    //remote user画面作为主屏幕
                    setMixerRect(1, mPresetMainLeft, mPresetMainTop,
                            mPresetMainWidth, mPresetMainHeight, mPresetMainMode);
                    setMixerRect(2, mPresetSubLeft, mPresetSubTop,
                            mPresetSubWidth, mPresetSubHeight, mPresetSubMode);
                } else {
                    //本地camera画面作为主屏幕
                    setMixerRect(1, mPresetMainLeft, mPresetMainTop,
                            mPresetMainWidth, mPresetMainHeight, mPresetMainMode);
                    setMixerRect(2, mPresetSubLeft, mPresetSubTop,
                            mPresetSubWidth, mPresetSubHeight, mPresetSubMode);
                }
            } else if (remote_user_count == 2) {
                //3人连麦，remote user平分屏幕上半部分， camera居中显示在下半部分
                setMixerRect(1, 0.f, 0.f, 0.5f, 0.5f, mode);
                setMixerRect(2, 0.5f, 0.f, 0.5f, 0.5f, mode);
                setMixerRect(3, 0.25f, 0.5f, 0.5f, 0.5f, mode);
            } else if (remote_user_count >= KMCMultiUserRTCClient.MAX_REMOTE_USER) { //demo最多支持4人连麦
                //4人连麦，4人4等分屏幕，camera显示在右下角
                setMixerRect(1, 0.f, 0.f, 0.5f, 0.5f, mode);
                setMixerRect(2, 0.5f, 0.f, 0.5f, 0.5f, mode);
                setMixerRect(3, 0.f, 0.5f, 0.5f, 0.5f, mode);
                setMixerRect(4, 0.5f, 0.5f, 0.5f, 0.5f, mode);
            }
        }
    }

    private void setMixerRect(int index, float left, float top, float width,
                              float height, int mode) {
        if (index < 0 || index > mImgTexMixer.getSinkPinNum()) {
            Log.e(TAG, "index out of bounds of mixer sin pin number");
            return;
        }

        mImgTexMixer.setRenderRect(index, left, top, width, height, 1.0f);
        mImgTexPreviewMixer.setRenderRect(index, left, top, width, height, 1.0f);

        mImgTexMixer.setScalingMode(index, mode);
        mImgTexPreviewMixer.setScalingMode(index, mode);
    }

    private float mPresetSubLeft = 0.6f;
    private float mPresetSubTop = 0.05f;
    private float mPresetSubWidth = 0.35f;
    private float mPresetSubHeight = 0.35f;
    private int mPresetSubMode = SCALING_MODE_CENTER_CROP;

    private float mPresetMainLeft = 0.f;
    private float mPresetMainTop = 0.f;
    private float mPresetMainWidth = 1.f;
    private float mPresetMainHeight = 1.f;
    private int mPresetMainMode = SCALING_MODE_CENTER_CROP;

    /**
     * 设置rtc远端图像窗口大小和位置，只在两人连麦时生效
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
    }

    /**
     *  设置本地camera图像窗口大小和位置，只在两人连麦时生效
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
    }

    /**
     * get rect for sub sceen
     * @return
     */
    public RectF getSubScreenRect() {
        return new RectF(mPresetSubLeft, mPresetSubTop, mPresetSubWidth + mPresetSubLeft,
                mPresetSubHeight + mPresetSubTop);
    }

    /**
     * 设置主屏幕，只在两人连麦时生效
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

    public void switchMainScreen() {
        if (mRTCMainScreen == RTC_MAIN_SCREEN_REMOTE) {
            mRTCMainScreen = RTC_MAIN_SCREEN_CAMERA;
        } else if (mRTCMainScreen == RTC_MAIN_SCREEN_CAMERA) {
            mRTCMainScreen = RTC_MAIN_SCREEN_REMOTE;
        }

        setRTCMainScreen(mRTCMainScreen);
        updateRTCScreen();
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

    public KMCMultiUserRTCClient getRTCClient() {
        return mRTCClient;
    }

    public int getRTCUserCount() {
        return mRTCClient.getUserList().size() + 1;
    }

    public void setAgoraLogPath(String path) {
        mRTCClient.setLogPath(path);
    }

    /**
     * enable stat module or not
     * the stat module is enabled by default
     * @param enable
     */
    public void enableStat(boolean enable) {
        mRTCClient.enableStat(enable);
    }

    /**
     * set the log listener
     * @param listener
     */
    public void setRTCLogListener(OnLogEventListener listener) {
        mRTCClient.setLogListener(listener);
    }

    /**
     * 该方法调节录音信号音量
     * @param volume 录音信号音量可在 0~400 范围内进行调节:
     *               0: 静音
     *               100: 原始音量
     *               400: 最大可为原始音量的 4 倍(自带溢出保护)
     * @return 0: 方法调用成功, <0: 方法调用失败
     */
    public int adjustRecordingSignalVolume(int volume) {
        return mRTCClient.getRTCWrapper().adjustRecordingSignalVolume(volume);
    }

    /**
     * 该方法调节播放信号音量
     * @param volume 播放信号音量可在 0~400 范围内进行调节:
     *               0: 静音
     *               100: 原始音量
     *               400: 最大可为原始音量的 4 倍(自带溢出保护)
     * @return 0: 方法调用成功, <0: 方法调用失败
     */
    public int adjustPlaybackSignalVolume(int volume) {
        return mRTCClient.getRTCWrapper().adjustPlaybackSignalVolume(volume);
    }
}
