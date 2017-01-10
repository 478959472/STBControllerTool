package com.huawei.android.stbcontrollertool;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by 47895 on 2017/1/8.
 */

public class MainIntentService extends IntentService {
    private static final String TAG ="MainIntentService";

    public MainIntentService() {
        super(TAG);
    }
    public static Intent newIntent(Context context) {
        return new Intent(context, MainIntentService.class);
    }
    @Override
    protected void onHandleIntent(Intent intent) {
        for(int i=0;i<5;i++){
            synchronized (this) {
                try {
                    wait(1000);
                } catch (Exception e) {
                }
            }
            Log.i(TAG, "服务运行中： " + i);
        }
    }
}
