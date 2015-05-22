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

        AVOSCloud.initialize(this, "3to6v0iwtuvl48ivv1wa315e317c87q2v10rdmsgk2iu1age", "vleycgu6dv8h0n6c42swp4z776xzoxwhmx8evsl19eobg7f1");

    }
}