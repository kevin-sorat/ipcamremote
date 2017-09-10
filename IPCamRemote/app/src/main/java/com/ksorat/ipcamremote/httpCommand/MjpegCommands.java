package com.ksorat.ipcamremote.httpCommand;


public class MjpegCommands extends IPCamCommands {
	private static MjpegCommands _commands;
	
	private MjpegCommands() {
		videoStreamURLTemp = "{0}/videostream.cgi?user={1}&pwd={2}&resolution=32";
		videoStreamURLLowResTemp = "{0}/videostream.cgi?user={1}&pwd={2}&resolution=8";
		
		upURLTemp = "{0}/decoder_control.cgi?command=0&user={1}&pwd={2}";
		upLeftURLTemp = "{0}/decoder_control.cgi?command=91&user={1}&pwd={2}";
		leftURLTemp = "{0}/decoder_control.cgi?command=6&user={1}&pwd={2}";
		downLeftURLTemp = "{0}/decoder_control.cgi?command=93&user={1}&pwd={2}";
		downURLTemp = "{0}/decoder_control.cgi?command=2&user={1}&pwd={2}";
		downRightURLTemp = "{0}/decoder_control.cgi?command=92&user={1}&pwd={2}";
		rightURLTemp = "{0}/decoder_control.cgi?command=4&user={1}&pwd={2}";
		upRightURLTemp = "{0}/decoder_control.cgi?command=90&user={1}&pwd={2}";
		stopURLTemp = "{0}/decoder_control.cgi?command=1&user={1}&pwd={2}"; 
		
		emailNotificationURLTemp = "{0}/set_alarm.cgi?user={1}&pwd={2}&motion_armed={3}&motion_sensitivity={4}&input_armed=&sounddetect_armed={5}&sounddetect_sensitivity={6}&iolinkage=&mail={7}&upload_internal=";
		alarmEnableURLTemp = "{0}/set_alarm.cgi?user={1}&pwd={2}&motion_armed={3}&sounddetect_armed={4}";
		getParamsURLTemp = "{0}/get_params.cgi?user={1}&pwd={2}";
		getMiscURLTemp = "{0}/get_misc.cgi?user={1}&pwd={2}";

		irOnURLTemp = "{0}/decoder_control.cgi?command=95&user={1}&pwd={2}";
		irOffURLTemp = "{0}/decoder_control.cgi?command=94&user={1}&pwd={2}";
		// For Foscam clone
		irOnURL2Temp = "{0}/set_misc.cgi?led_mode=2&user={1}&pwd={2}";
		// For Foscam clone
		irOffURL2Temp = "{0}/set_misc.cgi?led_mode=1&user={1}&pwd={2}";
		
		presetSet1URLTemp = "{0}/decoder_control.cgi?command=30&user={1}&pwd={2}";
		presetGo1URLTemp = "{0}/decoder_control.cgi?command=31&user={1}&pwd={2}";
		presetSet2URLTemp = "{0}/decoder_control.cgi?command=32&user={1}&pwd={2}";
		presetGo2URLTemp = "{0}/decoder_control.cgi?command=33&user={1}&pwd={2}";
		presetSet3URLTemp = "{0}/decoder_control.cgi?command=34&user={1}&pwd={2}";
		presetGo3URLTemp = "{0}/decoder_control.cgi?command=35&user={1}&pwd={2}";
		presetSet4URLTemp = "{0}/decoder_control.cgi?command=36&user={1}&pwd={2}";
		presetGo4URLTemp = "{0}/decoder_control.cgi?command=37&user={1}&pwd={2}";
		presetSet5URLTemp = "{0}/decoder_control.cgi?command=38&user={1}&pwd={2}";
		presetGo5URLTemp = "{0}/decoder_control.cgi?command=39&user={1}&pwd={2}";
		presetSet6URLTemp = "{0}/decoder_control.cgi?command=40&user={1}&pwd={2}";
		presetGo6URLTemp = "{0}/decoder_control.cgi?command=41&user={1}&pwd={2}";
		presetSet7URLTemp = "{0}/decoder_control.cgi?command=42&user={1}&pwd={2}";
		presetGo7URLTemp = "{0}/decoder_control.cgi?command=43&user={1}&pwd={2}";
		presetSet8URLTemp = "{0}/decoder_control.cgi?command=44&user={1}&pwd={2}";
		presetGo8URLTemp = "{0}/decoder_control.cgi?command=45&user={1}&pwd={2}";
		
		hPatrolURLTemp = "{0}/set_misc.cgi?user={1}&pwd={2}&ptz_auto_patrol_interval=30&ptz_auto_patrol_type=1";
		vPatrolURLTemp = "{0}/set_misc.cgi?user={1}&pwd={2}&ptz_auto_patrol_interval=30&ptz_auto_patrol_type=2";
		stopPatrolURLTemp = "{0}/set_misc.cgi?user={1}&pwd={2}&ptz_auto_patrol_interval=0&ptz_auto_patrol_type=0";
		/*
		hPatrolURLTemp = "{0}/moveptz.xml?dir=leftright&user={1}&pwd={2}";
		vPatrolURLTemp = "{0}/moveptz.xml?dir=updown&user={1}&pwd={2}";
		stopPatrolURLTemp = "{0}/moveptz.xml?dir=stop&user={1}&pwd={2}";
		*/
		
		getCameraParamsURLTemp = "{0}/get_camera_params.cgi?user={1}&pwd={2}";
		cameraControlURLTemp = "{0}/camera_control.cgi?user={1}&pwd={2}&param={3}&value={4}";
		snapshotURLTemp = "{0}/snapshot.cgi?user={1}&pwd={2}";
	}
	
	public static IPCamCommands getInstance() {
		if (_commands == null) {
			_commands = new MjpegCommands();
		}
		return _commands; 
	}
}
