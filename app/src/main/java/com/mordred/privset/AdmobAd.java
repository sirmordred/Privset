package com.mordred.privset;

import android.content.Context;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;

/**
 * Created by mordred on 27.07.2017.
 */

public class AdmobAd {

    private InterstitialAd mInterstitialAd;
    private boolean isAdShown = false;

    public AdmobAd(Context ctx) {
        if (mInterstitialAd == null) {
            mInterstitialAd = new InterstitialAd(ctx);
            mInterstitialAd.setAdUnitId(ctx.getString(R.string.interstitial_ad_unit_id));
            if (mInterstitialAd != null) {
                mInterstitialAd.setAdListener(new AdListener() {
                    @Override
                    public void onAdLoaded() {
                        if (mInterstitialAd.isLoaded()) {
                            mInterstitialAd.show();
                            isAdShown = true;
                        }
                    }
                    @Override
                    public void onAdClosed() {
                        isAdShown = false;
                    }
                });
            }
        }
    }

    public void showAd() {
        if (!isAdShown) {
            mInterstitialAd.loadAd(new AdRequest.Builder()
                    .build());
        }
    }
}
