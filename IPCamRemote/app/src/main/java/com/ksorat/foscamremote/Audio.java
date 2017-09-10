package com.ksorat.foscamremote;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import com.ipc.sdk.AVStreamData;
import com.ipc.sdk.FSApi;

public class Audio implements Runnable {

	private static final String TAG = "IPCamRemote:Audio";
	private static final String MESSAGE = "Received an exception";
	
	private AVStreamData audioStreamData = new AVStreamData();
	private AudioTrack mAudioTrack;
	private boolean hasPlayStart = false;
	private boolean isThreadRun = true;
	
	private int channelId = 0;

	public void start() {

		int minBufSize = AudioTrack.getMinBufferSize(8000,
				AudioFormat.CHANNEL_OUT_MONO,
				AudioFormat.ENCODING_PCM_16BIT);

		mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 8000,
				AudioFormat.CHANNEL_OUT_MONO,
				AudioFormat.ENCODING_PCM_16BIT, minBufSize * 2,
				AudioTrack.MODE_STREAM);

		mAudioTrack.play();
		hasPlayStart = true;
		new Thread(this).start();
	}

	public void stop() {
		if (hasPlayStart) {
			hasPlayStart = false;
			mAudioTrack.stop();
		}

		isThreadRun = false;
	}

	public void run() {

		while (isThreadRun) {
			// Play
			try {
				FSApi.getAudioStreamData(audioStreamData, channelId);
			} catch (Exception e) {
				continue;
			}
			
			if (audioStreamData.dataLen > 0) {
				try {
					mAudioTrack.write(audioStreamData.data, 0,
							audioStreamData.dataLen);
				} catch (Exception e) {
					Log.e(TAG, MESSAGE, e);
				}
			} else {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					Log.e(TAG, MESSAGE, e);
				}
			}
		}
	}
}
