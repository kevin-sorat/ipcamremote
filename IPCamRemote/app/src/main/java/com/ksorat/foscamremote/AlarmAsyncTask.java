package com.ksorat.foscamremote;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

import com.ksorat.ipcamremote.tcpCommand.IPCameraTCPCommand;
import com.ksorat.ipcamremote.tcpCommand.LoginRequest;
import com.ksorat.ipcamremote.tcpCommand.VerifyRequest;

import android.util.Log;

public class AlarmAsyncTask extends AudioAsyncTask {
	//private boolean isReadyToValidate = true;
	
	@Override
	protected String doInBackground(String... params) {
		
		//android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
		
		// Connect to Foscam
		try {
			if (params != null && params.length > 0) {
	    		String serverURL = params[0];
	    		String[] tokens = serverURL.split(":");
	    		
	    		if (tokens != null && tokens.length >= 3) {
		    		socket = new Socket(tokens[1].replace("//", ""), Integer.parseInt(tokens[2]));
		    		socket.setKeepAlive(true);
		            out = socket.getOutputStream();
				    dos = new DataOutputStream(out);
				    in = socket.getInputStream();
				    dis = new DataInputStream(in);
		           
		            // Send Login_Req packet to camera
		            LoginRequest loginRequestCommand = new LoginRequest();
		    		byte[] loginRequestData = loginRequestCommand.getByteData();
		    		sendTCPRequest(loginRequestData, dos);
		    		
		            // Receive Login_Resp packet from camera
		            if (!validateTCPLoginResponse(dis)) {
		            	return null;
		            }
		            
		            // Send Verify_Req packet to camera
			        VerifyRequest verifyRequestCommand = new VerifyRequest(params[1], params[2]);
			        byte[] verifyRequestData = verifyRequestCommand.getByteData(); 
			        sendTCPRequest(verifyRequestData, dos);
			        
			        // Receive Verify_Resp packet from camera
			        if (!validateTCPVerifyResponse(dis)) {
			        	return null;
			        }
			        
			        // Attempt to receive and parse the alarm type data
        			return String.valueOf(validateAlarmResponse(dis));
	    		}
			}
	    } catch (Exception e) {
	    	Log.e(TAG, MESSAGE, e);
	    } finally {
	    	// Close any streams and sockets
			// Disconnect from Foscam
	    	closeSockets();
	    }
	    return null;
	}

	@Override
	protected void onPostExecute(String result) {	
		// If result is not null and is not -1
		if (result != null && !result.equals("-1")) {
			
			// Absorb the notification if the time frame has not expired yet
			// This is to limit the number of notifications to once in x number of minutes
			long currentTime = System.currentTimeMillis();
			IPCamData data = IPCamData.getInstance();
			if (currentTime > data.lastNotififiedTime+data.notificationPeriod) {
				data.lastNotififiedTime = currentTime;
				
				// Display notification
				if (result.equals("0")) {
					// Alarm disabled
					final String str = "Alarm disabled";
					displayNotification(str);
				} else if (result.equals("1")) {
					// Motion detected
					final String str = "Motion detected.";
					displayNotification(str);
				} else if (result.equals("2")) {
					// External alarm
					final String str = "External alarm";
					displayNotification(str);
				} else {
					// Could be sound detected
					final String str = "Sound detected";
					displayNotification(str);
				}
			}
		// Else do executeAlarmAsynTask again
		} else {
			//invokeExecuteAlarmAsynTask();
		}
	}
	
	/*
	private void invokeExecuteAlarmAsynTask() {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			Log.e(TAG, MESSAGE, e);
		}
		final MainActivity mainAct = (MainActivity) context;
		mainAct.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mainAct.executeAlarmAsyncTask();
			}
		});
	}
	*/
	
	@Override
	protected void closeSockets() {
		try {
			super.closeSockets();
		} catch (Exception e) {
	    	Log.e(TAG, MESSAGE, e);
		}
	}
	
	protected int validateAlarmResponse(DataInputStream dis) {
		try {
			byte[] command = new byte[4];
			dis.readFully(command);

			byte[] opcodeBytes = new byte[2];
			dis.readFully(opcodeBytes);
			short opcode = IPCameraTCPCommand
					.bytesToShortLittleEndian(opcodeBytes);

			// If the opcode is not the expected one
			if (opcode != 25) {
				// Skip 9 bytes
				byte[] retain = new byte[9];
				dis.readFully(retain);

				// Get body length
				byte[] bodyLengthBytes = new byte[4];
				dis.readFully(bodyLengthBytes);

				int bodyLength = IPCameraTCPCommand
						.bytesToIntLittleEndian(bodyLengthBytes);
				// Repeat again once more
				dis.readFully(bodyLengthBytes);
				// Skip to next packet and use recursive call
				byte[] bytesToSkip = new byte[bodyLength];
				dis.readFully(bytesToSkip);

				// Recursive call
				return validateAlarmResponse(dis);

			// Else the opcode is the expected one
			} else {
				// Skip to the body
				byte[] skip17Bytes = new byte[17];
				dis.readFully(skip17Bytes);
				// Get the result code (0 = ok)
				byte[] resultBytes = new byte[1];
				dis.readFully(resultBytes);
				
				byte result = resultBytes[0];
				if (result >= 0 && result <= 2) {
					return (int)result;
				} else {
					// Do nothing
					return -1;
				}
			}
		} catch (Exception e) {
			Log.e(TAG, MESSAGE, e);
			return -1;
		}
	}
	
	private void displayNotification(final String message) {
		final IPCamSchedulingService schedulingService = (IPCamSchedulingService) context;
		schedulingService.executeAndroidNotification();
	}
}