package com.ksyun.mc.AgoraVRTCDemo.kit;

import android.content.Context;

import com.ksyun.mc.agoravrtc.AudioFormat;
import com.ksyun.mc.agoravrtc.KMCAgoraEventListener;
import com.ksyun.mc.agoravrtc.KMCAgoraVRTC;
import com.ksyun.mc.agoravrtc.KMCAgoraVRTCCallback;
import com.ksyun.mc.agoravrtc.KMCAuthResultListener;
import com.ksyun.mc.agoravrtc.VideoFormat;
import com.ksyun.media.streamer.capture.ImgTexSrcPin;
import com.ksyun.media.streamer.framework.AVConst;
import com.ksyun.media.streamer.framework.AudioBufFormat;
import com.ksyun.media.streamer.framework.AudioBufFrame;
import com.ksyun.media.streamer.framework.ImgBufFrame;
import com.ksyun.media.streamer.framework.ImgTexFormat;
import com.ksyun.media.streamer.framework.ImgTexFrame;
import com.ksyun.media.streamer.framework.SinkPin;
import com.ksyun.media.streamer.framework.SrcPin;
import com.ksyun.media.streamer.util.gles.GLRender;

import java.nio.ByteBuffer;

/**
 * Created by sujia on 2017/7/17.
 */

public class KMCAgoraVRTCClient implements KMCAgoraVRTCCallback {
    private KMCAgoraVRTC mRTCWrapper;

    //send data to rtc
    private SinkPin<ImgBufFrame> mVideoSinkPin;

    private ImgTexFormat mImgTexFormat;
    private AudioBufFormat mRemoteAudioBufFormat;
    private AudioBufFormat mLocalAudioBufFormat;

    //remote video data src pin
    private ImgTexSrcPin mImgTexSrcPin;
    private SrcPin<AudioBufFrame> mRemoteAudioSrcPin;
    private SrcPin<AudioBufFrame> mLocalAudioSrcPin;

    public KMCAgoraVRTCClient(GLRender glRender, Context context) {
        this.mRTCWrapper = new KMCAgoraVRTC(context);
        mRTCWrapper.setDataCallback(this);

        mVideoSinkPin = new RTCLocalImgSinkPin();
        mImgTexSrcPin = new ImgTexSrcPin(glRender);
        mRemoteAudioSrcPin = new SrcPin<>();
        mLocalAudioSrcPin = new SrcPin<>();
    }

    public void authorize(String token, KMCAuthResultListener listener) {
        if (mRTCWrapper != null) {
            mRTCWrapper.authorize(token,true, listener);
        }
    }

    public void release() {
        if (mRTCWrapper != null) {
            mRTCWrapper.release();
        }
        mRTCWrapper = null;
    }

    public SinkPin<ImgBufFrame> getVideoSinkPin() {
        return mVideoSinkPin;
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
    }

    public void stopReceiveRemoteData() {
        if (mRTCWrapper != null) {
            mRTCWrapper.stopReceiveRemoteData();
        }
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

    public class RTCLocalImgSinkPin extends SinkPin<ImgBufFrame> {

        @Override
        public void onFormatChanged(Object format) {

        }

        @Override
        public void onFrameAvailable(ImgBufFrame frame) {
            //send video frame to peer
            if (mRTCWrapper != null) {
                byte[] buf = new byte[frame.buf.remaining()];
                frame.buf.get(buf);
                mRTCWrapper.sendVideoFrame(buf,
                        frame.format.width,
                        frame.format.height,
                        frame.format.orientation,
                        frame.pts);
            }
        }

        @Override
        public void onDisconnect(boolean recursive) {

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
