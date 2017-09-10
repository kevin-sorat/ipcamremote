package com.ksorat.foscamremote;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import com.ksorat.ipcamremote.tcpCommand.AudioData;
import com.ksorat.ipcamremote.tcpCommand.AudioEndRequest;
import com.ksorat.ipcamremote.tcpCommand.AudioStartRequest;
import com.ksorat.ipcamremote.tcpCommand.KeepAliveRequest;
import com.ksorat.ipcamremote.tcpCommand.LoginRequest;
import com.ksorat.ipcamremote.tcpCommand.LoginRequestAV;
import com.ksorat.ipcamremote.tcpCommand.VerifyRequest;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

public class ReceiveAudioAsyncTask extends AudioAsyncTask {
	
	protected AudioTrack myAudio;
	
	@Override
	protected String doInBackground(String... params) {
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
			        
			        //AudioEndRequest audioEndRequest = new AudioEndRequest();
			        //byte[] audioEndData = audioEndRequest.getByteData();
			        //sendTCPRequest(audioEndData, dos);
			        
			        //GetConfiguredCameraRequest getConfiguredCameraRequest = new GetConfiguredCameraRequest();
			        //byte[] getConfiguredCameraData = getConfiguredCameraRequest.getByteData();
			        //sendTCPRequest(getConfiguredCameraData, dos);
			        
			        //validateTCPGetConfiguredCameraResponse(dis);
		            
			        /*
		            // Send Video_Start_Req packet to camera
		            VideoStartRequest videoStartCommand = new VideoStartRequest();
		            byte[] videoStartData = videoStartCommand.getByteData();
		            sendTCPRequest(videoStartData, dos);
		            
		            // Receive Video_Start_resp packet from camera
		            connectionID = validateVideoStartResponse(dis);
		            if (connectionID == -1) {
		            	return null;
		            }
		            */
		            
		            // Send Audio_Start_Req packet to camera
		            AudioStartRequest audioStartCommand = new AudioStartRequest();
		            byte[] audioStartData = audioStartCommand.getByteData();
		            sendTCPRequest(audioStartData, dos);
		            
		            // Receive Video_Start_resp packet from camera
		            int connID = validateAudioStartResponse(dis);
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
		            
		            //DecoderControlRequest decoderControlRequest = new DecoderControlRequest();
		            //byte[] decoderControlData = decoderControlRequest.getByteData();
		            //sendTCPRequest(decoderControlData, dos); 
		            
		            //validateVideoDataResponse(dis);
		            
		            int sampleRate = 8000;
		            int minBufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
		            
		            myAudio = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_MONO,
		            		AudioFormat.ENCODING_PCM_16BIT, minBufferSize*4, AudioTrack.MODE_STREAM);
		            
		            //myAudio.setStereoVolume(AudioTrack.getMaxVolume(), AudioTrack.getMaxVolume());
		            /*
		            PresetReverb pr = new PresetReverb(0, myAudio.getAudioSessionId());
		            pr.setPreset(PresetReverb.PRESET_SMALLROOM);
		            pr.setEnabled(true);
		            myAudio.attachAuxEffect(pr.getId());
		            myAudio.setAuxEffectSendLevel((float)0.7);
		            */
		            myAudio.play();
		            
		            long startTime = System.currentTimeMillis();
		            while (!isDone) {
		            	long currentTime = System.currentTimeMillis();
		            	long timeChanged = currentTime - startTime;
		            	// If one minute is up
		            	if (timeChanged >= 60000) {
		            		// Send Keep_Alive packet to the Foscam
		            		KeepAliveRequest keepAliveRequest = new KeepAliveRequest();
					        byte[] keepAliveData = keepAliveRequest.getByteData();
					        sendTCPRequest(keepAliveData, dos);
		            		// Reset the timer
		            		startTime = System.currentTimeMillis();
		            	// One minute is not up yet
		            	} else {
		            		// Do nothing
		            	}
		            	// Read audio stream
		            	AudioData audioData = validateAudioDataResponse(dis2);
		            	// Play the audio
		            	playWav(audioData.getDataContent());
		            }
	    		}
	    	}
	    } catch (UnknownHostException e) {
	    	Log.e(TAG, MESSAGE, e);
	    } catch (IOException e) {
	    	Log.e(TAG, MESSAGE, e);
	    } catch (Exception e) {
	    	Log.e(TAG, MESSAGE, e);
	    } finally {
	    	closeSockets();
	    }
	    return null;
	}
	
	@Override
    protected void onPostExecute(String result) {
	}
	
	private void playWav(byte[] mp3SoundByteArray) {	
		byte[] temp = decompress(mp3SoundByteArray);
		myAudio.write(temp, 0, temp.length);
		myAudio.flush();
	}
	
	@Override
	protected void closeSockets() {
		try {
			super.closeSockets();
			
			// End the audio
    		AudioEndRequest audioEndRequest = new AudioEndRequest();
	        byte[] audioEndData = audioEndRequest.getByteData();
	        sendTCPRequest(audioEndData, dos);
			
	        if (myAudio != null) {
	        	myAudio.stop();
		        myAudio.release();
	        }
		} catch (Exception e) {
	    	//Log.e(TAG, MESSAGE, e);
		}
	}
}