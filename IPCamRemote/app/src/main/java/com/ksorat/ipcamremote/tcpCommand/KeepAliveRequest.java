package com.ksorat.ipcamremote.tcpCommand;

import java.nio.ByteBuffer;

import android.util.Log;

public class KeepAliveRequest extends IPCameraTCPCommand {
	private static final String TAG = "IPCamRemote:KeepAliveRequest";
	private static final String MESSAGE = "Received an exception";

	public KeepAliveRequest() {
		_protocolHeader = "MO_O";
		_opcode = 255;
		_bodyLength = 0;
	}

	public byte[] getByteData() {
		ByteBuffer byteBuffer = ByteBuffer.allocate(HEADER_LENGTH);
		
		try {
			byte[] protocolHeader = _protocolHeader.getBytes();
			byteBuffer.put(protocolHeader);
			byteBuffer.put(shortToBytesLittleEndian(_opcode));
			byteBuffer.position(byteBuffer.position()+9); // Skip 9 bytes (INT8 + BINARY_STREAM[8])
			byteBuffer.put(intToByteLittleEndian(_bodyLength));
			byteBuffer.put(intToByteLittleEndian(_bodyLength)); // Yes, do it twice.
			//byteBuffer.putInt(0);
			
		} catch (Exception e) {
			Log.e(TAG, MESSAGE, e);
		}
		
		byteBuffer.flip();
		return byteBuffer.array();
	}
}
