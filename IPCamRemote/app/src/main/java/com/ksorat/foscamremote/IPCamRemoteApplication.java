package com.ksorat.foscamremote;

import android.app.Application;

/**
 * Created by kevin_sorat on 6/22/15.
 */
public class IPCamRemoteApplication extends Application {

    private static boolean _isMainActivityVisible;
    private static boolean _isResumeFromWear;

    public static boolean isMainActivityVisible() {
        return _isMainActivityVisible;
    }

    public static void activityResumed() {
        _isMainActivityVisible = true;
    }

    public static void activityPaused() {
        _isMainActivityVisible = false;
    }

    public static boolean isResumeFromWear() {
        return _isResumeFromWear;
    }

    public static void setIsResumeFromWear(boolean isResumeFromWear) {
        _isResumeFromWear = isResumeFromWear;
    }
}
