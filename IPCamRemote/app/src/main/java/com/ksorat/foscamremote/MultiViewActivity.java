package com.ksorat.foscamremote;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.MessageFormat;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.ipc.sdk.FSApi;
import com.ksorat.ipcamremote.httpCommand.IPCamType;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.Toast;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabSpec;

public class MultiViewActivity extends FragmentActivity {

	private static final String TAG = "IPCamRemote:MultiViewActivity";
	private static final String MESSAGE = "Received an exception";
	private static final String PAGE_1_ID = "page1Spec";
	private static final String PAGE_2_ID = "page2Spec";
	private static final String PAGE_3_ID = "page3Spec";
	private static final int NUM_CAM_PER_PAGE = 6;
	public static final String MY_AD_UNIT_ID = "a150d6921128c0a";
	public static final String TEST_DEVICE_ID = "658307f6";

	private View[] mvArray;
	private MjpegInputStream[] mjpegInputStreamResultArray;
	private FrameLayout[] frameLayoutArray;
	private boolean[] hasMVArray;
	private boolean isTabHostSetup;
	private boolean isFromOnCreate;
	public boolean isFromOnPause;
	
	private int channelId;	// For H264 camera
	
	private AdView adView;

	private final String mjpegVideoStreamURLTemp = "{0}/videostream.cgi?user={1}&pwd={2}&resolution=8";
	//private final String h264VideoStreamURLTemp = "{0}/cgi-bin/CGIStream.cgi?cmd=GetMJStream&usr={1}&pwd={2}";
	//private boolean isFromChangeView = false;

	private Fragment fragment1;
	private Fragment fragment2;
	private Fragment fragment3;
	private int currentTabIndex = 0;
	private boolean suppressTabChangeHandler = false;
	private boolean isAlreadyRemoveBusy = false;
	private int referenceCounting = 0;
	private boolean skipVideoViewClickListener = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		setContentView(R.layout.activity_multi_view);
		isTabHostSetup = false;
		
		IPCamData data = IPCamData.getInstance();
		int currentFoscamIndex = data.getCurrentIPCamIndex();
		
		if (currentFoscamIndex >= 0 && currentFoscamIndex <= 5) {
			currentTabIndex = 0;
		} else if (currentFoscamIndex >= 6 && currentFoscamIndex <= 11) {
			currentTabIndex = 1;
		} else if (currentFoscamIndex >= 12 && currentFoscamIndex <= 17) {
			currentTabIndex = 2;
		}
		
		isFromOnCreate = true;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		destroyAd();
		destroyVideoViewSource();
		destroyTabs();
		// Help refresh the screen much faster
		System.gc();
		// Stop multi view activity
		this.finish();

		// If not coming from clicking Change View button
		// if (!isFromChangeView) {
		// System.exit(0);
		// }
	}

	@Override
	public void onResume() {
		super.onResume();
		
		isFromOnPause = false;
		
		if (mvArray == null)
			mvArray = new View[NUM_CAM_PER_PAGE];

		if (mjpegInputStreamResultArray == null)
			mjpegInputStreamResultArray = new MjpegInputStream[NUM_CAM_PER_PAGE];
		
		if (frameLayoutArray == null)
			frameLayoutArray = new FrameLayout[NUM_CAM_PER_PAGE];
		
		processScreenTimeout();
		
		// If coming from onCreate
		if (isFromOnCreate) {
			// Execute this block then immediately return
			createTabs();
			createAd();
			isFromOnCreate = false;
			return;
		}
		
		setContentView(R.layout.activity_multi_view);
		isTabHostSetup = false;
		createTabs();
		createAd();
	}

	@Override
	public void onPause() {
		super.onPause();
		
		isFromOnPause = true;
		
		destroyAd();
		destroyVideoViewSource();
		destroyTabs();
		
		// Help refresh the screen much faster
		System.gc();
	}

	// This will get called when the screen orientation has changed
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		setContentView(R.layout.activity_multi_view);
		isTabHostSetup = false;
		createTabs();
		createAd();
	}
	
	/*
	public void restoreMVs() {
		if (mvArray == null)
			mvArray = new View[NUM_CAM_PER_PAGE];
		for (int i = 0; i < mvArray.length; i++) {
			if (mvArray[i] != null) {
				((MjpegView) mvArray[i]).stopPlayback();
				((MjpegView) mvArray[i]).destroyDrawingCache();
				((MjpegView) mvArray[i]).destroyMjpegInputStream();
				mvArray[i].setOnClickListener(null);
				mvArray[i] = null;
			}
			// Restore mjpeg view
			updateMVFromExistingResult(i);
		}
	}
	
	public void restoreMV(int camIndex) {
		int index = getIndexFromCamIndex(camIndex);
		
		if (mvArray == null)
			mvArray = new View[NUM_CAM_PER_PAGE];
		MjpegView mv = (MjpegView) mvArray[index];
		if (mvArray[index] != null) {
			((MjpegView) mvArray[index]).stopPlayback();
			((MjpegView) mvArray[index]).destroyDrawingCache();
			((MjpegView) mvArray[index]).destroyMjpegInputStream();
			mvArray[index].setOnClickListener(null);
			mvArray[index] = null;
		}
		// Restore mjpeg view
		updateMVFromExistingResult(camIndex);
	}
	*/
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		invokeFragmentManagerNoteStateNotSaved();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void invokeFragmentManagerNoteStateNotSaved() {
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
	
	public void createTabs() {
		try {
			fragment1 = new MultiViewFragmentPage1();
			fragment2 = new MultiViewFragmentPage2();
			fragment3 = new MultiViewFragmentPage3();
	
			TabHost tabHost = (TabHost) findViewById(android.R.id.tabhost);
			if (!isTabHostSetup) {
				tabHost.setup();
				isTabHostSetup = true;
			}
			// Temporarily disable the tab until the mjpeg views are done displaying
			tabHost.getTabWidget().setVisibility(View.INVISIBLE);
			tabHost.setOnTabChangedListener(new MultiViewTabChangedListener());
			
			TabSpec page1Spec = tabHost.newTabSpec(PAGE_1_ID);
			page1Spec.setIndicator("1 - 6");
			page1Spec.setContent(new TabHost.TabContentFactory() {
				public View createTabContent(String tag) {
					return findViewById(R.id.realtabcontent);
				}
			});
	
			TabSpec page2Spec = tabHost.newTabSpec(PAGE_2_ID);
			page2Spec.setIndicator("7 - 12");
			page2Spec.setContent(new TabHost.TabContentFactory() {
				public View createTabContent(String tag) {
					return findViewById(R.id.realtabcontent);
				}
			});
	
			TabSpec page3Spec = tabHost.newTabSpec(PAGE_3_ID);
			page3Spec.setIndicator("13 - 18");
			page3Spec.setContent(new TabHost.TabContentFactory() {
				public View createTabContent(String tag) {
					return findViewById(R.id.realtabcontent);
				}
			});
	
			suppressTabChangeHandler = true;
			// Adding all TabSpec to TabHost
			tabHost.addTab(page1Spec);
			tabHost.addTab(page2Spec);
			tabHost.addTab(page3Spec);
			tabHost.setCurrentTab(currentTabIndex);
			
			destroyVideoViewSource();
			if (currentTabIndex == 0) {
				pushFragments(fragment1);
				createVideoViewsInPage(PAGE_1_ID);
			} else if (currentTabIndex == 1) {
				pushFragments(fragment2);
				createVideoViewsInPage(PAGE_2_ID);
			} else if (currentTabIndex == 2) {
				pushFragments(fragment3);
				createVideoViewsInPage(PAGE_3_ID);
			}
			suppressTabChangeHandler = false;
		} catch (Exception e) {
			Log.e(TAG, MESSAGE, e);
		}
	}
	
	public void destroyTabs() {
		TabHost tabHost = (TabHost) findViewById(android.R.id.tabhost);
		tabHost.setOnTabChangedListener(null);
		tabHost.destroyDrawingCache();
		tabHost.clearAllTabs();
	}
	
	public void createAd() {
		IPCamData data = IPCamData.getInstance();
		// Display ad, if the user has not bought the In-app purchase to remove the ad.
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
				
				// Initiate a generic request to load it with an ad
				adView.loadAd(adRequest);
			} catch (NullPointerException e) {
				Log.e(TAG, MESSAGE, e);
			} catch (Exception e) {
				Log.e(TAG, MESSAGE, e);
			}
		}
	}
	
	private void destroyAd() {
		if (adView != null) {
			LinearLayout layout = (LinearLayout) findViewById(R.id.linearLayout1);
			layout.removeAllViews();
			adView.removeAllViews();
			adView.destroy();
		}
	}
	
	private void pushFragments(Fragment fragment) {
		try {
			FragmentManager manager = getSupportFragmentManager();
			android.support.v4.app.FragmentTransaction ft = manager
					.beginTransaction();
			ft.replace(R.id.realtabcontent, fragment);
			ft.commit();
		} catch (Exception e) {
			Log.e(TAG, MESSAGE, e);
		}
	}
	
	@SuppressLint("NewApi")
	public void createMjpegViewSource(String videoStreamURL, int camIndex) {
		try {
			String URL = videoStreamURL;
			// Must do the following asynchronously in Android 4.1
			AsyncReadMjpegInputStream asyncReadMjpegInputStream = new AsyncReadMjpegInputStream();
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
				asyncReadMjpegInputStream.execute(URL, String.valueOf(camIndex));
			} else {
				asyncReadMjpegInputStream.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, URL, String.valueOf(camIndex));
			}
			
		} catch (IllegalStateException ex) {
			Log.e(TAG, MESSAGE, ex);
		} catch (Exception e) {
			Log.e(TAG, MESSAGE, e);
		}
	}

	private void destroyVideoViewSource() {	
		Handler handlerTimer = new Handler();
		handlerTimer.postDelayed(new Runnable(){
			public void run() {
				if (mvArray != null) {
					for (int i = 0; i < mvArray.length; i++) {
						if (mvArray[i] instanceof VideoView) {
							VideoView vv = (VideoView) mvArray[i];
							if (vv != null) {
								vv.stopVideoStream();
								android.os.SystemClock.sleep(500);
								vv.clearScreen();
								vv.stop();
								vv = null;
							}
						} else {
							MjpegView mv = (MjpegView) mvArray[i];
							if (mv != null) {
								mv.stopPlayback();
								mv.destroyDrawingCache();
								mv.destroyMjpegInputStream();
								mv.setOnClickListener(null);
								mv = null;
							}
						}
					}
				}
				mvArray = null;
				mjpegInputStreamResultArray = null;
				frameLayoutArray = null;
			}}, 300);
		
		removeMjpegViews();
	}

	private void removeMjpegViews() {
		if (frameLayoutArray != null) {
			for (int i = 0; i < NUM_CAM_PER_PAGE; i++) {
				if (frameLayoutArray[i] != null) {
					frameLayoutArray[i].removeAllViewsInLayout();
				}
			}
		}
	}

	public String generateMJPEGVideoStreamURLString(Object[] args) {
		return MessageFormat.format(mjpegVideoStreamURLTemp, args);
	}
	
	/*
	// Not being used now
	public String generateH264VideoStreamURLString(Object[] args) {
		return MessageFormat.format(h264VideoStreamURLTemp, args);
	}
	*/

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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.multi_view, menu);
		return true;
	}

	/*
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.change_view:
			destroyAd();
			destroyMjegViewSource();
			destroyTabs();
			// Help refresh the screen much faster
			System.gc();
			// Stop multi view activity
			this.finish();

			// isFromChangeView = true;
			break;
		}
		return true;
	}
	*/
	
	private void updateMV(MjpegInputStream result, int camIndex) {
		try {
			int index = getIndexFromCamIndex(camIndex);
			if (mjpegInputStreamResultArray == null)
				mjpegInputStreamResultArray = new MjpegInputStream[NUM_CAM_PER_PAGE];
			// Cache the result for orientation change
			mjpegInputStreamResultArray[index] = result;
			updateMVFromExistingResult(camIndex);
		} catch (Exception e) {
			Log.e(TAG, MESSAGE, e);
		}
	}
	
	private void removeUnwantedProgressBars() {
		for (int x=0 ; x<hasMVArray.length ; x++) {
			if (!hasMVArray[x]) {
				clearFrame(x);
			}
		}
		Handler handlerTimer = new Handler();
		handlerTimer.postDelayed(new Runnable(){
			public void run() {
				// Enable the tab after the mjpeg views are done displaying
				TabHost tabHost = (TabHost) findViewById(android.R.id.tabhost);
				tabHost.getTabWidget().setVisibility(View.VISIBLE);
			}}, 300);
	}

	public void updateMVFromExistingResult(int camIndex) {
		try {
			int index = getIndexFromCamIndex(camIndex);
			if (mvArray == null)
				mvArray = new View[NUM_CAM_PER_PAGE];
			
			mvArray[index] = new MjpegView(this, camIndex);
			if (mjpegInputStreamResultArray == null)
				mjpegInputStreamResultArray = new MjpegInputStream[NUM_CAM_PER_PAGE];
			((MjpegView) mvArray[index]).setSource(mjpegInputStreamResultArray[index]);
			((MjpegView) mvArray[index]).setDisplayMode(MjpegView.SIZE_BEST_FIT);
			mvArray[index].setOnClickListener(new VideoViewClickListener());
			// mv.showFps(true);
			// mv.startPlayback();	// setSource will start the playback
			
			addVideoViewToFrameLayout(mvArray[index], camIndex);
			
		} catch (IllegalStateException ex) {
			Log.e(TAG, MESSAGE, ex);
		} catch (Exception e) {
			Log.e(TAG, MESSAGE, e);
		}
	}
	
	private void clearFrame(int camIndex) {
		try {
			if (frameLayoutArray == null)
				frameLayoutArray = new FrameLayout[NUM_CAM_PER_PAGE];
			
			if (currentTabIndex == 0) {
				// Do nothing
			} else if (currentTabIndex == 1) {
				camIndex += 6;
			} else if (currentTabIndex == 2) {
				camIndex += 12;
			}
			
			switch (camIndex) {
				case 0:
					frameLayoutArray[0] = (FrameLayout) findViewById(R.id.frameLayout11);
					if (frameLayoutArray[0] != null) {
						frameLayoutArray[0].removeAllViews();
					}
					break;
				case 1:
					frameLayoutArray[1] = (FrameLayout) findViewById(R.id.frameLayout12);
					if (frameLayoutArray[1] != null) {
						frameLayoutArray[1].removeAllViews();
					}
					break;
				case 2:
					frameLayoutArray[2] = (FrameLayout) findViewById(R.id.frameLayout13);
					if (frameLayoutArray[2] != null) {
						frameLayoutArray[2].removeAllViews();
					}
					break;
				case 3:
					frameLayoutArray[3] = (FrameLayout) findViewById(R.id.frameLayout14);
					if (frameLayoutArray[3] != null) {
						frameLayoutArray[3].removeAllViews();
					}
					break;
				case 4:
					frameLayoutArray[4] = (FrameLayout) findViewById(R.id.frameLayout15);
					if (frameLayoutArray[4] != null) {
						frameLayoutArray[4].removeAllViews();
					}
					break;
				case 5:
					frameLayoutArray[5] = (FrameLayout) findViewById(R.id.frameLayout16);
					if (frameLayoutArray[5] != null) {
						frameLayoutArray[5].removeAllViews();
					}
					break;
				case 6:
					frameLayoutArray[0] = (FrameLayout) findViewById(R.id.frameLayout17);
					if (frameLayoutArray[0] != null) {
						frameLayoutArray[0].removeAllViews();
					}
					break;
				case 7:
					frameLayoutArray[1] = (FrameLayout) findViewById(R.id.frameLayout18);
					if (frameLayoutArray[1] != null) {
						frameLayoutArray[1].removeAllViews();
					}
					break;
				case 8:
					frameLayoutArray[2] = (FrameLayout) findViewById(R.id.frameLayout19);
					if (frameLayoutArray[2] != null) {
						frameLayoutArray[2].removeAllViews();
					}
					break;
				case 9:
					frameLayoutArray[3] = (FrameLayout) findViewById(R.id.frameLayout20);
					if (frameLayoutArray[3] != null) {
						frameLayoutArray[3].removeAllViews();
					}
					break;
				case 10:
					frameLayoutArray[4] = (FrameLayout) findViewById(R.id.frameLayout21);
					if (frameLayoutArray[4] != null) {
						frameLayoutArray[4].removeAllViews();
					}
					break;
				case 11:
					frameLayoutArray[5] = (FrameLayout) findViewById(R.id.frameLayout22);
					if (frameLayoutArray[5] != null) {
						frameLayoutArray[5].removeAllViews();
					}
					break;
				case 12:
					frameLayoutArray[0] = (FrameLayout) findViewById(R.id.frameLayout23);
					if (frameLayoutArray[0] != null) {
						frameLayoutArray[0].removeAllViews();
					}
					break;
				case 13:
					frameLayoutArray[1] = (FrameLayout) findViewById(R.id.frameLayout24);
					if (frameLayoutArray[1] != null) {
						frameLayoutArray[1].removeAllViews();
					}
					break;
				case 14:
					frameLayoutArray[2] = (FrameLayout) findViewById(R.id.frameLayout25);
					if (frameLayoutArray[2] != null) {
						frameLayoutArray[2].removeAllViews();
					}
					break;
				case 15:
					frameLayoutArray[3] = (FrameLayout) findViewById(R.id.frameLayout26);
					if (frameLayoutArray[3] != null) {
						frameLayoutArray[3].removeAllViews();
					}
					break;
				case 16:
					frameLayoutArray[4] = (FrameLayout) findViewById(R.id.frameLayout27);
					if (frameLayoutArray[4] != null) {
						frameLayoutArray[4].removeAllViews();
					}
					break;
				case 17:
					frameLayoutArray[5] = (FrameLayout) findViewById(R.id.frameLayout28);
					if (frameLayoutArray[5] != null) {
						frameLayoutArray[5].removeAllViews();
					}
					break;
			}
		} catch (IllegalStateException ex) {
			Log.e(TAG, MESSAGE, ex);
		} catch (Exception e) {
			Log.e(TAG, MESSAGE, e);
		}
	}
	
	private void createVideoViewsInPage(String tabId) {
		// Create mjpeg and h264 view for all cameras, if they exist
		IPCamData data = IPCamData.getInstance();
		int currentFoscamIndex = data.getCurrentIPCamIndex();
		isAlreadyRemoveBusy = false;
		skipVideoViewClickListener = true;
		channelId = 0;
		
		hasMVArray = new boolean[NUM_CAM_PER_PAGE];
		
		for (int i = 0; i < NUM_CAM_PER_PAGE; i++) {
			int index = i;
			if (tabId == PAGE_1_ID) {
				// Do nothing
			} else if (tabId == PAGE_2_ID) {
				index += 6;
			} else if (tabId == PAGE_3_ID) {
				index += 12;
			}
			
			data.setCurrentIPCamIndex(index);

			// Only create video view for the cameras slot that have been
			// configured
			String serverURL = data.getServerURL();
			if (serverURL != null && !serverURL.equals("")) {
				// Only alter serverURL for non-public cameras
				if (!data.getPortNumberOrPublic().equals(IPCamera.PUBLIC)) {
					serverURL += ":" + data.getPortNumberOrPublic();
				}
				String admin = data.getUserName();
				String password = data.getPassword();
				Object[] args = { serverURL, admin, password };

				hasMVArray[i] = true;
				final int finalIndex = index;
				
				if (data.getIPCamType().equals(IPCamType.H264)) {
					final IPCamData finalData = IPCamData.getInstance();
					// Need to assign values here, otherwise the actual values from data object will be unexpected due to multi-threading
					final String ipAddr = finalData.getServerURLNoHttp();
					final String userName = finalData.getUserName();
					final String userPassword = finalData.getPassword();
					int portNumber = 80;
					String portNumberStr = finalData.getPortNumberOrPublic();
					try {
						portNumber = Integer.parseInt(portNumberStr);
					} catch (Exception e) {
						Log.e(TAG, MESSAGE, e);
					}
					final int finalPortNumber = portNumber;
					
					final int screenMode = finalData.getScreenMode();
					Handler handlerTimer = new Handler();
					handlerTimer.postDelayed(new Runnable(){
						public void run() {
							createH264ViewSource(ipAddr, userName, userPassword, finalPortNumber, finalIndex, screenMode);
						}}, 300);
				} else {
					final String videoStreamURL; 
					// Only call generateVideoStreamURLString for non-public cameras
					if (data.getPortNumberOrPublic().equals(IPCamera.PUBLIC)) {
						videoStreamURL = serverURL;
					} else {
						videoStreamURL = generateMJPEGVideoStreamURLString(args);
					}
					Handler handlerTimer = new Handler();
					handlerTimer.postDelayed(new Runnable(){
						public void run() {
							createMjpegViewSource(videoStreamURL, finalIndex);
						}}, 300);
				}
			} else {
				hasMVArray[i] = false;
			}
		}
		// Need to do this after all Foscams have been displayed
		data.setCurrentIPCamIndex(currentFoscamIndex);
		
		Handler removeUnwantedProgressBarsHandler = new Handler();
		// Remove all unneeded busy icons after a few seconds
		// (Cannot remove right away because the UI components under tab are not ready yet)
		removeUnwantedProgressBarsHandler.postDelayed(RemoveUnwantedProgressBarsRunnable, 3000);
	}
	
	private void createH264ViewSource(String ipAddr, String userName, String password, int portNumber, int camIndex, int screenMode) {
		int devType = 1; // 0 = MJPEG, 1 = H264
		int streamType = 0; // 0 = sub stream, 1 = main stream
		String uid = "";
		
		if (channelId > 3) {
			String message = "Maximum (4) channels for H.264 cameras have been reached!";
			Log.i(TAG, message);
			Toast toast = Toast.makeText(getApplicationContext(), message,
					Toast.LENGTH_LONG);
			toast.show();
			return;
		}
		
		try {	
			FSApi.usrLogOut(channelId);
			FSApi.usrLogIn(devType, ipAddr, userName, password, streamType, portNumber, portNumber, uid, channelId);
			
			if (mvArray == null)
				mvArray = new View[NUM_CAM_PER_PAGE];
			
			DisplayMetrics metric = new DisplayMetrics();
	        getWindowManager().getDefaultDisplay().getMetrics(metric);
	        int index = getIndexFromCamIndex(camIndex);
	        mvArray[index] = new VideoView(this, metric.widthPixels, metric.heightPixels, channelId, screenMode);
	        channelId++;
	        mvArray[index].setClickable(true);	// Somehow this is needed, unlike MjpegView
	        mvArray[index].setOnClickListener(new VideoViewClickListener());
			
	        ((VideoView) mvArray[index]).start();
	        ((VideoView) mvArray[index]).startVideoStream();
			
			addVideoViewToFrameLayout(mvArray[index], camIndex);
			
			if (!isAlreadyRemoveBusy && referenceCounting<=0) {
				removeUnwantedProgressBars();
				isAlreadyRemoveBusy = true;
				skipVideoViewClickListener = false;
			}
			
		} catch (Exception e) {
			Log.e(TAG, MESSAGE, e);
		}
	}
	
	private Runnable RemoveUnwantedProgressBarsRunnable = new Runnable() {
		@Override
	    public void run() {
			// If all six frames still contain busy widgets
			if (!containMjpegViews())
				removeUnwantedProgressBars();
	    }
	};
	
	private boolean containMjpegViews() {
		for (int x=0 ; x<hasMVArray.length ; x++) {
			if (hasMVArray[x]) {
				return true;
			}
		}
		return false;
	}

	private class AsyncReadMjpegInputStream extends
			AsyncTask<String, Void, MjpegInputStream> {

		private int camIndex = -1;

		@Override
		protected MjpegInputStream doInBackground(String... params) {
			MjpegInputStream response = null;
			if (params != null && params.length > 0) {
				String URL = params[0];
				camIndex = Integer.parseInt(params[1]);
				if (URL != null) {
					try {
						referenceCounting++;
						response = MjpegInputStream.read(URL);
					} catch (Exception e) {
						Log.e(TAG, MESSAGE, e);
						return null;
					}
				}
			}
			return response;
		}

		@Override
		protected void onPostExecute(MjpegInputStream result) {
			updateMV(result, camIndex);
			referenceCounting--;
			if (!isAlreadyRemoveBusy && referenceCounting<=0) {
				removeUnwantedProgressBars();
				isAlreadyRemoveBusy = true;
				skipVideoViewClickListener = false;
			}
		}
	}

	public static class MultiViewFragmentPage1 extends Fragment {

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {

			View rootView = inflater.inflate(R.layout.multi_view_page1,
					container, false);
			/*
			 * Bundle args = getArguments(); ((TextView)
			 * rootView.findViewById(android.R.id.text1)).setText(
			 * Integer.toString(args.getInt(ARG_OBJECT)));
			 */

			return rootView;
		}
	}

	public static class MultiViewFragmentPage2 extends Fragment {

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {

			View rootView = inflater.inflate(R.layout.multi_view_page2,
					container, false);
			return rootView;
		}
	}

	public static class MultiViewFragmentPage3 extends Fragment {

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {

			View rootView = inflater.inflate(R.layout.multi_view_page3,
					container, false);
			return rootView;
		}
	}

	private class MultiViewTabChangedListener implements OnTabChangeListener {

		@Override
		public void onTabChanged(String tabId) {
			
			if (!suppressTabChangeHandler) {
				// Temporarily disable the tab until the mjpeg views are done displaying
				TabHost tabHost = (TabHost) findViewById(android.R.id.tabhost);
				tabHost.getTabWidget().setVisibility(View.INVISIBLE);
				
				destroyVideoViewSource();
				
				if (tabId.equals(PAGE_1_ID)) {
					pushFragments(fragment1);
					currentTabIndex = 0;
				} else if (tabId.equals(PAGE_2_ID)) {
					pushFragments(fragment2);
					currentTabIndex = 1;
				} else if (tabId.equals(PAGE_3_ID)) {
					pushFragments(fragment3);
					currentTabIndex = 2;
				}
				
				createVideoViewsInPage(tabId);
			}
		}
	}
	
	private class VideoViewClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			
			// Skip the click handler, if the mjpeg views are not done displaying
			if (skipVideoViewClickListener) {
				return;
			}
			
			FrameLayout currentFrameLayout = (FrameLayout) v.getParent();
			
			// Fix NPE defect
			if (currentFrameLayout == null) {
				return;
			}
			
			int camIndex = 0;
			if (currentTabIndex == 0) {
				// Do nothing
			} else if (currentTabIndex == 1) {
				camIndex += 6;
			} else if (currentTabIndex == 2) {
				camIndex += 12;
			}
			
			IPCamData data = IPCamData.getInstance();
			int currentFoscamIndex = data.getCurrentIPCamIndex();
			int newIndex = 0;
			switch (currentFrameLayout.getId()) {
				case R.id.frameLayout11:
					newIndex = camIndex;
					break;
				case R.id.frameLayout12:
					newIndex = camIndex+1;
					break;
				case R.id.frameLayout13:
					newIndex = camIndex+2;
					break;
				case R.id.frameLayout14:
					newIndex = camIndex+3;
					break;
				case R.id.frameLayout15:
					newIndex = camIndex+4;
					break;
				case R.id.frameLayout16:
					newIndex = camIndex+5;
					break;
				case R.id.frameLayout17:
					newIndex = camIndex;
					break;
				case R.id.frameLayout18:
					newIndex = camIndex+1;
					break;
				case R.id.frameLayout19:
					newIndex = camIndex+2;
					break;
				case R.id.frameLayout20:
					newIndex = camIndex+3;
					break;
				case R.id.frameLayout21:
					newIndex = camIndex+4;
					break;
				case R.id.frameLayout22:
					newIndex = camIndex+5;
					break;
				case R.id.frameLayout23:
					newIndex = camIndex;
					break;
				case R.id.frameLayout24:
					newIndex = camIndex+1;
					break;
				case R.id.frameLayout25:
					newIndex = camIndex+2;
					break;
				case R.id.frameLayout26:
					newIndex = camIndex+3;
					break;
				case R.id.frameLayout27:
					newIndex = camIndex+4;
					break;
				case R.id.frameLayout28:
					newIndex = camIndex+5;
					break;
			}
			
			if (newIndex != currentFoscamIndex) {
				data.fromMultiView = true;
				data.setCurrentIPCamIndex(newIndex);
			}
			
			destroyAd();
			destroyVideoViewSource();
			destroyTabs();
			// Help refresh the screen much faster
			System.gc();
			// Stop multi view activity
			finish();
		}
	}
	
	private int getIndexFromCamIndex(int camIndex) {
		int index = camIndex;
		if (camIndex >= 6 && camIndex <= 11) {
			index -= 6;
		} else if (camIndex >= 12) {
			index -= 12;
		}
		return index;
	}
	
	private void addVideoViewToFrameLayout(View view, int camIndex) {
		if (frameLayoutArray == null)
			frameLayoutArray = new FrameLayout[NUM_CAM_PER_PAGE];
		
		switch (camIndex) {
			case 0:
				frameLayoutArray[0] = (FrameLayout) findViewById(R.id.frameLayout11);
				if (frameLayoutArray[0] != null) {
					frameLayoutArray[0].removeAllViews();
					frameLayoutArray[0].addView(view);
				}
				break;
			case 1:
				frameLayoutArray[1] = (FrameLayout) findViewById(R.id.frameLayout12);
				if (frameLayoutArray[1] != null) {
					frameLayoutArray[1].removeAllViews();
					frameLayoutArray[1].addView(view);
				}
				break;
			case 2:
				frameLayoutArray[2] = (FrameLayout) findViewById(R.id.frameLayout13);
				if (frameLayoutArray[2] != null) {
					frameLayoutArray[2].removeAllViews();
					frameLayoutArray[2].addView(view);
				}
				break;
			case 3:
				frameLayoutArray[3] = (FrameLayout) findViewById(R.id.frameLayout14);
				if (frameLayoutArray[3] != null) {
					frameLayoutArray[3].removeAllViews();
					frameLayoutArray[3].addView(view);
				}
				break;
			case 4:
				frameLayoutArray[4] = (FrameLayout) findViewById(R.id.frameLayout15);
				if (frameLayoutArray[4] != null) {
					frameLayoutArray[4].removeAllViews();
					frameLayoutArray[4].addView(view);
				}
				break;
			case 5:
				frameLayoutArray[5] = (FrameLayout) findViewById(R.id.frameLayout16);
				if (frameLayoutArray[5] != null) {
					frameLayoutArray[5].removeAllViews();
					frameLayoutArray[5].addView(view);
				}
				break;
			case 6:
				frameLayoutArray[0] = (FrameLayout) findViewById(R.id.frameLayout17);
				if (frameLayoutArray[0] != null) {
					frameLayoutArray[0].removeAllViews();
					frameLayoutArray[0].addView(view);
				}
				break;
			case 7:
				frameLayoutArray[1] = (FrameLayout) findViewById(R.id.frameLayout18);
				if (frameLayoutArray[1] != null) {
					frameLayoutArray[1].removeAllViews();
					frameLayoutArray[1].addView(view);
				}
				break;
			case 8:
				frameLayoutArray[2] = (FrameLayout) findViewById(R.id.frameLayout19);
				if (frameLayoutArray[2] != null) {
					frameLayoutArray[2].removeAllViews();
					frameLayoutArray[2].addView(view);
				}
				break;
			case 9:
				frameLayoutArray[3] = (FrameLayout) findViewById(R.id.frameLayout20);
				if (frameLayoutArray[3] != null) {
					frameLayoutArray[3].removeAllViews();
					frameLayoutArray[3].addView(view);
				}
				break;
			case 10:
				frameLayoutArray[4] = (FrameLayout) findViewById(R.id.frameLayout21);
				if (frameLayoutArray[4] != null) {
					frameLayoutArray[4].removeAllViews();
					frameLayoutArray[4].addView(view);
				}
				break;
			case 11:
				frameLayoutArray[5] = (FrameLayout) findViewById(R.id.frameLayout22);
				if (frameLayoutArray[5] != null) {
					frameLayoutArray[5].removeAllViews();
					frameLayoutArray[5].addView(view);
				}
				break;
			case 12:
				frameLayoutArray[0] = (FrameLayout) findViewById(R.id.frameLayout23);
				if (frameLayoutArray[0] != null) {
					frameLayoutArray[0].removeAllViews();
					frameLayoutArray[0].addView(view);
				}
				break;
			case 13:
				frameLayoutArray[1] = (FrameLayout) findViewById(R.id.frameLayout24);
				if (frameLayoutArray[1] != null) {
					frameLayoutArray[1].removeAllViews();
					frameLayoutArray[1].addView(view);
				}
				break;
			case 14:
				frameLayoutArray[2] = (FrameLayout) findViewById(R.id.frameLayout25);
				if (frameLayoutArray[2] != null) {
					frameLayoutArray[2].removeAllViews();
					frameLayoutArray[2].addView(view);
				}
				break;
			case 15:
				frameLayoutArray[3] = (FrameLayout) findViewById(R.id.frameLayout26);
				if (frameLayoutArray[3] != null) {
					frameLayoutArray[3].removeAllViews();
					frameLayoutArray[3].addView(view);
				}
				break;
			case 16:
				frameLayoutArray[4] = (FrameLayout) findViewById(R.id.frameLayout27);
				if (frameLayoutArray[4] != null) {
					frameLayoutArray[4].removeAllViews();
					frameLayoutArray[4].addView(view);
				}
				break;
			case 17:
				frameLayoutArray[5] = (FrameLayout) findViewById(R.id.frameLayout28);
				if (frameLayoutArray[5] != null) {
					frameLayoutArray[5].removeAllViews();
					frameLayoutArray[5].addView(view);
				}
				break;
		}	
	}
}
