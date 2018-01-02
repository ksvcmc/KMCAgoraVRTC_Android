package com.ksyun.mc.AgoraVRTCDemo.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.ksyun.mc.AgoraVRTCDemo.R;
import com.ksyun.mc.AgoraVRTCDemo.utils.Utils;

/**
 * Created by sujia on 2017/11/28.
 */

public class MainActivity extends Activity {
    private final static String TAG = MainActivity.class.getSimpleName();
    private final static String CHANNEL_ID = "channel_id";
    private final static int PERMISSION_REQUEST_CAM_AUDIO_REC = 1;

    private EditText mRoomNameEditText;
    private String mChannelName;

    private Button mStartButton;
    private Button mSettingsButton;

    private LinearLayout mShowNote;
    private ImageView mCloseNote;

    private LinearLayout mContentLayout;
    private RelativeLayout mNoteLayout;
    private boolean mPermissionGranted = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
        setContentView(R.layout.activity_main);

        initView();

        startWithPermCheck();
    }

    private void initView() {
        mRoomNameEditText = (EditText) findViewById(R.id.room_name);
        mRoomNameEditText.requestFocus();
        mRoomNameEditText.addTextChangedListener(mWatcher);

        mStartButton = (Button) findViewById(R.id.start);
        mSettingsButton = (Button) findViewById(R.id.settings);
        mShowNote = (LinearLayout) findViewById(R.id.show_demo_note);
        mCloseNote = (ImageView) findViewById(R.id.close_note);

        mContentLayout = (LinearLayout) findViewById(R.id.content);
        mNoteLayout = (RelativeLayout) findViewById(R.id.note);
        mNoteLayout.setVisibility(View.GONE);

        mStartButton.setOnClickListener(new NoDoubleClickListener() {
            @Override
            public void onNoDoubleClick(View v) {
                onStartButtonClicked();
            }
        });
        mSettingsButton.setOnClickListener(new NoDoubleClickListener() {
            @Override
            public void onNoDoubleClick(View v) {
                onSettingsButtonClicked();
            }
        });
        mShowNote.setOnClickListener(new NoDoubleClickListener() {
            @Override
            public void onNoDoubleClick(View v) {
                onShowNoteClicked();
            }
        });
        mCloseNote.setOnClickListener(new NoDoubleClickListener() {
            @Override
            public void onNoDoubleClick(View v) {
                onCloseNoteClicked();
            }
        });
    }

    private void onSettingsButtonClicked() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private void startWithPermCheck() {
        int cameraPerm = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int audioPerm = ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO);
        if (cameraPerm != PackageManager.PERMISSION_GRANTED ||
                audioPerm != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                Toast.makeText(this, "No CAMERA or AudioRecord permission, please check",
                        Toast.LENGTH_LONG).show();
            } else {
                String[] permissions = { Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.READ_EXTERNAL_STORAGE};
                ActivityCompat.requestPermissions(this, permissions,
                        PERMISSION_REQUEST_CAM_AUDIO_REC);
            }
        } else {
            mPermissionGranted = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CAM_AUDIO_REC: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mPermissionGranted = true;
                } else {
                    Toast.makeText(this, "No CAMERA or AudioRecord permission， please check",
                            Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }

    private void onShowNoteClicked() {
        showNoteLayout(true);
    }

    private void onCloseNoteClicked() {
        showNoteLayout(false);
    }

    private void showNoteLayout(boolean show) {
        mNoteLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        mContentLayout.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void onStartButtonClicked() {
        if (!mPermissionGranted) {
            makeToast("没有CAMERA或者AudioRecord权限， 请检查您的设置");
            return;
        }

        if (!Utils.isNetworkConnectionAvailable(getApplicationContext())) {
            makeToast("网络连接失败，请检查您的网络设置");
            return;
        }

        InputMethodManager inputManager =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);
        mChannelName = mRoomNameEditText.getText().toString().trim();
        if (mChannelName != null && mChannelName.length() > 0 && mChannelName.length() < 32) {
            VideoChatActivity.startActivity(MainActivity.this, mChannelName);
        } else {
            showErrorDialog(R.string.channel_id_null);
        }
    }


    private void makeToast(final String str) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast toast = Toast.makeText(MainActivity.this, str, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }
        });
    }

    private void showErrorDialog(final int msgId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(MainActivity.this).setMessage(msgId).
                        setPositiveButton("好的", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create().show();

            }
        });
    }

    private TextWatcher mWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            int index = mRoomNameEditText.getSelectionStart() - 1;
            if (index >= 0 && isEmojiCharacter(editable.charAt(index))) {
                Editable edit = mRoomNameEditText.getText();
                edit.delete(index, index + 1);
            }
        }
    };

    /**
     * 判断是否是表情
     * @param codePoint
     * @return
     */
    private static boolean isEmojiCharacter(char codePoint) {
        boolean isScopeOf = (codePoint == 0x0) || (codePoint == 0x9) || (codePoint == 0xA) || (codePoint == 0xD)
                || ((codePoint >= 0x20) && (codePoint <= 0xD7FF) && (codePoint != 0x263a))
                || ((codePoint >= 0xE000) && (codePoint <= 0xFFFD))
                || ((codePoint >= 0x10000) && (codePoint <= 0x10FFFF));
        return !isScopeOf;

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LoadingDialog.dismissLoadingDialog();
    }
}
