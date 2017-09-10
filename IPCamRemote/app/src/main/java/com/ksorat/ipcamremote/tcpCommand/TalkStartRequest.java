package com.ksorat.ipcamremote.tcpCommand;

import java.nio.ByteBuffer;

import android.util.Log;

public class TalkStartRequest extends IPCameraTCPCommand {
	private static final String TAG = "IPCamRemote:AudioStartRequest";
	private static final String MESSAGE = "Received an exception";
	
	public TalkStartRequest() {
		_protocolHeader = "MO_O";
		_opcode = 11;
		_bodyLength = 1; // (INT8)
	}

	public byte[] getByteData() {
		ByteBuffer byteBuffer = ByteBuffer.allocate(HEADER_LENGTH + 1);
		
		try {
			byte[] protocolHeader = _protocolHeader.getBytes();
			byteBuffer.put(protocolHeader);
			byteBuffer.put(shortToBytesLittleEndian(_opcode));
			byteBuffer.position(byteBuffer.position()+9); // Skip 9 bytes (INT8 + BINARY_STREAM[8])
			byteBuffer.put(intToByteLittleEndian(_bodyLength));
			byteBuffer.put(intToByteLittleEndian(_bodyLength)); // Yes, do it twice.
			byteBuffer.put((byte) 1);
			
		} catch (Exception e) {
			Log.e(TAG, MESSAGE, e);
		}
		
		byteBuffer.flip();
		return byteBuffer.array();
	}
}
