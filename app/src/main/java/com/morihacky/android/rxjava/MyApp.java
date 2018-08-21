package com.morihacky.android.rxjava;

import android.support.multidex.MultiDexApplication;

import com.morihacky.android.rxjava.volley.MyVolley;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;

import timber.log.Timber;


public class MyApp extends MultiDexApplication {

    private static MyApp sApp;
    private RefWatcher mRefWatcher;

    public static MyApp get() {
        return sApp;
    }

    public static RefWatcher getRefWatcher() {
        return MyApp.get().mRefWatcher;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }

        sApp = (MyApp) getApplicationContext();
        mRefWatcher = LeakCanary.install(this);

        // for better RxJava debugging
        //RxJavaHooks.enableAssemblyTracking();

        // Initialize Volley
        MyVolley.init(this);

        Timber.plant(new Timber.DebugTree());
    }
}
