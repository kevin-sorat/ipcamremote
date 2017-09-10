package com.ksorat.foscamremote;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

import com.ksorat.ipcamremote.tcpCommand.LoginRequest;
import com.ksorat.ipcamremote.tcpCommand.LoginRequestAV;
import com.ksorat.ipcamremote.tcpCommand.TalkDataRequestAV;
import com.ksorat.ipcamremote.tcpCommand.TalkStartRequest;
import com.ksorat.ipcamremote.tcpCommand.VerifyRequest;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.util.Log;

public class SendAudioAsyncTask extends AudioAsyncTask {
	
	private AudioRecord ar;
	
	@Override
	protected String doInBackground(String... params) {
		
		//android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
		
		// Connect to Foscam
		try {
			if (params != null && params.length > 0) {
	    		String serverURL = params[0];
	    		String[] tokens = serverURL.split(":");
	    		
	    		if (tokens != null && tokens.length >= 3) {
		    		socket = new Socket(tokens[1].replace("//", ""), Integer.parseInt(tokens[2]));
	    			//socket = new Socket("ksorat.no-ip.org", 85);
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
			        
			        // Send Talk_Start_Req packet to camera
			        TalkStartRequest talkStartCommand = new TalkStartRequest();
		            byte[] talkStartData = talkStartCommand.getByteData();
		            sendTCPRequest(talkStartData, dos);
			        
			        // Receive Talk_Start_resp packet from camera
		            int connID = validateTalkStartResponse(dis);
		            if (connID == -1) {
		            	return null;
		            } else if (connID != 0) {
		            	// If connection ID does not exist yet, then save it as a static class variable
		            	connectionID = connID;
		            }
		            //Log.d("=====> connectionID", String.valueOf(connectionID));
		            
		            socket2 = new Socket(tokens[1].replace("//", ""), Integer.parseInt(tokens[2]));
	    			socket2.setKeepAlive(true);
		            out2 = socket2.getOutputStream();
				    dos2 = new DataOutputStream(out2);
				    in2 = socket2.getInputStream();
				    dis2 = new DataInputStream(in2);
		            
		            LoginRequestAV loginRequestAV = new LoginRequestAV(connectionID);
		            byte[] loginRequestAVData = loginRequestAV.getByteData();
		            sendTCPRequest(loginRequestAVData, dos2);
	    		}
			}
	    } catch (Exception e) {
	    	closeSockets();
	    	Log.e(TAG, MESSAGE, e);
	    }
		
		int sampleRate = 8000;
		int bufferSize = AudioRecord.getMinBufferSize(sampleRate, 
				AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
		ar = new AudioRecord(AudioSource.MIC, sampleRate,
				AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize*4);
		try {
			ar.startRecording();
		} catch (Exception e) {
	    	closeSockets();
	    	Log.e(TAG, MESSAGE, e);
	    }
		
		while (!isDone) {
			try {
				// Read the data from hardware device
				short[] buffer = new short[312];
				int numRead = ar.read(buffer, 0, buffer.length);
				
				// Compress from PCM to ADPCM
				byte[] byteBuffer = new byte[numRead / 2 + 4];
				int num = compress(buffer, 0, numRead, byteBuffer, 0);
				
				// Send data to Foscam
				TalkDataRequestAV talkDataRequestAV = new TalkDataRequestAV(byteBuffer, num);
	            byte[] talkDataRequestAVData = talkDataRequestAV.getByteData();
	            sendTCPRequest(talkDataRequestAVData, dos2);
				
			} catch (Exception e) {
		    	Log.e(TAG, MESSAGE, e);
			}
		}
		// Close any streams and sockets
		// Disconnect from Foscam
		closeSockets();
	    return null;
	}

	@Override
	protected void onPostExecute(String result) {
	}
	
	@Override
	protected void closeSockets() {
		try {
			super.closeSockets();
			ar.stop();
		} catch (Exception e) {
	    	Log.e(TAG, MESSAGE, e);
		}
	}
}
