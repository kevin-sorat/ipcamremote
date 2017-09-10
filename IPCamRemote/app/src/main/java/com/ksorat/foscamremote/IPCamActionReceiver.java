package com.ksorat.foscamremote;

import java.io.FileOutputStream;
import java.lang.reflect.Method;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

public class IPCamActionReceiver extends BroadcastReceiver {

	public static final String FILENAME_ANDROID_NOTIFICATION = "ip_cam_remote_android_notification";
	private static final String TAG = "IPCamRemote:IPCamActionReceiver";
	private static final String MESSAGE = "Received an exception";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		IPCamData data = IPCamData.getInstance();
	    // Remove notification from drawer
	 	cancelNotification(context, data.notificationID);
	 	
		if (intent.getAction().equals("Launch_App")) {
			launchApplication(context);
		} else if (intent.getAction().equals("Disable_Notification")) {
			disableAndroidNotification(context);
        }
		
		// Collapse status bar
		try {
			Object service = context.getSystemService("statusbar");
			Class<?> statusBarManager = Class.forName("android.app.StatusBarManager");
			
			int currentApiVersion = android.os.Build.VERSION.SDK_INT;
			if (currentApiVersion <= 16) {
				Method collapse = statusBarManager.getMethod("collapse");
				collapse.invoke(service);
			} else {
				Method collapse = statusBarManager.getMethod("collapsePanels");
				collapse.invoke(service);
			}
		} catch (Exception e) {
			Log.e(TAG, MESSAGE, e);
		}
	}
	
	private void launchApplication(Context context) {
		//start activity
	    Intent i = new Intent();
	    i.setClassName("com.ksorat.foscamremote", "com.ksorat.foscamremote.MainActivity");
	    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	    context.startActivity(i);
	}
	
	private void disableAndroidNotification(Context context) {
		IPCamData data = IPCamData.getInstance();
		data.getIPCamAlarmReceiver().cancelAlarm(context);
		data.isAndroidNotification = false;
		
		try {
			// Save state for Android notification to internal storage
			FileOutputStream fos1 = context.openFileOutput(FILENAME_ANDROID_NOTIFICATION,
					Context.MODE_PRIVATE);
			fos1.write(0);
			fos1.close();
		} catch (Exception e) {
			Log.e(TAG, MESSAGE, e);
		}
		
		// TODO: Also turn alarm alert off from IP camera side (do we really need to?)
		
		Toast toast1 = Toast.makeText(context, 
				context.getResources().getString(R.string.android_notification_has_been_disabled),
				Toast.LENGTH_LONG);
		toast1.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
		toast1.show();
		
		Toast toast2 = Toast.makeText(context, 
				context.getResources().getString(R.string.you_can_re_enable_the_notification),
				Toast.LENGTH_LONG);
		toast2.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
		toast2.show();
	}
	
	private void cancelNotification(Context context, int notifyId) {
        String s = Context.NOTIFICATION_SERVICE;
        NotificationManager notMan = (NotificationManager) context.getSystemService(s);
        notMan.cancel(notifyId);
    }
}
