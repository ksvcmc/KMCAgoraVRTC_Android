package com.ksyun.mc.AgoraVRTCDemo.ui;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.ksyun.mc.AgoraVRTCDemo.R;
import com.ksyun.mc.AgoraVRTCDemo.utils.StreamConfig;
import com.sevenheaven.segmentcontrol.SegmentControl;
import com.xw.repo.BubbleSeekBar;

/**
 * Created by sujia on 2017/11/28.
 */

public class SettingsActivity extends Activity {
    private final static String TAG = SettingsActivity.class.getSimpleName();

    private StreamConfig mConfig;

    private ScrollView mScrollView;
    private SegmentControl mPreviewResIdx;
    private SegmentControl mTargetResIdx;
    private SegmentControl mEncodeMethodIdx;
    private SegmentControl mVcodecIdIdx;
    private SegmentControl mAcodecProfileIdx;

    private BubbleSeekBar mFps;
    private BubbleSeekBar mVideoKBitrate;
    private BubbleSeekBar mAudioKBitrate;

    private TextView mSaveButton;
    private ImageView mBackoff;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        initView();
    }

    private void initView() {
        mConfig = new StreamConfig(this);
        mConfig.loadData();

        mScrollView = findViewById(R.id.scroll_view);
        mPreviewResIdx = findViewById(R.id.preview_resolution);
        mTargetResIdx = findViewById(R.id.target_resolution);
        mEncodeMethodIdx = findViewById(R.id.encode_mehtod);
        mVcodecIdIdx = findViewById(R.id.vcodec_id);
        mAcodecProfileIdx = findViewById(R.id.acodec_profile);

        mFps = findViewById(R.id.fps);
        mVideoKBitrate = findViewById(R.id.video_kbitrate);
        mAudioKBitrate = findViewById(R.id.audio_kbitrate);

        mBackoff = findViewById(R.id.back_off);
        mSaveButton = findViewById(R.id.save_config);

        initController();
    }

    private void initController() {
        mPreviewResIdx.setSelectedIndex(mConfig.preview_res_idx);
        mTargetResIdx.setSelectedIndex(mConfig.target_res_idx);
        mEncodeMethodIdx.setSelectedIndex(mConfig.encode_method_idx);
        mVcodecIdIdx.setSelectedIndex(mConfig.vcodec_id_idx);
        mAcodecProfileIdx.setSelectedIndex(mConfig.acodec_profile_idx);

        mFps.setProgress(mConfig.fps);
        mVideoKBitrate.setProgress(mConfig.video_kbitrate);
        mAudioKBitrate.setProgress(mConfig.audio_kbitrate);

        mScrollView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                mFps.correctOffsetWhenContainerOnScrolling();
                mVideoKBitrate.correctOffsetWhenContainerOnScrolling();
                mAudioKBitrate.correctOffsetWhenContainerOnScrolling();
            }
        });
        mPreviewResIdx.setOnSegmentControlClickListener(new SegmentControl.OnSegmentControlClickListener() {
            @Override
            public void onSegmentControlClick(int index) {
                mConfig.preview_res_idx = index;
            }
        });
        mTargetResIdx.setOnSegmentControlClickListener(new SegmentControl.OnSegmentControlClickListener() {
            @Override
            public void onSegmentControlClick(int index) {
                mConfig.target_res_idx = index;
            }
        });
        mEncodeMethodIdx.setOnSegmentControlClickListener(new SegmentControl.OnSegmentControlClickListener() {
            @Override
            public void onSegmentControlClick(int index) {
                mConfig.encode_method_idx = index;
            }
        });
        mVcodecIdIdx.setOnSegmentControlClickListener(new SegmentControl.OnSegmentControlClickListener() {
            @Override
            public void onSegmentControlClick(int index) {
                mConfig.vcodec_id_idx = index;
            }
        });
        mAcodecProfileIdx.setOnSegmentControlClickListener(new SegmentControl.OnSegmentControlClickListener() {
            @Override
            public void onSegmentControlClick(int index) {
                mConfig.acodec_profile_idx = index;
            }
        });

        mFps.setOnProgressChangedListener(new BubbleSeekBar.OnProgressChangedListener() {
            @Override
            public void onProgressChanged(int progress, float progressFloat) {
                mConfig.fps = progress;
            }

            @Override
            public void getProgressOnActionUp(int progress, float progressFloat) {

            }

            @Override
            public void getProgressOnFinally(int progress, float progressFloat) {

            }
        });
        mVideoKBitrate.setOnProgressChangedListener(new BubbleSeekBar.OnProgressChangedListener() {
            @Override
            public void onProgressChanged(int progress, float progressFloat) {
                mConfig.video_kbitrate = progress;
            }

            @Override
            public void getProgressOnActionUp(int progress, float progressFloat) {

            }

            @Override
            public void getProgressOnFinally(int progress, float progressFloat) {

            }
        });
        mAudioKBitrate.setOnProgressChangedListener(new BubbleSeekBar.OnProgressChangedListener() {
            @Override
            public void onProgressChanged(int progress, float progressFloat) {
                mConfig.audio_kbitrate = progress;
            }

            @Override
            public void getProgressOnActionUp(int progress, float progressFloat) {

            }

            @Override
            public void getProgressOnFinally(int progress, float progressFloat) {

            }
        });

        mBackoff.setOnClickListener(new NoDoubleClickListener() {
            @Override
            public void onNoDoubleClick(View v) {
                onBackOffClicked();
            }
        });

        mSaveButton.setOnClickListener(new NoDoubleClickListener() {
            @Override
            public void onNoDoubleClick(View v) {
                onSaveClicked();
            }
        });
    }

    private void onBackOffClicked() {
        finish();
    }

    private void onSaveClicked() {
        mConfig.saveData();
        finish();
    }

}
