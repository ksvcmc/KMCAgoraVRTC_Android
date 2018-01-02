package com.ksyun.mc.AgoraVRTCDemo.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.ksyun.mc.AgoraVRTCDemo.R;

import java.util.Calendar;

/**
 * Created by xiaoqiang on 2017/8/17.
 */

public class LoadingDialog extends Dialog {
    private static LoadingDialog mLoadingDialog;
    private long mLastClickTime;
    private int mBack = 0;
    private int mBackNum;

    private LoadingDialog(Context context, int theme, int backNum) {
        super(context, theme);
        initView();
        this.mBackNum = backNum;
    }
    private void initView(){
        RelativeLayout relativeLayout = new RelativeLayout(getContext());
        relativeLayout.setBackgroundColor(0x00000000);
        ProgressBar bar = new ProgressBar(getContext());
        RelativeLayout.LayoutParams params =
                new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        relativeLayout.addView(bar,params);
        setContentView(relativeLayout);

    }
    public static synchronized void showLoadingDialog(Activity mContext,
                                                      int backPressedNum){
        if(mLoadingDialog == null && !mContext.isFinishing()){
            mLoadingDialog = new LoadingDialog(mContext,
                    R.style.loading_dialog,backPressedNum);
            try {
                mLoadingDialog.show();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public static synchronized void showLoadingDialog(Activity mContext){
        showLoadingDialog(mContext,2);
    }

    @Override
    public void onBackPressed() {
        long currentTime = Calendar.getInstance().getTimeInMillis();
        if (currentTime - mLastClickTime < 2000 && isShowing()) {
            mBack++;
        } else {
            Toast.makeText(getContext(),"在按一次退出",Toast.LENGTH_SHORT).show();
            mLastClickTime = currentTime;
            mBack = 1;
        }
        if(mBack >= mBackNum){
            super.onBackPressed();
            if(getContext() instanceof Activity){
                ((Activity)getContext()).finish();
            }
            mLoadingDialog = null;
        }
    }

    public static synchronized void dismissLoadingDialog(){
        if(mLoadingDialog != null && mLoadingDialog.isShowing()){
            try {
                mLoadingDialog.dismiss();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        mLoadingDialog = null;
    }

}
