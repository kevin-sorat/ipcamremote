package com.ksorat.ipcamremote.httpCommand;

public enum IPCamType {
	// For Foscam FI8910W
	MJPEG, 
	// For Foscam FI9821W
	H264, 
	// For public IP Cameras
	PUBLIC,
	// For something else
	OTHERS;
	
	public static int toInt(IPCamType type) {
		int result = 0;
		switch (type) {
		case MJPEG:
			result = 0;
			break;
		case H264:
			result = 1;
			break;
		case PUBLIC:
			result = 2;
			break;
		default:
			result = 0;
			break;
		}
		return result;
	}
	
	public static IPCamType toEnum(int type) {
		IPCamType result = MJPEG;
		switch (type) {
		case 0:
			result = MJPEG;
			break;
		case 1:
			result = H264;
			break;
		case 2:
			result = PUBLIC;
			break;
		default:
			result = MJPEG;
			break;
		}
		return result;
	}
}