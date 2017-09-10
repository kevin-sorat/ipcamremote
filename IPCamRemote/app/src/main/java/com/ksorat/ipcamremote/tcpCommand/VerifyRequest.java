package com.ksorat.ipcamremote.tcpCommand;

import java.nio.ByteBuffer;
import android.util.Log;

public class VerifyRequest extends IPCameraTCPCommand {
	private static final String TAG = "IPCamRemote:VerifyRequest";
	private static final String MESSAGE = "Received an exception";
	
	private String _username;
	private String _password;

	public VerifyRequest(String username, String password) {
		_protocolHeader = "MO_O";
		_opcode = 2;
		_bodyLength = 26; // (BINARY_STREAM[13]+BINARY_STREAM[13])
		
		this._username = username;
		this._password = password;
	}

	public byte[] getByteData() {
		ByteBuffer byteBuffer = ByteBuffer.allocate(HEADER_LENGTH + 26);
		
		try {
			byte[] protocolHeader = _protocolHeader.getBytes();
			byteBuffer.put(protocolHeader);
			byteBuffer.put(shortToBytesLittleEndian(_opcode));
			byteBuffer.position(byteBuffer.position()+9); // Skip 9 bytes (INT8 + BINARY_STREAM[8])
			byteBuffer.put(intToByteLittleEndian(_bodyLength));
			byteBuffer.put(intToByteLittleEndian(_bodyLength)); // Yes, do it twice.
			
			byte[] username = _username.getBytes();
			int len = username.length;
			if (len > 13) {
				// Something is wrong
            	Log.d(TAG, MESSAGE);
				return null;
			}
			byteBuffer.put(username);
			byteBuffer.position(byteBuffer.position()+(13-len));
			
			byte[] password = _password.getBytes();
			len = password.length;
			if (len > 13) {
				// Something is wrong
            	Log.d(TAG, MESSAGE);
				return null;
			}
			byteBuffer.put(password);
			
		} catch (Exception e) {
			Log.e(TAG, MESSAGE, e);
		}
		
		byteBuffer.flip();
		return byteBuffer.array();
	}
}