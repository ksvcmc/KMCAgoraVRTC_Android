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

import io.agora.rtc.video.AgoraVideoFrame;

import static io.agora.rtc.internal.AudioRoutingController.TAG;

/**
 * Created by sujia on 2017/7/17.
 */

public class KMCAgoraVRTCClient implements KMCAgoraVRTCCallback, KMCAuthResultListener {
    private KMCAgoraVRTC mRTCWrapper;

    private PinAdapter<ImgBufFrame> mImgBufSinkPinAdapter;
    private PinAdapter<ImgTexFrame> mImgTexSinkPinAdapter;

    //send data to rtc
    private SinkPin<ImgBufFrame> mVideoSinkPin;
    private SinkPin<ImgTexFrame> mImgTexSinkPin;

    private ImgTexFormat mImgTexFormat;
    private AudioBufFormat mRemoteAudioBufFormat;
    private AudioBufFormat mLocalAudioBufFormat;

    //remote video data src pin
    private ImgTexSrcPin mImgTexSrcPin;
    private SrcPin<AudioBufFrame> mRemoteAudioSrcPin;
    private SrcPin<AudioBufFrame> mLocalAudioSrcPin;

    private GLRender mGLRender;

    private KMCAuthResultListener mAuthResultListener;
    private ImgTexToBuf mRTCImgTexToBuf;

    private boolean mRTCStarted = false;
    private boolean DEBUG = false;

    public KMCAgoraVRTCClient(GLRender glRender, Context context) {
        this.mRTCWrapper = new KMCAgoraVRTC(context);
        mGLRender = glRender;
        mRTCWrapper.setDataCallback(this);

        mImgBufSinkPinAdapter = new PinAdapter<>();
        mImgTexSinkPinAdapter = new PinAdapter<>();
        mVideoSinkPin = new RTCLocalImgSinkPin();
        mImgTexSinkPin = new RTCLocalImgTexSinkPin();

        mImgTexSrcPin = new ImgTexSrcPin(glRender);
        mRemoteAudioSrcPin = new SrcPin<>();
        mLocalAudioSrcPin = new SrcPin<>();
    }

    public void authorize(String token, KMCAuthResultListener listener) {
        if (mRTCWrapper != null) {
            mAuthResultListener = listener;
            mRTCWrapper.authorize(token, true, this);
        }
    }

    @Override
    public void onSuccess() {
        if (mAuthResultListener != null) {
            mAuthResultListener.onSuccess();
        }

        setUpPinConnection();
    }

    private void setUpPinConnection() {
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

    public void release() {
        if (mRTCWrapper != null) {
            mRTCWrapper.release();
        }
        mRTCWrapper = null;
    }

    public SinkPin<ImgBufFrame> getVideoSinkPin() {
        return mImgBufSinkPinAdapter.mSinkPin;
    }

    public SinkPin<ImgTexFrame> getImgTexSinkPin() {
        return mImgTexSinkPinAdapter.mSinkPin;
    }

    public SrcPin<ImgTexFrame> getImgTexSrcPin() {
        return mImgTexSrcPin;
    }

    public SrcPin<AudioBufFrame> getRemoteAudioSrcPin() {
        return mRemoteAudioSrcPin;
    }

    public SrcPin<AudioBufFrame> getLocalAudioSrcPin() {
        return mLocalAudioSrcPin;
    }

    public ImgTexFormat getImgTexFormat() {
        return mImgTexFormat;
    }

    public void joinChannel(String channel, int uid) {
        enableObserver(true);
        mRTCWrapper.joinChannel(channel, uid);
    }

    /**
     * join channel
     *
     * @param channel channel name
     * @param uid user id
     * @param retryTimes 发生错误重试次数，默认5次
     */
    public void joinChannel(String channel, int uid, int retryTimes) {
        enableObserver(true);
        mRTCWrapper.joinChannel(channel, uid, retryTimes);
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

    public class RTCLocalImgSinkPin extends SinkPin<ImgBufFrame> {

        @Override
        public void onFormatChanged(Object format) {

        }

        @Override
        public void onFrameAvailable(ImgBufFrame frame) {
            //send video frame to peer
            if (mRTCWrapper != null && mRTCStarted) {
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
            if (mRTCWrapper != null && mRTCStarted) {
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

    @Override
    public void onReceiveRemoteVideoFrame(ByteBuffer buffer, VideoFormat format, long pts) {
        if (mImgTexFormat == null) {
            mImgTexFormat = new ImgTexFormat(ImgTexFormat.COLOR_RGBA, format.width, format.height);
            if (mImgTexSrcPin != null &&
                    mImgTexSrcPin.isConnected()) {
                mImgTexSrcPin.onFormatChanged(mImgTexFormat);
            }
        } else if( mImgTexFormat.width != format.width ||
                mImgTexFormat.height != format.height) {
            mImgTexFormat = new ImgTexFormat(ImgTexFormat.COLOR_RGBA, format.width, format.height);
            if (mImgTexSrcPin != null &&
                    mImgTexSrcPin.isConnected()) {
                mImgTexSrcPin.onFormatChanged(mImgTexFormat);
            }
        }

        if (mImgTexSrcPin.isConnected()) {
            int orientation = 360 - format.orientation;
            mImgTexSrcPin.updateFrame(buffer, format.width * 4, format.width, format
                    .height, orientation, pts);
        }
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
        } else if(mLocalAudioBufFormat.sampleRate != format.sampleRate ||
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
        if (mLocalAudioSrcPin != null && mLocalAudioSrcPin.isConnected()) {
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
        } else if(mRemoteAudioBufFormat.sampleRate != format.sampleRate ||
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
        if (mRemoteAudioSrcPin != null && mRemoteAudioSrcPin.isConnected()) {
            mRemoteAudioSrcPin.onFrameAvailable(frame);
        }
    }
}
