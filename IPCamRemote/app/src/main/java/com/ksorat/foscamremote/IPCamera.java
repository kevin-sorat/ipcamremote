package com.ksorat.foscamremote;

import com.ksorat.ipcamremote.httpCommand.IPCamType;

public class IPCamera {
	public static final String PUBLIC = "public";
			
	public String cameraName = ""; // Must be unique
	public String serverURL = "";
	public String portNumberOrPublic = "";
	public String userName = "";
	public String password = "";
	public int flipControl = 0; // 0 = initial ; 1 = vertical rotate ; 2 = horizontal rotate ; 3 = vertical+horizontal rotate
	public IPCamType ipCamType = IPCamType.MJPEG;
	public int screenMode = 0;	// 0 = 4:3 ; 1 = 16:9
}
