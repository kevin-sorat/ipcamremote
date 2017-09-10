package com.ksorat.ipcamremote.tcpCommand;

import java.nio.ByteBuffer;
import android.util.Log;

public class LoginRequestAV extends IPCameraTCPCommand {
	
	private static final String TAG = "IPCamRemote:LoginRequestAV";
	private static final String MESSAGE = "Received an exception";

	private int _connectionID;
	
	public LoginRequestAV(int connectionID) {
		_protocolHeader = "MO_V";
		_opcode = 0;
		_bodyLength = 4;
		_connectionID = connectionID;
	}

	public byte[] getByteData() {
		ByteBuffer byteBuffer = ByteBuffer.allocate(HEADER_LENGTH + 4);
		
		try {
			byte[] protocolHeader = _protocolHeader.getBytes();
			byteBuffer.put(protocolHeader);
			byteBuffer.put(shortToBytesLittleEndian(_opcode));
			byteBuffer.position(byteBuffer.position()+9); // Skip 9 bytes (INT8 + BINARY_STREAM[8])
			byteBuffer.put(intToByteLittleEndian(_bodyLength));
			byteBuffer.put(intToByteLittleEndian(_bodyLength)); // Yes, do it twice.
			byteBuffer.put(intToByteLittleEndian(_connectionID));
			
		} catch (Exception e) {
			Log.e(TAG, MESSAGE, e);
		}
		
		byteBuffer.flip();
		return byteBuffer.array();
	}
}
