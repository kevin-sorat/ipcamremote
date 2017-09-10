package com.ksorat.ipcamremote;

import android.content.Intent;
import android.content.pm.PackageManager;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by kevin_sorat on 3/17/15.
 */
public class IPCamRemoteWearableListenerService extends WearableListenerService {
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        String path = messageEvent.getPath();
        if (path.equals("notification/open")) {
            PackageManager packageManager = getPackageManager();
            Intent mainIntent = packageManager.getLaunchIntentForPackage(getPackageName());
            startActivity(mainIntent);
        }
    }
}
