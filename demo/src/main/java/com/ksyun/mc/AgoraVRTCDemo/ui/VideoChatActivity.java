package com.ksyun.mc.AgoraVRTCDemo.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ksyun.mc.AgoraVRTCDemo.R;
import com.ksyun.mc.AgoraVRTCDemo.kit.KMCAgoraStreamer;
import com.ksyun.mc.AgoraVRTCDemo.kit.KMCMultiUserRTCStreamer;
import com.ksyun.mc.AgoraVRTCDemo.utils.StreamConfig;
import com.ksyun.mc.AgoraVRTCDemo.utils.Utils;
import com.ksyun.mc.agoravrtc.KMCAgoraEventListener;
import com.ksyun.mc.agoravrtc.KMCAuthResultListener;
import com.ksyun.mc.agoravrtc.stats.OnLogEventListener;
import com.ksyun.media.streamer.capture.camera.CameraTouchHelper;
import com.ksyun.media.streamer.kit.KSYStreamer;
import com.ksyun.media.streamer.kit.StreamerConstants;

import java.io.IOException;
import java.util.Random;

/**
 * Created by sujia on 2017/11/28.
 */

public class VideoChatActivity extends Activity{
    private static final String TAG = "VideoChatActivity";

    private final static int PERMISSION_REQUEST_CAMERA_AUDIOREC = 1;

    private final static String CHANNEL_NAME ="CHANNEL_NAME";

    private final static String STREAM_URL_PREFIX = "rtmp://mobile.kscvbu.cn/live/kmc_";

    private GLSurfaceView mCameraPreviewView;
    private CameraHintView mCameraHintView;

    private ImageView mStopButton;
    private ImageView mMoreOtionButton;
    private ImageView mSwitchCameraButton;
    private ImageView mMuteButton;
    private ImageView mStreamButton;
    private ImageView mInfoButton;
    private TextView mStreamInfo;
    private ImageView mChangeRTCMode;

    private KMCMultiUserRTCStreamer mStreamer;
    private Handler mMainHandler;

    private String mRecordUrl = "/sdcard/rec_test.mp4";

    private boolean mIsStreaming = false;
    private boolean mIsFileRecording = false;
    private boolean mIsCalling = false;
    private boolean mMuteAudio = false;
    private boolean mShowMoreInfo = false;
    private boolean mShowStreamInfo = false;

    //频道名
    private String mChannelName;

    //推流参数设置
    private StreamConfig mStreamConfig;

    private boolean mHWEncoderUnsupported;
    private boolean mSWEncoderUnsupported;

    //默认模式，大小窗口(画中画)
    public static final int RTC_DEFAULT_MODE = 0;
    //PK模式 1：1窗口
    public static final int RTC_PK_MODE = 1;
    private int mRTCMode = RTC_DEFAULT_MODE;
    private Boolean mShowBgPicture = false;

    public static void startActivity(MainActivity mainActivity,String channelName) {
        Intent intent = new Intent(mainActivity, VideoChatActivity.class);
        intent.putExtra(CHANNEL_NAME,channelName);
        mainActivity.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.video_chat_activity);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        initView();
    }

    private void initConfig() {
        mStreamConfig = new StreamConfig(this);
        mStreamConfig.loadData();

        Intent intent = getIntent();
        mChannelName = intent.getStringExtra(CHANNEL_NAME);
    }

    private void initView() {
        initConfig();

        mCameraHintView = findViewById(R.id.camera_hint);
        mCameraPreviewView = findViewById(R.id.camera_preview);

        mStopButton = findViewById(R.id.stop_rtc);
        mMoreOtionButton = findViewById(R.id.more);
        mSwitchCameraButton = findViewById(R.id.switch_camera);
        mMuteButton = findViewById(R.id.mute_audio);
        mStreamButton = findViewById(R.id.stream);
        mInfoButton = findViewById(R.id.get_stream_info);
        mStreamInfo = findViewById(R.id.stream_info);
        mChangeRTCMode = findViewById(R.id.change_rtc_mode);

        showMoreOption(false);
        showStreamInfo(false);
        mStopButton.setOnClickListener(mClickListener);
        mMoreOtionButton.setOnClickListener(mClickListener);
        mSwitchCameraButton.setOnClickListener(mClickListener);
        mMuteButton.setOnClickListener(mClickListener);
        mStreamButton.setOnClickListener(mClickListener);
        mInfoButton.setOnClickListener(mClickListener);
        mChangeRTCMode.setOnClickListener(mClickListener);

        initStreamer();
        mShowBgPicture = true;
    }

    private void initStreamer() {
        mMainHandler = new Handler();
        String token = "7920903db27923b537ce1beedb976cd1";
        mStreamer = new KMCMultiUserRTCStreamer(this);

        //set stream config
        mStreamer.setPreviewResolution(mStreamConfig.getPreviewResolution());
        mStreamer.setTargetResolution(mStreamConfig.getTargetResolution());
        mStreamer.setPreviewFps(mStreamConfig.fps);
        mStreamer.setTargetFps(mStreamConfig.fps);
        mStreamer.setEncodeMethod(mStreamConfig.getEncodeMethod());
        mStreamer.setVideoCodecId(mStreamConfig.getVcodecId());
        mStreamer.setAudioEncodeProfile(mStreamConfig.getAcodecProfile());
        mStreamer.setVideoKBitrate(mStreamConfig.video_kbitrate);
        mStreamer.setAudioKBitrate(mStreamConfig.audio_kbitrate);

        //demo内每个用户都可以发起直播，这里取随机数作为流名
        String url = STREAM_URL_PREFIX + new Random().nextInt(1000000);
//        String url = STREAM_URL_PREFIX + "test";
        mStreamer.setUrl(url);
        String stream_info = String.format("房间号:\n %s \n播放地址：\n %s \n ", mChannelName, mStreamer.getUrl());
        mStreamInfo.setText(stream_info);

        //日志打点回调
        mStreamer.setRTCLogListener(new OnLogEventListener() {
            @Override
            public void onLogEvent(String log) {
                Log.d(TAG, log);
            }
        });
        //do kmc rtc auth
        mStreamer.authorize(token, new KMCAuthResultListener() {
            @Override
            public void onSuccess() {
                makeToast("鉴权成功");
                mMainHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!isFinishing()) {
                            onRTCClick();
                        }
                    }
                }, 1000);
            }

            @Override
            public void onFailure(int errCode) {
                makeToast("鉴权失败！  错误码: " + errCode);
            }
        });
        mStreamer.setOnRTCEventListener(new KMCAgoraEventListener() {
            @Override
            public void onEvent(int event, Object... data) {
                switch (event) {
                    case KMCAgoraEventListener.USER_JOINED: {
                        int uid = (int) data[0];
                        Log.d(TAG, "用户 " + uid + " 加入频道");
                        break;
                    }

                    case KMCAgoraEventListener.JOIN_CHANNEL_RESULT: {
                        boolean success = (Boolean) data[0];
                        if (success) {
                            Log.d(TAG, "加入频道成功");
                            makeToast("加入频道成功");
                        }
                        break;
                    }

                    case KMCAgoraEventListener.FIRST_FRAME_DECODED: {
                        break;
                    }

                    case KMCAgoraEventListener.LEAVE_CHANNEL: {
                        Log.d(TAG, "退出频道");
                        break;
                    }

                    case KMCAgoraEventListener.USER_OFFLINE: {
                        int uid = (int) data[0];
                        Log.d(TAG, "用户 " + uid + " 离开频道");
                        break;
                    }

                    case KMCAgoraEventListener.ERROR: {
                        int errorCode = (Integer) data[0];
                    }
                }
            }
        });

        //设置声网log保存路径
        try {
            mStreamer.setAgoraLogPath(
                    Environment.getExternalStorageDirectory().getCanonicalPath() + "/agora_log.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }

        mStreamer.setDisplayPreview(mCameraPreviewView);

        // touch focus and zoom support
        CameraTouchHelper cameraTouchHelper = new CameraTouchHelper();
        cameraTouchHelper.setCameraCapture(mStreamer.getCameraCapture());
        mCameraPreviewView.setOnTouchListener(cameraTouchHelper);
        // set CameraHintView to show focus rect and zoom ratio
        cameraTouchHelper.setCameraHintView(mCameraHintView);

        mStreamer.setOnInfoListener(mOnInfoListener);
        mStreamer.setOnErrorListener(mOnErrorListener);

        //for rtc sub screen
        cameraTouchHelper.addTouchListener(mSubScreenTouchListener);
    }

    // Example to handle camera related operation
    private void setCameraAntiBanding50Hz() {
        Camera.Parameters parameters = mStreamer.getCameraCapture().getCameraParameters();
        if (parameters != null) {
            parameters.setAntibanding(Camera.Parameters.ANTIBANDING_50HZ);
            mStreamer.getCameraCapture().setCameraParameters(parameters);
        }
    }

    private KSYStreamer.OnInfoListener mOnInfoListener = new KSYStreamer.OnInfoListener() {
        @Override
        public void onInfo(int what, int msg1, int msg2) {
            switch (what) {
                case StreamerConstants.KSY_STREAMER_CAMERA_INIT_DONE:
                    Log.d(TAG, "KSY_STREAMER_CAMERA_INIT_DONE");
                    setCameraAntiBanding50Hz();
                    break;
                case StreamerConstants.KSY_STREAMER_OPEN_STREAM_SUCCESS:
                    Log.d(TAG, "KSY_STREAMER_OPEN_STREAM_SUCCESS");
                    break;
                case StreamerConstants.KSY_STREAMER_OPEN_FILE_SUCCESS:
                    Log.d(TAG, "KSY_STREAMER_OPEN_FILE_SUCCESS");
                    break;
                case StreamerConstants.KSY_STREAMER_FRAME_SEND_SLOW:
                    Log.d(TAG, "KSY_STREAMER_FRAME_SEND_SLOW " + msg1 + "ms");
                    Toast.makeText(VideoChatActivity.this, "Network not good!",
                            Toast.LENGTH_SHORT).show();
                    break;
                case StreamerConstants.KSY_STREAMER_EST_BW_RAISE:
                    Log.d(TAG, "BW raise to " + msg1 / 1000 + "kbps");
                    break;
                case StreamerConstants.KSY_STREAMER_EST_BW_DROP:
                    Log.d(TAG, "BW drop to " + msg1 / 1000 + "kpbs");
                    break;
                case StreamerConstants.KSY_STREAMER_CAMERA_FACEING_CHANGED:
                    break;
                default:
                    Log.d(TAG, "OnInfo: " + what + " msg1: " + msg1 + " msg2: " + msg2);
                    break;
            }
        }
    };

    private void handleEncodeError() {
        int encodeMethod = mStreamer.getVideoEncodeMethod();
        if (encodeMethod == StreamerConstants.ENCODE_METHOD_HARDWARE) {
            mHWEncoderUnsupported = true;
            if (mSWEncoderUnsupported) {
                mStreamer.setEncodeMethod(
                        StreamerConstants.ENCODE_METHOD_SOFTWARE_COMPAT);
                Log.e(TAG, "Got HW encoder error, switch to SOFTWARE_COMPAT mode");
            } else {
                mStreamer.setEncodeMethod(StreamerConstants.ENCODE_METHOD_SOFTWARE);
                Log.e(TAG, "Got HW encoder error, switch to SOFTWARE mode");
            }
        } else if (encodeMethod == StreamerConstants.ENCODE_METHOD_SOFTWARE) {
            mSWEncoderUnsupported = true;
            if (mHWEncoderUnsupported) {
                mStreamer.setEncodeMethod(
                        StreamerConstants.ENCODE_METHOD_SOFTWARE_COMPAT);
                Log.e(TAG, "Got SW encoder error, switch to SOFTWARE_COMPAT mode");
            } else {
                mStreamer.setEncodeMethod(StreamerConstants.ENCODE_METHOD_HARDWARE);
                Log.e(TAG, "Got SW encoder error, switch to HARDWARE mode");
            }
        }
    }

    private KSYStreamer.OnErrorListener mOnErrorListener = new KSYStreamer.OnErrorListener() {
        @Override
        public void onError(int what, int msg1, int msg2) {
            String errMsg;
            switch (what) {
                case StreamerConstants.KSY_STREAMER_ERROR_DNS_PARSE_FAILED:
                    errMsg = "KSY_STREAMER_ERROR_DNS_PARSE_FAILED";
                    break;
                case StreamerConstants.KSY_STREAMER_ERROR_CONNECT_FAILED:
                    errMsg = "KSY_STREAMER_ERROR_CONNECT_FAILED";
                    break;
                case StreamerConstants.KSY_STREAMER_ERROR_PUBLISH_FAILED:
                    Log.d(TAG, "KSY_STREAMER_ERROR_PUBLISH_FAILED");
                    errMsg = "KSY_STREAMER_ERROR_PUBLISH_FAILED";
                    break;
                case StreamerConstants.KSY_STREAMER_ERROR_CONNECT_BREAKED:
                    errMsg = "KSY_STREAMER_ERROR_CONNECT_FAILED";
                    break;
                case StreamerConstants.KSY_STREAMER_ERROR_AV_ASYNC:
                    errMsg = "KSY_STREAMER_ERROR_AV_ASYNC " + msg1 + "ms";
                    break;
                case StreamerConstants.KSY_STREAMER_VIDEO_ENCODER_ERROR_UNSUPPORTED:
                    errMsg = "KSY_STREAMER_VIDEO_ENCODER_ERROR_UNSUPPORTED";
                    break;
                case StreamerConstants.KSY_STREAMER_VIDEO_ENCODER_ERROR_UNKNOWN:
                    errMsg = "KSY_STREAMER_VIDEO_ENCODER_ERROR_UNKNOWN";
                    break;
                case StreamerConstants.KSY_STREAMER_AUDIO_ENCODER_ERROR_UNSUPPORTED:
                    errMsg = "KSY_STREAMER_AUDIO_ENCODER_ERROR_UNSUPPORTED";
                    break;
                case StreamerConstants.KSY_STREAMER_AUDIO_ENCODER_ERROR_UNKNOWN:
                    errMsg = "KSY_STREAMER_AUDIO_ENCODER_ERROR_UNKNOWN";
                    break;
                case StreamerConstants.KSY_STREAMER_AUDIO_RECORDER_ERROR_START_FAILED:
                    errMsg = "KSY_STREAMER_AUDIO_RECORDER_ERROR_START_FAILED";
                    break;
                case StreamerConstants.KSY_STREAMER_AUDIO_RECORDER_ERROR_UNKNOWN:
                    errMsg = "KSY_STREAMER_AUDIO_RECORDER_ERROR_UNKNOWN";
                    break;
                case StreamerConstants.KSY_STREAMER_CAMERA_ERROR_UNKNOWN:
                    errMsg = "KSY_STREAMER_CAMERA_ERROR_UNKNOWN";
                    break;
                case StreamerConstants.KSY_STREAMER_CAMERA_ERROR_START_FAILED:
                    errMsg = "KSY_STREAMER_CAMERA_ERROR_START_FAILED";
                    break;
                case StreamerConstants.KSY_STREAMER_CAMERA_ERROR_SERVER_DIED:
                    errMsg = "KSY_STREAMER_CAMERA_ERROR_SERVER_DIED";
                    break;
                //Camera was disconnected due to use by higher priority user.
                case StreamerConstants.KSY_STREAMER_CAMERA_ERROR_EVICTED:
                    errMsg = "KSY_STREAMER_CAMERA_ERROR_EVICTED";
                    break;
                default:
                    Log.d(TAG, "what=" + what + " msg1=" + msg1 + " msg2=" + msg2);
                    errMsg = "what=" + what + " msg1=" + msg1 + " msg2=" + msg2;
                    break;
            }

            if (!Utils.isNetworkConnectionAvailable(getApplicationContext())) {
                makeToast("网络连接失败，请检查您的网络设置");
            } else {
                makeToast("推流失败: " + errMsg + "， 请稍后重试");
            }

            switch (what) {
                case StreamerConstants.KSY_STREAMER_AUDIO_RECORDER_ERROR_START_FAILED:
                case StreamerConstants.KSY_STREAMER_AUDIO_RECORDER_ERROR_UNKNOWN:
                    break;
                case StreamerConstants.KSY_STREAMER_CAMERA_ERROR_UNKNOWN:
                case StreamerConstants.KSY_STREAMER_CAMERA_ERROR_START_FAILED:
                case StreamerConstants.KSY_STREAMER_CAMERA_ERROR_EVICTED:
                case StreamerConstants.KSY_STREAMER_CAMERA_ERROR_SERVER_DIED:
                    mStreamer.stopCameraPreview();
                    break;
                case StreamerConstants.KSY_STREAMER_ERROR_DNS_PARSE_FAILED:
                case StreamerConstants.KSY_STREAMER_ERROR_CONNECT_FAILED:
                case StreamerConstants.KSY_STREAMER_ERROR_PUBLISH_FAILED:
                case StreamerConstants.KSY_STREAMER_ERROR_CONNECT_BREAKED:
                case StreamerConstants.KSY_STREAMER_ERROR_AV_ASYNC:
                    stopStream();
                    break;
                case StreamerConstants.KSY_STREAMER_FILE_PUBLISHER_CLOSE_FAILED:
                case StreamerConstants.KSY_STREAMER_FILE_PUBLISHER_ERROR_UNKNOWN:
                case StreamerConstants.KSY_STREAMER_FILE_PUBLISHER_OPEN_FAILED:
                case StreamerConstants.KSY_STREAMER_FILE_PUBLISHER_FORMAT_NOT_SUPPORTED:
                case StreamerConstants.KSY_STREAMER_FILE_PUBLISHER_WRITE_FAILED:
                    stopRecord();
                    break;
                case StreamerConstants.KSY_STREAMER_VIDEO_ENCODER_ERROR_UNSUPPORTED:
                case StreamerConstants.KSY_STREAMER_VIDEO_ENCODER_ERROR_UNKNOWN:
                    handleEncodeError();
                    if (mIsStreaming) {
                        stopStream();
                        mMainHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                startStream();
                            }
                        }, 3000);
                    }
                    if (mIsFileRecording) {
                        stopRecord();
                        mMainHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                startRecord();
                            }
                        }, 50);
                    }
                    break;
                default:
                    stopStream();
                    break;
            }
        }
    };

    private void makeToast(final String str) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast toast = Toast.makeText(VideoChatActivity.this, str, Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        mStreamer.setDisplayPreview(mCameraPreviewView);
        mStreamer.onResume();
        mCameraHintView.hideAll();

        startCameraPreviewWithPermCheck();
        if (mShowBgPicture) {
            showBgPicture();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mStreamer.onPause();
        // setOffscreenPreview to enable camera capture in background
        mStreamer.setOffscreenPreview(mStreamer.getPreviewWidth(),
                mStreamer.getPreviewHeight());

        if (mShowBgPicture) {
            hideBgPicture();
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mMainHandler != null) {
            mMainHandler.removeCallbacksAndMessages(null);
            mMainHandler = null;
        }
        mStreamer.setOnLogEventListener(null);
        mStreamer.release();
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                onBackoffClick();
                return true;
            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }


    private void onBackoffClick() {
        new AlertDialog.Builder(VideoChatActivity.this).setCancelable(true)
                .setTitle("结束连麦?")
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {

                    }
                })
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        stopStream();
                        stopRTC();
                        finish();
                    }
                }).show();
    }

    private void startCameraPreviewWithPermCheck() {
        int cameraPerm = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int audioPerm = ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        if (cameraPerm != PackageManager.PERMISSION_GRANTED ||
                audioPerm != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                Log.e(TAG, "No CAMERA or AudioRecord permission, please check");
                Toast.makeText(this, "No CAMERA or AudioRecord permission, please check",
                        Toast.LENGTH_LONG).show();
            } else {
                String[] permissions = {Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.READ_EXTERNAL_STORAGE};
                ActivityCompat.requestPermissions(this, permissions,
                        PERMISSION_REQUEST_CAMERA_AUDIOREC);
            }
        } else {
            mStreamer.startCameraPreview();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CAMERA_AUDIOREC: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mStreamer.startCameraPreview();
                } else {
                    Log.e(TAG, "No CAMERA or AudioRecord permission");
                    Toast.makeText(this, "No CAMERA or AudioRecord permission",
                            Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }

    //start streaming
    private void startStream() {
        mStreamer.startStream();
        mIsStreaming = true;

        updateStartButtonIcon();
    }


    private void stopStream() {
        mStreamer.stopStream();
        mIsStreaming = false;

        updateStartButtonIcon();
    }

    //start recording to a local file
    private void startRecord() {
        mStreamer.startRecord(mRecordUrl);
        mIsFileRecording = true;
    }

    private void stopRecord() {
        mStreamer.stopRecord();
        mIsFileRecording = false;
    }

    private void startRTC() {
        mStreamer.startRTC(mChannelName);
        mIsCalling = true;
    }

    private void stopRTC() {
        mStreamer.stopRTC();
        mIsCalling = false;
    }

    private void onRecordClick() {
        if (mIsFileRecording) {
            stopRecord();
        } else {
            startRecord();
        }
    }

    private void onRTCClick() {
        if (mIsCalling) {
            stopRTC();
        } else {
            startRTC();
        }
    }

    Button.OnClickListener mClickListener = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.stop_rtc: {
                    onStopButtonClicked();
                    break;
                }

                case R.id.more: {
                    onMoreOptionClicked();
                    break;
                }

                case R.id.switch_camera: {
                    onSwitchCamera();
                    break;
                }

                case R.id.mute_audio: {
                    onMuteAudio();
                    break;
                }

                case R.id.stream: {
                    onStreamerButtonClicked();
                    break;
                }

                case R.id.get_stream_info: {
                    onInfoButtonClicked();
                    break;
                }

                case R.id.change_rtc_mode: {
                    onChangeRTCModeClicked();
                    break;
                }
            }
        }
    };

    private void onStopButtonClicked() {
        onBackoffClick();
    }

    private void showMoreOption(boolean enable) {
        int visibility = enable ? View.VISIBLE : View.GONE;
        mSwitchCameraButton.setVisibility(visibility);
        mMuteButton.setVisibility(visibility);
        mStreamButton.setVisibility(visibility);
        mInfoButton.setVisibility(visibility);
        mChangeRTCMode.setVisibility(visibility);
    }

    private void onMoreOptionClicked() {
        mShowMoreInfo = !mShowMoreInfo;
        showMoreOption(mShowMoreInfo);
    }

    private void onSwitchCamera() {
        mStreamer.switchCamera();
        mCameraHintView.hideAll();
    }

    private void onMuteAudio() {
       if (mMuteAudio) {
           mMuteAudio = false;
           mMuteButton.setImageResource(R.drawable.audio_on);
       } else {
           mMuteAudio = true;
           mMuteButton.setImageResource(R.drawable.audio_off);
       }
       mStreamer.setMuteAudio(mMuteAudio);
    }

    private void onStreamerButtonClicked() {
        if (mIsStreaming) {
            stopStream();
        } else {
            startStream();
        }
    }

    private void updateStartButtonIcon() {
        if (mIsStreaming) {
            mStreamButton.setImageResource(R.drawable.stream_off);
        } else {
            mStreamButton.setImageResource(R.drawable.stream_on);
        }
    }

    private void showStreamInfo(boolean show) {
        mStreamInfo.setVisibility(show ? View.VISIBLE : View.GONE);
    }


    private void onInfoButtonClicked() {
        mShowStreamInfo = !mShowStreamInfo;
        showStreamInfo(mShowStreamInfo);
    }

    private void onChangeRTCModeClicked() {
        if (mStreamer.getRTCUserCount() != 2) {
            makeToast("只在两人连麦时可修改RTC模式");
            return;
        }
        if (mRTCMode == RTC_DEFAULT_MODE) {
            mRTCMode = RTC_PK_MODE;
            mChangeRTCMode.setImageResource(R.drawable.pk_mode);
        } else {
            mRTCMode = RTC_DEFAULT_MODE;
            mChangeRTCMode.setImageResource(R.drawable.default_mode);
        }

        changeRTCMode();
    }

    private void changeRTCMode() {
        if (mRTCMode == RTC_DEFAULT_MODE) {
            //设置连麦时小窗口位置尺寸
            mStreamer.setRTCSubScreenRect(0.6f, 0.05f, 0.35f, 0.35f, KMCAgoraStreamer
                    .SCALING_MODE_CENTER_CROP);
            //设置连麦时本地camera窗口位置尺寸
            mStreamer.setRTCMainScreenRect(0.f, 0.f, 1.0f, 1.0f,
                    KMCAgoraStreamer.SCALING_MODE_CENTER_CROP);
        } else if (mRTCMode == RTC_PK_MODE) {
            //设置连麦时小窗口位置尺寸
            mStreamer.setRTCSubScreenRect(0.5f, 0.25f, 0.5f, 0.5f, KMCAgoraStreamer
                    .SCALING_MODE_CENTER_CROP);
            //设置连麦时本地camera窗口位置尺寸
            mStreamer.setRTCMainScreenRect(0.f, 0.25f, 0.5f, 0.5f,
                    KMCAgoraStreamer.SCALING_MODE_CENTER_CROP);
        }
        if (mStreamer.getRTCUserCount() == 2) {
            mStreamer.updateRTCScreen();
        }
    }

    public void showBgPicture() {
        mStreamer.setBgPictureRect(0.f, 0.f, 1.0f, 1.0f,
                1.0f, KMCMultiUserRTCStreamer.SCALING_MODE_CENTER_CROP);
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.stream_bg);
        mStreamer.showBgPicture(bitmap);
    }

    public void hideBgPicture() {
        mStreamer.hideBgPicture();
    }


    /***********************************
     * for sub move&switch
     ********************************/
    private float mSubTouchStartX;
    private float mSubTouchStartY;
    private float mLastRawX;
    private float mLastRawY;
    private float mLastX;
    private float mLastY;
    private float mSubMaxX = 0;   //小窗可移动的最大X轴距离
    private float mSubMaxY = 0;  //小窗可以移动的最大Y轴距离
    private boolean mIsSubMoved = false;  //小窗是否移动过了，如果移动过了，ACTION_UP时不触发大小窗内容切换
    private int SUB_TOUCH_MOVE_MARGIN = 30;  //触发移动的最小距离

    private CameraTouchHelper.OnTouchListener mSubScreenTouchListener = new CameraTouchHelper.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            //获取相对屏幕的坐标，即以屏幕左上角为原点
            mLastRawX = event.getRawX();
            mLastRawY = event.getRawY();
            // 预览区域的大小
            int width = view.getWidth();
            int height = view.getHeight();
            //小窗的位置信息
            RectF subRect = mStreamer.getSubScreenRect();
            int left = (int) (subRect.left * width);
            int right = (int) (subRect.right * width);
            int top = (int) (subRect.top * height);
            int bottom = (int) (subRect.bottom * height);
            int subWidth = right - left;
            int subHeight = bottom - top;


            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    //只有在小屏区域才触发位置改变
                    if (isSubScreenArea(event.getX(), event.getY(), left, right, top, bottom)) {
                        //获取相对sub区域的坐标，即以sub左上角为原点
                        mSubTouchStartX = event.getX() - left;
                        mSubTouchStartY = event.getY() - top;
                        mLastX = event.getX();
                        mLastY = event.getY();
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    int moveX = (int) Math.abs(event.getX() - mLastX);
                    int moveY = (int) Math.abs(event.getY() - mLastY);
                    if (mSubTouchStartX > 0f && mSubTouchStartY > 0f && (
                            (moveX > SUB_TOUCH_MOVE_MARGIN) ||
                                    (moveY > SUB_TOUCH_MOVE_MARGIN))) {
                        //触发移动
                        mIsSubMoved = true;
                        //updateSubPosition(width, height, subWidth, subHeight);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    //未移动并且在小窗区域，则触发大小窗切换
                    if (!mIsSubMoved && isSubScreenArea(event.getX(), event.getY(), left, right,
                            top, bottom)) {
                        mStreamer.switchMainScreen();
                    }

                    mIsSubMoved = false;
                    mSubTouchStartX = 0f;
                    mSubTouchStartY = 0f;
                    mLastX = 0f;
                    mLastY = 0f;
                    break;
            }

            return true;
        }
    };

    /**
     * 是否在小窗区域移动
     *
     * @param x      当前点击的相对小窗左上角的x坐标
     * @param y      当前点击的相对小窗左上角的y坐标
     * @param left   小窗左上角距离预览区域左上角的x轴距离
     * @param right  小窗右上角距离预览区域左上角的x轴距离
     * @param top    小窗左上角距离预览区域左上角的y轴距离
     * @param bottom 小窗右上角距离预览区域左上角的y轴距离
     * @return
     */
    private boolean isSubScreenArea(float x, float y, int left, int right, int top, int bottom) {
        //只在两人连麦，画中画模式下切换窗口
        if (mRTCMode != RTC_DEFAULT_MODE ||
                mStreamer.getRTCUserCount() != 2) {
            return false;
        }

        if (x > left && x < right &&
                y > top && y < bottom) {
            return true;
        }

        return false;
    }

    /**
     * 触发移动小窗
     *
     * @param screenWidth 预览区域width
     * @param sceenHeight 预览区域height
     * @param subWidth    小窗区域width
     * @param subHeight   小窗区域height
     */
    private void updateSubPosition(int screenWidth, int sceenHeight, int subWidth, int subHeight) {
        mSubMaxX = screenWidth - subWidth;
        mSubMaxY = sceenHeight - subHeight;

        //更新浮动窗口位置参数
        float newX = (mLastRawX - mSubTouchStartX);
        float newY = (mLastRawY - mSubTouchStartY);

        //不能移出预览区域最左边和最上边
        if (newX < 0) {
            newX = 0;
        }

        if (newY < 0) {
            newY = 0;
        }

        //不能移出预览区域最右边和最下边
        if (newX > mSubMaxX) {
            newX = mSubMaxX;
        }

        if (newY > mSubMaxY) {
            newY = mSubMaxY;
        }
        //小窗的width和height不发生变化
        RectF subRect = mStreamer.getSubScreenRect();
        float width = subRect.width();
        float height = subRect.height();

        float left = newX / screenWidth;
        float top = newY / sceenHeight;

        mStreamer.setRTCSubScreenRect(left, top, width, height,
                KMCAgoraStreamer.SCALING_MODE_CENTER_CROP);
    }
}
