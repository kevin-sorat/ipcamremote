package com.ksorat.foscamremote;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.ipc.sdk.FSApi;

public class Talk implements Runnable {
	
	private static final String TAG = "IPCamRemote:Talk";
	private static final String MESSAGE = "Received an exception";

	private AudioRecord mAudioRecord;
	private boolean hasRecordStart = false;
	private boolean isThreadRun = true;
	private byte[] buffer = new byte[960];
	private int bytesRead = 0;
	private int sendTalk = 0;
	private int deviceType = 1;
	
	private int channelId = 0;

	public void start() {
		new Thread(this).start();
	}

	public void stop() {
		if (hasRecordStart) {
			hasRecordStart = false;
			mAudioRecord.stop();
			mAudioRecord.release();
			mAudioRecord = null;
		}

		isThreadRun = false;
	}

	public void startTalk(int devType) {
		int minBufSize = AudioRecord.getMinBufferSize(8000,
				AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT);

		mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, 8000,
				AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT,
				minBufSize);

		try {
			mAudioRecord.startRecording();
		} catch (Exception e) {
			Log.e(TAG, MESSAGE, e);
		}

		hasRecordStart = true;
		deviceType = devType;

		FSApi.startTalk(channelId);
		sendTalk = 1;
	}

	public void stopTalk() {
		FSApi.stopTalk(channelId);
		if (hasRecordStart) {
			hasRecordStart = false;
			try {
				mAudioRecord.stop();
				mAudioRecord.release();
				mAudioRecord = null;
			} catch (Exception e) {
				Log.e(TAG, MESSAGE, e);
			}
		}
		sendTalk = 0;
	}

	public void run() {

		int bufLen = 960;

		while (isThreadRun) {
			// Record
			if (sendTalk == 1) {
				// For MJPEG
				if (deviceType == 0) {
					bufLen = 640;
				// For H264
				} else if (deviceType == 1) {
					bufLen = 960;
				} else {
					bufLen = 960;
				}

				try {
					bytesRead = mAudioRecord.read(buffer, 0, bufLen);
					if (bytesRead > 0) {
						FSApi.sendTalkFrame(buffer, bufLen, channelId);
					}

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
