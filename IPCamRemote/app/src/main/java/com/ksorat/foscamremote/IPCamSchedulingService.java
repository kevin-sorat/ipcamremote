package com.ksorat.foscamremote;

import java.io.File;
import java.io.FileInputStream;
import java.util.StringTokenizer;
import com.ksorat.ipcamremote.httpCommand.IPCamType;
import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class IPCamSchedulingService extends IntentService {
	private String serverURL = ""; // "http://ksorat.no-ip.org:85";
	private String admin = "";
	private String password = "";
	
	public static final String FILENAME = "foscam_remote_";
	public static final String FILENAME_CURRENT_CAM = "foscam_remote_current_cam";
	public static final String FILENAME_IP_CAM_TYPE = "ip_cam_remote_ip_cam_type";
	public static final String FILENAME_ANDROID_NOTIFICATION = "ip_cam_remote_android_notification";
	private static final String TAG = "IPCamRemote:SchedSvc";
	private static final String MESSAGE = "Received an exception";
	
	public IPCamSchedulingService() {
		super("IPCamSchedulingService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		IPCamData data = IPCamData.getInstance();
		if (data.getServerURL() == null || data.getServerURL().equals("")) {
			loadDataFromInternalStorage();
			int currentIndex = loadCurrentCameraIndex();
			data.setCurrentIPCamIndex(currentIndex);
		}
		
		if (data.isAndroidNotification) {
			if (serverURL.equals("")) {
				generateURLStrings();
			}
			//Log.d("executeAlarmAsyncTask", "=========================> serverURL: " + 
				//	serverURL + ", admin: " + admin + ", password: " + password);
			executeAlarmAsyncTask();
		}
		
		// Release the wake lock provided by the BroadcastReceiver.
        IPCamAlarmReceiver.completeWakefulIntent(intent);
	}
	
	@SuppressLint("NewApi")
	protected void executeAlarmAsyncTask() {
		IPCamData data = IPCamData.getInstance();
		// Only allow one alarmAsyncTask to be executed at a time
		if (data.alarmAsyncTask == null || data.alarmAsyncTask.getStatus().equals(AsyncTask.Status.FINISHED)) {
			data.alarmAsyncTask = new AlarmAsyncTask();
			data.alarmAsyncTask.context = this;

			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
				data.alarmAsyncTask.execute(serverURL, admin, password);
			} else {
				data.alarmAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverURL, admin, password);
			}
		}
	}
	
	private int loadCurrentCameraIndex() {
		String tempStr = "";
		try {
			File file = getApplicationContext().getFileStreamPath(
					FILENAME_CURRENT_CAM);
			if (file.exists()) {
				FileInputStream fis = openFileInput(FILENAME_CURRENT_CAM);
				byte[] byteArray = new byte[256];
				fis.read(byteArray);
				fis.close();
				tempStr = new String(byteArray);
			}
		} catch (Exception e) {
			Log.e(TAG, MESSAGE, e);
		}

		if (tempStr != null && !tempStr.equals("")) {
			try {
				tempStr = tempStr.trim(); // need to do this or the string
											// cannot be parsed to int
				int tempInt = Integer.parseInt(tempStr);
				return tempInt;
			} catch (Exception ex) {
				return 0;
			}
		}
		return 0;
	}
	
	private void loadDataFromInternalStorage() {
		IPCamData data = IPCamData.getInstance();
		for (int i = 0; i < IPCamData.NUM_IPCAMS; i++) {
			String persistentDataStr = "";
			try {
				String filename = FILENAME + (i + 1);
				File file = getApplicationContext().getFileStreamPath(filename);
				if (file.exists()) {
					FileInputStream fis = openFileInput(filename);
					byte[] byteArray = new byte[256];
					fis.read(byteArray);
					fis.close();
					persistentDataStr = new String(byteArray);
				}
			} catch (Exception e) {
				Log.e(TAG, MESSAGE, e);
			}

			try {
				StringTokenizer tokenizer = new StringTokenizer(persistentDataStr, "|");
				int numTokens = tokenizer.countTokens();
	
				// Set values and states using the persistent data from a file
				if (numTokens > 0) {
					data.setCurrentIPCamIndex(i);
					if (numTokens >= 1) {
						data.setCameraName(tokenizer.nextToken());
					}
					if (numTokens >= 2) {
						data.setServerURL(tokenizer.nextToken());
					}
					if (numTokens >= 3) {
						data.setPortNumberOrPublic(tokenizer.nextToken());
					}
					// For public cameras
					if (numTokens == 4) {
						data.setUserName(tokenizer.nextToken().trim());
					}
					// For user's cameras
					if (numTokens >= 5) {
						data.setUserName(tokenizer.nextToken());
						data.setPassword(tokenizer.nextToken().trim());
					}
				}
			} catch (Exception e) {
				Log.e(TAG, MESSAGE, e);
			}
			
			try {
				// Load int for ipCamType enum
				String filename = FILENAME_IP_CAM_TYPE + (i + 1);
				File file = getApplicationContext().getFileStreamPath(filename);
				if (file.exists()) {
					FileInputStream fis = openFileInput(filename);
					int tempInt = fis.read();
					fis.close();
					IPCamera[] camArray = data.getIPCamArray();
					camArray[i].ipCamType = IPCamType.toEnum(tempInt);
				}
			} catch (Exception e) {
				Log.e(TAG, MESSAGE, e);
			}
		}
		
		try {
			// Load state for Android notification boolean
			String filename = FILENAME_ANDROID_NOTIFICATION;
			File file = getApplicationContext().getFileStreamPath(filename);
			if (file.exists()) {
				FileInputStream fis = openFileInput(filename);
				int tempInt = fis.read();
				fis.close();
				if (tempInt == 1) {
					data.isAndroidNotification = true;
				} else {
					data.isAndroidNotification = false;
				}
			}
		} catch (Exception e) {
			Log.e(TAG, MESSAGE, e);
		}
	}
	
	private void generateURLStrings() {
		IPCamData data = IPCamData.getInstance();
		// If public camera is selected
		if (data.getPortNumberOrPublic().equals(IPCamera.PUBLIC)) {
			// Do nothing
		// Else use foscam URLs
		} else {
			serverURL = data.getServerURL() + ":" + data.getPortNumberOrPublic();
			admin = data.getUserName();
			password = data.getPassword();
		}
	}
	
	protected void executeAndroidNotification() {
		// Define sound URI, the sound to be played when there's a notification
		Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		
		// Open intent
		//Intent openIntent = new Intent(this, MainActivity.class);
		//PendingIntent openPendingIntent = PendingIntent.getActivity(this, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		Intent launchIntent = new Intent(this, IPCamActionReceiver.class);
		launchIntent.setAction("Launch_App");
		PendingIntent launchPendingIntent = PendingIntent.getBroadcast(this.getApplicationContext(), 13572468, launchIntent, 0);
		
		// Disable intent
		Intent disableReceive = new Intent(this, IPCamActionReceiver.class);
		disableReceive.setAction("Disable_Notification");
		PendingIntent disablePendingIntent = PendingIntent.getBroadcast(this.getApplicationContext(), 13572468, disableReceive, 0);
		
	    IPCamData data = IPCamData.getInstance();
		NotificationCompat.Builder mBuilder =
			    new NotificationCompat.Builder(this)
			    .setSmallIcon(R.drawable.ic_launcher)
			    .setContentTitle(getResources().getString(R.string.app_name))
			    .setContentText(data.getCameraName() + ": " + getResources().getString(R.string.motion_detected))
			    .setContentIntent(launchPendingIntent)
			    .setSound(soundUri)
			    .setAutoCancel(true)
				.addAction(0, getResources().getString(R.string.launch), launchPendingIntent)
				.addAction(0, getResources().getString(R.string.disable), disablePendingIntent);
		
		// Sets an ID for the notification
		int mNotificationId = data.notificationID;
		// Gets an instance of the NotificationManager service
		NotificationManager mNotifyMgr = 
		        (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		// Builds the notification and issues it.
		mNotifyMgr.notify(mNotificationId, mBuilder.build());
	}
}
