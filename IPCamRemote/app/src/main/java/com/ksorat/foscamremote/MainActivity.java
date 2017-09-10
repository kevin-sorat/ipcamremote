package com.ksorat.foscamremote;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.android.vending.billing.IInAppBillingService;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.ipc.sdk.FSApi;
import com.ksorat.ipcamremote.httpCommand.IPCamCommands;
import com.ksorat.ipcamremote.httpCommand.IPCamType;

public class MainActivity extends FragmentActivity {
	public static final String FILENAME = "foscam_remote_";
	public static final String FILENAME_FLIP_CONTROL = "foscam_remote_flip_control_";
	public static final String FILENAME_CURRENT_CAM = "foscam_remote_current_cam";
	public static final String FILENAME_SCREEN_TIMEOUT = "foscam_remote_screen_timeout";
	public static final String FILENAME_KEEP_SPEAKER_ON = "ip_cam_remote_keep_speaker_on";
	public static final String FILENAME_ANDROID_NOTIFICATION = "ip_cam_remote_android_notification";
	public static final String FILENAME_IS_LOW_RES = "ip_cam_remote_is_low_res";
	public static final String FILENAME_SCREEN_MODE = "ip_cam_remote_screen_mode";
	public static final String DISPLAY_AD = "ip_cam_remote_display_ad";
	public static final String FILENAME_IP_CAM_TYPE = "ip_cam_remote_ip_cam_type";
	public static final String SNAPSHOT_FOLDER = "IP Cam Remote";
	public static final String SNAPSHOT_NAME = "Snapshot";
	public static final String MY_AD_UNIT_ID = "a150d6921128c0a"; // "a150d4eec88bb8d";
	public static final String TEST_DEVICE_ID = "658307f6";
	private static final String LICENSE_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApsW5onOeGLzE6REZDkrcsOxgcu9rZSxBUD1PYlAXn8PG2KLtmqIOfOtL2pYvBk+CRCFSBRw3AvTelnPn0VInrWdwKTe1e02ahjy3JhEAFamJpWuPLRq1a9WEbPKL+kiv1o3UfAVgpNs5y5OKr3RTNt4HGECKu2Zudvb0iG518ZrfoTA4VnvsHX094hcUJmGqH2f1tW8O1CfFjlUcsb43";
	private static final int REQUEST_CODE = 1001;
	private static final String TAG = "IPCamRemote:MainAct";
	private static final String MESSAGE = "Received an exception";
	private static final String FOR_CONNECTION_SETTINGS = "forConnectionSettings";
	private static final String FOR_CONNECTION_SETTINGS_AFTER_ITEM_SELECTION = "forConnectionSettingsAfterItemSelection";
	private static final String FOR_NOTIFICATION_SETTINGS = "forNotificationSettings";
	private static final String FOR_SCREEN_SETTINGS = "forScreenSettings";
	private static final String FOR_MIRROR_FLIP_SETTINGS = "forMirrorFlipSettings";
	private static final String FOR_MOVEMENT = "forMovement";
	private static final String FOR_STOP_MOVEMENT = "forStopMovement";

	private static final int REQUEST_RESOLVE_ERROR = 1000;
	private static final int SHORT_WAIT_PERIOD = 1000;

	// Billing response codes
	public static final int BILLING_RESPONSE_RESULT_OK = 0;
	public static final int BILLING_RESPONSE_RESULT_USER_CANCELED = 1;
	public static final int BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE = 3;
	public static final int BILLING_RESPONSE_RESULT_ITEM_UNAVAILABLE = 4;
	public static final int BILLING_RESPONSE_RESULT_DEVELOPER_ERROR = 5;
	public static final int BILLING_RESPONSE_RESULT_ERROR = 6;
	public static final int BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED = 7;
	public static final int BILLING_RESPONSE_RESULT_ITEM_NOT_OWNED = 8;

	private MjpegView mv;
	private VideoView vv;
	private Audio mAudio;
	private Talk mTalk;
	private int channelId = 0;
	private JoystickView joystick;

	private String serverURL = ""; // "http://ksorat.no-ip.org:85";
	private String admin = "";
	private String password = "";
	
	private String videoStreamURL = "";
	private String videoStreamURLLowRes = "";
	private String upURL = "";
	private String upLeftURL = "";
	private String leftURL = "";
	private String downLeftURL = "";
	private String downURL = "";
	private String downRightURL = "";
	private String rightURL = "";
	private String upRightURL = "";
	private String stopURL = "";
	private String emailNotificationURL = "";
	private String alarmEnableURL = "";
	private String getParamsURL = "";
	private String getMiscURL = "";

	private String irAutoURL = "";
	private String irManualURL = "";
	private String irOnURL = "";
	private String irOffURL = "";
	private String irOnURL2 = "";
	private String irOffURL2 = "";
	private String hPatrolURL = "";
	private String vPatrolURL = "";
	private String stopPatrolURL = "";
	private String getCameraParamsURL = "";
	private String getMirrorAndFlipSettingURL = "";
	private String mirrorVideoURL = "";
	private String flipVideoURL = "";
	private String cameraControlURL = "";
	private String setBrightnessURL = "";
	private String setContrastURL = "";
	//private String setSubStreamFormatURL = "";
	private String snapshotURL = "";
	//private String setSnapConfigURL = "";
	
	private String presetSet1URL = "";
	private String presetGo1URL = "";
	private String presetSet2URL = "";
	private String presetGo2URL = "";
	private String presetSet3URL = "";
	private String presetGo3URL = "";
	private String presetSet4URL = "";
	private String presetGo4URL = "";
	private String presetSet5URL = "";
	private String presetGo5URL = "";
	private String presetSet6URL = "";
	private String presetGo6URL = "";
	private String presetSet7URL = "";
	private String presetGo7URL = "";
	private String presetSet8URL = "";
	private String presetGo8URL = "";
	
	private String presetDelete1URL = "";
	private String presetDelete2URL = "";
	private String presetDelete3URL = "";
	private String presetDelete4URL = "";
	private String presetDelete5URL = "";
	private String presetDelete6URL = "";
	private String presetDelete7URL = "";
	private String presetDelete8URL = "";
	
	private AsyncHttpCall movementCall;
	private AdView adView;
	//private String bufferedMovementURL;
	private boolean isFromOnCreate;
	public boolean isReCreatingMjpeg;
	private boolean isDisplaySeekbars = false;
	private View brightnessContrastView;

	private ReceiveAudioAsyncTask receiveAudioAsyncTask;
	private SendAudioAsyncTask sendAudioAsyncTask;
	private boolean speakerPreviouslyOn = false;

	private ArrayAdapter<String> adapter;
	private ArrayAdapter<String> presetAdapter;
	private MjpegInputStream mjpegInputStreamResult;

	private ProgressBar progressBar;
	private FrameLayout progressFrameLayout;
	private TextView connectionErrorTextView;

	private IInAppBillingService mService;
	private boolean isFullscreen;
	private boolean isUsedToBeFullscreen = false;
	private boolean prevIsLowRes = false;
	private int prevScreenMode = VideoView.SIZE_4_3;
	private int currentPresetIndex = 0;
	private boolean ignorePresetChange = true;
	private Menu menu;
	private String prevMovementURL;
	
	private GestureDetector mGestureDetector;
	
	boolean isUIEnabled;

	private ServiceConnection mServiceConn = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
			mService = null;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mService = IInAppBillingService.Stub.asInterface(service);
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Intent intent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
		// This is the key line
		intent.setPackage("com.android.vending");
		bindService(intent, mServiceConn, Context.BIND_AUTO_CREATE);

		IPCamData data = IPCamData.getInstance();
		data.mainActivity = this;

		removeConnectionSettingsDialog();
		loadDataFromInternalStorage();
		//getParametersFromIPCam();
		//getMiscFromIPCam();
		//getCameraParamsFromIPCam();

		new IsInAppPurchasedAsyncTask().execute();

		performInitialization();

		int currentIndex = loadCurrentCameraIndex();
		Spinner cameras_spinner = (Spinner) findViewById(R.id.cameras_spinner);
		if (cameras_spinner != null) {
			// int currentIPCamIndex = data.getCurrentIPCamIndex();
			String[] tempArray = getResources().getStringArray(
					R.array.cameras_array);
			for (int i = 0; i < IPCamData.NUM_IPCAMS; i++) {
				data.setCurrentIPCamIndex(i);
				String tempName = data.getCameraName();
				if (tempName != null && !tempName.equals("")) {
					tempArray[i] += " " + tempName;
				}
			}
			data.setCurrentIPCamIndex(currentIndex);

			// Create an ArrayAdapter using the string array and a default
			// spinner layout
			adapter = new ArrayAdapter<String>(this,
					android.R.layout.simple_spinner_item, tempArray);

			// Specify the layout to use when the list of choices appears
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			// Apply the adapter to the spinner
			cameras_spinner.setAdapter(adapter);
			cameras_spinner.setOnItemSelectedListener(new CameraSpinnerItemSelectedListener());
			cameras_spinner.setSelection(currentIndex);
			
			processAlarmNotification(true);
		}
		isFromOnCreate = true;
		isReCreatingMjpeg = false;
		isFullscreen = false;
		
		FSApi.Init();
		
		if (mAudio == null) {
			mAudio = new Audio();
			mAudio.start();
		}
		
		if (mTalk == null) {
			mTalk = new Talk();
			mTalk.start();
		}
	}

	void performInitialization() {
		int currentIndex = loadCurrentCameraIndex();
		IPCamData data = IPCamData.getInstance();
		// Set back the saved current IPCam index here
		data.setCurrentIPCamIndex(currentIndex);

		joystick = (JoystickView) findViewById(R.id.joystickView1);
		joystick.setOnJostickMovedListener(_listener);

		ToggleButton hPatrolToggleButton = (ToggleButton) findViewById(R.id.hPatrolToggleButton);
		ToggleButton vPatrolToggleButton = (ToggleButton) findViewById(R.id.vPatrolToggleButton);
		ToggleButton speakerToggleButton = (ToggleButton) findViewById(R.id.speakerToggleButton);
		ToggleButton micToggleButton = (ToggleButton) findViewById(R.id.micToggleButton);

		hPatrolToggleButton
				.setOnCheckedChangeListener(new HPatrolToggleButtonCheckedChangeListener());
		vPatrolToggleButton
				.setOnCheckedChangeListener(new VPatrolToggleButtonCheckedChangeListener());
		speakerToggleButton
				.setOnCheckedChangeListener(new SpeakerToggleButtonCheckedChangeListener());

		/*
		 * if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
		 * micToggleButton.setOnCheckedChangeListener(new
		 * micToggleButtonCheckedChangeListener()); } else {
		 * micToggleButton.setOnTouchListener(new micButtonTouchListener()); }
		 */
		micToggleButton.setOnTouchListener(new MicButtonTouchListener());
		
		mGestureDetector = new GestureDetector(this, new DoubleTapGestureListener());
	}

	private void displayAd() {
		IPCamData data = IPCamData.getInstance();
		// Display ad, if the user has not bought the In-app purchase to remove
		// the ad.
		if (data.displayAd) {
			try {
				// Create the adView
				adView = new AdView(this);
				adView.setAdUnitId(MY_AD_UNIT_ID);
				adView.setAdSize(AdSize.BANNER);
				
				LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
						LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				params.gravity = Gravity.CENTER_HORIZONTAL;
				adView.setLayoutParams(params);
				LinearLayout layout = (LinearLayout) findViewById(R.id.linearLayout1);
				// Add the adView to it
				layout.addView(adView);

				AdRequest adRequest = new AdRequest.Builder().build();
				
				/*
				 * adRequest.addTestDevice(AdRequest.TEST_EMULATOR); // Emulator
				 * adRequest.addTestDevice(TEST_DEVICE_ID); // Test Android
				 * Device
				 */
				// Initiate a generic request to load it with an ad
				adView.loadAd(adRequest);
			} catch (NullPointerException e) {
				Log.e(TAG, MESSAGE, e);
			} catch (Exception e) {
				Log.e(TAG, MESSAGE, e);
			}
		}
	}

	private void removeAd() {
		if (adView != null) {
			LinearLayout layout = (LinearLayout) findViewById(R.id.linearLayout1);
			layout.removeView(adView);
			adView.destroy();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (mServiceConn != null) {
			unbindService(mServiceConn);
		}

		destroyVideoViewSource();
		removeConnectionSettingsDialog();
		removeNotificationSettingsDialog();
		removeScreenSettingsDialog();

		Spinner cameras_spinner = (Spinner) findViewById(R.id.cameras_spinner);
		ToggleButton hPatrolToggleButton = (ToggleButton) findViewById(R.id.hPatrolToggleButton);
		ToggleButton vPatrolToggleButton = (ToggleButton) findViewById(R.id.vPatrolToggleButton);

		cameras_spinner.setOnItemSelectedListener(null);
		hPatrolToggleButton.setOnCheckedChangeListener(null);
		vPatrolToggleButton.setOnCheckedChangeListener(null);
		joystick.setOnJostickMovedListener(null);
		joystick = null;

		if (adView != null) {
			LinearLayout layout = (LinearLayout) findViewById(R.id.linearLayout1);
			layout.removeAllViews();
			adView.removeAllViews();
			adView.destroy();
		}
		
		if (mAudio != null) {
			mAudio.stop();
			mAudio = null;
		}
		
		if (mTalk != null) {
			mTalk.stop();
			mTalk = null;
		}
		
		FSApi.usrLogOut(channelId);
		FSApi.Uninit();

		// Help refresh the screen much faster
		System.gc();
		System.exit(0);
	}

	/*
	 * @Override public void onStart() { super.onStart();
	 * 
	 * 
	 * }
	 * 
	 * @Override public void onStop() { super.onStop();
	 * 
	 * }
	 */

	@Override
	public void onResume() {
		super.onResume();

		// From wear
		boolean isResumeFromWear = IPCamRemoteApplication.isResumeFromWear();

		// Remember current state (foreground) of main activity
		IPCamRemoteApplication.activityResumed();
		
		// Disable UI until the view creation is finished
		disableUI();

		IPCamData data = IPCamData.getInstance();
		// If coming from clicking the mjpeg view in MultiViewActivity
		if (data.fromMultiView || isResumeFromWear) {
			
			if (isFullscreen) {
				// Must toggle to non-fullscreen first to set the camera index, 
				// then toggle back to fullscreen mode later in updateMVFromExistingResult
				isUsedToBeFullscreen = true;
				isFullscreen = false;
				toggleFullScreenMode();
				
				try {
					Thread.sleep(SHORT_WAIT_PERIOD);
				} catch (InterruptedException e) {
					Log.e(TAG, MESSAGE, e);
				}
			}
			
			// Make the selection from the spinner instead using the selected
			// index coming from MultiViewActivity
			Spinner cameras_spinner = (Spinner) findViewById(R.id.cameras_spinner);
			cameras_spinner.setSelection(data.getCurrentIPCamIndex());
			data.fromMultiView = false;
			// Do not continue further in this function
			return;
		}

		processScreenTimeout();

		// This is to prevent createMjpegViewSource from getting called twice
		// during the app start up
		if (isFromOnCreate) {
			isFromOnCreate = false;

			return;
		}

		if (data.getCameraName() != null && !data.getCameraName().equals("")) {
			showBusy();

			Handler handlerTimer = new Handler();
			handlerTimer.postDelayed(new Runnable() {
				public void run() {
					IPCamData data = IPCamData.getInstance();
					// If the current camera type is H264
					if (data.getIPCamType().equals(IPCamType.H264)) {
						createH264ViewSource();
					} else {
						// Else the current camera must be MJPEG or Public
						createMjpegViewSource();
					}
				}
			}, 300);
		} else {
			destroyVideoViewSource();
			displayConnectionErrorMessage();
			showConnectionSettingsDialog();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		// Remember current state (background) of main activity
		IPCamRemoteApplication.activityPaused();

		destroyVideoViewSource();
		removeConnectionSettingsDialog();
		removeNotificationSettingsDialog();
		removeScreenSettingsDialog();

		// Help refresh the screen much faster
		System.gc();
	}

	// This will get called when the screen orientation has changed
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		ToggleButton hPatrolToggleButton = (ToggleButton) findViewById(R.id.hPatrolToggleButton);
		ToggleButton vPatrolToggleButton = (ToggleButton) findViewById(R.id.vPatrolToggleButton);
		ToggleButton speakerToggleButton = (ToggleButton) findViewById(R.id.speakerToggleButton);

		// Store the check states of toggle buttons
		boolean isHPatrolChecked = hPatrolToggleButton.isChecked();
		boolean isVPatrolChecked = vPatrolToggleButton.isChecked();
		boolean isSpeakerChecked = speakerToggleButton.isChecked();

		removeConnectionSettingsDialog();
		removeNotificationSettingsDialog();
		removeScreenSettingsDialog();

		setContentView(R.layout.activity_main);

		// Must terminate the microphone
		terminateMic();

		// Retrieve the new references of toggle buttons after the orientation
		// change
		hPatrolToggleButton = (ToggleButton) findViewById(R.id.hPatrolToggleButton);
		vPatrolToggleButton = (ToggleButton) findViewById(R.id.vPatrolToggleButton);
		speakerToggleButton = (ToggleButton) findViewById(R.id.speakerToggleButton);

		// Restore the states of toggle buttons
		hPatrolToggleButton.setChecked(isHPatrolChecked);
		vPatrolToggleButton.setChecked(isVPatrolChecked);
		speakerToggleButton.setChecked(isSpeakerChecked);

		performInitialization();
		displayAd();
		toggleFullScreenMode();

		// Restore camera spinner
		Spinner cameras_spinner = (Spinner) findViewById(R.id.cameras_spinner);
		IPCamData data = IPCamData.getInstance();
		cameras_spinner.setOnItemSelectedListener(null);
		cameras_spinner.setAdapter(adapter);
		int currentIndex = loadCurrentCameraIndex();
		data.ignoreSetSelection = true;
		cameras_spinner.setSelection(currentIndex);
		cameras_spinner.setOnItemSelectedListener(new CameraSpinnerItemSelectedListener());

		// Restore video view
		if (data.getCameraName() != null && !data.getCameraName().equals("")) {
			if (data.getIPCamType().equals(IPCamType.H264)) {
				hideBrightnessAndContrast();
				if (vv != null) {
					FrameLayout prevFl = (FrameLayout) vv.getParent();
					if (prevFl != null) {
						prevFl.removeAllViewsInLayout();
					}
					
					FrameLayout fl1 = (FrameLayout) findViewById(R.id.frameLayout1);
					if (fl1 != null) {
						fl1.removeAllViewsInLayout();
						fl1.addView(vv);
						enableUI();
					}
				}
			} else if (data.getIPCamType().equals(IPCamType.MJPEG)) {
				if (mv != null) {
					mv.stopPlayback();
					mv.destroyDrawingCache();
					mv.destroyMjpegInputStream();
					mv.setOnClickListener(null);
					mv = null;
				}
				mv = new MjpegView(this);
				updateMVFromExistingResult();
			}
		} else {
			destroyVideoViewSource();
			displayConnectionErrorMessage();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		invokeFragmentManagerNoteStateNotSaved();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		// No action here, call super to delegate to Fragments
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode != REQUEST_CODE) {
			return;
		}

		try {
			// If In-app purchase was successful, remove the ad
			if (resultCode == Activity.RESULT_OK) {
				IPCamData ipcam_data = IPCamData.getInstance();
				// Set the display ad flag to false
				ipcam_data.displayAd = false;
				saveDataToInternalStorage();
				// Remove the existing ad, if exists
				removeAd();
				ipcam_data.isInAppPurchased = true;
				hideOption(R.id.remove_ad_in_app);
				hideOption(R.id.remove_ad);
				showOption(R.id.display_ad);
				
				Toast toast = Toast.makeText(getApplicationContext(), 
        				getResources().getString(R.string.in_app_success),
    					Toast.LENGTH_LONG);
    			toast.show();
			// Else In-app purchase was not successful, display a message
			} else {
				Toast toast = Toast.makeText(getApplicationContext(), 
        				getResources().getString(R.string.in_app_failed),
    					Toast.LENGTH_LONG);
    			toast.show();
			}
		} catch (Exception e) {
			Log.e(TAG, MESSAGE, e);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void invokeFragmentManagerNoteStateNotSaved() {
		/**
		 * For post-Honeycomb devices
		 * */
		if (Build.VERSION.SDK_INT < 11) {
			return;
		}
		try {
			Class cls = getClass();
			do {
				cls = cls.getSuperclass();
			} while (!"Activity".equals(cls.getSimpleName()));
			Field fragmentMgrField = cls.getDeclaredField("mFragments");
			fragmentMgrField.setAccessible(true);

			Object fragmentMgr = fragmentMgrField.get(this);
			cls = fragmentMgr.getClass();

			Method noteStateNotSavedMethod = cls.getDeclaredMethod(
					"noteStateNotSaved", new Class[] {});
			noteStateNotSavedMethod.invoke(fragmentMgr, new Object[] {});
			Log.d("DLOutState", "Successful call for noteStateNotSaved!!!");
		} catch (Exception ex) {
			Log.e("DLOutState", "Exception on worka FM.noteStateNotSaved", ex);
		}
	}

	void disableUI() { 
		//Spinner cameras_spinner = (Spinner) findViewById(R.id.cameras_spinner);
		ToggleButton hPatrolToggleButton = (ToggleButton) findViewById(R.id.hPatrolToggleButton);
		ToggleButton vPatrolToggleButton = (ToggleButton) findViewById(R.id.vPatrolToggleButton);
		ToggleButton speakerToggleButton = (ToggleButton) findViewById(R.id.speakerToggleButton);
		ToggleButton micToggleButton = (ToggleButton) findViewById(R.id.micToggleButton);

		//if (cameras_spinner != null)
			//cameras_spinner.setEnabled(false);
		if (hPatrolToggleButton != null)
			hPatrolToggleButton.setEnabled(false);
		if (vPatrolToggleButton != null)
			vPatrolToggleButton.setEnabled(false);
		if (speakerToggleButton != null)
			speakerToggleButton.setEnabled(false);
		if (micToggleButton != null)
			micToggleButton.setEnabled(false);
		
		hideOption(R.id.take_pict);
		hideOption(R.id.notification_settings);
		hideOption(R.id.screen_settings);
		hideOption(R.id.go_to_monitoring_system);
		
		// Set the joystick handle to gray
		joystick.setHandleColor(Color.rgb(60, 60, 60));
		joystick.invalidate();
		
		isUIEnabled = false;
	}

	void enableUI() {
		//Spinner cameras_spinner = (Spinner) findViewById(R.id.cameras_spinner);
		ToggleButton hPatrolToggleButton = (ToggleButton) findViewById(R.id.hPatrolToggleButton);
		ToggleButton vPatrolToggleButton = (ToggleButton) findViewById(R.id.vPatrolToggleButton);
		ToggleButton speakerToggleButton = (ToggleButton) findViewById(R.id.speakerToggleButton);
		ToggleButton micToggleButton = (ToggleButton) findViewById(R.id.micToggleButton);

		//if (cameras_spinner != null)
			//cameras_spinner.setEnabled(true);
		if (hPatrolToggleButton != null)
			hPatrolToggleButton.setEnabled(true);
		if (vPatrolToggleButton != null)
			vPatrolToggleButton.setEnabled(true);
		if (speakerToggleButton != null)
			speakerToggleButton.setEnabled(true);
		if (micToggleButton != null)
			micToggleButton.setEnabled(true);
		
		showOption(R.id.take_pict);
		showOption(R.id.notification_settings);
		showOption(R.id.screen_settings);
		showOption(R.id.go_to_monitoring_system);
		
		// Set the joystick handle to blue
		joystick.setHandleColor(Color.rgb(20, 20, 150));
		joystick.invalidate();
		
		isUIEnabled = true;
	}

	private void getParametersFromIPCam() {
		// Get all parameter values from the IP Cam
		executeURL(getParamsURL, FOR_CONNECTION_SETTINGS);
	}

	public void getParametersFromIPCamAfterItemSelection() {
		// Get all parameter values from the IP Cam
		executeURL(getParamsURL, FOR_CONNECTION_SETTINGS_AFTER_ITEM_SELECTION);
	}

	private void getMiscFromIPCam() {
		// Get misc. parameter values from the IP Cam
		executeURL(getMiscURL, FOR_NOTIFICATION_SETTINGS);
	}

	private void getCameraParamsFromIPCam() {
		// Get camera params from the IP Cam
		executeURL(getCameraParamsURL, FOR_SCREEN_SETTINGS);
		
		// For H264 camera, also execute another command to retrieve mirror and flip
		try {
			Thread.sleep(SHORT_WAIT_PERIOD);
		} catch (InterruptedException e) {
			Log.e(TAG, MESSAGE, e);
		}
		executeURL(getMirrorAndFlipSettingURL, FOR_MIRROR_FLIP_SETTINGS);
	}
	
	/*
	// Not being used now
	private void setSubStreamFormat() {
		// Set substream format of H264 camera to MJPEG format (1)
		IPCamData data = IPCamData.getInstance();
		if (data.getIPCamType().equals(IPCamType.H264)) {
			IPCamCommands commands = data.getIPCamCommands();
			Object[] args = { serverURL, admin, password, String.valueOf(1) };
			setSubStreamFormatURL = MessageFormat.format(commands.setSubStreamFormatURLTemp, args);
			executeURL(setSubStreamFormatURL);
		}
	}
	*/
	
	/*
	// Not being used now
	private void setSnapConfig() {
		IPCamData data = IPCamData.getInstance();
		if (data.getIPCamType().equals(IPCamType.H264)) {
			IPCamCommands commands = data.getIPCamCommands();
			Object[] args = { serverURL, admin, password };
			setSnapConfigURL = MessageFormat.format(commands.setSnapConfigURLTemp, args);
			executeURL(setSnapConfigURL);
		}
	}
	*/
	
	@SuppressLint("Recycle")
	private void removeConnectionSettingsDialog() {
		FragmentManager fm = getSupportFragmentManager();
		if (fm == null)
			return;
		FragmentTransaction ft = fm.beginTransaction();
		Fragment prev = fm.findFragmentByTag("connectionSettingsDialog");
		if (prev != null) {
			ft.remove(prev);
		}
		ft.addToBackStack(null);
	}

	@SuppressLint("Recycle")
	private void removeNotificationSettingsDialog() {
		FragmentManager fm = getSupportFragmentManager();
		if (fm == null)
			return;
		FragmentTransaction ft = fm.beginTransaction();
		Fragment prev = fm.findFragmentByTag("notificationSettingsDialog");
		if (prev != null) {
			ft.remove(prev);
		}
		ft.addToBackStack(null);
	}

	@SuppressLint("Recycle")
	private void removeScreenSettingsDialog() {
		FragmentManager fm = getSupportFragmentManager();
		if (fm == null)
			return;
		FragmentTransaction ft = fm.beginTransaction();
		Fragment prev = fm.findFragmentByTag("screenSettingsDialog");
		if (prev != null) {
			ft.remove(prev);
		}
		ft.addToBackStack(null);
	}

	private void showConnectionSettingsDialog() {
		getParametersFromIPCam();
		removeConnectionSettingsDialog();
		hideBrightnessAndContrast();

		FragmentManager fm = getSupportFragmentManager();
		if (fm == null)
			return;
		FragmentTransaction ft = fm.beginTransaction();

		Fragment conn = fm.findFragmentByTag("connectionSettingsDialog");
		Fragment notify = fm.findFragmentByTag("notificationSettingsDialog");
		Fragment screen = fm.findFragmentByTag("screenSettingsDialog");
		if (conn == null && notify == null && screen == null) {
			// Create and show the dialog.
			DialogFragment newFragment = IPCamSettingsDialogFragment
					.getInstance();
			ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
			newFragment.show(ft, "connectionSettingsDialog");
		}
	}

	private void showNotificationSettingsDialog() {
		getParametersFromIPCam();
		removeNotificationSettingsDialog();
		hideBrightnessAndContrast();

		FragmentManager fm = getSupportFragmentManager();
		if (fm == null)
			return;
		FragmentTransaction ft = fm.beginTransaction();

		Fragment conn = fm.findFragmentByTag("connectionSettingsDialog");
		Fragment notify = fm.findFragmentByTag("notificationSettingsDialog");
		Fragment screen = fm.findFragmentByTag("screenSettingsDialog");
		if (conn == null && notify == null && screen == null) {
			// Create and show the dialog.
			DialogFragment newFragment = NotificationSettingsDialogFragment
					.getInstance();
			ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
			newFragment.show(ft, "notificationSettingsDialog");
		}
	}

	private void showScreenSettingsDialog() {
		getParametersFromIPCam();
		removeScreenSettingsDialog();
		hideBrightnessAndContrast();

		FragmentManager fm = getSupportFragmentManager();
		if (fm == null)
			return;
		FragmentTransaction ft = fm.beginTransaction();

		Fragment conn = fm.findFragmentByTag("connectionSettingsDialog");
		Fragment notify = fm.findFragmentByTag("notificationSettingsDialog");
		Fragment screen = fm.findFragmentByTag("screenSettingsDialog");
		if (conn == null && notify == null && screen == null) {
			// Create and show the dialog.
			DialogFragment newFragment = ScreenSettingsDialogFragment
					.getInstance();
			ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
			newFragment.show(ft, "screenSettingsDialog");
		}
	}
	
	@SuppressLint("ClickableViewAccessibility")
	private void createH264ViewSource() {
		IPCamData data = IPCamData.getInstance();
		int devType = 1; // 0 = MJPEG, 1 = H264
		String ipAddr = data.getServerURLNoHttp();
		String userName = data.getUserName();
		String password = data.getPassword();
		int streamType = 0; // 0 = sub stream, 1 = main stream
		String uid = "";
		int portNumber = 80;
		String portNumberStr = data.getPortNumberOrPublic();
		try {
			portNumber = Integer.parseInt(portNumberStr);
		} catch (Exception e) {
			Log.e(TAG, MESSAGE, e);
			dismissBusy();
			displayConnectionErrorMessage();
		}
		
		try {
			destroyVideoViewSource();
			
			// Disable UI until the view creation is finished
			disableUI();
			
			FSApi.usrLogOut(channelId);
			FSApi.usrLogIn(devType, ipAddr, userName, password, streamType, portNumber, portNumber, uid, channelId);
			if (vv == null) {
				DisplayMetrics metric = new DisplayMetrics();
		        getWindowManager().getDefaultDisplay().getMetrics(metric);
				vv = new VideoView(this, metric.widthPixels, metric.heightPixels, channelId, data.getScreenMode());
				vv.setClickable(true);	// Somehow this is needed, unlike MjpegView
				vv.setOnTouchListener(new OnTouchListener() {
					@SuppressLint("ClickableViewAccessibility")
					@Override
					public boolean onTouch(View v, MotionEvent event) {
						mGestureDetector.onTouchEvent(event);
						return false;
					}
				});
			}

			vv.start();
			vv.startVideoStream();
			
			hideBrightnessAndContrast();
			FrameLayout fl1 = (FrameLayout) findViewById(R.id.frameLayout1);
			if (fl1 != null) {
				fl1.removeAllViewsInLayout();
				fl1.addView(vv);
			}
			
		} catch (Exception e) {
			Log.e(TAG, MESSAGE, e);
			dismissBusy();
			displayConnectionErrorMessage();
		}
	}

	@SuppressLint("NewApi")
	private void createMjpegViewSource() {
		try {
			destroyVideoViewSource();
			
			// Disable UI until the view creation is finished
			disableUI();
			mv = new MjpegView(this);

			String URL;
			IPCamData data = IPCamData.getInstance();
			// If the low res mode was selected
			if (data.isLowRes) {
				URL = videoStreamURLLowRes;
			} else {
				URL = videoStreamURL;
			}
			
			// Must do the following asynchronously in Android 4.1
			AsyncReadMjpegInputStream asyncReadMjpegInputStream = new AsyncReadMjpegInputStream();
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
				asyncReadMjpegInputStream.execute(URL);
			} else {
				asyncReadMjpegInputStream.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, URL);
			}
		} catch (IllegalStateException ex) {
			Log.e(TAG, MESSAGE, ex);
			Toast toast = Toast
					.makeText(getApplicationContext(),
							R.string.your_camera_url_path_is_invalid,
							Toast.LENGTH_LONG);
			toast.show();
			dismissBusy();
		} catch (Exception e) {
			Log.e(TAG, MESSAGE, e);
			dismissBusy();
		}
	}

	private void updateMV(MjpegInputStream result) {
		mjpegInputStreamResult = result; // Cache the result for orientation change
		updateMVFromExistingResult();
		dismissBusy();
	}

	@SuppressLint("ClickableViewAccessibility")
	private void updateMVFromExistingResult() {
		try {
			if (mv != null) {
				mv.setSource(mjpegInputStreamResult);
				mv.setDisplayMode(MjpegView.SIZE_BEST_FIT);
				//mv.setOnClickListener(new MjpegViewClickListener());
				mv.setOnTouchListener(new OnTouchListener() {
					@Override
					public boolean onTouch(View v, MotionEvent event) {
						mGestureDetector.onTouchEvent(event);
						return false;
					}
				});
				//mv.showFps(true);

				hideBrightnessAndContrast();
				FrameLayout fl1 = (FrameLayout) findViewById(R.id.frameLayout1);
				if (fl1 != null) {
					fl1.removeAllViewsInLayout();
					if (mv.getMjpegInputStream() != null) {
						fl1.addView(mv);
						
						IPCamData data = IPCamData.getInstance();
						// If public camera is selected
						if (data.getPortNumberOrPublic().equals(IPCamera.PUBLIC)) {
							disableUI();
						} else {
							enableUI();
						}
					} else {
						displayConnectionErrorMessage();
					}
				}
			}
		} catch (IllegalStateException ex) {
			Log.e(TAG, MESSAGE, ex);
			Toast toast = Toast
					.makeText(getApplicationContext(),
							R.string.your_camera_url_path_is_invalid,
							Toast.LENGTH_LONG);
			toast.show();
		} catch (Exception e) {
			Log.e(TAG, MESSAGE, e);
		}
		
		if (isUsedToBeFullscreen) {
			isFullscreen = true;
			isUsedToBeFullscreen = false;
		}
		toggleFullScreenMode();
	}

	protected void displayConnectionErrorMessage() {
		try {
			if (connectionErrorTextView == null) {
				connectionErrorTextView = new TextView(getApplicationContext());
				connectionErrorTextView.setGravity(Gravity.CENTER);
				connectionErrorTextView
						.setText(R.string.ipcam_view_cannot_be_displayed);
				connectionErrorTextView.setTextColor(Color.LTGRAY);
				connectionErrorTextView.setPadding(6, 6, 6, 6);
			}
			FrameLayout fl1 = (FrameLayout) findViewById(R.id.frameLayout1);
			if (connectionErrorTextView.getParent() == null) {
				fl1.addView(connectionErrorTextView);
			}
		} catch (Exception e) {
			Log.e(TAG, MESSAGE, e);
		}
	}

	protected void destroyVideoViewSource() {
		IPCamData data = IPCamData.getInstance();
		
		if (vv != null) {
			vv.stopVideoStream();
			android.os.SystemClock.sleep(500);
			vv.clearScreen();
			vv.stop();
			vv = null;
		} 
		
		if (mv != null) {
			mv.stopPlayback();
			mv.destroyDrawingCache();
			mv.destroyMjpegInputStream();
			mv.setOnClickListener(null);
			mv = null;
		}
		
		// Clear the cache because we no longer need it at this point
		mjpegInputStreamResult = null;

		// If the "Keep Speaker On" feature is OFF
		// OR if camera type is H264 (because we cannot keep the speaker on for H264 yet)
		if (!data.keepSpeakerOn || data.getIPCamType().equals(IPCamType.H264)) {
			// This will turn off the speaker, by toggle its button to off
			uncheckSpeakerToggleButton();
		}
		uncheckMicToggleButton();

		hideBrightnessAndContrast();
		FrameLayout fl1 = (FrameLayout) findViewById(R.id.frameLayout1);
		if (fl1.getChildCount() > 0) {
			fl1.removeAllViewsInLayout();
		}
	}

	private void terminateSpeaker() {
		IPCamData data = IPCamData.getInstance();
		if (data.getIPCamType().equals(IPCamType.H264)) {
			FSApi.stopAudioStream(channelId);
			// Help refresh the screen much faster
			System.gc();
		} else {
			if (receiveAudioAsyncTask != null) {
				receiveAudioAsyncTask.isDone = true;
				// receiveAudioAsyncTask.closeSockets(); // this will cause a crash!
				receiveAudioAsyncTask = null;
				// Help refresh the screen much faster
				System.gc();
			}
		}
	}

	private void terminateMic() {
		IPCamData data = IPCamData.getInstance();
		if (data.getIPCamType().equals(IPCamType.H264)) {
			if (mTalk != null) {
				mTalk.stopTalk();
				// Help refresh the screen much faster
				System.gc();
			}
		} else {
			if (sendAudioAsyncTask != null) {
				sendAudioAsyncTask.isDone = true;
				// sendAudioAsyncTask.closeSockets();
				sendAudioAsyncTask = null;
				// Help refresh the screen much faster
				System.gc();
			}
		}
	}

	public void generateURLStrings() {
		IPCamData data = IPCamData.getInstance();
		// If public camera is selected
		if (data.getPortNumberOrPublic().equals(IPCamera.PUBLIC)) {
			// Use the predefined video stream URL 
			videoStreamURL = data.getServerURL();
			videoStreamURLLowRes = data.getServerURL();
		// Else use foscam URLs
		} else {
			serverURL = data.getServerURL() + ":" + data.getPortNumberOrPublic();
			admin = data.getUserName();
			password = data.getPassword();
			Object[] args = { serverURL, admin, password };
			
			// TODO: Create commands object based on type of the ip camera (Mjpeg or H.264)
			IPCamCommands commands = data.getIPCamCommands();
			
			videoStreamURL = MessageFormat.format(commands.videoStreamURLTemp, args);
			videoStreamURLLowRes = MessageFormat.format(commands.videoStreamURLLowResTemp, args);
			
			upURL = MessageFormat.format(commands.upURLTemp, args);
			upLeftURL = MessageFormat.format(commands.upLeftURLTemp, args);
			leftURL = MessageFormat.format(commands.leftURLTemp, args);
			downLeftURL = MessageFormat.format(commands.downLeftURLTemp, args);
			downURL = MessageFormat.format(commands.downURLTemp, args);
			downRightURL = MessageFormat.format(commands.downRightURLTemp, args);
			rightURL = MessageFormat.format(commands.rightURLTemp, args);
			upRightURL = MessageFormat.format(commands.upRightURLTemp, args);
			stopURL = MessageFormat.format(commands.stopURLTemp, args);
	
			// emailNotificationURL = MessageFormat.format(emailNotificationURLTemp,
			// args);
			getParamsURL = MessageFormat.format(commands.getParamsURLTemp, args);
			getMiscURL = MessageFormat.format(commands.getMiscURLTemp, args);
			getCameraParamsURL = MessageFormat.format(commands.getCameraParamsURLTemp, args);
			getMirrorAndFlipSettingURL = MessageFormat.format(commands.getMirrorAndFlipSettingURLTemp, args);
			
			irAutoURL = MessageFormat.format(commands.irAutoURLTemp, args);
			irManualURL = MessageFormat.format(commands.irManualURLTemp, args);
			irOnURL = MessageFormat.format(commands.irOnURLTemp, args);
			irOffURL = MessageFormat.format(commands.irOffURLTemp, args);
			irOnURL2 = MessageFormat.format(commands.irOnURL2Temp, args);
			irOffURL2 = MessageFormat.format(commands.irOffURL2Temp, args);
			
			presetSet1URL = MessageFormat.format(commands.presetSet1URLTemp, args);
			presetGo1URL = MessageFormat.format(commands.presetGo1URLTemp, args);
			presetSet2URL = MessageFormat.format(commands.presetSet2URLTemp, args);
			presetGo2URL = MessageFormat.format(commands.presetGo2URLTemp, args);
			presetSet3URL = MessageFormat.format(commands.presetSet3URLTemp, args);
			presetGo3URL = MessageFormat.format(commands.presetGo3URLTemp, args);
			presetSet4URL = MessageFormat.format(commands.presetSet4URLTemp, args);
			presetGo4URL = MessageFormat.format(commands.presetGo4URLTemp, args);
			presetSet5URL = MessageFormat.format(commands.presetSet5URLTemp, args);
			presetGo5URL = MessageFormat.format(commands.presetGo5URLTemp, args);
			presetSet6URL = MessageFormat.format(commands.presetSet6URLTemp, args);
			presetGo6URL = MessageFormat.format(commands.presetGo6URLTemp, args);
			presetSet7URL = MessageFormat.format(commands.presetSet7URLTemp, args);
			presetGo7URL = MessageFormat.format(commands.presetGo7URLTemp, args);
			presetSet8URL = MessageFormat.format(commands.presetSet8URLTemp, args);
			presetGo8URL = MessageFormat.format(commands.presetGo8URLTemp, args);
			
			presetDelete1URL = MessageFormat.format(commands.presetDelete1URLTemp, args);
			presetDelete2URL = MessageFormat.format(commands.presetDelete2URLTemp, args);
			presetDelete3URL = MessageFormat.format(commands.presetDelete3URLTemp, args);
			presetDelete4URL = MessageFormat.format(commands.presetDelete4URLTemp, args);
			presetDelete5URL = MessageFormat.format(commands.presetDelete5URLTemp, args);
			presetDelete6URL = MessageFormat.format(commands.presetDelete6URLTemp, args);
			presetDelete7URL = MessageFormat.format(commands.presetDelete7URLTemp, args);
			presetDelete8URL = MessageFormat.format(commands.presetDelete8URLTemp, args);
	
			hPatrolURL = MessageFormat.format(commands.hPatrolURLTemp, args);
			vPatrolURL = MessageFormat.format(commands.vPatrolURLTemp, args);
			stopPatrolURL = MessageFormat.format(commands.stopPatrolURLTemp, args);
			snapshotURL = MessageFormat.format(commands.snapshotURLTemp, args);
		}
	}

	public void clearButtonClickConnectionSettingsHandler() {
		generateURLStrings();
		saveDataToInternalStorage();

		// Update the array of cameras for the spinner with the new camera name
		String[] tempArray = getResources().getStringArray(
				R.array.cameras_array);
		IPCamData data = IPCamData.getInstance();
		int currentFoscamIndex = data.getCurrentIPCamIndex();
		for (int i = 0; i < IPCamData.NUM_IPCAMS; i++) {
			data.setCurrentIPCamIndex(i);
			String tempName = data.getCameraName();
			if (tempName != null && !tempName.equals("")) {
				tempArray[i] += " " + tempName;
			}
		}
		data.setCurrentIPCamIndex(currentFoscamIndex);
		adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, tempArray);

		// Specify the layout to use when the list of choices appears
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		// Apply the adapter to the spinner
		Spinner cameras_spinner = (Spinner) findViewById(R.id.cameras_spinner);
		cameras_spinner.setAdapter(adapter);

		data.ignoreSetSelection = true; // this is to avoid getting Connection
										// dialog to display in this specific
										// case
		cameras_spinner.setSelection(currentFoscamIndex);

		saveCurrentCameraIndex();
		destroyVideoViewSource();
		displayConnectionErrorMessage();
		currentPresetIndex = 0;

		// data.ignoreSetSelection = false;
	}

	public void okButtonClickConnectionSettingsHandler() {

		// TODO: Only execute the below code if there were actually changes.

		generateURLStrings();
		
		IPCamData data = IPCamData.getInstance();
		
		// Fix the problem with createMjpegViewSource getting called twice 
		// for public cam when it is first added
		// Note: Not sure why this is needed.
		/*
		if (!data.getPortNumberOrPublic().equals(IPCamera.PUBLIC)) {
			createMjpegViewSource();
		}
		*/
		
		saveDataToInternalStorage();

		// Update the array of cameras for the spinner with the new camera name
		String[] tempArray = getResources().getStringArray(
				R.array.cameras_array);
		int currentFoscamIndex = data.getCurrentIPCamIndex();
		for (int i = 0; i < IPCamData.NUM_IPCAMS; i++) {
			data.setCurrentIPCamIndex(i);
			String tempName = data.getCameraName();
			if (tempName != null && !tempName.equals("")) {
				tempArray[i] += " " + tempName;
			}
		}
		data.setCurrentIPCamIndex(currentFoscamIndex);
		adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, tempArray);

		// Specify the layout to use when the list of choices appears
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		// Apply the adapter to the spinner
		Spinner cameras_spinner = (Spinner) findViewById(R.id.cameras_spinner);
		cameras_spinner.setAdapter(adapter);
		cameras_spinner.setSelection(currentFoscamIndex);

		// Toast toast = Toast.makeText(getApplicationContext(),
		// data.getCameraName(), Toast.LENGTH_LONG);
		// toast.show();
	}

	public void okButtonClickNotificaionSettingsHandler() {
		processAlarmNotification(true);
		saveDataToInternalStorage();
	}
	
	private void processAlarmNotification(boolean executeURL) {
		IPCamData data = IPCamData.getInstance();
		IPCamCommands commands = data.getIPCamCommands();
		
		// By default alarm is disabled and email notification is also disabled
		String isMotionAlarmEnabled = "";
		String isSoundAlarmEnabled = "";
		String isEmailNotificationEnabled = "0";
		
		// TODO: Change the logic around here is accurately handle all detection types (motion and sound)
		// and all notification types (android and email).
		
		if (data.isMotionDetection()) {
			isMotionAlarmEnabled = "1";
		}
		if (data.isSoundDetection()) {
			isSoundAlarmEnabled = "1";
		}
		if (data.isEmailNotification()) {
			isEmailNotificationEnabled = "1";
		}
				
		/*
		// If email notification is enabled
		if (data.isEmailNotification()) {
			// Enable both alarm and email notification
			isAlarmEnabled = "1";
			isEmailNotificationEnabled = "1";
		}
		*/
		
		IPCamAlarmReceiver alarm = data.getIPCamAlarmReceiver();
		// If Android notification is enabled
		if (data.isAndroidNotification) {
			// Stop any pre-existing Android notification schedule
			// stopScheduledAlarmDetection();
			// Enable alarm (in case it is not enabled yet)
			//if (!isAlarmEnabled.equals("1")) {
				//isAlarmEnabled = "1";
			//}
			// runScheduledAlarmDetection();
			alarm.cancelAlarm(this); // Should I cancel the alarm service first?
			alarm.setAlarm(this);
		} else {
			alarm.cancelAlarm(this);
		}
		
		if (data.getIPCamType().equals(IPCamType.MJPEG)) {
			if (executeURL && serverURL.equals("") && (isMotionAlarmEnabled.equals("1") || isSoundAlarmEnabled.equals("1"))) {
				generateURLStrings();
				Object[] args = { serverURL, admin, password, isMotionAlarmEnabled, isSoundAlarmEnabled };
				alarmEnableURL = MessageFormat.format(commands.alarmEnableURLTemp, args);
				// Tell MJPEG camera to enable/disable notification
				executeURL(alarmEnableURL);
			} else if (executeURL && serverURL != null && !serverURL.equals("")) {
				Object[] args = { serverURL, admin, password, isMotionAlarmEnabled, data.getMotionSensitivity(), isSoundAlarmEnabled, data.getSoundSensitivity(), isEmailNotificationEnabled };
				emailNotificationURL = MessageFormat.format(commands.emailNotificationURLTemp, args);
				// Tell MJPEG camera to enable/disable notification and set sensitivity
				executeURL(emailNotificationURL);
			}
		} else if (data.getIPCamType().equals(IPCamType.H264)) {
			if (executeURL && serverURL != null && !serverURL.equals("")) {
				int motionSensitivity = data.getMotionSensitivity();
				Object[] args1 = { serverURL, admin, password, isMotionAlarmEnabled, data.getIPCamParamsLinkage(), motionSensitivity};
				alarmEnableURL = MessageFormat.format(commands.alarmEnableURLTemp, args1);
				// Tell H264 camera to enable/disable motion detection and set motion sensitivity
				executeURL(alarmEnableURL);
				
				/*
				try {
					Thread.sleep(SHORT_WAIT_PERIOD);
				} catch (InterruptedException e) {
					Log.e(TAG, MESSAGE, e);
				}
				
				// Note: This is not working properly because it will set SMTP to enabled but will also erase the server settings
				// Set SMTP email configurations
				Object[] args2 = { serverURL, admin, password, isEmailNotificationEnabled};
				emailNotificationURL = MessageFormat.format(commands.emailNotificationURLTemp, args2);
				// Tell H264 camera to enable/disable email notification
				executeURL(emailNotificationURL);
				*/
			}
		}
		
		// Toast toast = Toast.makeText(getApplicationContext(),
		// emailNotificationURL, Toast.LENGTH_LONG);
		// toast.show();
	}

	public void okButtonClickDisplaySettingsHandler() {
		IPCamData data = IPCamData.getInstance();
		IPCamCommands commands = data.getIPCamCommands();
		IPCamType camType = data.getIPCamType();
		
		if (camType == IPCamType.MJPEG) {
			if (data.getFlip() == 0) {
				Object[] args = { serverURL, admin, password, "5", "0" };
				cameraControlURL = MessageFormat.format(commands.cameraControlURLTemp, args);
				executeURL(cameraControlURL);
			} else if (data.getFlip() == 1) {
				Object[] args = { serverURL, admin, password, "5", "1" };
				cameraControlURL = MessageFormat.format(commands.cameraControlURLTemp, args);
				executeURL(cameraControlURL);
			} else if (data.getFlip() == 2) {
				Object[] args = { serverURL, admin, password, "5", "2" };
				cameraControlURL = MessageFormat.format(commands.cameraControlURLTemp, args);
				executeURL(cameraControlURL);
			} else if (data.getFlip() == 3) {
				Object[] args = { serverURL, admin, password, "5", "3" };
				cameraControlURL = MessageFormat.format(commands.cameraControlURLTemp, args);
				executeURL(cameraControlURL);
			}
		} else if (camType == IPCamType.H264) {
			if (data.getFlip() == 0) {
				Object[] args1 = { serverURL, admin, password, "0" };
				Object[] args2 = { serverURL, admin, password, "0" };
				mirrorVideoURL = MessageFormat.format(commands.mirrorVideoURLTemp, args1);
				flipVideoURL = MessageFormat.format(commands.flipVideoURLTemp, args2);
				executeURL(mirrorVideoURL);
				executeURL(flipVideoURL);
			} else if (data.getFlip() == 1) {
				Object[] args1 = { serverURL, admin, password, "0" };
				Object[] args2 = { serverURL, admin, password, "1" };
				mirrorVideoURL = MessageFormat.format(commands.mirrorVideoURLTemp, args1);
				flipVideoURL = MessageFormat.format(commands.flipVideoURLTemp, args2);
				executeURL(mirrorVideoURL);
				executeURL(flipVideoURL);
			} else if (data.getFlip() == 2) {
				Object[] args1 = { serverURL, admin, password, "1" };
				Object[] args2 = { serverURL, admin, password, "0" };
				mirrorVideoURL = MessageFormat.format(commands.mirrorVideoURLTemp, args1);
				flipVideoURL = MessageFormat.format(commands.flipVideoURLTemp, args2);
				executeURL(mirrorVideoURL);
				executeURL(flipVideoURL);
			} else if (data.getFlip() == 3) {
				Object[] args1 = { serverURL, admin, password, "1" };
				Object[] args2 = { serverURL, admin, password, "1" };
				mirrorVideoURL = MessageFormat.format(commands.mirrorVideoURLTemp, args1);
				flipVideoURL = MessageFormat.format(commands.flipVideoURLTemp, args2);
				executeURL(mirrorVideoURL);
				executeURL(flipVideoURL);
			}
		}
		
		processScreenTimeout();
		saveDataToInternalStorage();
		
		// If the resolution selection has changed OR
		// the screen mode has changed
		if (data.isLowRes != prevIsLowRes || data.getScreenMode() != prevScreenMode) {
			showBusy();
			reCreateVideoView();
		}
		prevIsLowRes = data.isLowRes;
		prevScreenMode = data.getScreenMode();

		// Toast toast = Toast.makeText(getApplicationContext(),
		// emailNotificationURL, Toast.LENGTH_LONG);
		// toast.show();
	}

	private void processScreenTimeout() {
		IPCamData data = IPCamData.getInstance();
		if (data.keepScreenOn) {
			// Keeping screen on
			this.getWindow().addFlags(
					WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		} else {
			// Remove keeping screen on flag
			this.getWindow().clearFlags(
					WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
	}

	private void saveCurrentCameraIndex() {
		IPCamData data = IPCamData.getInstance();
		String tempFoscamIndex = Integer.toString(data.getCurrentIPCamIndex());

		// Save the data to a file
		try {
			FileOutputStream fos = openFileOutput(FILENAME_CURRENT_CAM,
					Context.MODE_PRIVATE);
			fos.write(tempFoscamIndex.getBytes());
			fos.close();
		} catch (Exception e) {
			Log.e(TAG, MESSAGE, e);
		}
	}

	private int loadCurrentCameraIndex() {
		String tempStr = "";
		try {
			File file = getApplicationContext().getFileStreamPath(
					FILENAME_CURRENT_CAM);
			if (file.exists()) {
				FileInputStream fis = openFileInput(FILENAME_CURRENT_CAM);
				byte[] byteArray = new byte[256];
				fis.read(byteArray);
				fis.close();
				tempStr = new String(byteArray);
			}
		} catch (Exception e) {
			Log.e(TAG, MESSAGE, e);
		}

		if (tempStr != null && !tempStr.equals("")) {
			try {
				tempStr = tempStr.trim(); // need to do this or the string
											// cannot be parsed to int
				int tempInt = Integer.parseInt(tempStr);
				return tempInt;
			} catch (Exception ex) {
				return 0;
			}
		}
		return 0;
	}

	private void saveDataToInternalStorage() {
		IPCamData data = IPCamData.getInstance();
		int tempFoscamIndex = data.getCurrentIPCamIndex();
		
		String cameraName = data.getCameraName();
		String serverURL = data.getServerURL();
		String portNumber = data.getPortNumberOrPublic();
		String userName = data.getUserName();
		String password = data.getPassword();
		
		String camInfoFileName = FILENAME + (tempFoscamIndex + 1);
		String camFlipControlFileName = FILENAME_FLIP_CONTROL + (tempFoscamIndex + 1);
		String camTypeFileName = FILENAME_IP_CAM_TYPE + (tempFoscamIndex + 1);
		String camScreenModeFileName = FILENAME_SCREEN_MODE + (tempFoscamIndex + 1);
		
		// Save the general data to a file
		try {
			// Save state for screen timeout boolean
			FileOutputStream fos1 = openFileOutput(FILENAME_SCREEN_TIMEOUT,
					Context.MODE_PRIVATE);
			fos1.write((data.keepScreenOn) ? 1 : 0);
			fos1.close();
			
			// Save state for keep speaker on boolean
			FileOutputStream fos7 = openFileOutput(FILENAME_KEEP_SPEAKER_ON,
					Context.MODE_PRIVATE);
			fos7.write((data.keepSpeakerOn) ? 1 : 0);
			fos7.close();
			
			// Save state for Android notification
			FileOutputStream fos8 = openFileOutput(FILENAME_ANDROID_NOTIFICATION,
					Context.MODE_PRIVATE);
			fos8.write((data.isAndroidNotification) ? 1 : 0);
			fos8.close();

			// Save state for display ad boolean DISPLAY_AD
			FileOutputStream fos2 = openFileOutput(DISPLAY_AD,
					Context.MODE_PRIVATE);
			fos2.write((data.displayAd) ? 1 : 0);
			fos2.close();
			
			// Save state for isLowRes boolean
			FileOutputStream fos3 = openFileOutput(FILENAME_IS_LOW_RES,
					Context.MODE_PRIVATE);
			fos3.write((data.isLowRes) ? 1 : 0);
			fos3.close();
			
			if (cameraName != null && cameraName.length() > 0 && 
					serverURL != null && serverURL.length() > 0 &&
					portNumber != null && portNumber.length() > 0) {
			
				String persistentDataStr = cameraName + "|";
				persistentDataStr += serverURL + "|";
				persistentDataStr += portNumber + "|";
				persistentDataStr += userName + "|";
				persistentDataStr += password;
		
				// Save the specific camera data to a file
				FileOutputStream fos4 = openFileOutput(camInfoFileName, Context.MODE_PRIVATE);
				fos4.write(persistentDataStr.getBytes());
				fos4.close();
				
				FileOutputStream fos5 = openFileOutput(camFlipControlFileName, Context.MODE_PRIVATE);
				fos5.write(data.getFlipControl());
				fos5.close();
				
				// Save state for ipCamType enum
				FileOutputStream fos6 = openFileOutput(camTypeFileName, Context.MODE_PRIVATE);
				IPCamType type = data.getIPCamType();
				int typeInt = IPCamType.toInt(type);
				fos6.write(typeInt);
				fos6.close();
				
				// Save screenMode
				FileOutputStream fos9 = openFileOutput(camScreenModeFileName, Context.MODE_PRIVATE);
				int screenMode = data.getScreenMode();
				fos9.write(screenMode);
				fos9.close();
			// If there is nothing to save for the current camera index, then the data should be erased if it exists
			} else {
				List<String> fileList = Arrays.asList(fileList());
				
				if (fileList.contains(camInfoFileName)) {
					deleteFile(camInfoFileName);
				}
				if (fileList.contains(camFlipControlFileName)) {
					deleteFile(camFlipControlFileName);
				}
				if (fileList.contains(camTypeFileName)) {
					deleteFile(camTypeFileName);
				}
				if (fileList.contains(camScreenModeFileName)) {
					deleteFile(camScreenModeFileName);
				}
			}
		} catch (Exception e) {
			Log.e(TAG, MESSAGE, e);
		}
	}

	private void loadDataFromInternalStorage() {
		IPCamData data = IPCamData.getInstance();
		for (int i = 0; i < IPCamData.NUM_IPCAMS; i++) {
			String persistentDataStr = "";
			try {
				String filename = FILENAME + (i + 1);
				File file = getApplicationContext().getFileStreamPath(filename);
				if (file.exists()) {
					FileInputStream fis = openFileInput(filename);
					byte[] byteArray = new byte[256];
					fis.read(byteArray);
					fis.close();
					persistentDataStr = new String(byteArray);
				}
			} catch (Exception e) {
				Log.e(TAG, MESSAGE, e);
			}

			try {
				StringTokenizer tokenizer = new StringTokenizer(persistentDataStr, "|");
				int numTokens = tokenizer.countTokens();
	
				// Set values and states using the persistent data from a file
				if (numTokens > 0) {
					data.setCurrentIPCamIndex(i);
					if (numTokens >= 1) {
						data.setCameraName(tokenizer.nextToken());
					}
					if (numTokens >= 2) {
						String serverURL = tokenizer.nextToken();
						// Append to the front of server URL if protocol does not exist
						if (!serverURL.contains("http://") && !serverURL.contains("https://")) {
							serverURL = "http://" + serverURL;
						}
						data.setServerURL(serverURL);
					}
					if (numTokens >= 3) {
						data.setPortNumberOrPublic(tokenizer.nextToken());
					}
					// For public cameras
					if (numTokens == 4) {
						data.setUserName(tokenizer.nextToken().trim());
					}
					// For user's cameras
					if (numTokens >= 5) {
						data.setUserName(tokenizer.nextToken());
						data.setPassword(tokenizer.nextToken().trim());
					}
				}
				/*
				if (numTokens > 0 && numTokens <= 4) {
					data.setCurrentIPCamIndex(i);
					data.setCameraName(tokenizer.nextToken());
					data.setServerURL(tokenizer.nextToken());
					data.setPortNumberOrPublic(tokenizer.nextToken());
					data.setUserName(tokenizer.nextToken().trim());
			    // For user's cameras
				} else if (numTokens >= 5) {
					data.setCurrentIPCamIndex(i);
					data.setCameraName(tokenizer.nextToken());
					data.setServerURL(tokenizer.nextToken());
					data.setPortNumberOrPublic(tokenizer.nextToken());
					data.setUserName(tokenizer.nextToken());
					data.setPassword(tokenizer.nextToken().trim());
				}
				*/
			} catch (Exception e) {
				Log.e(TAG, MESSAGE, e);
			}
			
			try {
				String filename = FILENAME_FLIP_CONTROL + (i + 1);
				File file = getApplicationContext().getFileStreamPath(filename);
				if (file.exists()) {
					FileInputStream fis = openFileInput(filename);
					int tempInt = fis.read();
					fis.close();
					IPCamera[] camArray = data.getIPCamArray();
					camArray[i].flipControl = tempInt;
				}
			} catch (Exception e) {
				Log.e(TAG, MESSAGE, e);
			}
			
			try {
				// Load int for ipCamType enum
				String filename = FILENAME_IP_CAM_TYPE + (i + 1);
				File file = getApplicationContext().getFileStreamPath(filename);
				if (file.exists()) {
					FileInputStream fis = openFileInput(filename);
					int tempInt = fis.read();
					fis.close();
					IPCamera[] camArray = data.getIPCamArray();
					camArray[i].ipCamType = IPCamType.toEnum(tempInt);
				}
			} catch (Exception e) {
				Log.e(TAG, MESSAGE, e);
			}
			
			try {
				// Load int for screenMode
				String filename = FILENAME_SCREEN_MODE + (i + 1);
				File file = getApplicationContext().getFileStreamPath(filename);
				if (file.exists()) {
					FileInputStream fis = openFileInput(filename);
					int tempInt = fis.read();
					fis.close();
					IPCamera[] camArray = data.getIPCamArray();
					camArray[i].screenMode = prevScreenMode = tempInt;
				}
			} catch (Exception e) {
				Log.e(TAG, MESSAGE, e);
			}
		}

		try {
			// Load state for screen timeout boolean
			String filename = FILENAME_SCREEN_TIMEOUT;
			File file = getApplicationContext().getFileStreamPath(filename);
			if (file.exists()) {
				FileInputStream fis = openFileInput(filename);
				int tempInt = fis.read();
				fis.close();
				if (tempInt == 1) {
					data.keepScreenOn = true;
				} else {
					data.keepScreenOn = false;
				}
			}
		} catch (Exception e) {
			Log.e(TAG, MESSAGE, e);
		}
		
		try {
			// Load state for keep speaker on boolean
			String filename = FILENAME_KEEP_SPEAKER_ON;
			File file = getApplicationContext().getFileStreamPath(filename);
			if (file.exists()) {
				FileInputStream fis = openFileInput(filename);
				int tempInt = fis.read();
				fis.close();
				if (tempInt == 1) {
					data.keepSpeakerOn = true;
				} else {
					data.keepSpeakerOn = false;
				}
			}
		} catch (Exception e) {
			Log.e(TAG, MESSAGE, e);
		}
		
		try {
			// Load state for Android notification boolean
			String filename = FILENAME_ANDROID_NOTIFICATION;
			File file = getApplicationContext().getFileStreamPath(filename);
			if (file.exists()) {
				FileInputStream fis = openFileInput(filename);
				int tempInt = fis.read();
				fis.close();
				if (tempInt == 1) {
					data.isAndroidNotification = true;
				} else {
					data.isAndroidNotification = false;
				}
			}
		} catch (Exception e) {
			Log.e(TAG, MESSAGE, e);
		}

		try {
			// Load state for screen timeout boolean
			String filename = DISPLAY_AD;
			File file = getApplicationContext().getFileStreamPath(filename);
			if (file.exists()) {
				FileInputStream fis = openFileInput(filename);
				int tempInt = fis.read();
				fis.close();
				if (tempInt == 1) {
					data.displayAd = true;
				} else {
					data.displayAd = false;
				}
			}
		} catch (Exception e) {
			data.displayAd = false;
			Log.e(TAG, MESSAGE, e);
		}
		
		try {
			// Load state for isLowRes boolean
			String filename = FILENAME_IS_LOW_RES;
			File file = getApplicationContext().getFileStreamPath(filename);
			if (file.exists()) {
				FileInputStream fis = openFileInput(filename);
				int tempInt = fis.read();
				fis.close();
				if (tempInt == 1) {
					data.isLowRes = prevIsLowRes = true;
				} else {
					data.isLowRes = prevIsLowRes = false;
				}
			}
		} catch (Exception e) {
			Log.e(TAG, MESSAGE, e);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		this.menu = menu;
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		IPCamData data = IPCamData.getInstance();
		// If the user has not purchased the In-app product
		if (!data.isInAppPurchased) {
			hideOption(R.id.remove_ad);
			hideOption(R.id.display_ad);
			showOption(R.id.remove_ad_in_app);
			// Else if the user has purchased, but chose to display ad
		} else if (data.displayAd) {
			hideOption(R.id.remove_ad_in_app);
			hideOption(R.id.display_ad);
			showOption(R.id.remove_ad);
			// Else the user has purchased and chose to remove ad
		} else {
			hideOption(R.id.remove_ad_in_app);
			hideOption(R.id.remove_ad);
			showOption(R.id.display_ad);
		}
		
		if (data.getPortNumberOrPublic().equals(IPCamera.PUBLIC)) {
			hideOption(R.id.take_pict);
			hideOption(R.id.notification_settings);
			hideOption(R.id.screen_settings);
			hideOption(R.id.go_to_monitoring_system);
		} else {
			showOption(R.id.take_pict);
			showOption(R.id.notification_settings);
			showOption(R.id.screen_settings);
			showOption(R.id.go_to_monitoring_system);
		}
		
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.connection_settings:
			showConnectionSettingsDialog();
			break;
		case R.id.notification_settings:
			showNotificationSettingsDialog();
			break;
		case R.id.screen_settings:
			showScreenSettingsDialog();
			break;
		/*
		case R.id.enter_fullscreen:
			isFullscreen = true;
			toggleFullScreenMode();
			break;
		case R.id.exit_fullscreen:
			isFullscreen = false;
			toggleFullScreenMode();
			break;
		*/
		case R.id.go_to_monitoring_system:
			hideBrightnessAndContrast();
			// If the current camera has the valid URL
			if ((mv != null && mv.getMjpegInputStream() != null) || vv != null) {
				try {
					String url = new String(serverURL);
					if (!url.contains("http://") && !url.contains("https://")) {
						url = "http://" + url;
					}
					// Open the web browser and redirect to the Foscam URL
					Intent browserIntent = new Intent(Intent.ACTION_VIEW,
							Uri.parse(url.toLowerCase(Locale.ENGLISH)));
					startActivity(browserIntent);
				} catch (Exception e) {
					Log.e(TAG, MESSAGE, e);
					Toast toast = Toast.makeText(getApplicationContext(),
							R.string.your_camera_url_path_is_invalid,
							Toast.LENGTH_LONG);
					toast.show();
				}
			} else {
				// Else display an error message
				Toast toast = Toast.makeText(getApplicationContext(),
						R.string.your_camera_url_path_is_invalid,
						Toast.LENGTH_LONG);
				toast.show();
			}
			break;
		case R.id.take_pict:
			// Take a snapshot by calling snapshot.cgi
			new TakePictureAsyncTask().execute();
			break;
		case R.id.change_view:
			// Pause the main activity
			this.onPause();
			// Start multi view activity
			Intent multiViewIntent = new Intent(this, MultiViewActivity.class);
			startActivity(multiViewIntent);
			break;
		case R.id.remove_ad_in_app:
			// Invoke In-app purchase
			new purchaseInAppProdAsyncTask().execute();
			break;
		case R.id.remove_ad:
			IPCamData data = IPCamData.getInstance();
			// Set the display ad flag to false
			data.displayAd = false;
			saveDataToInternalStorage();
			// Remove the existing ad, if exists
			removeAd();
			hideOption(R.id.remove_ad);
			showOption(R.id.display_ad);
			break;
		case R.id.display_ad:
			data = IPCamData.getInstance();
			// Set the display ad flag to true
			data.displayAd = true;
			saveDataToInternalStorage();
			// Add the ad
			displayAd();
			hideOption(R.id.display_ad);
			showOption(R.id.remove_ad);
			break;
		}
		return true;
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void hideActionBar() {
		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().hide();
		}
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void showActionBar() {
		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().show();
		}
	}
	
	private void toggleFullScreenMode() {
		RelativeLayout relativeLayout1 = (RelativeLayout) findViewById(R.id.relativeLayout1);
		if (relativeLayout1 != null) {
			if (isFullscreen) {
				relativeLayout1.setVisibility(View.GONE);
				hideActionBar();
				/*
				hideOption(R.id.enter_fullscreen);
				showOption(R.id.exit_fullscreen);
				*/
			} else {
				relativeLayout1.setVisibility(View.VISIBLE);
				showActionBar();
				/*
				hideOption(R.id.exit_fullscreen);
				showOption(R.id.enter_fullscreen);
				*/
			}
		}
	}

	private void hideOption(int id) {
		if (menu != null) {
			MenuItem item = menu.findItem(id);
			item.setVisible(false);
		}
	}

	private void showOption(int id) {
		if (menu != null) {
			MenuItem item = menu.findItem(id);
			item.setVisible(true);
		}
	}

	@SuppressLint("NewApi")
	private void executeURL(String... params) {
		try {
			AsyncHttpCall asyncHttpCall = new AsyncHttpCall();
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
				asyncHttpCall.execute(params);
			} else {
				asyncHttpCall.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
			}
		} catch (Exception e) {
			Log.e(TAG, MESSAGE, e);
		}
	}
	
	/*
	@SuppressLint("NewApi")
	private void executeURLForStopMovement(String url) {
		try {
			AsyncHttpCall asyncHttpCall = new AsyncHttpCall();
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
				asyncHttpCall.execute(url, FOR_STOP_MOVEMENT);
			} else {
				asyncHttpCall.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, url, FOR_STOP_MOVEMENT);
			}
		} catch (Exception e) {
			Log.e(TAG, MESSAGE, e);
		}
	}
	*/
	
	@SuppressLint("NewApi")
	private void executeURLForMovement(String url) {
		
		uncheckHPatrolToggleButton();
		uncheckVPatrolToggleButton();
		
		try {
			// If the prev movement is the same as the new one
			if (prevMovementURL != null && prevMovementURL.equals(url)) {
				// Do nothing (to improve performance)
			} else if (movementCall == null) {
				movementCall = new AsyncHttpCall();
				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
					movementCall.execute(url, FOR_MOVEMENT);
				} else {
					movementCall.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, url, FOR_MOVEMENT);
				}
			} else {
				// If there is an already executing movement
				// store the most recent moment to be executed when the
				// executing movement is done
				//bufferedMovementURL = url;
			}
			prevMovementURL = url;
		} catch (Exception e) {
			Log.e(TAG, MESSAGE, e);
		}
	}

	private void uncheckHPatrolToggleButton() {
		ToggleButton hPatrolToggleButton = (ToggleButton) findViewById(R.id.hPatrolToggleButton);
		if (hPatrolToggleButton.isChecked()) {
			hPatrolToggleButton.setChecked(false);
		}
	}

	private void uncheckVPatrolToggleButton() {
		ToggleButton vPatrolToggleButton = (ToggleButton) findViewById(R.id.vPatrolToggleButton);
		if (vPatrolToggleButton.isChecked()) {
			vPatrolToggleButton.setChecked(false);
		}
	}

	private void checkSpeakerToggleButton() {
		ToggleButton speakerToggleButton = (ToggleButton) findViewById(R.id.speakerToggleButton);
		if (!speakerToggleButton.isChecked()) {
			speakerToggleButton.setChecked(true);
		}
	}

	private void uncheckSpeakerToggleButton() {
		ToggleButton speakerToggleButton = (ToggleButton) findViewById(R.id.speakerToggleButton);
		if (speakerToggleButton.isChecked()) {
			speakerToggleButton.setChecked(false);
		}
	}

	private void uncheckMicToggleButton() {
		ToggleButton micToggleButton = (ToggleButton) findViewById(R.id.micToggleButton);
		if (micToggleButton.isChecked()) {
			micToggleButton.setChecked(false);
		}
	}

	public void reCreateVideoView() {
		// Create foscam video view
		Handler handlerTimer = new Handler();
		handlerTimer.postDelayed(new Runnable() {
			public void run() {
				IPCamData data = IPCamData.getInstance();
				// If the current camera type is H264
				if (data.getIPCamType().equals(IPCamType.H264)) {
					createH264ViewSource();
				} else {
					// Else the current camera must be MJPEG or Public
					createMjpegViewSource();
				}
			}
		}, 300);

		isReCreatingMjpeg = false;
	}

	public void setInitialPatrolState() {
		IPCamData data = IPCamData.getInstance();
		ToggleButton hPatrolToggleButton = (ToggleButton) findViewById(R.id.hPatrolToggleButton);
		ToggleButton vPatrolToggleButton = (ToggleButton) findViewById(R.id.vPatrolToggleButton);

		if (hPatrolToggleButton != null) {
			hPatrolToggleButton.setOnCheckedChangeListener(null);
			// If hPatrol is activated
			if (data.getPatrolType() == 1)
				hPatrolToggleButton.setChecked(true);
			else
				hPatrolToggleButton.setChecked(false);
			hPatrolToggleButton
					.setOnCheckedChangeListener(new HPatrolToggleButtonCheckedChangeListener());
		}

		if (vPatrolToggleButton != null) {
			vPatrolToggleButton.setOnCheckedChangeListener(null);
			// If vPatrol is activated
			if (data.getPatrolType() == 2)
				vPatrolToggleButton.setChecked(true);
			else
				vPatrolToggleButton.setChecked(false);
			vPatrolToggleButton
					.setOnCheckedChangeListener(new VPatrolToggleButtonCheckedChangeListener());
		}
	}

	public void showBrighnessAndContrast() {
		// Create brightness/contrast view
		//LayoutInflater inflater = (LayoutInflater) getApplicationContext()
				//.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		// Fix for the crashing in Android 2.3, when the preset spinner is clicked
		// Solution from: http://stackoverflow.com/questions/3824007/selecting-android-spinner-causes-windowmanagerbadtokenexception
		LayoutInflater inflater = getLayoutInflater();
		
		brightnessContrastView = inflater.inflate(R.layout.brightness_contrast, null);

		if (brightnessContrastView != null) {
			// Add it to the frame layout
			FrameLayout fl1 = (FrameLayout) findViewById(R.id.frameLayout1);
			fl1.addView(brightnessContrastView);
			IPCamData data = IPCamData.getInstance();
			
			TextView brightnessTextView = (TextView) brightnessContrastView
					.findViewById(R.id.brightnessTextView);
			TextView contrastTextView = (TextView) brightnessContrastView
					.findViewById(R.id.contrastTextView);

			// Set the values for brightness and contrast seekbars
			SeekBar brightnessSeekBar = (SeekBar) brightnessContrastView
					.findViewById(R.id.brightnessSeekBar);
			// Max value for brightness is different for H264 and MJPEG cameras
			if (data.getIPCamType().equals(IPCamType.H264)) {
				brightnessSeekBar.setMax(100);
			} else {
				brightnessSeekBar.setMax(255);
			}

			brightnessSeekBar.setProgress(data.getBrightness());
			brightnessSeekBar
					.setOnSeekBarChangeListener(new BrightnessSeekBarChangeListener());

			SeekBar contrastSeekBar = (SeekBar) brightnessContrastView
					.findViewById(R.id.contrastSeekBar);
			// Max value for contrast is different for H264 and MJPEG cameras
			if (data.getIPCamType().equals(IPCamType.H264)) {
				contrastSeekBar.setMax(100);
			} else {
				contrastSeekBar.setMax(6);
			}
			contrastSeekBar.setProgress(data.getContrast());
			contrastSeekBar
					.setOnSeekBarChangeListener(new ContrastSeekBarChangeListener());
			
			Button irOnButton = (Button) brightnessContrastView
					.findViewById(R.id.irOnButton);
			irOnButton.setOnClickListener(new IR_OnButtonClickListener());

			Button irOffButton = (Button) brightnessContrastView
					.findViewById(R.id.irOffButton);
			irOffButton.setOnClickListener(new IR_OffButtonClickListener());
			
			Button fullscreenToggleButton = (Button) brightnessContrastView
					.findViewById(R.id.fullScreenToggleButton);
			fullscreenToggleButton.setOnClickListener(new FullScreenToggleButtonClickListener());
			
			LinearLayout presetLinearLayout = (LinearLayout) brightnessContrastView
					.findViewById(R.id.presetLinearLayout);
			Spinner preset_spinner = (Spinner) findViewById(R.id.presetSpinner);
			
			String[] tempArray = getResources().getStringArray(R.array.preset_array);
			// Create an ArrayAdapter using the string array and a default
			// spinner layout
			presetAdapter = new ArrayAdapter<String>(this,
					android.R.layout.simple_spinner_item, tempArray);
			
			// Specify the layout to use when the list of choices appears
			presetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			// Apply the adapter to the spinner
			preset_spinner.setAdapter(presetAdapter);
			// Set the selection using the stored index value
			preset_spinner.setOnItemSelectedListener(new PresetSpinnerItemSelectedListener());
			ignorePresetChange = true;
			preset_spinner.setSelection(currentPresetIndex);
			
			Button setButton = (Button) brightnessContrastView.findViewById(R.id.setButton);
			setButton.setOnClickListener(new PresetSetButtonClickListener());
			
			Button goButton = (Button) brightnessContrastView.findViewById(R.id.goButton);
			goButton.setOnClickListener(new PresetGoButtonClickListener());
			
			isDisplaySeekbars = true;
			
			// If public camera is selected
			if (data.getPortNumberOrPublic().equals(IPCamera.PUBLIC)) {
				// Hide some UI controls which are not relevant
				brightnessTextView.setVisibility(View.GONE);
				brightnessSeekBar.setVisibility(View.GONE);
				contrastTextView.setVisibility(View.GONE);
				contrastSeekBar.setVisibility(View.GONE);
				irOnButton.setVisibility(View.GONE);
				irOffButton.setVisibility(View.GONE);
				presetLinearLayout.setVisibility(View.GONE);
			// Else user IP camera is selected
			} else {
				// Show all UI controls
				brightnessTextView.setVisibility(View.VISIBLE);
				brightnessSeekBar.setVisibility(View.VISIBLE);
				contrastTextView.setVisibility(View.VISIBLE);
				contrastSeekBar.setVisibility(View.VISIBLE);
				irOnButton.setVisibility(View.VISIBLE);
				irOffButton.setVisibility(View.VISIBLE);
				presetLinearLayout.setVisibility(View.VISIBLE);
			}
			
			if (isFullscreen) {
				fullscreenToggleButton.setText(R.string.exit_fullscreen);
			} else {
				fullscreenToggleButton.setText(R.string.enter_fullscreen);
			}
		}
	}

	public void hideBrightnessAndContrast() {
		FrameLayout fl1 = (FrameLayout) findViewById(R.id.frameLayout1);
		if (brightnessContrastView != null) {
			SeekBar brightnessSeekBar = (SeekBar) brightnessContrastView
					.findViewById(R.id.brightnessSeekBar);
			brightnessSeekBar.setOnSeekBarChangeListener(null);
			SeekBar contrastSeekBar = (SeekBar) brightnessContrastView
					.findViewById(R.id.contrastSeekBar);
			contrastSeekBar.setOnSeekBarChangeListener(null);
			Button irOnButton = (Button) brightnessContrastView
					.findViewById(R.id.irOnButton);
			irOnButton.setOnClickListener(null);
			Button irOffButton = (Button) brightnessContrastView
					.findViewById(R.id.irOffButton);
			irOffButton.setOnClickListener(null);
			Button fullscreenToggleButton = (Button) brightnessContrastView
					.findViewById(R.id.fullScreenToggleButton);
			fullscreenToggleButton.setOnClickListener(null);
			fl1.removeView(brightnessContrastView);
			brightnessContrastView = null;
		}
		isDisplaySeekbars = false;
	}

	public void showBusy() {
		try {
			hideBrightnessAndContrast();
			FrameLayout fl1 = (FrameLayout) findViewById(R.id.frameLayout1);
			if (fl1 != null) {

				if (progressBar == null) {
					progressBar = new ProgressBar(MainActivity.this);
					progressBar.setIndeterminate(true);
				}

				if (progressFrameLayout == null) {
					progressFrameLayout = new FrameLayout(MainActivity.this);
					FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
							96, 96);
					params.gravity = Gravity.CENTER;
					progressFrameLayout.setLayoutParams(params);
					progressFrameLayout.addView(progressBar);
				}
				if (connectionErrorTextView != null) {
					fl1.removeView(connectionErrorTextView);
				}
				if (progressFrameLayout.getParent() == null) {
					fl1.addView(progressFrameLayout);
				}
			}
		} catch (Exception e) {
			Log.e(TAG, MESSAGE, e);
		}
	}

	private void dismissBusy() {
		try {
			FrameLayout fl1 = (FrameLayout) findViewById(R.id.frameLayout1);
			if (fl1 != null && progressFrameLayout != null) {
				fl1.removeView(progressFrameLayout);
			}
		} catch (Exception e) {
			Log.e(TAG, MESSAGE, e);
		}
	}

	private JoystickMovedListener _listener = new JoystickMovedListener() {
		@Override
		public void OnMoved(int pan, int tilt) {
			int VAL1 = IPCamData.MOVEMENT_HIGH_VAL;
			int VAL2 = IPCamData.MOVEMENT_LOW_VAL;
			
			// Filter out the values which are too low
			if (Math.abs(pan) < VAL2 && Math.abs(tilt) < VAL2) {
				//Log.d(TAG, "Filtered out the values which are too low.");
				return;
			}
			
			IPCamData data = IPCamData.getInstance();
			// 0 = initial ; 1 = vertical rotate ; 2 = horizontal rotate ; 3 =
			// vertical+horizontal rotate
			int flipVal = data.getFlipControl();

			// Move up
			if (Math.abs(pan) <= VAL2 && tilt < -VAL1) {
				if (flipVal == 0 || flipVal == 2)
					executeURLForMovement(upURL);
				else
					executeURLForMovement(downURL);
			}
			// Move up left
			else if (pan < -VAL1 && tilt < -VAL1) {
				if (flipVal == 0)
					executeURLForMovement(upLeftURL);
				else if (flipVal == 1)
					executeURLForMovement(downLeftURL);
				else if (flipVal == 2)
					executeURLForMovement(upRightURL);
				else if (flipVal == 3)
					executeURLForMovement(downRightURL);
			}
			// Move left
			else if (pan < -VAL1 && Math.abs(tilt) <= VAL2) {
				if (flipVal == 0 || flipVal == 1)
					executeURLForMovement(leftURL);
				else
					executeURLForMovement(rightURL);
			}
			// Move down left
			else if (pan < -VAL1 && tilt > VAL1) {
				if (flipVal == 0)
					executeURLForMovement(downLeftURL);
				else if (flipVal == 1)
					executeURLForMovement(upLeftURL);
				else if (flipVal == 2)
					executeURLForMovement(downRightURL);
				else if (flipVal == 3)
					executeURLForMovement(upRightURL);
			}
			// Move down
			else if (Math.abs(pan) <= VAL2 && tilt > VAL1) {
				if (flipVal == 0 || flipVal == 2)
					executeURLForMovement(downURL);
				else
					executeURLForMovement(upURL);
			}
			// Move down right
			else if (pan > VAL1 && tilt > VAL1) {
				if (flipVal == 0)
					executeURLForMovement(downRightURL);
				else if (flipVal == 1)
					executeURLForMovement(upRightURL);
				else if (flipVal == 2)
					executeURLForMovement(downLeftURL);
				else if (flipVal == 3)
					executeURLForMovement(upLeftURL);
			}
			// Move right
			else if (pan > VAL1 && Math.abs(tilt) <= VAL2) {
				if (flipVal == 0 || flipVal == 1)
					executeURLForMovement(rightURL);
				else
					executeURLForMovement(leftURL);
			}
			// Move up right
			else if (pan > VAL1 && tilt < -VAL1) {
				if (flipVal == 0)
					executeURLForMovement(upRightURL);
				else if (flipVal == 1)
					executeURLForMovement(downRightURL);
				else if (flipVal == 2)
					executeURLForMovement(upLeftURL);
				else if (flipVal == 3)
					executeURLForMovement(downLeftURL);
			}
		}

		@Override
		public void OnReleased() {
			stopMovement();
		}

		@Override
		public void OnReturnedToCenter() {
			stopMovement();
		}
		
		private void stopMovement() {
			
			// If the prev movement is the same as the new one
			if (prevMovementURL != null && prevMovementURL.equals(stopURL)) {
				// Do nothing (to improve performance)
				return;
			}
			
			// Cancel in-progress movement, if it exists
			if (movementCall != null) {
				movementCall.cancel(true);
			}
			movementCall = null;
			//bufferedMovementURL = null;
			executeURLForMovement(stopURL);
		}
	};

	private class HPatrolToggleButtonCheckedChangeListener implements
			OnCheckedChangeListener {

		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {

			if (isChecked) {
				// If foscam view does not get displayed for some reasons (e.g.
				// no connection)
				if ((mv == null || mv.getMjpegInputStream() == null) && vv == null) {
					buttonView.setChecked(false);
					return;
				}
				uncheckVPatrolToggleButton();
				try {
					Thread.sleep(SHORT_WAIT_PERIOD);
				} catch (InterruptedException e) {
					Log.e(TAG, MESSAGE, e);
				}
				executeURL(hPatrolURL);
			} else {
				executeURL(stopURL);
				executeURL(stopPatrolURL);
			}
		}
	}

	private class VPatrolToggleButtonCheckedChangeListener implements
			OnCheckedChangeListener {

		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			if (isChecked) {
				// If Foscam view does not get displayed for some reasons (e.g.
				// no connection)
				if ((mv == null || mv.getMjpegInputStream() == null) && vv == null) {
					buttonView.setChecked(false);
					return;
				}
				uncheckHPatrolToggleButton();
				try {
					Thread.sleep(SHORT_WAIT_PERIOD);
				} catch (InterruptedException e) {
					Log.e(TAG, MESSAGE, e);
				}
				executeURL(vPatrolURL);
			} else {
				executeURL(stopURL);
				executeURL(stopPatrolURL);
			}
		}
	}

	private class SpeakerToggleButtonCheckedChangeListener implements
			OnCheckedChangeListener {

		@SuppressLint("NewApi")
		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			
			// Terminate previous SendAudioAsyncTask, if it exists
			terminateSpeaker();
			
			if (isChecked) {
				IPCamData data = IPCamData.getInstance();
				if (data.getIPCamType().equals(IPCamType.H264)) {
					FSApi.startAudioStream(channelId);
				} else {
					receiveAudioAsyncTask = new ReceiveAudioAsyncTask();
					receiveAudioAsyncTask.context = getApplicationContext();
					if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
						receiveAudioAsyncTask.execute(serverURL, admin, password);
					} else {
						receiveAudioAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverURL, admin, password);
					}
				}
			}
		}
	}

	private class MicButtonTouchListener implements OnTouchListener {

		@SuppressLint({ "NewApi", "ClickableViewAccessibility" })
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				// Terminate previous SendAudioAsyncTask, if it exists
				terminateMic();
				
				IPCamData data = IPCamData.getInstance();
				// Turn on the mic
				if (data.getIPCamType().equals(IPCamType.H264)) {
					if (mTalk != null) {
						mTalk.startTalk(1);
					}
				} else {
					sendAudioAsyncTask = new SendAudioAsyncTask();
					sendAudioAsyncTask.context = getApplicationContext();
					if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
						sendAudioAsyncTask.execute(serverURL, admin, password);
					} else {
						sendAudioAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverURL, admin, password);
					}
				}

				((CompoundButton) v).setChecked(true);

				ToggleButton speakerToggleButton = (ToggleButton) findViewById(R.id.speakerToggleButton);
				if (speakerToggleButton.isChecked()) {
					// Temporarily turn off the speaker
					speakerToggleButton.setChecked(false);
					speakerPreviouslyOn = true;
				}
				Toast toast = Toast.makeText(getApplicationContext(),
						R.string.mic_on, Toast.LENGTH_SHORT);
				toast.setGravity(Gravity.CENTER_HORIZONTAL, 0, 0);
				toast.show();
				break;
			case MotionEvent.ACTION_UP:
				// Delay mic termination a little to improve usability
				Handler handlerTimer = new Handler();
				handlerTimer.postDelayed(new Runnable() {
					public void run() {
						terminateMic();
						
						if (speakerPreviouslyOn) {
							// Turn the speaker back on, if it was on before
							checkSpeakerToggleButton();
							speakerPreviouslyOn = false;
						}

						Toast toast = Toast.makeText(getApplicationContext(),
								R.string.mic_off, Toast.LENGTH_SHORT);
						toast.setGravity(Gravity.CENTER_HORIZONTAL, 0, 0);
						toast.show();
					}
				}, 600);
				((CompoundButton) v).setChecked(false);
				break;
			}
			return true;
		}

	}

	private class CameraSpinnerItemSelectedListener implements
			OnItemSelectedListener {

		@Override
		public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {

			IPCamData data = IPCamData.getInstance();
			if (!data.ignoreSetSelection) {
				uncheckSpeakerToggleButton();
				uncheckMicToggleButton();

				Spinner cameras_spinner = (Spinner) findViewById(R.id.cameras_spinner);
				int selectedIndex = cameras_spinner.getSelectedItemPosition();
				data.setCurrentIPCamIndex(selectedIndex);
				saveCurrentCameraIndex();
				currentPresetIndex = 0;
				
				// If the selected camera already contains the data
				if (data.getCameraName() != null
						&& !data.getCameraName().equals("")) {
					if (!isReCreatingMjpeg) {
						showBusy();
						isReCreatingMjpeg = true;
						generateURLStrings();
						// This will invoke reCreateMjpegView method at the end
						getParametersFromIPCamAfterItemSelection();
						getMiscFromIPCam();
						getCameraParamsFromIPCam();
						// For H264 camera only. Prepare for MultiViewActivity. Set substream format to MJPEG
						//setSubStreamFormat();
						//setSnapConfig();	// Problem: cannot set pic quality
						// process alarm notification, but don't execute URL command
						processAlarmNotification(false);
					}
				} else {
					destroyVideoViewSource();
					displayConnectionErrorMessage();
					uncheckHPatrolToggleButton();
					uncheckVPatrolToggleButton();
					// Else display Connection Settings dialog
					showConnectionSettingsDialog();
				}
			} else {
				data.ignoreSetSelection = false;
			}
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
			// TODO Auto-generated method stub
		}
	}
	
	private class PresetSpinnerItemSelectedListener implements OnItemSelectedListener {

		@Override
		public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
			if (!ignorePresetChange) {
				currentPresetIndex = arg2;
			}
			ignorePresetChange = false;
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
			// TODO Auto-generated method stub
		}
	}

	/*
	private class MjpegViewClickListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			//IPCamData data = IPCamData.getInstance();
			//if (!isDisplaySeekbars && !data.getPortNumberOrPublic().equals(IPCamera.PUBLIC)) {
			if (!isDisplaySeekbars) {
				showBrighnessAndContrast();
			} else {
				hideBrightnessAndContrast();
			}
		}
	}
	*/
	
	private class BrightnessSeekBarChangeListener implements
			OnSeekBarChangeListener {

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			// TODO Auto-generated method stub
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			// TODO Auto-generated method stub
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			IPCamData data = IPCamData.getInstance();
			IPCamCommands commands = data.getIPCamCommands();
			int progress = seekBar.getProgress();
			data.setBrightness(progress);
			
			if (data.getIPCamType().equals(IPCamType.H264)) {
				Object[] args = { serverURL, admin, password, String.valueOf(progress) };
				setBrightnessURL = MessageFormat.format(commands.setBrightnessURLTemp, args);
				executeURL(setBrightnessURL);
			} else {
				Object[] args = { serverURL, admin, password, "1", String.valueOf(progress) };
				cameraControlURL = MessageFormat.format(commands.cameraControlURLTemp, args);
				executeURL(cameraControlURL);
			}
		}
	}

	private class ContrastSeekBarChangeListener implements
			OnSeekBarChangeListener {

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			// TODO Auto-generated method stub
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			// TODO Auto-generated method stub
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			IPCamData data = IPCamData.getInstance();
			IPCamCommands commands = data.getIPCamCommands();
			int progress = seekBar.getProgress();
			data.setContrast(progress);
			
			if (data.getIPCamType().equals(IPCamType.H264)) {
				Object[] args = { serverURL, admin, password, String.valueOf(progress) };
				setContrastURL = MessageFormat.format(commands.setContrastURLTemp, args);
				executeURL(setContrastURL);
			} else {
				Object[] args = { serverURL, admin, password, "2", String.valueOf(progress) };
				cameraControlURL = MessageFormat.format(commands.cameraControlURLTemp, args);
				executeURL(cameraControlURL);
			}
		}
	}

	private class IR_OnButtonClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			IPCamData data = IPCamData.getInstance();
			if (data.getIPCamType().equals(IPCamType.H264)) {
				// IR ON should behave like IR ON
				executeURL(irAutoURL);
			} else {
				executeURL(irOnURL);
				executeURL(irOnURL2);	// For Foscam clone
			}
		}
	}

	private class IR_OffButtonClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			IPCamData data = IPCamData.getInstance();
			if (data.getIPCamType().equals(IPCamType.H264)) {
				// Set IR mode to manual first
				executeURL(irManualURL);
				try {
					Thread.sleep(SHORT_WAIT_PERIOD);
				} catch (InterruptedException e) {
					Log.e(TAG, MESSAGE, e);
				}
				// Then turn the IR off
				executeURL(irOffURL);
			} else {
				executeURL(irOffURL);
				executeURL(irOffURL2);	// For Foscam clone
			}
		}
	}
	
	private class PresetSetButtonClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			IPCamData data = IPCamData.getInstance();
			switch (currentPresetIndex) {
				case 0:
					if (data.getIPCamType().equals(IPCamType.H264)) {
						executeURL(presetDelete1URL);
						try {
							Thread.sleep(SHORT_WAIT_PERIOD);
						} catch (InterruptedException e) {
							Log.e(TAG, MESSAGE, e);
						}
					}
					executeURL(presetSet1URL);
					break;
				case 1:
					if (data.getIPCamType().equals(IPCamType.H264)) {
						executeURL(presetDelete2URL);
						try {
							Thread.sleep(SHORT_WAIT_PERIOD);
						} catch (InterruptedException e) {
							Log.e(TAG, MESSAGE, e);
						}
					}
					executeURL(presetSet2URL);
					break;
				case 2:
					if (data.getIPCamType().equals(IPCamType.H264)) {
						executeURL(presetDelete3URL);
						try {
							Thread.sleep(SHORT_WAIT_PERIOD);
						} catch (InterruptedException e) {
							Log.e(TAG, MESSAGE, e);
						}
					}
					executeURL(presetSet3URL);
					break;
				case 3:
					if (data.getIPCamType().equals(IPCamType.H264)) {
						executeURL(presetDelete4URL);
						try {
							Thread.sleep(SHORT_WAIT_PERIOD);
						} catch (InterruptedException e) {
							Log.e(TAG, MESSAGE, e);
						}
					}
					executeURL(presetSet4URL);
					break;
				case 4:
					if (data.getIPCamType().equals(IPCamType.H264)) {
						executeURL(presetDelete5URL);
						try {
							Thread.sleep(SHORT_WAIT_PERIOD);
						} catch (InterruptedException e) {
							Log.e(TAG, MESSAGE, e);
						}
					}
					executeURL(presetSet5URL);
					break;
				case 5:
					if (data.getIPCamType().equals(IPCamType.H264)) {
						executeURL(presetDelete6URL);
						try {
							Thread.sleep(SHORT_WAIT_PERIOD);
						} catch (InterruptedException e) {
							Log.e(TAG, MESSAGE, e);
						}
					}
					executeURL(presetSet6URL);
					break;
				case 6:
					if (data.getIPCamType().equals(IPCamType.H264)) {
						executeURL(presetDelete7URL);
						try {
							Thread.sleep(SHORT_WAIT_PERIOD);
						} catch (InterruptedException e) {
							Log.e(TAG, MESSAGE, e);
						}
					}
					executeURL(presetSet7URL);
					break;
				case 7:
					if (data.getIPCamType().equals(IPCamType.H264)) {
						executeURL(presetDelete8URL);
						try {
							Thread.sleep(SHORT_WAIT_PERIOD);
						} catch (InterruptedException e) {
							Log.e(TAG, MESSAGE, e);
						}
					}
					executeURL(presetSet8URL);
					break;
				default:
					break;
			}
		}
	}
	
	private class PresetGoButtonClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			switch (currentPresetIndex) {
			case 0:
				executeURL(presetGo1URL);
				break;
			case 1:
				executeURL(presetGo2URL);
				break;
			case 2:
				executeURL(presetGo3URL);
				break;
			case 3:
				executeURL(presetGo4URL);
				break;
			case 4:
				executeURL(presetGo5URL);
				break;
			case 5:
				executeURL(presetGo6URL);
				break;
			case 6:
				executeURL(presetGo7URL);
				break;
			case 7:
				executeURL(presetGo8URL);
				break;
			default:
				break;
			}
		}
	}
	
	private class FullScreenToggleButtonClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			// If we are not in fullscreen mode
			if (!isFullscreen) {
				isFullscreen = true;
				toggleFullScreenMode();
				Button fullscreenToggleButton = (Button) brightnessContrastView
						.findViewById(R.id.fullScreenToggleButton);
				// Change button label
				fullscreenToggleButton.setText(R.string.exit_fullscreen);
			// Else we are already in fullscreen mode
			} else {
				isFullscreen = false;
				toggleFullScreenMode();
				Button fullscreenToggleButton = (Button) brightnessContrastView
						.findViewById(R.id.fullScreenToggleButton);
				// Change button label
				fullscreenToggleButton.setText(R.string.enter_fullscreen);
			}
		}
	}

	private class AsyncHttpCall extends AsyncTask<String, Void, HttpResponse> {

		String specialParam = "";

		@Override
		protected HttpResponse doInBackground(String... params) {

			if (isCancelled() || params == null)
				return null;

			int count = params.length;
			IPCamData data = IPCamData.getInstance();
			try {
				if (count == 2) {
					specialParam = params[1];
				}
			} catch (Exception e) {
				Log.e(TAG, MESSAGE, e);
			}
			
			if (specialParam != null) {
				if (specialParam.equals(FOR_STOP_MOVEMENT)) {
					// If this AsyncHttpCall is for stopping the camera movement, give it the highest priority
					android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO - 1);
				} else if (specialParam.equals(FOR_MOVEMENT)) {
					// If this AsyncHttpCall is for other camera movement, give it the high priority
					android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
				} else {
					android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DEFAULT);
				}
			}
			
			HttpResponse response = null;
			if (count > 0) {
				if (data.getServerURL() != null && data.getPortNumberOrPublic() != null
						&& data.getUserName() != null
						&& !data.getServerURL().equals("")
						&& !data.getPortNumberOrPublic().equals("")
						&& !data.getUserName().equals("")) {
					try {
						DefaultHttpClient httpclient = new DefaultHttpClient();
						String url = params[0]; // Cannot pass param directly
												// below as a string
						if (!url.contains("http://") && !url.contains("https://")) {
							url = "http://" + url;
						}
						
						if (isCancelled()) {
							return null;
						}
						
						response = httpclient.execute(new HttpGet(URI
								.create(url)));
					} catch (IllegalStateException e) {
						// Ignore for now...
					} catch (Exception e) {
						// Ignore for now...
						//Log.e(TAG, MESSAGE, e);
					}
				}
			}
			return response;
		}

		@Override
		protected void onPostExecute(HttpResponse result) {
			if (isCancelled()) {
				return;
			}
			
			IPCamData data = IPCamData.getInstance();
			if (specialParam.equals(FOR_CONNECTION_SETTINGS)) {
				// Parse the parameters from the response into a hash table
				data.parseIPCamParams(result);
			} else if (specialParam
					.equals(FOR_CONNECTION_SETTINGS_AFTER_ITEM_SELECTION)) {
				// Parse the parameters from the response into a hash table
				data.parseIPCamParamsAfterItemSelection(result);
			} else if (specialParam.equals(FOR_NOTIFICATION_SETTINGS)) {
				// Parse the misc. parameters from the response into a hash
				// table
				data.parseIPCamMisc(result);
			} else if (specialParam.equals(FOR_SCREEN_SETTINGS)) {
				// Parse the camera params from the response into a hash table
				data.parseIPCamCameraParams(result);
			} else if (specialParam.equals(FOR_MIRROR_FLIP_SETTINGS)) {
				// Parse the mirror and flip values for H264 camera
				data.parseMirrorAndFlipParams(result);
			} else if (specialParam.equals(FOR_MOVEMENT)) {
				movementCall = null;
				
				/*
				// It seems we don't need this anymore
				if (bufferedMovementURL != null) {
					executeURLForMovement(bufferedMovementURL);
				}
				*/
			}
		}
	}

	private class AsyncReadMjpegInputStream extends
			AsyncTask<String, Void, MjpegInputStream> {
		@Override
		protected MjpegInputStream doInBackground(String... params) {
			
			//android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DISPLAY);
			
			MjpegInputStream response = null;
			if (params != null && params.length > 0) {
				String URL = params[0];
				if (URL != null) {
					try {
						response = MjpegInputStream.read(URL);
					} catch (Exception e) {
						Log.e(TAG, MESSAGE, e);
						dismissBusy();
						return null;
					}
				}
			}
			return response;
		}

		@Override
		protected void onPostExecute(MjpegInputStream result) {
			updateMV(result);
		}
	}

	private class TakePictureAsyncTask extends AsyncTask<Void, Void, Boolean> {

		private String fname = "";

		@Override
		protected Boolean doInBackground(Void... params) {
			return takePicture();
		}

		@Override
		protected void onPostExecute(Boolean bool) {
			String resultString;
			// Check the result before displaying the toast message
			if (bool) {
				resultString = MessageFormat.format(
						getResources().getString(
								R.string.snapshot_has_been_created), fname);
				playCameraSound();
			} else {
				resultString = getResources().getString(
						R.string.snapshot_could_not_be_taken);
			}

			Toast toast = Toast.makeText(getApplicationContext(), resultString,
					Toast.LENGTH_LONG);
			toast.show();
		}

		private Boolean takePicture() {
			File file = null;
			try {
				// Create a new file
				String root = Environment.getExternalStorageDirectory()
						.toString();
				File myDir = new File(root + "/" + SNAPSHOT_FOLDER);
				myDir.mkdirs();
				fname = SNAPSHOT_NAME + System.currentTimeMillis() + ".jpg";
				file = new File(myDir, fname);
	
				if (file.exists()) {
					file.delete();
				}
			} catch (Exception e) {
				Log.e(TAG, MESSAGE, e);
				return false;
			}
			
			try {
				Bitmap bitmap = loadImageFromUrl(snapshotURL);

				// If bitmap could not be obtain, then return the false status
				if (bitmap == null) {
					return false;
				}

				// Write the bitmap image to a file
				FileOutputStream out = new FileOutputStream(file);
				bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
				out.flush();
				out.close();

				// For Android KitKat or newer
				if (Build.VERSION.SDK_INT >= 19) {
					// Display the saved image in Gallery
					MediaScannerConnection.scanFile(getApplicationContext(),
							new String[] { file.toString() }, null,
							new MediaScannerConnection.OnScanCompletedListener() {
						 		public void onScanCompleted(String path, Uri uri) {
						 				Log.i("ExternalStorage", "Scanned " + path + ":");
						 				Log.i("ExternalStorage", "-> uri=" + uri);
						 		}
					});
				} else {
					try {
						// This should work for Pre-KitKat (4.3 and older)
						sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
								Uri.parse("file://"
										+ Environment.getExternalStorageDirectory())));
					} catch (Exception e) {
						// If it did not work, try the new way (same as KitKat)
						MediaScannerConnection.scanFile(getApplicationContext(),
								new String[] { file.toString() }, null,
								new MediaScannerConnection.OnScanCompletedListener() {
							 		public void onScanCompleted(String path, Uri uri) {
							 				Log.i("ExternalStorage", "Scanned " + path + ":");
							 				Log.i("ExternalStorage", "-> uri=" + uri);
							 		}
						});
					}
				}
				return true;
			} catch (Exception e) {
				Log.e(TAG, MESSAGE, e);
			}
			return false;
		}

		private Bitmap loadImageFromUrl(String url) {
			URL m;
			InputStream i = null;
			BufferedInputStream bis = null;
			ByteArrayOutputStream out = null;
			try {
				m = new URL(url);
				i = (InputStream) m.getContent();
				bis = new BufferedInputStream(i, 1024 * 8);
				out = new ByteArrayOutputStream();
				int len = 0;
				byte[] buffer = new byte[1024];
				while ((len = bis.read(buffer)) != -1) {
					out.write(buffer, 0, len);
				}
				out.close();
				bis.close();

				byte[] data = out.toByteArray();
				Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0,
						data.length);
				return bitmap;
			} catch (MalformedURLException e1) {
				e1.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}

		private void playCameraSound() {
			AudioManager am = (AudioManager) getBaseContext().getSystemService(
					Context.AUDIO_SERVICE);
			int volume = am.getStreamVolume(AudioManager.STREAM_NOTIFICATION);

			if (volume != 0) {
				MediaPlayer mp = MediaPlayer.create(getBaseContext(),
				// Uri.parse("file:///system/media/audio/ui/camera_click.ogg"));

						// Better to use the sound file we provide
						Uri.parse("android.resource://" + getPackageName()
								+ "/" + R.raw.camera_click));
				if (mp != null) {
					mp.start();
				}
			}

			/*
			 * // Below code does not work SoundPool soundPool = new
			 * SoundPool(1, AudioManager.STREAM_NOTIFICATION, 0); int
			 * shutterSound = soundPool.load(getBaseContext(),
			 * R.raw.camera_click, 0); soundPool.play(shutterSound, 1f, 1f, 0,
			 * 0, 1);
			 */
		}
	}

	private class IsInAppPurchasedAsyncTask extends
			AsyncTask<Void, Void, Boolean> {
		@Override
		protected Boolean doInBackground(Void... params) {
			return isInAppPurchased();
		}

		@Override
		protected void onPostExecute(Boolean bool) {
			IPCamData data = IPCamData.getInstance();
			// Display ad if the In-app product has not been bought
			data.isInAppPurchased = bool;
			if (!data.isInAppPurchased) {
				data.displayAd = true;
				displayAd();
				// Or display it, if the user has selected display ad before
			} else if (data.displayAd) {
				displayAd();
			} else {
			}
		}

		private boolean isInAppPurchased() {
			ArrayList<String> skuList = new ArrayList<String>();

			skuList.add("remove_ad_ip_cam_remote");
			// skuList.add("android.test.purchased");
			// skuList.add("android.test.canceled");

			Bundle querySkus = new Bundle();
			querySkus.putStringArrayList("ITEM_ID_LIST", skuList);

			try {
				String packageName = getPackageName();

				// Query for details about the In-app purchase
				Bundle skuDetails = null;
				while (skuDetails == null) {
					try {
						skuDetails = mService.getSkuDetails(3, packageName,
								"inapp", querySkus);
					} catch (NullPointerException e) {
					}
				}

				int response = skuDetails.getInt("RESPONSE_CODE");
				if (response == 0) {
					ArrayList<String> responseList = skuDetails
							.getStringArrayList("DETAILS_LIST");

					if (responseList != null && responseList.size() > 0) {
						String tempStr = responseList.get(0);

						// {"title":"Remove Ad from IP Cam Remote (IP Cam Remote with Audio)","price":"$2.99","type":"inapp",
						// "description":"This product removes all advertising which appears at the bottom of the screen.",
						// "productId":"remove_ad_ip_cam_remote"}

						JSONObject object = new JSONObject(tempStr);

						// String title = object.getString("title");
						// String price = object.getString("price");
						String type = object.getString("type");
						// String description = object.getString("description");
						String productId = object.getString("productId");

						// Get the buy intend bundle
						Bundle buyIntentBundle = mService.getBuyIntent(3,
								getPackageName(), productId, type, LICENSE_KEY);

						int response2 = buyIntentBundle.getInt("RESPONSE_CODE");

						// If this In-app product has already been bought
						if (response2 == BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED) {
							return true;
							// Else Proceed with the purchase
						} else {
							return false;
						}
					}
				}
			} catch (RemoteException e) {
				Log.e(TAG, MESSAGE, e);
				// When the in-app verification failed for any reasons, do not display ad to give user a favor.
				return true;
			} catch (Exception e) {
				Log.e(TAG, MESSAGE, e);
				// When the in-app verification failed for any reasons, do not display ad to give user a favor.
				return true;
			}
			// When the in-app verification failed for any reasons, do not display ad to give user a favor.
			return true;
		}
	}

	private class purchaseInAppProdAsyncTask extends
			AsyncTask<Void, Void, Boolean> {
		@Override
		protected Boolean doInBackground(Void... params) {
			return purchaseInAppProduct();
		}

		@Override
		protected void onPostExecute(Boolean result) {
		}

		private boolean purchaseInAppProduct() {
			ArrayList<String> skuList = new ArrayList<String>();
			skuList.add("remove_ad_ip_cam_remote");
			Bundle querySkus = new Bundle();
			querySkus.putStringArrayList("ITEM_ID_LIST", skuList);

			try {
				String packageName = getPackageName();

				// Query for details about the In-app purchase
				Bundle skuDetails = null;
				while (skuDetails == null) {
					try {
						skuDetails = mService.getSkuDetails(3, packageName,
								"inapp", querySkus);
					} catch (NullPointerException e) {
					}
				}

				int response = skuDetails.getInt("RESPONSE_CODE");
				if (response == 0) {
					ArrayList<String> responseList = skuDetails
							.getStringArrayList("DETAILS_LIST");

					if (responseList != null && responseList.size() > 0) {
						String tempStr = responseList.get(0);

						JSONObject object = new JSONObject(tempStr);
						String type = object.getString("type");
						String productId = object.getString("productId");

						// Get the buy intend bundle
						Bundle buyIntentBundle = mService.getBuyIntent(3,
								getPackageName(), productId, type, LICENSE_KEY);

						int response2 = buyIntentBundle.getInt("RESPONSE_CODE");
						// Proceed with the purchase
						if (response2 == BILLING_RESPONSE_RESULT_OK) {
							// Making a purchase
							PendingIntent pendingIntent = buyIntentBundle
									.getParcelable("BUY_INTENT");
							startIntentSenderForResult(
									pendingIntent.getIntentSender(),
									REQUEST_CODE, new Intent(),
									Integer.valueOf(0), Integer.valueOf(0),
									Integer.valueOf(0));
							return true;
						}
					}
				}
			} catch (RemoteException e) {
				Log.e(TAG, MESSAGE, e);
				return false;
			} catch (Exception e) {
				Log.e(TAG, MESSAGE, e);
				return false;
			}
			return false;
		}
	}
	
	private class DoubleTapGestureListener extends GestureDetector.SimpleOnGestureListener {
		@Override
		public void onLongPress(MotionEvent e) {
			// When a long press happens, reset the scale factor of the VideoView
			if (vv != null) {
				vv.resetScaleFactor();
			}
			
			// When a long press happens, reset the scale factor of the MjegView
			if (mv != null) {
				mv.resetScaleFactor();
			}
	    }
		
		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			if (!isDisplaySeekbars) {
				showBrighnessAndContrast();
			} else {
				hideBrightnessAndContrast();
			}
            return true;
		}
	}
}