package com.ksorat.ipcamremote.tcpCommand;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

public abstract class IPCameraTCPCommand {

	public static final Charset UTF_8 = Charset.forName("UTF-8");
	public static final int HEADER_LENGTH = 23; //BINARY_STREAM [4]+INT16+INT8+BINARY_STREAM [8]+INT32+INT32 => 4+2+1+8+4+4
	
	// Based on the specification from:
	// http://translate.google.com/translate?hl=en&prev=/search%3Fq%3DIP%2Bcamera%2B%2522MO_V%2522%2Bcommand%26hl%3Den%26safe%3Doff%26biw%3D1366%26bih%3D599%26prmd%3Dimvns&rurl=translate.google.com&sl=zh-CN&u=http://www.sistemasorp.es/blog/IPCameraProtocol.doc
	protected String _protocolHeader;
	protected short _opcode;
	protected int _bodyLength;
	
	public static byte[] shortToBytesLittleEndian(short value) {
		ByteBuffer tempBuffer = ByteBuffer.allocate(2);
		tempBuffer.order(ByteOrder.LITTLE_ENDIAN);
		tempBuffer.putShort(value);
		return tempBuffer.array();
	}
	
	public static byte[] intToByteLittleEndian(int value) {
		ByteBuffer tempBuffer = ByteBuffer.allocate(4);
		tempBuffer.order(ByteOrder.LITTLE_ENDIAN);
		tempBuffer.putInt(value);
		return tempBuffer.array();
	}
	
	public static short bytesToShortLittleEndian(byte[] value) {
		ByteBuffer tempBuffer = ByteBuffer.allocate(2);
		tempBuffer.order(ByteOrder.LITTLE_ENDIAN);
		tempBuffer.put(value);
		tempBuffer.flip();
		return tempBuffer.getShort();
	}
	
	public static int bytesToIntLittleEndian(byte[] value) {
		ByteBuffer tempBuffer = ByteBuffer.allocate(4);
		tempBuffer.order(ByteOrder.LITTLE_ENDIAN);
		tempBuffer.put(value);
		tempBuffer.flip();
		return tempBuffer.getInt();
	}
	
	public static byte[] bytesToBytesLittleEndian(byte[] value) {
		ByteBuffer tempBuffer = ByteBuffer.allocate(value.length);
		tempBuffer.order(ByteOrder.LITTLE_ENDIAN);
		tempBuffer.put(value);
		tempBuffer.flip();
		return tempBuffer.array();
	}
	
	/*
	public static int bytesToIntBigEndian(byte[] value) {
		ByteBuffer tempBuffer = ByteBuffer.allocate(4);
		tempBuffer.order(ByteOrder.BIG_ENDIAN);
		tempBuffer.put(value);
		tempBuffer.flip();
		return tempBuffer.getInt();
	}
	*/
}