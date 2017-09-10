package com.ksorat.foscamremote;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Properties;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class MjpegInputStream extends DataInputStream {
	private final byte[] SOI_MARKER = { (byte) 0xFF, (byte) 0xD8 };
	private final byte[] EOF_MARKER = { (byte) 0xFF, (byte) 0xD9 };
	private final String CONTENT_LENGTH = "Content-Length";
	private final static int HEADER_MAX_LENGTH = 100;
	private final static int FRAME_MAX_LENGTH = 40000 + HEADER_MAX_LENGTH;
	private int mContentLength = -1;
    private static final String TAG = "IPCamRemote";
    private static final String MESSAGE = "Received an exception";

	public static MjpegInputStream read(String url) {
		HttpResponse res;
		DefaultHttpClient httpclient;
		MjpegInputStream tempMjpegInputStream = null;
		HttpParams httpParameters = new BasicHttpParams();
		
		try {
			if (!url.contains("http://") && !url.contains("https://")) {
				url = "http://" + url;
			}
			
			// Set the timeout in milliseconds until a connection is established.
			// The default value is zero, that means the timeout is not used.
			//int timeoutConnection = 0;
			//HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
			
			// Set the default socket timeout (SO_TIMEOUT)
			// in milliseconds which is the timeout for waiting for data.
			int timeoutSocket = 10000;	// 10 seconds
			HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
			
			httpclient = new DefaultHttpClient(httpParameters);
			
			res = httpclient.execute(new HttpGet(URI.create(url)));
			InputStream inputStream = res.getEntity().getContent();
			tempMjpegInputStream = new MjpegInputStream(inputStream);
		} catch (ClientProtocolException e) {
			Log.e(TAG, MESSAGE, e);
		} catch (IOException e) {
			Log.e(TAG, MESSAGE, e);
		} catch (Exception e) {
			Log.e(TAG, MESSAGE, e);
		}
		return tempMjpegInputStream;
	}

	public MjpegInputStream(InputStream in) {
		super(new BufferedInputStream(in, FRAME_MAX_LENGTH));
	}

	private int getEndOfSeqeunce(DataInputStream in, byte[] sequence)
			throws IOException {
		try {
			int seqIndex = 0;
			byte c = 0;
			for (int i = 0; i < FRAME_MAX_LENGTH; i++) {
				try {
					c = (byte) in.readUnsignedByte();
				} catch (EOFException e) {
					//Log.e(TAG, MESSAGE, e);
					return -1;
				}
				if (c == sequence[seqIndex]) {
					seqIndex++;
					if (seqIndex == sequence.length)
						return i + 1;
				} else
					seqIndex = 0;
			}
		} catch (Exception e) {
			Log.e(TAG, MESSAGE, e);
			return -1;
		}
		return -1;
	}

	private int getStartOfSequence(DataInputStream in, byte[] sequence)
			throws IOException {
		int end = getEndOfSeqeunce(in, sequence);
		return (end < 0) ? (-1) : (end - sequence.length);
	}

	private int parseContentLength(byte[] headerBytes) throws IOException,
			NumberFormatException {
		ByteArrayInputStream headerIn = new ByteArrayInputStream(headerBytes);
		Properties props = new Properties();
		props.load(headerIn);
		return Integer.parseInt(props.getProperty(CONTENT_LENGTH));
	}

	public Bitmap readMjpegFrame() throws IOException {
		int headerLen = 0;
		byte[] frameData = null;
		try {
			mark(FRAME_MAX_LENGTH);
			headerLen = getStartOfSequence(this, SOI_MARKER);
			if (headerLen < 0) {
				return null;
			}
			reset();
			byte[] header = new byte[headerLen];
			if (available() > 0) {
				readFully(header);
			}
			mContentLength = parseContentLength(header);
			reset();
			frameData = new byte[mContentLength];
			skipBytes(headerLen);
			if (available() > 0) {
				readFully(frameData);
			}
		} catch (NumberFormatException nfe) {
			mContentLength = getEndOfSeqeunce(this, EOF_MARKER);
		} catch (Exception e) {
			Log.e(TAG, MESSAGE, e);
		}
		return BitmapFactory.decodeStream(new ByteArrayInputStream(frameData));
	}
}
