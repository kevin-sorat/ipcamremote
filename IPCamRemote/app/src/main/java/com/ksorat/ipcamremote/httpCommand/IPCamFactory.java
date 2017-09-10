package com.ksorat.ipcamremote.httpCommand;

public class IPCamFactory {
	public static IPCamCommands createIPCamCommands(IPCamType ipCamType) {
		IPCamCommands commands = null;
		switch (ipCamType) {
		case MJPEG:
			commands = MjpegCommands.getInstance();
			break;
		case H264:
			commands = H264Commands.getInstance();
			break;
		default:
			commands = MjpegCommands.getInstance();
			break;
		}
		return commands;
	}
}
