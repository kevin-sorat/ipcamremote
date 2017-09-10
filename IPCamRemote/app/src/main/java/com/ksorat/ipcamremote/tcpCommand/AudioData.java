package com.ksorat.ipcamremote.tcpCommand;

public class AudioData {
	private int _timestamp;
	private int _packSeqNum; 		// In ascending order starting from 0
	private int _aquisitionTime;	// To the current time the number of seconds from 1970.1.1 00:00
	private byte _format;			// 0: adpcm
	private int _dataLength;		// 160
	private byte[] dataContent;
	
	public int get_timestamp() {
		return _timestamp;
	}

	public void set_timestamp(int _timestamp) {
		this._timestamp = _timestamp;
	}

	public int get_packSeqNum() {
		return _packSeqNum;
	}

	public void set_packSeqNum(int _packSeqNum) {
		this._packSeqNum = _packSeqNum;
	}

	public int get_aquisitionTime() {
		return _aquisitionTime;
	}

	public void set_aquisitionTime(int _aquisitionTime) {
		this._aquisitionTime = _aquisitionTime;
	}

	public byte get_format() {
		return _format;
	}

	public void set_format(byte _format) {
		this._format = _format;
	}

	public int get_dataLength() {
		return _dataLength;
	}

	public void set_dataLength(int _dataLength) {
		this._dataLength = _dataLength;
	}

	public byte[] getDataContent() {
		return dataContent;
	}

	public void setDataContent(byte[] dataContent) {
		this.dataContent = dataContent;
	}
}
