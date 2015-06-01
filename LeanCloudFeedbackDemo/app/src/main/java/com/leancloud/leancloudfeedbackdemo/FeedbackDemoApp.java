package com.leancloud.leancloudfeedbackdemo;

import android.app.Application;

import com.avos.avoscloud.AVOSCloud;

/**
 * Created by fengjunwen on 5/22/15.
 */
public class FeedbackDemoApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        AVOSCloud.initialize(this, "sl9sxfb9d9x0sc5g8c5wsv00f4nvztrgo5qcx4i4sjk1myn3", "ysz2l1zttl802m3qq8lvvqnmw8tnp4wiiuwe5xtydcjxrwtg");

    }
}