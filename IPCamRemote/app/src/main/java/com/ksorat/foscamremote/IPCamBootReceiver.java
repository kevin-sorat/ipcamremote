package com.ksorat.foscamremote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class IPCamBootReceiver extends BroadcastReceiver {
	
	public static final String FILENAME = "foscam_remote_";
	public static final String FILENAME_CURRENT_CAM = "foscam_remote_current_cam";
	public static final String FILENAME_IP_CAM_TYPE = "ip_cam_remote_ip_cam_type";
	public static final String FILENAME_ANDROID_NOTIFICATION = "ip_cam_remote_android_notification";
	//private static final String TAG = "IPCamRemote:IPCamBootReceiver";
	//private static final String MESSAGE = "Received an exception";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
			IPCamData data = IPCamData.getInstance();
			data.getIPCamAlarmReceiver().setAlarm(context);
			
			/*
			if (data.getServerURL() == null || data.getServerURL().equals("")) {
				// Read data from internal storage
				loadDataFromInternalStorage(context);
				int currentIndex = loadCurrentCameraIndex(context);
				data.setCurrentIPCamIndex(currentIndex);
			}
			if (data.isAndroidNotification) {
				data.getIPCamAlarmReceiver().setAlarm(context);
			}
			*/
        }
	}
	
	/*
	private void loadDataFromInternalStorage(Context context) {
		IPCamData data = IPCamData.getInstance();
		for (int i = 0; i < IPCamData.NUM_IPCAMS; i++) {
			String persistentDataStr = "";
			try {
				String filename = FILENAME + (i + 1);
				File file = context.getFileStreamPath(filename);
				if (file.exists()) {
					FileInputStream fis = context.openFileInput(filename);
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
				File file = context.getFileStreamPath(filename);
				if (file.exists()) {
					FileInputStream fis = context.openFileInput(filename);
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
			File file = context.getFileStreamPath(filename);
			if (file.exists()) {
				FileInputStream fis = context.openFileInput(filename);
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
	
	private int loadCurrentCameraIndex(Context context) {
		String tempStr = "";
		try {
			File file = context.getFileStreamPath(
					FILENAME_CURRENT_CAM);
			if (file.exists()) {
				FileInputStream fis = context.openFileInput(FILENAME_CURRENT_CAM);
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
				tempStr = tempStr.trim(); 	// need to do this or the string
											// cannot be parsed to int
				int tempInt = Integer.parseInt(tempStr);
				return tempInt;
			} catch (Exception ex) {
				return 0;
			}
		}
		return 0;
	}
	*/
}
