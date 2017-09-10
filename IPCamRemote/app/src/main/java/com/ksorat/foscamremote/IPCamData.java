package com.ksorat.foscamremote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.ksorat.ipcamremote.httpCommand.IPCamCommands;
import com.ksorat.ipcamremote.httpCommand.IPCamFactory;
import com.ksorat.ipcamremote.httpCommand.IPCamType;

import android.os.AsyncTask;
import android.util.Log;

public class IPCamData {
	public static final int NUM_IPCAMS = 18; 
	public static final String FILENAME = "foscam_remote_";
	
	public static final int MOVEMENT_HIGH_VAL = 7;
	public static final int MOVEMENT_LOW_VAL = 3;
	
	private static final String TAG = "IPCamRemote:IPCamData";
	private static final String MESSAGE = "Received an exception";
	private static final String MORE_THAN_ONE_CGI_RESULT = "There are more than one CGI_Result!";
	
	private static IPCamData _data;
	private IPCamera[] _ipCamArray = new IPCamera[NUM_IPCAMS];
	private int _currentIPCamIndex = 0;
	public boolean ignoreSetSelection = false;
	public boolean keepScreenOn = false;
	public boolean keepSpeakerOn = false;
	public boolean isAndroidNotification = false;
	public boolean isLowRes = false;
	public boolean fromMultiView = false;
	public boolean isInAppPurchased = false;
	public boolean displayAd = false;
	public IPCamera[] availablePublicCameras;
	
	// Only 1 instance of focamParams and foscamMisc are created and stored to save memory footprint
	private HashMap<String, String> ipCamParams; 	// For setting patrol type, motion/sound detection and sensitivity, etc.
	private HashMap<String, String> ipCamMisc; 		// For getting auto patrol type
	private HashMap<String, String> ipCamCameraParams; 	// For getting/setting brightness, contrast, and flip type, 
														// e.g. {contrast=3, brightness=184, resolution=32, flip=0, fps=0, mode=1}
	
	public MainActivity mainActivity;
	
	protected AlarmAsyncTask alarmAsyncTask;
	// This will absorb the notification when the app is just created
	protected long lastNotififiedTime = System.currentTimeMillis();
	protected long notificationPeriod = 30000;	// 30 seconds
	protected int notificationID = 001;
	private IPCamAlarmReceiver _alarmReceiver;
	private boolean isMirrorH264 = false;
	private boolean isFlipH264 = false;
	
	private IPCamData() {
		for (int i=0 ; i<NUM_IPCAMS ; i++) {
			_ipCamArray[i] = new IPCamera();
		}
	}
	
	public static IPCamData getInstance() {
		if (_data == null) {
			_data = new IPCamData();
		}
		return _data;
	}
	
	public IPCamAlarmReceiver getIPCamAlarmReceiver() {
		if (_alarmReceiver == null) {
			_alarmReceiver = new IPCamAlarmReceiver();
		}
		return _alarmReceiver;
	}
	
	public IPCamera[] getIPCamArray() {
		return _ipCamArray;
	}
	
	public int getCurrentIPCamIndex() {
		return _currentIPCamIndex;
	}
	
	public void setCurrentIPCamIndex(int index) {
		if (index >= 0 && index <= NUM_IPCAMS)
			_currentIPCamIndex = index;
		else
			_currentIPCamIndex = 0;
	}
	
	public void parseIPCamParams(HttpResponse response) {
		// Cannot access the network call in the main thread
		// Must use an async call instead
		new AsyncParseIPCamParams().execute(response);
	}
	
	public void parseIPCamParamsAfterItemSelection(HttpResponse response) {
		new AsyncParseIPCamParamsAfterItemSelection().execute(response);
	}
	
	public void parseIPCamMisc(HttpResponse response) {
		new AsyncParseIPCamMisc().execute(response);
	}
	
	public void parseIPCamCameraParams(HttpResponse response) {
		new AsyncParseIPCamCameraParams().execute(response);
	}
	
	public void parseMirrorAndFlipParams(HttpResponse response) {
		new AsyncParseMirrorAndFlipParams().execute(response);
	}
	
	public int getFlip() {
		IPCamType camType = getIPCamType();
		if (camType == IPCamType.MJPEG) {
			if (ipCamCameraParams != null) {
				String strValue = ipCamCameraParams.get("flip");
				try {
					int value = Integer.parseInt(strValue);
					return value;
				} 
				catch (Exception ex) {
					return 0;
				} 
			} else {
				return 0;
			}
		} else if (camType == IPCamType.H264) {	
			if (isFlipH264 && !isMirrorH264) {
				return 1;
			} else if (!isFlipH264 && isMirrorH264) {
				return 2;
			} else if (isFlipH264 && isMirrorH264) {
				return 3;
			}
			return 0;
		}
		return 0;
	}
	
	public void setFlip(int val) {
		// 0 = initial ; 1 = vertical rotate ; 2 = horizontal rotate ; 3 = vertical+horizontal rotate
		if (val < 0 || val > 3)
			val = 0;
		
		IPCamType camType = getIPCamType();
		if (camType == IPCamType.MJPEG) {
			if (ipCamCameraParams != null) {
				String str = String.valueOf(val);
				ipCamCameraParams.put("flip", str);
			}
		} else if (camType == IPCamType.H264) {	
			if (val == 0) {
				isFlipH264 = false;
				isMirrorH264 = false;
			} else if (val == 1) {
				isFlipH264 = true;
				isMirrorH264 = false;
			} else if (val == 2) {
				isFlipH264 = false;
				isMirrorH264 = true;
			} else if (val == 3) {
				isFlipH264 = true;
				isMirrorH264 = true;
			}
		}
	}
	
	public int getFlipControl() {
		return _ipCamArray[_currentIPCamIndex].flipControl;
	}

	public void setFlipControl(int val) {
		// 0 = initial ; 1 = vertical rotate ; 2 = horizontal rotate ; 3 = vertical+horizontal rotate
		if (val < 0 || val > 3)
			val = 0;
		
		_ipCamArray[_currentIPCamIndex].flipControl = val;
	}
	
	public int getBrightness() {
		if (ipCamCameraParams != null) {
			String strValue = ipCamCameraParams.get("brightness");
			try {
				int value = Integer.parseInt(strValue);
				return value;
			} 
			catch (Exception ex) {
				return 0;
			} 
		} else {
			return 0;
		}
	}
	
	public void setBrightness(int val) {
		/*
		// This is not suitable for H264 camera
		if (val < 0 || val > 255)
			val = 0;
		*/
		if (ipCamCameraParams != null) {
			String str = String.valueOf(val);
			ipCamCameraParams.put("brightness", str);
		}
	}
	
	public int getContrast() {
		if (ipCamCameraParams != null) {
			String strValue = ipCamCameraParams.get("contrast");
			try {
				int value = Integer.parseInt(strValue);
				return value;
			} 
			catch (Exception ex) {
				return 0;
			} 
		} else {
			return 0;
		}
	}
	
	public void setContrast(int val) {
		/*
		// This is not suitable for H264 camera
		if (val < 0 || val > 6)
			val = 0;
		*/
		if (ipCamCameraParams != null) {
			String str = String.valueOf(val);
			ipCamCameraParams.put("contrast", str);
		}
	}
	
	public boolean isEmailNotification() {
		if (ipCamParams != null) {
			IPCamType camType = getIPCamType();
			String strValue = null;
			
			if (camType == IPCamType.MJPEG) {
				strValue = ipCamParams.get("alarm_mail");
				try {
					if (strValue.equals("1")) {
						return true;
					} else {
						return false;
					}
				} 
				catch (Exception ex) {
					return false;
				} 
			} else if (camType == IPCamType.H264) {
				strValue = ipCamParams.get("linkage");
				try {
					int intVal = Integer.parseInt(strValue);
					// Convert to binary
					String binaryStr = Integer.toBinaryString(intVal);
					if (isEMailEnabledBinary(binaryStr)) {
						return true;
					} else {
						return false;
					}
				} catch (Exception ex) {
					return false;
				} 
			}
			return false;
		} else {
			return false;
		}
	}
	
	public String getIPCamParamsLinkage() {
		if (ipCamParams != null) {
			IPCamType camType = getIPCamType();
			
			if (camType == IPCamType.H264) {
				return ipCamParams.get("linkage");
			}
		}
		return null;
	}
	
	private boolean isEMailEnabledBinary(String binaryStr) {
		if (binaryStr != null && binaryStr.length() >= 2) {
			// The send email bit is bit 1 which is second to last in binary string
			if (binaryStr.charAt(binaryStr.length()-2) == '1') {
				return true;
			}
		}
		return false;
	}
	
	private void setEMailEnabledBinary(String binaryStr, boolean value) {
		if (binaryStr != null && binaryStr.length() > 0) {
			if (binaryStr.equals("0") && value) {
				int intVal = Integer.parseInt("10", 2);
				ipCamParams.put("linkage", String.valueOf(intVal));
			} else if (binaryStr.equals("1") && value) {
				int intVal = Integer.parseInt("11", 2);
				ipCamParams.put("linkage", String.valueOf(intVal));
			} else if (binaryStr.length() >= 2) {
				StringBuilder binaryStrBuilder = new StringBuilder(binaryStr);
				if (value) {
					// The send email bit is bit 1 which is second to last in binary string
					binaryStrBuilder.setCharAt(binaryStr.length()-2, '1');
					// Convert back to integer
					int intVal = Integer.parseInt(binaryStrBuilder.toString(), 2);
					ipCamParams.put("linkage", String.valueOf(intVal));
				} else {
					binaryStrBuilder.setCharAt(binaryStr.length()-2, '0');
					int intVal = Integer.parseInt(binaryStrBuilder.toString(), 2);
					ipCamParams.put("linkage", String.valueOf(intVal));
				}
			}
		}
	}
	
	public void setEmailNotification(boolean value) {
		if (ipCamParams != null) {
			IPCamType camType = getIPCamType();
			
			if (camType == IPCamType.MJPEG) {
				if (value) {
					ipCamParams.put("alarm_mail", "1");
				} else {
					ipCamParams.put("alarm_mail", "0");
				}
			} else if (camType == IPCamType.H264) {
				try {
					String strValue = ipCamParams.get("linkage");
					if (strValue != null && strValue.length() > 0) {
						int intVal = Integer.parseInt(strValue);
						String binaryStr = Integer.toBinaryString(intVal);
						setEMailEnabledBinary(binaryStr, value);
					}
				} catch (Exception e) {
					Log.e(TAG, MESSAGE, e);
				} 
			}
		}
	}
	
	public void setMotionDetection(boolean value) {
		if (ipCamParams != null) {
			IPCamType camType = getIPCamType();
			if (camType == IPCamType.MJPEG) {
				if (value) {
					ipCamParams.put("alarm_motion_armed", "1");
				} else {
					ipCamParams.put("alarm_motion_armed", "0");
				}
			} else if (camType == IPCamType.H264) {
				if (value) {
					ipCamParams.put("isEnable", "1");
				} else {
					ipCamParams.put("isEnable", "0");
				}
			}
		}
	}
	
	public boolean isMotionDetection() {
		if (ipCamParams != null) {
			IPCamType camType = getIPCamType();
			String strValue = null;
			
			if (camType == IPCamType.MJPEG) {
				strValue = ipCamParams.get("alarm_motion_armed");
			} else if (camType == IPCamType.H264) {
				strValue = ipCamParams.get("isEnable");
			}
			
			try {
				if (strValue != null && strValue.equals("1")) {
					return true;
				} else {
					return false;
				}
			} 
			catch (Exception e) {
				Log.e(TAG, MESSAGE, e);
				return false;
			}
			
		} else {
			return false;
		}
	}
	
	public void setSoundDetection(boolean value) {
		if (ipCamParams != null) {
			if (value) {
				ipCamParams.put("alarm_sounddetect_armed", "1");
			} else {
				ipCamParams.put("alarm_sounddetect_armed", "0");
			}
		}
	}
	
	public boolean isSoundDetection() {
		if (ipCamParams != null) {
			String strValue = ipCamParams.get("alarm_sounddetect_armed");
			try {
				if (strValue.equals("1")) {
					return true;
				} else {
					return false;
				}
			} 
			catch (Exception e) {
				//Log.e(TAG, MESSAGE, e);
				return false;
			} 
		} else {
			return false;
		}
	}
	
	public int getMotionSensitivity() {
		if (ipCamParams == null)
			return 0;
		try
		{
			IPCamType camType = getIPCamType();
			if (camType == IPCamType.MJPEG) {
				int tempInt = Integer.parseInt(ipCamParams.get("alarm_motion_sensitivity"));
				if (tempInt >= 0 && tempInt <= 9) {
					return tempInt;
				}
			} else if (camType == IPCamType.H264) {
				int tempInt = Integer.parseInt(ipCamParams.get("sensitivity"));
				if (tempInt >= 0 && tempInt <= 4) {
					return tempInt;
				}
			}
			return 0;
		} catch (Exception e) {
			Log.e(TAG, MESSAGE, e);
			return 0;
		}
	}
	
	public void setMotionSensitivity(int sensitivity) {
		if (ipCamParams != null) {
			IPCamType camType = getIPCamType();
			if (camType == IPCamType.MJPEG) {
				if (sensitivity >= 0 && sensitivity <= 9) {
					ipCamParams.put("alarm_motion_sensitivity", Integer.toString(sensitivity));
				} else {
					ipCamParams.put("alarm_motion_sensitivity", "0");
				}
			} else if (camType == IPCamType.H264) {
				if (sensitivity >= 0 && sensitivity <= 4) {
					if (sensitivity == 0) {
						ipCamParams.put("sensitivity", "4");
					} else if (sensitivity == 1) {
						ipCamParams.put("sensitivity", "3");
					} else if (sensitivity == 2) {
						ipCamParams.put("sensitivity", "0");
					} else if (sensitivity == 3) {
						ipCamParams.put("sensitivity", "1");
					} else if (sensitivity == 4) {
						ipCamParams.put("sensitivity", "2");
					}
				}
			}
		}
	}
	
	public int getSoundSensitivity() {
		if (ipCamParams == null)
			return 0;
		try
		{
			int tempInt = Integer.parseInt(ipCamParams.get("alarm_sounddetect_sensitivity"));
			if (tempInt >= 0 && tempInt <= 9) {
				return tempInt;
			}
			return 0;
		} catch (Exception e) {
			//Log.e(TAG, MESSAGE, e);
			return 0;
		}
	}
	
	public void setSoundSensitivity(int sensitivity) {
		if (ipCamParams != null) {
			if (sensitivity >= 0 && sensitivity <= 9)
				ipCamParams.put("alarm_sounddetect_sensitivity", Integer.toString(sensitivity));
			else
				ipCamParams.put("alarm_sounddetect_sensitivity", "0");
		}
	}
	
	public int getPatrolType() {
		if (ipCamMisc == null)
			return 0;
		
		IPCamType camType = getIPCamType();
		if (camType == IPCamType.MJPEG) {
			try {
				int tempInt = Integer.parseInt(ipCamMisc.get("ptz_auto_patrol_type"));
				if (tempInt >= 0 && tempInt <= 3)
					return tempInt;
				else
					return 0;
			} catch (NumberFormatException e) {
				Log.e(TAG, MESSAGE, e);
				return 0;
			} catch (Exception e) {
				Log.e(TAG, MESSAGE, e);
				return 0;
			}
		} else if (camType == IPCamType.H264) {
			// TODO: Return correct patrol type for H264
		}
		return 0;
	}
	
	public void setPatrolType(int patrolType) {
		IPCamType camType = getIPCamType();
		if (camType == IPCamType.MJPEG) {
			if (patrolType >= 0 && patrolType <= 3) {
				ipCamParams.put("ptz_auto_patrol_type", Integer.toString(patrolType));
			} else {
				ipCamParams.put("ptz_auto_patrol_type", "0");
			}
		} else if (camType == IPCamType.H264) {
			
		}
	}

	public String getCameraName() {
		return _ipCamArray[_currentIPCamIndex].cameraName;
	}

	public void setCameraName(String _cameraName) {
		_ipCamArray[_currentIPCamIndex].cameraName = (_cameraName != null) ? _cameraName : "";
	}

	public String getServerURL() {
		return _ipCamArray[_currentIPCamIndex].serverURL;
	}
	
	public String getServerURLNoHttp() {
		String serverURL = _ipCamArray[_currentIPCamIndex].serverURL;
		if (serverURL.contains("http://")) {
			serverURL = serverURL.replace("http://", "");
		} else if (serverURL.contains("https://")) {
			serverURL = serverURL.replace("https://", "");
		}
		return serverURL;
	}

	public void setServerURL(String _serverURL) {
		_ipCamArray[_currentIPCamIndex].serverURL = (_serverURL != null) ? _serverURL : "";
	}

	public String getPortNumberOrPublic() {
		return _ipCamArray[_currentIPCamIndex].portNumberOrPublic;
	}

	public void setPortNumberOrPublic(String _portNumber) {
		_ipCamArray[_currentIPCamIndex].portNumberOrPublic = (_portNumber != null) ? _portNumber : "";
	}

	public String getUserName() {
		return _ipCamArray[_currentIPCamIndex].userName;
	}

	public void setUserName(String _userName) {
		_ipCamArray[_currentIPCamIndex].userName = (_userName != null) ? _userName : "";
	}

	public String getPassword() {
		return _ipCamArray[_currentIPCamIndex].password;
	}

	public void setPassword(String _password) {
		_ipCamArray[_currentIPCamIndex].password = (_password != null) ? _password : "";
	}
	
	public IPCamCommands getIPCamCommands() {
		return IPCamFactory.createIPCamCommands(_ipCamArray[_currentIPCamIndex].ipCamType);
	}
	
	public IPCamType getIPCamType() {
		return _ipCamArray[_currentIPCamIndex].ipCamType;
	}
	
	public void setIPCamType(IPCamType type) {
		_ipCamArray[_currentIPCamIndex].ipCamType = type;
	}
	
	public int getScreenMode() {
		return _ipCamArray[_currentIPCamIndex].screenMode;
	}

	public void setScreenMode(int _screenMode) {
		_ipCamArray[_currentIPCamIndex].screenMode = _screenMode;
	}

	
	private class AsyncParseIPCamParams extends AsyncTask<HttpResponse, Void, Void> {
		@Override
        protected Void doInBackground(HttpResponse... params) {
			if (params != null && params.length > 0) {
				ipCamParams = null;
				HttpResponse response = params[0];
				if (response != null) {
					try {
						HttpEntity r_entity = response.getEntity();
						String entityString = new String(EntityUtils.toString(r_entity));
						
						// If xmlString starts with "<", then we have XML structure which contains param values for H264
						if (entityString.startsWith("<")) {
							parseParamsH264(entityString);
						// Else we have regular text which contains param values for MJPEG
						} else {
							//BufferedReader rd = new BufferedReader(new InputStreamReader(r_entity.getContent()));
				    		parseParamsMJPEG(entityString);
						}
						
				    /*
				     {ddns_user=ksorat, user8_name=, user2_pri=2, alarm_ioout_level=1, alarm_sounddetect_sensitivity=5, alarm_schedule_wed_2=0, 
				     alarm_schedule_wed_1=0, alarm_schedule_wed_0=0, dev8_user=, dev4_user=, wifi_keyformat=0, alarm_schedule_sun_2=0, 
				     alarm_schedule_sun_1=0, alarm_schedule_sun_0=0, user5_name=, wifi_mode=0, dev9_user=, dev7_host=, dev3_port=0, 
				     ftp_numberoffiles=0, mail_sender=kevin.sorat@gmail.com, alarm_schedule_enable=0, alarm_upload_interval=0, ftp_schedule_tue_2=0, 
				     dhcp_vendor=, dev2_port=0, ftp_schedule_tue_1=0, ftp_schedule_tue_0=0, dns=0.0.0.0, user4_pri=0, tz=14400, dev8_port=0, 
				     user1_name=admin, ftp_mode=0, user7_name=, gateway=0.0.0.0, mail_pwd=michaelJORDAN23, dev4_host=, user4_pwd=, user7_pwd=, 
				     user1_pwd=, dev3_pwd=, alarm_input_armed=0, dev9_port=0, user3_pwd=qqkaykay11, ftp_port=0, dev9_host=, ddns_pwd=michaelJORDAN23, 
				     ntp_svr=time.nist.gov, dev3_host=, sys_ver=11.37.2.46, ddns_proxy_port=0, dev6_port=0, wifi_key4_bits=0, dev5_alias=, 
				     alarm_motion_compensation=1, dev5_pwd=, ftp_upload_interval=0, dev6_host=, dev8_host=, wifi_enable=1, msn_friend10=, user5_pwd=, 
				     ftp_svr=, alarm_schedule_fri_2=0, alarm_schedule_fri_1=0, alarm_schedule_fri_0=0, dev7_pwd=, user2_pwd=kaewgull, ftp_schedule_sat_0=0, 
				     ftp_schedule_sat_1=0, ftp_schedule_sat_2=0, alarm_preset=0, dev5_host=, user8_pri=0, alarm_motion_sensitivity=0, mail_port=465, 
				     wifi_key3_bits=0, ftp_dir=, dev5_user=, alarm_http_url=, user5_pri=0, user3_name=qqdoris, alarm_schedule_tue_2=0, user6_name=, 
				     alarm_schedule_tue_1=0, alarm_schedule_tue_0=0, msn_user=, alarm_mail=1, ntp_enable=1, user3_pri=2, dev2_pwd=, user6_pwd=, dev6_user=, 
				     wifi_authtype=0, dev7_port=0, wifi_key1_bits=0, ddns_proxy_svr=, ftp_retain=0, dev2_user=, dev9_pwd=, alarm_schedule_thu_2=0, 
				     now=1353384238, alarm_schedule_thu_1=0, alarm_schedule_thu_0=0, alarm_msn=0, pppoe_pwd=, port=85, dev8_alias=, ftp_schedule_sun_2=0, 
				     user7_pri=0, alarm_sounddetect_armed=0, ftp_schedule_sun_1=0, ftp_schedule_sun_0=0, ftp_schedule_mon_0=0, ftp_schedule_thu_0=0, 
				     ftp_schedule_mon_1=0, ftp_schedule_mon_2=0, wifi_key2_bits=0, dev4_port=0, upnp_enable=1, ftp_schedule_thu_1=0, ftp_schedule_thu_2=0, 
				     dev6_alias=, daylight_saving_time=0, ftp_pwd=, wifi_country=1, alarm_http=0, alarm_motion_armed=0, user4_name=, mail_svr=smtp.gmail.com, 
				     wifi_key1=B9FABD51B1, wifi_key2=, wifi_key3=, wifi_key4=, user1_pri=2, alarm_iolinkage=0, user2_name=ksorat, alarm_schedule_mon_0=0, 
				     mask=0.0.0.0, msn_friend9=, mail_inet_ip=1, msn_friend7=, msn_friend8=, msn_friend5=, alarm_schedule_mon_1=0, msn_friend6=, 
				     alarm_schedule_mon_2=0, dev7_alias=, decoder_baud=12, dev4_alias=, ftp_user=, wifi_defkey=0, dev7_user=, dev6_pwd=, 
				     ftp_schedule_enable=0, msn_friend2=, ftp_schedule_wed_2=0, mail_receiver4=, dev5_port=0, msn_friend1=, mail_receiver3=, msn_friend4=, 
				     ftp_schedule_wed_0=0, msn_friend3=, ftp_schedule_wed_1=0, mail_receiver2=qqdoris@hotmail.com, mail_receiver1=kevin.sorat@gmail.com, 
				     ftp_schedule_fri_0=0, alarm_schedule_sat_1=0, dev3_alias=, alarm_schedule_sat_2=0, ftp_schedule_fri_1=0, app_ver=2.4.10.2, msn_pwd=, 
				     alarm_schedule_sat_0=0, ftp_schedule_fri_2=0, pppoe_user=, dev9_alias=, mail_user=kevin.sorat@gmail.com, alarm_ioin_level=1, 
				     mail_tls=1, wifi_wpa_psk=, wifi_encrypt=1, id=000DC5DB4C1A, ddns_host=ksorat.no-ip.org, ddns_service=17, alias=Black Foscam, dev2_alias=, 
				     dev4_pwd=, user6_pri=0, pppoe_enable=0, ip=0.0.0.0, wifi_ssid=6G8T4, dev3_user=, user8_pwd=, dev8_pwd=, ftp_filename=, dev2_host=}
				     */  
					} catch (Exception e) {
						Log.e(TAG, MESSAGE, e);
						return null;
					}
				}
			}
			return null;
		}
	}
	
	private void parseParamsH264(String xmlString) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        InputSource is;
        NodeList list;
        
		builder = factory.newDocumentBuilder();
		is = new InputSource(new StringReader(xmlString));
		Document doc = builder.parse(is);
		list = doc.getElementsByTagName("CGI_Result");
		
		if (list != null && list.getLength() > 0) {
			if (list.getLength() != 1) {
				throw new Exception(MORE_THAN_ONE_CGI_RESULT);
			} else {
				Node firstNode = list.item(0);
				if (firstNode.getNodeType() == Node.ELEMENT_NODE) {
					ipCamParams = new HashMap<String, String>();
                    Element firstElement = (Element) firstNode;

                    NodeList isEnableList = firstElement.getElementsByTagName("isEnable");
                    Element isEnableElement = (Element)isEnableList.item(0);
                    NodeList textIsEnableList = isEnableElement.getChildNodes();
                    String isEnableVal = ((Node)textIsEnableList.item(0)).getNodeValue().trim();
                    ipCamParams.put("isEnable", isEnableVal);
                    
                    NodeList linkageList = firstElement.getElementsByTagName("linkage");
                    Element linkageElement = (Element)linkageList.item(0);
                    NodeList textLinkageList = linkageElement.getChildNodes();
                    String linkageVal = ((Node)textLinkageList.item(0)).getNodeValue().trim();
                    ipCamParams.put("linkage", linkageVal);

                    NodeList sensitivityList = firstElement.getElementsByTagName("sensitivity");
                    Element sensitivityElement = (Element)sensitivityList.item(0);
                    NodeList textSensitivityList = sensitivityElement.getChildNodes();
                    String sensitivityVal = ((Node)textSensitivityList.item(0)).getNodeValue().trim();
                    ipCamParams.put("sensitivity", sensitivityVal);
				}
			}
		}
	}
	
	private void parseParamsMJPEG(String textString) throws IOException {
		ipCamParams = new HashMap<String, String>();
		String[] lines = textString.split(System.getProperty("line.separator"));
		for (String line : lines) {
	    	line = line.replace("var ", "");
	    	line = line.replace(";", "");
	    	line = line.replaceAll("\'", "");
	    	StringTokenizer tokenizer = new StringTokenizer(line, "=");
	    	if (tokenizer.countTokens() == 2) {
	    		ipCamParams.put(tokenizer.nextToken(), tokenizer.nextToken());
	    	} else if (tokenizer.countTokens() == 1) {
	    		ipCamParams.put(tokenizer.nextToken(), "");
	    	}
	    }
	}
	
	private class AsyncParseIPCamParamsAfterItemSelection extends AsyncTask<HttpResponse, Void, Void> {
		@Override
        protected Void doInBackground(HttpResponse... params) {
			if (params != null && params.length > 0) {
				ipCamParams = null;
				HttpResponse response = params[0];
				if (response != null) {
					try {
						HttpEntity r_entity = response.getEntity();
						String entityString = new String(EntityUtils.toString(r_entity));
						
						// If xmlString starts with "<", then we have XML structure which contains param values for H264
						if (entityString.startsWith("<")) {
							parseParamsH264(entityString);
						// Else we have regular text which contains param values for MJPEG
						} else {
							//BufferedReader rd = new BufferedReader(new InputStreamReader(r_entity.getContent()));
				    		parseParamsMJPEG(entityString);
						}
					} catch (Exception e) {
						Log.e(TAG, MESSAGE, e);
						return null;
					}
				}
			}
			return null;
		}
		
		@Override
        protected void onPostExecute(Void v) {
			try {
				if (mainActivity != null) {
					mainActivity.reCreateVideoView();
				}
			}catch (Exception e) {
				Log.e(TAG, MESSAGE, e);
			}
		} 
	}
	
	private class AsyncParseIPCamMisc extends AsyncTask<HttpResponse, Void, Void> {
		@Override
        protected Void doInBackground(HttpResponse... params) {
			if (params != null && params.length > 0) {
				ipCamMisc = null;
				HttpResponse response = params[0];
				if (response != null) {
					try {
						// Get the response
					    BufferedReader rd = new BufferedReader
					      (new InputStreamReader(response.getEntity().getContent()));
					    
					    String line = "";
					    ipCamMisc = new HashMap<String, String>();
					    while ((line = rd.readLine()) != null) {
					    	line = line.replace("var ", "");
					    	line = line.replace(";", "");
					    	line = line.replaceAll("\'", "");
					    	StringTokenizer tokenizer = new StringTokenizer(line, "=");
					    	if (tokenizer.countTokens() == 2) {
					    		ipCamMisc.put(tokenizer.nextToken(), tokenizer.nextToken());
					    	} else if (tokenizer.countTokens() == 1) {
					    		ipCamMisc.put(tokenizer.nextToken(), "");
					    	}
					    } 
					} catch (IOException e) {
						Log.e(TAG, MESSAGE, e);
						return null;
					} catch (NullPointerException e) {
						Log.e(TAG, MESSAGE, e);
						return null;
					}
				}
			}
			return null;
		}
		
		@Override
        protected void onPostExecute(Void v) {
			try {
				if (mainActivity != null) {
					mainActivity.setInitialPatrolState();
				}
			}catch (Exception e) {
				Log.e(TAG, MESSAGE, e);
			}
		} 
	}
	
	private class AsyncParseMirrorAndFlipParams extends AsyncTask<HttpResponse, Void, Void> {
		@Override
        protected Void doInBackground(HttpResponse... params) {
			if (params != null && params.length > 0) {
				HttpResponse response = params[0];
				if (response != null) {
					try {
						HttpEntity r_entity = response.getEntity();
						String entityString = new String(EntityUtils.toString(r_entity));
						
						if (entityString.startsWith("<")) {
							parseMirrorAndFlipParamsH264(entityString);
						}
					} catch (Exception e) {
						Log.e(TAG, MESSAGE, e);
						return null;
					}
				}
			}
			return null;
		}
	}
	
	private void parseMirrorAndFlipParamsH264(String xmlString) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        InputSource is;
        NodeList list;
        
		builder = factory.newDocumentBuilder();
		is = new InputSource(new StringReader(xmlString));
		Document doc = builder.parse(is);
		list = doc.getElementsByTagName("CGI_Result");
		
		if (list != null && list.getLength() > 0) {
			if (list.getLength() != 1) {
				throw new Exception(MORE_THAN_ONE_CGI_RESULT);
			} else {
				Node firstNode = list.item(0);
				if (firstNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element firstElement = (Element) firstNode;

                    NodeList isMirrorList = firstElement.getElementsByTagName("isMirror");
                    Element isMIrrorElement = (Element)isMirrorList.item(0);
                    NodeList textIsMirrorList = isMIrrorElement.getChildNodes();
                    String isMirrorVal = ((Node)textIsMirrorList.item(0)).getNodeValue().trim();
                    isMirrorH264 = (isMirrorVal.equals("1"));

                    NodeList isFlipList = firstElement.getElementsByTagName("isFlip");
                    Element isFlipElement = (Element)isFlipList.item(0);
                    NodeList textIsFlipList = isFlipElement.getChildNodes();
                    String isFlipVal = ((Node)textIsFlipList.item(0)).getNodeValue().trim();
                    isFlipH264 = (isFlipVal.equals("1"));
				}
			}
		}
	}
	
	private class AsyncParseIPCamCameraParams extends AsyncTask<HttpResponse, Void, Void> {
		@Override
        protected Void doInBackground(HttpResponse... params) {
			if (params != null && params.length > 0) {
				ipCamCameraParams = null;
				HttpResponse response = params[0];
				if (response != null) {
					try {
						HttpEntity r_entity = response.getEntity();
						String entityString = new String(EntityUtils.toString(r_entity));
						
						// If xmlString starts with "<", then we have XML structure which contains param values for H264
						if (entityString.startsWith("<")) {
							parseCameraParamsH264(entityString);
						// Else we have regular text which contains param values for MJPEG
						} else {
							//BufferedReader rd = new BufferedReader(new InputStreamReader(r_entity.getContent()));
				    		parseCameraParamsMJPEG(entityString);
						}
					} catch (Exception e) {
						Log.e(TAG, MESSAGE, e);
						return null;
					}
				}
			}
			return null;
		}
	}
	
	private void parseCameraParamsH264(String xmlString) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        InputSource is;
        NodeList list;
        
		builder = factory.newDocumentBuilder();
		is = new InputSource(new StringReader(xmlString));
		Document doc = builder.parse(is);
		list = doc.getElementsByTagName("CGI_Result");
		
		if (list != null && list.getLength() > 0) {
			if (list.getLength() != 1) {
				throw new Exception(MORE_THAN_ONE_CGI_RESULT);
			} else {
				Node firstNode = list.item(0);
				if (firstNode.getNodeType() == Node.ELEMENT_NODE) {
					ipCamCameraParams = new HashMap<String, String>();	// TODO: This should be done somewhere else before reaching here
                    Element firstElement = (Element) firstNode;

                    NodeList brightnessList = firstElement.getElementsByTagName("brightness");
                    Element brightnessElement = (Element)brightnessList.item(0);
                    NodeList textBrightnessList = brightnessElement.getChildNodes();
                    String brightnessVal = ((Node)textBrightnessList.item(0)).getNodeValue().trim();
                    ipCamCameraParams.put("brightness", brightnessVal);

                    NodeList contrastList = firstElement.getElementsByTagName("contrast");
                    Element contrastElement = (Element)contrastList.item(0);
                    NodeList textContrastList = contrastElement.getChildNodes();
                    String contrastVal = ((Node)textContrastList.item(0)).getNodeValue().trim();
                    ipCamCameraParams.put("contrast", contrastVal);
				}
			}
		}
	}
	
	private void parseCameraParamsMJPEG(String textString) throws IOException {
		ipCamCameraParams = new HashMap<String, String>();	// TODO: This should be done somewhere else before reaching here
		String[] lines = textString.split(System.getProperty("line.separator"));
		for (String line : lines) {
			line = line.replace("var ", "");
			line = line.replace(";", "");
			line = line.replaceAll("\'", "");
			StringTokenizer tokenizer = new StringTokenizer(line, "=");
			if (tokenizer.countTokens() == 2) {
				ipCamCameraParams.put(tokenizer.nextToken(), tokenizer.nextToken());
			} else if (tokenizer.countTokens() == 1) {
				ipCamCameraParams.put(tokenizer.nextToken(), "");
			}
		}
	}
}
