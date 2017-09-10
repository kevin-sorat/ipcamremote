package com.ksorat.foscamremote;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.SystemClock;
import android.support.v4.content.WakefulBroadcastReceiver;

public class IPCamAlarmReceiver extends WakefulBroadcastReceiver {

	// The app's AlarmManager, which provides access to the system alarm services.
    private AlarmManager alarmMgr;
    // The pending intent that is triggered when the alarm fires.
    private PendingIntent alarmIntent;
    
    public IPCamAlarmReceiver() {
    	super();
    }
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Intent service = new Intent(context, IPCamSchedulingService.class);
		startWakefulService(context, service);
	}
	
	public void setAlarm(Context context) {
        alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, IPCamAlarmReceiver.class);
        alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        
        IPCamData data = IPCamData.getInstance();
        // Wake up the device to fire the alarm in 10 seconds, and every 20 seconds after that
        alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 
        		SystemClock.elapsedRealtime() + 10000, // AlarmManager.INTERVAL_FIFTEEN_MINUTES
        		data.notificationPeriod, 
        		alarmIntent);
        
        // Enable {@code SampleBootReceiver} to automatically restart the alarm when the
        // device is rebooted.
        ComponentName receiver = new ComponentName(context, IPCamBootReceiver.class);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);  
	}
	
	public void cancelAlarm(Context context) {
        // If the alarm has been set, cancel it.
		if (alarmMgr == null) {
			alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		}
        alarmMgr.cancel(alarmIntent);
        
        // Disable {@code SampleBootReceiver} so that it doesn't automatically restart the 
        // alarm when the device is rebooted.
        ComponentName receiver = new ComponentName(context, IPCamBootReceiver.class);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }
}
