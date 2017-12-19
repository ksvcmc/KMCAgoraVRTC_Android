package com.ksyun.mc.AgoraVRTCDemo.kit;

import android.graphics.Bitmap;

import com.ksyun.media.streamer.capture.ImgTexSrcPin;
import com.ksyun.media.streamer.framework.ImgTexFrame;
import com.ksyun.media.streamer.framework.SrcPin;
import com.ksyun.media.streamer.util.gles.GLRender;

/**
 * Capture texture from picture.
 */

public class PictureCapture {
    private final static String TAG = "PictureCapture";
    private final static int MAX_PIC_LEN = 2048;

    private ImgTexSrcPin mImgTexSrcPin;

    public PictureCapture(GLRender glRender) {
        mImgTexSrcPin = new ImgTexSrcPin(glRender);
    }

    public SrcPin<ImgTexFrame> getSrcPin() {
        return mImgTexSrcPin;
    }

    public void start(Bitmap bitmap) {
        mImgTexSrcPin.updateFrame(bitmap, true);
    }

    public void stop() {
        mImgTexSrcPin.updateFrame(null, false);
    }
}
