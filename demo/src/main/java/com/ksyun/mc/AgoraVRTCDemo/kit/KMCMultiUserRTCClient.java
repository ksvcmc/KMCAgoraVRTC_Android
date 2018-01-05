package com.ksyun.mc.AgoraVRTCDemo.kit;

import android.content.Context;
import android.opengl.GLES20;
import android.os.Build;
import android.util.Log;

import com.ksyun.mc.agoravrtc.AudioFormat;
import com.ksyun.mc.agoravrtc.KMCAgoraEventListener;
import com.ksyun.mc.agoravrtc.KMCAgoraVRTC;
import com.ksyun.mc.agoravrtc.KMCAgoraVRTCCallback;
import com.ksyun.mc.agoravrtc.KMCAuthResultListener;
import com.ksyun.mc.agoravrtc.VideoFormat;
import com.ksyun.mc.agoravrtc.stats.OnLogEventListener;
import com.ksyun.media.streamer.capture.ImgTexSrcPin;
import com.ksyun.media.streamer.encoder.ImgTexToBuf;
import com.ksyun.media.streamer.framework.AVConst;
import com.ksyun.media.streamer.framework.AudioBufFormat;
import com.ksyun.media.streamer.framework.AudioBufFrame;
import com.ksyun.media.streamer.framework.ImgBufFrame;
import com.ksyun.media.streamer.framework.ImgTexFormat;
import com.ksyun.media.streamer.framework.ImgTexFrame;
import com.ksyun.media.streamer.framework.PinAdapter;
import com.ksyun.media.streamer.framework.SinkPin;
import com.ksyun.media.streamer.framework.SrcPin;
import com.ksyun.media.streamer.util.gles.GLRender;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import io.agora.rtc.video.AgoraVideoFrame;

/**
 * Created by sujia on 2017/7/17.
 */

public class KMCMultiUserRTCClient implements KMCAgoraVRTCCallback, KMCAuthResultListener {
    private final static String TAG = KMCMultiUserRTCClient.class.getSimpleName();
    private KMCAgoraVRTC mRTCWrapper;

    private PinAdapter<ImgBufFrame> mImgBufSinkPinAdapter;
    private PinAdapter<ImgTexFrame> mImgTexSinkPinAdapter;

    public final static int MAX_REMOTE_USER = 3;
    private List<Integer> mUserList;
    private HashMap<Integer, ImgTexSrcPin> mImgTexSrcPins;
    private HashMap<Integer, ImgTexFormat> mImgTexFormats;

    //send data to rtc
    private SinkPin<ImgBufFrame> mVideoSinkPin;
    private SinkPin<ImgTexFrame> mImgTexSinkPin;

    private AudioBufFormat mRemoteAudioBufFormat;
    private AudioBufFormat mLocalAudioBufFormat;

    private SrcPin<AudioBufFrame> mRemoteAudioSrcPin;
    private SrcPin<AudioBufFrame> mLocalAudioSrcPin;

    private GLRender mGLRender;

    private KMCAuthResultListener mAuthResultListener;
    private ImgTexToBuf mRTCImgTexToBuf;

    private boolean mRTCStarted = false;
    private boolean mMuteLocalAudio = false;
    private boolean mMuteRemoteAudios = false;
    private boolean mMuteLocalVideo = false;
    private boolean mMuteRemoteVideos = false;
    private boolean DEBUG = false;

    public KMCMultiUserRTCClient(GLRender glRender, Context context) {
        this.mRTCWrapper = new KMCAgoraVRTC(context);
        mGLRender = glRender;
        mRTCWrapper.setDataCallback(this);

        mImgBufSinkPinAdapter = new PinAdapter<>();
        mImgTexSinkPinAdapter = new PinAdapter<>();

        mUserList = new LinkedList<>();
        mImgTexFormats = new HashMap<>();
        mImgTexSrcPins = new HashMap<>();

        mVideoSinkPin = new RTCLocalImgSinkPin();
        mImgTexSinkPin = new RTCLocalImgTexSinkPin();

        mRemoteAudioSrcPin = new SrcPin<>();
        mLocalAudioSrcPin = new SrcPin<>();
    }

    public void authorize(String token, KMCAuthResultListener listener) {
        if (mRTCWrapper != null) {
            mAuthResultListener = listener;
            mRTCWrapper.authorize(token, true, this);
        }
    }

    /**
     * enable stat module or not
     * the stat module is enabled by default
     * @param enable
     */
    public void enableStat(boolean enable) {
        mRTCWrapper.enableStat(enable);
    }

    /**
     * set the log listener
     * @param listener
     */
    public void setLogListener(OnLogEventListener listener) {
        mRTCWrapper.setLogListener(listener);
    }

    @Override
    public void onSuccess() {
        if (mAuthResultListener != null) {
            mAuthResultListener.onSuccess();
        }

        setUpSinkPinConnection();
    }

    private void setUpSinkPinConnection() {
        mImgTexSinkPinAdapter.mSrcPin.disconnect(false);
        mImgBufSinkPinAdapter.mSrcPin.disconnect(false);

        /*
         * 优先使用美颜后texture的形式发送视频
         * 如果当前设备不支持texture硬编码的话，则
         * 1.  4.4及以上版本的设备使用ImgTexToBuf将美颜后texture数据转换为yuv数据
         * 2.  4.4以下版本的设备直接使用摄像头yuv数据(没有美颜)
         */
        if (mRTCWrapper.getMediaManager().getRtcEngine().isTextureEncodeSupported()) {
            mImgTexSinkPinAdapter.mSrcPin.connect(mImgTexSinkPin);
            //设置使用texture数据作为视频外部数据源
            mRTCWrapper.setExternalVideoSource(true, true, true);
        } else {
            mRTCWrapper.setExternalVideoSource(true, false, true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                mRTCImgTexToBuf = new ImgTexToBuf(mGLRender);
                mImgTexSinkPinAdapter.mSrcPin.connect(mRTCImgTexToBuf.mSinkPin);

                //send local video to rtc module sink pin
                mRTCImgTexToBuf.mSrcPin.connect(mVideoSinkPin);
                mRTCImgTexToBuf.start();
            } else {
                mImgBufSinkPinAdapter.mSrcPin.connect(mVideoSinkPin);
            }
        }
    }

    @Override
    public void onFailure(int errCode) {
        if (mAuthResultListener != null) {
            mAuthResultListener.onFailure(errCode);
        }
    }

    synchronized public void release() {
        if (mRTCWrapper != null) {
            mRTCWrapper.release();
        }
        mRTCWrapper = null;

        clearSrcPins();
    }

    synchronized public void clearSrcPins() {
        if (mUserList != null) {
            mUserList.clear();
        }
        if (mImgTexSrcPins != null) {
            for (HashMap.Entry<Integer, ImgTexSrcPin> entry : mImgTexSrcPins.entrySet()) {
                entry.getValue().disconnect(false);
            }
            mImgTexSrcPins.clear();
        }
        if (mImgTexFormats != null) {
            mImgTexFormats.clear();
        }
    }

    public SinkPin<ImgBufFrame> getVideoSinkPin() {
        return mImgBufSinkPinAdapter.mSinkPin;
    }

    public SinkPin<ImgTexFrame> getImgTexSinkPin() {
        return mImgTexSinkPinAdapter.mSinkPin;
    }

    public SrcPin<AudioBufFrame> getRemoteAudioSrcPin() {
        return mRemoteAudioSrcPin;
    }

    public SrcPin<AudioBufFrame> getLocalAudioSrcPin() {
        return mLocalAudioSrcPin;
    }

    public void joinChannel(String channel, int uid) {
        enableObserver(true);
        mRTCWrapper.joinChannel(channel, uid);
    }

    public void leaveChannel() {
        enableObserver(false);
        mRTCWrapper.leaveChannel();
    }

    private void enableObserver(boolean enable) {
        if (mRTCWrapper != null) {
            mRTCWrapper.enableObserver(enable);
        }
    }

    public void startReceiveRemoteData() {
        if (mRTCWrapper != null) {
            mRTCWrapper.startReceiveRemoteData();
        }

        mRTCStarted = true;
    }

    public void stopReceiveRemoteData() {
        if (mRTCWrapper != null) {
            mRTCWrapper.stopReceiveRemoteData();
        }

        mRTCStarted = false;
    }

    public void registerEventListener(KMCAgoraEventListener listener) {
        if (mRTCWrapper != null) {
            mRTCWrapper.registerEventListener(listener);
        }
    }

    public void setVideoProfile(int profile, boolean swap) {
        if (mRTCWrapper != null) {
            mRTCWrapper.setVideoProfile(profile, swap);
        }
    }

    public KMCAgoraVRTC getRTCWrapper() {
        return mRTCWrapper;
    }

    public List<Integer> getUserList() {
        return mUserList;
    }

    public void setLogPath(String path) {
        if (mRTCWrapper != null) {
            mRTCWrapper.setAgoraLogPath(path);
        }
    }

    public class RTCLocalImgSinkPin extends SinkPin<ImgBufFrame> {

        @Override
        public void onFormatChanged(Object format) {

        }

        @Override
        public void onFrameAvailable(ImgBufFrame frame) {
            //send video frame to peer
            if (mRTCWrapper != null && mRTCStarted
                    && !mMuteLocalVideo) {
                byte[] buf = new byte[frame.buf.remaining()];
                frame.buf.get(buf);
                mRTCWrapper.sendVideoFrame(buf,
                        frame.format.width,
                        frame.format.height,
                        0,
                        frame.pts);
            }
        }

        @Override
        public void onDisconnect(boolean recursive) {

        }
    }

    public class RTCLocalImgTexSinkPin extends SinkPin<ImgTexFrame> {
        @Override
        public void onFormatChanged(Object o) {

        }

        @Override
        public void onFrameAvailable(ImgTexFrame frame) {
            if (mRTCWrapper != null && mRTCStarted
                    && !mMuteLocalVideo) {
                AgoraVideoFrame vf = new AgoraVideoFrame();
                switch (frame.format.colorFormat) {
                    case GLES20.GL_TEXTURE_2D:
                        vf.format = AgoraVideoFrame.FORMAT_TEXTURE_2D;
                        break;
                    case ImgTexFormat.COLOR_EXTERNAL_OES:
                        vf.format = AgoraVideoFrame.FORMAT_TEXTURE_OES;
                        break;
                    default:
                        vf.format = AgoraVideoFrame.FORMAT_TEXTURE_2D;
                }
                vf.stride = frame.format.width;
                vf.height = frame.format.height;
                vf.textureID = frame.textureId;
                vf.eglContext11 = mGLRender.getEGL10Context();
                vf.eglContext14 = mGLRender.getEGLContext();
                vf.transform = frame.texMatrix;
                vf.timeStamp = frame.pts;
                vf.syncMode = true;

                boolean result = mRTCWrapper.sendVideoFrame(vf);
                if (DEBUG) {
                    Log.d(TAG, "send frame : " + result);
                }
            }
        }
    }

    synchronized public void addUser(int userId) {
        if (!mImgTexSrcPins.containsKey(userId)) {
//            int size = mUserList.size();
//            if (size < MAX_REMOTE_USER) {
                mUserList.add(userId);

                ImgTexSrcPin srcPin = new ImgTexSrcPin(mGLRender);
                srcPin.setUseSyncMode(true);
                mImgTexSrcPins.put(userId, srcPin);
//            }
        }
    }

    synchronized public void removeUser(int userId) {
        if (mImgTexSrcPins.containsKey(userId)) {
            mUserList.remove(Integer.valueOf(userId));

            ImgTexSrcPin srcPin = mImgTexSrcPins.get(userId);
            if (srcPin != null &&
                    srcPin.isConnected()) {
                srcPin.disconnect(false);
            }
            mImgTexSrcPins.remove(userId);
        }

        if (mImgTexFormats.containsKey(userId)) {
            mImgTexFormats.remove(userId);
        }
    }

    synchronized public ImgTexSrcPin getImgTexSrcPin(int userId) {
        if (mImgTexSrcPins.containsKey(userId)) {
            return mImgTexSrcPins.get(userId);
        } else {
            return null;
        }
    }

    public ImgTexFormat getImgTexFormat(int index) {
        if (index >=0 && index < mImgTexFormats.size()) {
            return mImgTexFormats.get(index);
        } else {
            return null;
        }
    }

    synchronized private ImgTexFormat getTexFormat(int uid) {
        if (mImgTexFormats.containsKey(uid)) {
            return mImgTexFormats.get(uid);
        } else {
            return null;
        }
    }

    synchronized public boolean hasRemoteConnected() {
        return mImgTexSrcPins.size() > 0;
    }

    @Override
    synchronized public void onReceiveRemoteVideoFrame(int uid, ByteBuffer buffer, VideoFormat format, long pts) {
        ImgTexSrcPin srcPin = getImgTexSrcPin(uid);
        if (mMuteRemoteVideos ||
                srcPin == null ||
                !srcPin.isConnected()) {
            return;
        }

        ImgTexFormat texFormat = getTexFormat(uid);
        if (texFormat == null) {
            texFormat = new ImgTexFormat(ImgTexFormat.COLOR_RGBA, format.width, format.height);
            srcPin.onFormatChanged(texFormat);
        } else if (texFormat.width != format.width ||
                texFormat.height != format.height) {
            texFormat = new ImgTexFormat(ImgTexFormat.COLOR_RGBA, format.width, format.height);
            srcPin.onFormatChanged(texFormat);
        }
        mImgTexFormats.put(uid, texFormat);

        int orientation = 360 - format.orientation;
        srcPin.updateFrame(buffer, format.width * 4, format.width, format
                .height, orientation, pts);
    }

    @Override
    public void onReceiveLocalAudioFrame(ByteBuffer buffer, AudioFormat format, long pts) {
        if (mLocalAudioBufFormat == null) {
            int sampleFormat = AVConst.AV_SAMPLE_FMT_S16;
            if (format.bytesPerSample == 2) {
                sampleFormat = AVConst.AV_SAMPLE_FMT_S16;
            }
            mLocalAudioBufFormat = new AudioBufFormat(sampleFormat, format.sampleRate,
                    format.channels);
            mLocalAudioSrcPin.onFormatChanged(mLocalAudioBufFormat);
        } else if (mLocalAudioBufFormat.sampleRate != format.sampleRate ||
                mLocalAudioBufFormat.channels != format.channels) {
            int sampleFormat = AVConst.AV_SAMPLE_FMT_S16;
            if (format.bytesPerSample == 2) {
                sampleFormat = AVConst.AV_SAMPLE_FMT_S16;
            }
            mLocalAudioBufFormat = new AudioBufFormat(sampleFormat, format.sampleRate,
                    format.channels);
            mLocalAudioSrcPin.onFormatChanged(mLocalAudioBufFormat);
        }

        AudioBufFrame frame = new AudioBufFrame(mLocalAudioBufFormat, buffer, pts);
        if (mLocalAudioSrcPin != null && mLocalAudioSrcPin.isConnected()
                && !mMuteLocalAudio) {
            mLocalAudioSrcPin.onFrameAvailable(frame);
        }
    }

    @Override
    public void onReceiveRemoteAudioFrame(ByteBuffer buffer, AudioFormat format, long pts) {
        if (mRemoteAudioBufFormat == null) {
            int sampleFormat = AVConst.AV_SAMPLE_FMT_S16;
            if (format.bytesPerSample == 2) {
                sampleFormat = AVConst.AV_SAMPLE_FMT_S16;
            }
            mRemoteAudioBufFormat = new AudioBufFormat(sampleFormat, format.sampleRate,
                    format.channels);
            mRemoteAudioSrcPin.onFormatChanged(mRemoteAudioBufFormat);
        } else if (mRemoteAudioBufFormat.sampleRate != format.sampleRate ||
                mRemoteAudioBufFormat.channels != format.channels) {
            int sampleFormat = AVConst.AV_SAMPLE_FMT_S16;
            if (format.bytesPerSample == 2) {
                sampleFormat = AVConst.AV_SAMPLE_FMT_S16;
            }
            mRemoteAudioBufFormat = new AudioBufFormat(sampleFormat, format.sampleRate,
                    format.channels);
            mRemoteAudioSrcPin.onFormatChanged(mRemoteAudioBufFormat);
        }

        AudioBufFrame frame = new AudioBufFrame(mRemoteAudioBufFormat, buffer, pts);
        if (mRemoteAudioSrcPin != null && mRemoteAudioSrcPin.isConnected()
                && !mMuteRemoteAudios) {
            mRemoteAudioSrcPin.onFrameAvailable(frame);
        }
    }


    /**
     * 允许/禁止播放远端用户的音频流，即对所有远端用户进行静音与否
     * 该方法不影响音频数据流的接收，只是不播放音频流
     * @param mute true 静音
     */
    public void muteAllRemoteAudioStreams(boolean mute) {
        if (mRTCWrapper != null) {
            mRTCWrapper.muteAllRemoteAudioStreams(mute);
        }
        mMuteRemoteAudios = mute;
    }

    /**
     * 允许/禁止往网络发送本地音频流
     * 该方法不影响录音状态，并没有禁用麦克风
     * @param mute true 允许, false 禁止
     */
    public void muteLocalAudioStream(boolean mute) {
        if (mRTCWrapper != null) {
            mRTCWrapper.muteLocalAudioStream(mute);
        }
        mMuteLocalAudio = mute;
    }


    /**
     * 允许/禁止往网络发送本地视频流
     *
     * @param mute
     */
    public void muteLocalVideoStream(boolean mute) {
        mMuteLocalVideo = mute;
    }

    /**
     * 允许/禁止播放远端用户视频流
     *
     * @param mute
     */
    public void muteAllRemoteVideoStreams(boolean mute) {
        mMuteRemoteVideos = mute;
    }

}
