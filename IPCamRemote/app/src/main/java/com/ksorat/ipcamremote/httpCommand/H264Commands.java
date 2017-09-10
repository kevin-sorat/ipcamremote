package com.ksorat.ipcamremote.httpCommand;


public class H264Commands extends IPCamCommands {
	private static IPCamCommands _commands;
	
	private H264Commands() {
		// videoStreamURLs are unused because FSApi is used instead
		/*
		videoStreamURLTemp = "{0}/cgi-bin/CGIStream.cgi?cmd=GetMJStream&usr={1}&pwd={2}";
		videoStreamURLLowResTemp = "{0}/cgi-bin/CGIStream.cgi?cmd=GetMJStream&usr={1}&pwd={2}";
		*/
		
		upURLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=ptzMoveUp&usr={1}&pwd={2}";
		upLeftURLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=ptzMoveTopLeft&usr={1}&pwd={2}";
		leftURLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=ptzMoveLeft&usr={1}&pwd={2}"; 
		downLeftURLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=ptzMoveBottomLeft&usr={1}&pwd={2}";
		downURLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=ptzMoveDown&usr={1}&pwd={2}";
		downRightURLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=ptzMoveBottomRight&usr={1}&pwd={2}";
		rightURLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=ptzMoveRight&usr={1}&pwd={2}";
		upRightURLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=ptzMoveTopRight&usr={1}&pwd={2}";
		stopURLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=ptzStopRun&usr={1}&pwd={2}"; 
		
		emailNotificationURLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=setSMTPConfig&usr={1}&pwd={2}&isEnable={3}";
		alarmEnableURLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=setMotionDetectConfig&usr={1}&pwd={2}&isEnable={3}&linkage={4}&sensitivity={5}&triggerInterval=10&schedule0=281474976710655&schedule1=281474976710655&schedule2=281474976710655&schedule3=281474976710655&schedule4=281474976710655&schedule5=281474976710655&schedule6=281474976710655&area0=1023&area1=1023&area2=1023&area3=1023&area4=1023&area5=1023&area6=1023&area7=1023&area8=1023&area9=1023";
		getParamsURLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=getMotionDetectConfig&usr={1}&pwd={2}";
		getMiscURLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=getSMTPConfig&usr={1}&pwd={2}";
		//getMotionDetectionConfigURLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=getMotionDetectConfig&usr={1}&pwd={2}";

		irAutoURLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=setInfraLedConfig&mode=0&usr={1}&pwd={2}";
		irManualURLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=setInfraLedConfig&mode=1&usr={1}&pwd={2}";
		irOnURLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=openInfraLed&usr={1}&pwd={2}";
		irOffURLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=closeInfraLed&usr={1}&pwd={2}";
		// For Foscam clone
		irOnURL2Temp = "{0}/set_misc.cgi?led_mode=2&user={1}&pwd={2}";
		// For Foscam clone
		irOffURL2Temp = "{0}/set_misc.cgi?led_mode=1&user={1}&pwd={2}";
		
		presetSet1URLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=ptzAddPresetPoint&name=preset1&usr={1}&pwd={2}";
		presetGo1URLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=ptzGotoPresetPoint&name=preset1&usr={1}&pwd={2}";
		presetSet2URLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=ptzAddPresetPoint&name=preset2&usr={1}&pwd={2}";
		presetGo2URLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=ptzGotoPresetPoint&name=preset2&usr={1}&pwd={2}";
		presetSet3URLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=ptzAddPresetPoint&name=preset3&usr={1}&pwd={2}";
		presetGo3URLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=ptzGotoPresetPoint&name=preset3&usr={1}&pwd={2}";
		presetSet4URLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=ptzAddPresetPoint&name=preset4&usr={1}&pwd={2}";
		presetGo4URLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=ptzGotoPresetPoint&name=preset4&usr={1}&pwd={2}";
		presetSet5URLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=ptzAddPresetPoint&name=preset5&usr={1}&pwd={2}";
		presetGo5URLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=ptzGotoPresetPoint&name=preset5&usr={1}&pwd={2}";
		presetSet6URLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=ptzAddPresetPoint&name=preset6&usr={1}&pwd={2}";
		presetGo6URLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=ptzGotoPresetPoint&name=preset6&usr={1}&pwd={2}";
		presetSet7URLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=ptzAddPresetPoint&name=preset7&usr={1}&pwd={2}";
		presetGo7URLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=ptzGotoPresetPoint&name=preset7&usr={1}&pwd={2}";
		presetSet8URLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=ptzAddPresetPoint&name=preset8&usr={1}&pwd={2}";
		presetGo8URLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=ptzGotoPresetPoint&name=preset8&usr={1}&pwd={2}";
		
		presetDelete1URLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=ptzDeletePresetPoint&name=preset1&usr={1}&pwd={2}";
		presetDelete2URLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=ptzDeletePresetPoint&name=preset2&usr={1}&pwd={2}";
		presetDelete3URLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=ptzDeletePresetPoint&name=preset3&usr={1}&pwd={2}";
		presetDelete4URLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=ptzDeletePresetPoint&name=preset4&usr={1}&pwd={2}";
		presetDelete5URLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=ptzDeletePresetPoint&name=preset5&usr={1}&pwd={2}";
		presetDelete6URLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=ptzDeletePresetPoint&name=preset6&usr={1}&pwd={2}";
		presetDelete7URLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=ptzDeletePresetPoint&name=preset7&usr={1}&pwd={2}";
		presetDelete8URLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=ptzDeletePresetPoint&name=preset8&usr={1}&pwd={2}";
		
		hPatrolURLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=ptzStartCruise&mapName=Horizontal&usr={1}&pwd={2}";
		vPatrolURLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=ptzStartCruise&mapName=Vertical&usr={1}&pwd={2}";
		stopPatrolURLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=ptzStopCruise&usr={1}&pwd={2}";
		
		getCameraParamsURLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=getImageSetting&usr={1}&pwd={2}";
		cameraControlURLTemp = "{0}/camera_control.cgi?user={1}&pwd={2}&param={3}&value={4}";
		snapshotURLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=snapPicture2&usr={1}&pwd={2}";
		setSnapConfigURLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=setSnapConfig&snapPicQuality=2&saveLocation=2&usr={1}&pwd={2}";
		
		// For H264 camera only
		setBrightnessURLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=setBrightness&brightness={3}&usr={1}&pwd={2}";
		setContrastURLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=setContrast&constrast={3}&usr={1}&pwd={2}";
		setSubStreamFormatURLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=setSubStreamFormat&format={3}&usr={1}&pwd={2}";
		getMirrorAndFlipSettingURLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=getMirrorAndFlipSetting&usr={1}&pwd={2}";
		mirrorVideoURLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=mirrorVideo&usr={1}&pwd={2}&isMirror={3}";
		flipVideoURLTemp = "{0}/cgi-bin/CGIProxy.fcgi?cmd=flipVideo&usr={1}&pwd={2}&isFlip={3}";
		
	}
	
	public static IPCamCommands getInstance() {
		if (_commands == null) {
			_commands = new H264Commands();
		}
		return _commands; 
	}
}
