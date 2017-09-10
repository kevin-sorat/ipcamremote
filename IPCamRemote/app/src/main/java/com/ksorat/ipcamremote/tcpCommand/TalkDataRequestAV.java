package com.ksorat.ipcamremote.tcpCommand;

import java.nio.ByteBuffer;

import android.util.Log;

public class TalkDataRequestAV extends IPCameraTCPCommand {
	private static final String TAG = "IPCamRemote:TalkDataRequestAV";
	private static final String MESSAGE = "Received an exception";

	private static int packSeqNum = 0;
	private byte[] _dataContent;
	private int _length = 0;
	
	public TalkDataRequestAV(byte[] dataContent, int length) {
		_protocolHeader = "MO_V";
		_opcode = 3;
		_bodyLength = 17 + length;
		_dataContent = dataContent;
		_length = length;
	}

	public byte[] getByteData() {
		ByteBuffer byteBuffer = ByteBuffer.allocate(HEADER_LENGTH + _bodyLength);
		
		try {
			byte[] protocolHeader = _protocolHeader.getBytes();
			byteBuffer.put(protocolHeader);
			byteBuffer.put(shortToBytesLittleEndian(_opcode));
			byteBuffer.position(byteBuffer.position()+9); // Skip 9 bytes (INT8 + BINARY_STREAM[8])
			byteBuffer.put(intToByteLittleEndian(_bodyLength));
			byteBuffer.put(intToByteLittleEndian(_bodyLength)); // Yes, do it twice.
			
			int timeStamp = (int) System.currentTimeMillis();
			byteBuffer.put(intToByteLittleEndian(timeStamp));
			
			byteBuffer.put(intToByteLittleEndian(packSeqNum++));
			
			int aquisitionTime = timeStamp/1000;
			byteBuffer.put(intToByteLittleEndian(aquisitionTime));
			
			byte format = 0;
			byteBuffer.put(format);
			
			byteBuffer.put(intToByteLittleEndian(_length));
			
			byteBuffer.put(_dataContent);
			
		} catch (Exception e) {
			Log.e(TAG, MESSAGE, e);
		}
		
		byteBuffer.flip();
		return byteBuffer.array();
	}
}
