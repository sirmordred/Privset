package com.mordred.privset;

import android.app.Application;
import android.content.Context;

import com.google.android.gms.ads.MobileAds;

/**
 * Created by mordred on 23.10.2017.
 */

public class AppClass extends Application {
    @Override
    public void onCreate() {
        // Initialize the Mobile Ads SDK.
        MobileAds.initialize(getApplicationContext(), getString(R.string.admob_app_id));
        getApplicationContext().getSharedPreferences("otherPreferences", Context.MODE_PRIVATE).edit().putBoolean("APP_START",true).commit();
        super.onCreate();
    }
}
