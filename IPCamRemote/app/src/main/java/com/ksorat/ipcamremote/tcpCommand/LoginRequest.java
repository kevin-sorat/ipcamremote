package com.ksorat.ipcamremote.tcpCommand;

import java.nio.ByteBuffer;
import android.util.Log;

public class LoginRequest extends IPCameraTCPCommand {
	
	private static final String TAG = "IPCamRemote:LoginRequest";
	private static final String MESSAGE = "Received an exception";

	public LoginRequest() {
		_protocolHeader = "MO_O";
		_opcode = 0;
		_bodyLength = 0;
	}

	public byte[] getByteData() {
		ByteBuffer byteBuffer = ByteBuffer.allocate(HEADER_LENGTH);
		
		try {
			//byteBuffer.put(Base64.decode(getProtocolHeader().getBytes(), Base64.DEFAULT));
			byte[] protocolHeader = _protocolHeader.getBytes();
			byteBuffer.put(protocolHeader);
			
			// The rest are zeros, so we can skip it here
			//byteBuffer.put(shortToBytesLittleEndian(_opcode));
			//byteBuffer.position(byteBuffer.position()+9); // Skip 9 bytes (INT8 + BINARY_STREAM[8])
			//byteBuffer.put(intToByteLittleEndian(_bodyLength));
			//byteBuffer.put(intToByteLittleEndian(_bodyLength)); // Yes, do it twice.
			
		} catch (Exception e) {
			Log.e(TAG, MESSAGE, e);
		}
		
		byteBuffer.flip();
		return byteBuffer.array();
	}
}