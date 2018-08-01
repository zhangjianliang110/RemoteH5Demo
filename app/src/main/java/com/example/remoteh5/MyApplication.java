package com.example.remoteh5;

import com.remote.webmode.H5Bridge;
import com.remote.webmode.util.ThreadPoolFactory;

import android.app.Application;

/**
 * 描述：
 * 作者：mady@akulaku.com
 * 时间： 2018/4/9
 */

public class MyApplication extends Application {

    private static MyApplication mInstance;

    private Application.ActivityLifecycleCallbacks mActivityLifecycleCallbacks;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        initH5();
    }

    /** 注册给web端调用的方法 */
    private void initH5() {
        ThreadPoolFactory.instance().fixExecutor(new Runnable() {
            @Override
            public void run() {
                H5Bridge.getInstance().register(mInstance, JsNativeInterface.class);
            }
        });
    }
}