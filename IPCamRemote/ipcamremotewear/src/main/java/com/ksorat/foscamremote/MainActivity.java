package com.ksorat.foscamremote;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;
import android.support.wearable.view.CircledImageView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class MainActivity extends Activity implements
        DataApi.DataListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    public static final long TIMEOUT_MS = 1000;
    public static final int ANIMATION_DURATION = 200;
    public static final int NUM_CAM_VIEWS = 18;
    public static final int NUM_PAGES = 3;

    // For shared preferences
    public static final String PREFS_NAME = "IPCamRemoteWearPrefsFile";

    private static final String TAG = "IPCam:Wear";
    private static final String RETRIEVE_PICT = "com.soratsoft.ipcamremote.wear.retrieve.pict";
    private static final String SEND_PICT = "com.soratsoft.ipcamremote.wear.send.pict";
    private static final String SEND_TIME = "com.soratsoft.ipcamremote.wear.send.time";
    private static final String SEND_CAM_INDEX = "com.soratsoft.ipcamremote.wear.send.camIndex";
    private static final String SEND_CAM_NAME = "com.soratsoft.ipcamremote.wear.send.camName";
    private static final String TIME_PATH = "/time";
    private static final String IMAGE_PATH = "/image";
    private static final String START_MAIN_ACT_PATH = "/startMainActivity";
    private static final int OPEN_ON_PHONE_REQUEST = 1111;
    private static final long TIME_OUT_PERIOD = 10000;  // 10 seconds

    private GoogleApiClient mGoogleApiClient;
    private ViewPager viewPager;
    private FragmentPagerAdapter adapterViewPager;
    private PagerAdapter pagerAdapter;
    private ViewFlipper viewFlipper;
    private LinearLayout openOnPhoneView;
    private LinearLayout helpView;
    private CircledImageView openCircledImageView;
    private CircledImageView helpCircledImageView;
    private TextView[] camViewsArray;
    private TextView statusTextView;
    private int currentCamViewIndex;
    private Bitmap bitmap;
    private RelativeLayout loadingPanel;
    private RelativeLayout watermarkPanel;

    private Animation inFromTop;
    private Animation outToBottom;
    private Animation inFromBottom;
    private Animation outToTop;
    /*
    private Animation inFromLeft;
    private Animation outToRight;
    private Animation inFromRight;
    private Animation outToLeft;
    */

    private boolean isDisplayImageOK = false;
    private long currentThreadId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        // Restore preferences
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        // Set the current cam view
        currentCamViewIndex = settings.getInt("currentCamViewIndex", 0);

        initAnimations();

        final WatchViewStub stub = (WatchViewStub)findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                loadingPanel = (RelativeLayout) findViewById(R.id.loadingPanel);
                watermarkPanel = (RelativeLayout) findViewById(R.id.watermarkPanel);
                openCircledImageView = (CircledImageView) findViewById(R.id.openCircledImageView);
                helpCircledImageView = (CircledImageView) findViewById(R.id.helpCircledImageView);
                statusTextView = (TextView) findViewById(R.id.statusTextView);

                camViewsArray = new TextView[MainActivity.NUM_CAM_VIEWS];
                viewPager = (ViewPager) findViewById(R.id.viewPager);
                adapterViewPager = new MyPagerAdapter(getFragmentManager());
                viewPager.setAdapter(adapterViewPager);
            }
        });
    }

    private void initAnimations() {
        AccelerateInterpolator accelerateInterpolator = new AccelerateInterpolator();
        inFromTop = new TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, -1.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f);
        inFromTop.setDuration(ANIMATION_DURATION);
        inFromTop.setInterpolator(accelerateInterpolator);

        outToBottom = new TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 1.0f);
        outToBottom.setDuration(ANIMATION_DURATION);
        outToBottom.setInterpolator(accelerateInterpolator);

        inFromBottom = new TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 1.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f);
        inFromBottom.setDuration(ANIMATION_DURATION);
        inFromBottom.setInterpolator(accelerateInterpolator);

        outToTop = new TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, -1.0f);
        outToTop.setDuration(ANIMATION_DURATION);
        outToTop.setInterpolator(accelerateInterpolator);

        /*
        inFromLeft = new TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, -1.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f);
        inFromLeft.setDuration(ANIMATION_DURATION);
        inFromLeft.setInterpolator(accelerateInterpolator);

        outToRight = new TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 1.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f);
        outToRight.setDuration(ANIMATION_DURATION);
        outToRight.setInterpolator(accelerateInterpolator);

        inFromRight = new TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, 1.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f);
        inFromRight.setDuration(ANIMATION_DURATION);
        inFromRight.setInterpolator(accelerateInterpolator);

        outToLeft = new TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, -1.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f);
        outToLeft.setDuration(ANIMATION_DURATION);
        outToLeft.setInterpolator(accelerateInterpolator);
        */
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Connected to Google Api Service");
        }
        Wearable.DataApi.addListener(mGoogleApiClient, this);

        // Load the image automatically at the start
        requestImageFromMobile();
    }


    @Override
    public void onConnectionSuspended(int i) {
        // Do nothing
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "onConnectionFailed: Failed to connect, with result: " + connectionResult);
        displayErrorMessage();
    }

    @Override
    protected void onStop() {
        if (null != mGoogleApiClient && mGoogleApiClient.isConnected()) {
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        super.onStop();

        // Save preferences
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("currentCamViewIndex", currentCamViewIndex);
        // Commit the edit
        editor.commit();
    }

    // Create a data map and put data in it
    private void requestImageFromMobile() {
        // Prevent user from generating more UI events
        isDisplayImageOK = false;

        PutDataMapRequest putDataMapReq = PutDataMapRequest.create(TIME_PATH);
        // Note: data changed event will be triggered only if the value has changed (e.g. time)

        DataMap map = putDataMapReq.getDataMap();
        map.putLong(SEND_TIME, new Date().getTime());
        map.putInt(SEND_CAM_INDEX, currentCamViewIndex);

        PendingResult<DataApi.DataItemResult> pendingResult =
            Wearable.DataApi.putDataItem(mGoogleApiClient, putDataMapReq.asPutDataRequest());

        Log.d(TAG, "requestImageFromMobile: IP Cam #" + (currentCamViewIndex+1));

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                loadingPanel.setVisibility(View.VISIBLE);
                watermarkPanel.setVisibility(View.VISIBLE);
                statusTextView.setText(MessageFormat.format(getResources().getString(R.string.status_text),
                        currentCamViewIndex+1, getResources().getString(R.string.waiting_text)));
            }
        });

        pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(final DataApi.DataItemResult result) {
                if(result.getStatus().isSuccess()) {
                    //Log.d(TAG, "requestImageFromMobile: Success");
                    Runnable timeoutRunner = new TimeoutRunner();
                    Thread timeoutThread = new Thread(timeoutRunner);
                    currentThreadId = timeoutThread.getId();
                    timeoutThread.start();
                } else {
                    Log.d(TAG, "requestImageFromMobile: Fail");
                    displayErrorMessage();
                }
            }
        });
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        //Log.d(TAG, "onDataChanged: " + dataEvents);
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED && event.getDataItem().getUri().getPath().equals(IMAGE_PATH)) {
                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                Asset asset = dataMapItem.getDataMap().getAsset(SEND_PICT);
                int cameraIndex = dataMapItem.getDataMap().getInt(SEND_CAM_INDEX);
                String cameraName = dataMapItem.getDataMap().getString(SEND_CAM_NAME);

                // If bitmap asset is null
                if (asset == null) {
                    Log.d(TAG, "onDataChanged: Asset is null!: IP Cam #" + (currentCamViewIndex+1));
                    displayErrorMessage();
                    // Else there is a picture to display
                } else {
                    bitmap = loadBitmapFromAsset(asset);
                    //Log.d(TAG, "bitmap.getByteCount(): " + bitmap.getByteCount());
                    Log.d(TAG, "onDataChanged: Camera Index: " + cameraIndex + ", Name: " + cameraName);
                    // Display bitmap in imageView
                    displayImage(cameraIndex, cameraName);
                }
            }
            /*
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                Log.d(TAG, "DataItem deleted: " + event.getDataItem().getUri());
            }
            */
        }
    }

    private void displayImage(final int cameraIndex, final String cameraName) {
        // If current camera index is not the same as the returned cameraIndex,
        // then skip the update because the UI is now on a different camera index's page
        if (cameraIndex == currentCamViewIndex) {
            runOnUiThread(new Runnable() {
                @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                @Override
                public void run() {
                    try {
                        BitmapDrawable bd = new BitmapDrawable(getResources(), bitmap);
                        camViewsArray[cameraIndex].setBackground(bd);
                        if (!camViewsArray[cameraIndex].getText().equals("")) {
                            camViewsArray[cameraIndex].setText("");
                        }
                        loadingPanel.setVisibility(View.GONE);
                        watermarkPanel.setVisibility(View.VISIBLE);

                        // TODO: This might be wrong, status text should be attached to a particular IP cam
                        statusTextView.setText(MessageFormat.format(getResources().getString(R.string.status_text),
                                currentCamViewIndex+1, cameraName));
                        isDisplayImageOK = true;
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
        }
    }

    private void displayErrorMessage() {
        if (!camViewsArray[currentCamViewIndex].getText().equals(R.string.error_msg)) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        camViewsArray[currentCamViewIndex].setBackground(null);
                        camViewsArray[currentCamViewIndex].setText(R.string.error_msg);
                        loadingPanel.setVisibility(View.GONE);
                        watermarkPanel.setVisibility(View.GONE);
                        statusTextView.setText(MessageFormat.format(getResources().getString(R.string.status_text),
                                currentCamViewIndex+1, getResources().getString(R.string.error_text)));
                        isDisplayImageOK = false;
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
        }
    }

    private Bitmap loadBitmapFromAsset(Asset asset) {
        if (asset == null) {
            throw new IllegalArgumentException("Asset must not be null.");
        }
        ConnectionResult result =
                mGoogleApiClient.blockingConnect(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        if (!result.isSuccess()) {
            return null;
        }

        // Convert asset into a file descriptor and block until it's ready
        InputStream assetInputStream = Wearable.DataApi.getFdForAsset(
                mGoogleApiClient, asset).await().getInputStream();

        if (assetInputStream == null) {
            Log.w(TAG, "Requested an unknown Asset.");
            return null;
        }
        // Decode the stream into a bitmap
        return BitmapFactory.decodeStream(assetInputStream);
    }

    private class MyPagerAdapter extends FragmentPagerAdapter {

        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }

        @Override
        // Returns the fragment to display for that page
        public Fragment getItem(int position) {
            Fragment fragment;
            switch (position) {
                case 0:
                    fragment = new CameraPageFragment();
                    
                    viewFlipper = (ViewFlipper) findViewById(R.id.ipCamViewFlipper);
                    // Create 18 cam views
                    for (int i = 0; i < MainActivity.NUM_CAM_VIEWS; i++) {
                        camViewsArray[i] = new TextView(MainActivity.this);
                        camViewsArray[i].setId(i);
                        camViewsArray[i].setTextAppearance(MainActivity.this, android.R.style.TextAppearance_DeviceDefault_Small);
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT);
                        camViewsArray[i].setLayoutParams(params);
                        camViewsArray[i].setGravity(Gravity.CENTER);

                        // Add to viewFlipper
                        viewFlipper.addView(camViewsArray[i]);
                    }

                    // Set swipe (fling) and long click listener
                    final GestureDetector gd = new GestureDetector(new MyGestureListener());
                    viewPager.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(final View view, final MotionEvent event) {
                            if (gd.onTouchEvent(event)) {
                                return false;
                            } else {
                                return true;
                            }
                        }
                    });
                    viewFlipper.setDisplayedChild(currentCamViewIndex);
                    viewPager.addView(viewFlipper, position);
                    break;
                case 1:
                    /*
                    fragment = new OpenOnPhonePageFragment();
                    openOnPhoneView = (LinearLayout) findViewById(R.id.openOnPhoneView);
                    viewPager.addView(openOnPhoneView, position);
                    openCircledImageView = (CircledImageView) findViewById(R.id.openCircledImageView);
                    openCircledImageView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            launchApplication(MainActivity.this);
                        }
                    });
                    break;
                    */
                case 2:
                    /*
                    fragment = new HelpPageFragment();
                    helpView = (LinearLayout) findViewById(R.id.helpView);
                    viewPager.addView(helpView, position);
                    helpCircledImageView = (CircledImageView) findViewById(R.id.helpCircledImageView);
                    helpCircledImageView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // TODO: Display Help contents
                        }
                    });
                    break;
                    */
                default:
                    fragment = new Fragment();
            }
            return fragment;
        }

        // Returns the page title for the top indicator
        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Camera View";
                case 1:
                    return getResources().getString(R.string.open_on_phone);
                case 2:
                    return getResources().getString(R.string.help);
                default:
                    return null;
            }
        }

        /*
        @Override
        protected Object instantiateItem(ViewGroup viewGroup, int row, int column) {
            View view = null;
            if (row == 0) {
                // Camera view flipper page
                if (column == 0) {
                    view = View.inflate(MainActivity.this, R.layout.ipcam_view_flipper, null);
                    viewFlipper = (ViewFlipper) view;

                    // Create 18 cam views
                    for (int i = 0; i < MainActivity.NUM_CAM_VIEWS; i++) {
                        camViewsArray[i] = new TextView(MainActivity.this);
                        camViewsArray[i].setId(i);
                        camViewsArray[i].setTextAppearance(MainActivity.this,
                                android.R.style.TextAppearance_DeviceDefault_Small);
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT);
                        camViewsArray[i].setLayoutParams(params);
                        camViewsArray[i].setGravity(Gravity.CENTER);

                        // Add to viewFlipper
                        viewFlipper.addView(camViewsArray[i]);
                    }

                    // Set swipe (fling) and long click listener
                    final GestureDetector gd = new GestureDetector(new MyGestureListener());
                    viewPager.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(final View view, final MotionEvent event) {
                            if (gd.onTouchEvent(event)) {
                                return false;
                            } else {
                                return true;
                            }
                        }
                    });
                    viewFlipper.setDisplayedChild(currentCamViewIndex);
                    viewPager.addView(view);
                // "Open on Phone" page
                } else if (column == 1) {
                    view = View.inflate(MainActivity.this, R.layout.open_on_phone, null);
                    LinearLayout linearLayout = (LinearLayout) view;
                    viewPager.addView(view);
                    openCircledImageView = (CircledImageView) findViewById(R.id.openCircledImageView);
                    openCircledImageView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            launchApplication(MainActivity.this);
                        }
                    });
                // "Help" page
                } else if (column == 2) {
                    view = View.inflate(MainActivity.this, R.layout.help_page, null);
                    LinearLayout linearLayout = (LinearLayout) view;
                    viewPager.addView(view);
                    helpCircledImageView = (CircledImageView) findViewById(R.id.helpCircledImageView);
                    helpCircledImageView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // TODO: Display Help contents
                        }
                    });
                }
            }
            return view;
        }

        @Override
        protected void destroyItem(ViewGroup viewGroup, int i, int i1, Object o) {
            viewGroup.removeView((View) o);
        }

        @Override
        public boolean isViewFromObject(View view, Object o) {
            return (view == o);
        }
        */
    }

    public class CameraPageFragment extends Fragment {
        private int pageIndex = 0;

        // Store instance variables based on arguments passed
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        // Inflate the view for the fragment based on layout XML
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            // Get view at the current page index
            View view = viewPager.getChildAt(pageIndex);
            // If view is not null
            if (view != null) {
                // Just return it
                return view;
            // Else view is null
            } else if (pageIndex == 0) {
                // Camera view flipper page
                view = inflater.inflate(R.layout.ipcam_view_flipper, container, false);
            } else if (pageIndex == 1) {
                // "Open on Phone" page
                view = inflater.inflate(R.layout.open_on_phone, container, false);
            } else if (pageIndex == 2) {
                // "Help" page
                view = inflater.inflate(R.layout.help_page, co             });
            }
            return view;
        }
    }

    public class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_MIN_DISTANCE = 60;
        private static final int SWIPE_MAX_OFF_PATH = 250;
        private static final int SWIPE_THRESHOLD_VELOCITY = 60;

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            //Log.d(TAG, "onFling");
            if ((Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH) &&
                    (Math.abs(e1.getX() - e2.getX()) > SWIPE_MAX_OFF_PATH))
                return false;
            // If swiping up
            if (e1.getY() - e2.getY() > SWIPE_MIN_DISTANCE
                    && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
                // If we are currently in "viewFlipper"
                if (viewPager.getCurrentItem() == 0) {
                    showNextCamView();
                    return true;
                }
            // If swiping down
            } else if (e2.getY() - e1.getY() > SWIPE_MIN_DISTANCE
                    && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
                // If we are currently in "viewFlipper"
                if (viewPager.getCurrentItem() == 0) {
                    showPreviousCamView();
                    return true;
                }
            // If swiping left
            } else if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE
                    && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                // If we are currently in "viewFlipper"
                if (viewPager.getCurrentItem() == 0) {
                    // Go to "Open on Phone" view
                    showOpenOnPhone();
                    return true;
                // If we are currently in "Open on Phone" view
                } else if (viewPager.getCurrentItem() == 1) {
                    // Go to "Help" view
                    showHelp();
                    return true;
                } else {
                    return true;
                }
            // If swiping right
            } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE
                    && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                // If we are currently in "Open on Phone"
                if (viewPager.getCurrentItem() == 1) {
                    // Go back to "viewFlipper"
                    showViewFlipper();
                    return true;
                // If we are currently in "Help"
                } else if (viewPager.getCurrentItem() == 2) {
                    showOpenOnPhone();
                    return true;
                } else {
                    return true;
                }
            }
            return super.onFling(e1, e2, velocityX, velocityY);
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            //Log.d(TAG, "onSingleTapConfirmed");
            // If we are currently in "viewFlipper"
            if (viewPager.getCurrentItem() == 0) {
                if (loadingPanel.getVisibility() != View.VISIBLE) {
                    requestImageFromMobile();
                }
            }
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            //Log.d(TAG, "onLongPress");
        }
    }

    private void showViewFlipper() {
        viewPager.setCurrentItem(0, true);
    }

    private void showOpenOnPhone() {
        viewPager.setCurrentItem(1, true);
    }

    private void showHelp() {
        viewPager.setCurrentItem(2, true);
    }

    private void showPreviousCamView() {
        //Log.d(TAG, "showPreviousCamView");

        // If we are already at the first camera index
        if (currentCamViewIndex <= 0) {
            // Loop it ahead to the last camera
            currentCamViewIndex = NUM_CAM_VIEWS-1;
        } else {
            // Otherwise, move to the previous camera
            currentCamViewIndex--;
        }
        requestImageFromMobile();

        viewFlipper.setInAnimation(inFromTop);
        viewFlipper.setOutAnimation(outToBottom);
        viewFlipper.showPrevious();
    }

    private void showNextCamView() {
        //Log.d(TAG, "showNextCamView");

        // If we are already at the last camera index
        if (currentCamViewIndex >= NUM_CAM_VIEWS-1) {
            // Loop it back to the first camera
            currentCamViewIndex = 0;
        } else {
            // Otherwise, move to the next camera
            currentCamViewIndex++;
        }
        requestImageFromMobile();

        viewFlipper.setInAnimation(inFromBottom);
        viewFlipper.setOutAnimation(outToTop);
        viewFlipper.showNext();
    }

    void launchApplication(Context context) {
        startConfirmationActivity(ConfirmationActivity.OPEN_ON_PHONE_ANIMATION,
                getResources().getString(R.string.open_on_phone));
    }

    // Displaying "Open on Mobile" animation and sending message to mobile app to start main activity
    private void startConfirmationActivity(int animationType, String message) {
        // For displaying "Open on Mobile" animation
        Intent confirmationActivity = new Intent(this, ConfirmationActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION)
                .putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, animationType)
                .putExtra(ConfirmationActivity.EXTRA_MESSAGE, message);
        startActivityForResult(confirmationActivity, OPEN_ON_PHONE_REQUEST);

        new Thread(new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult result =
                        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
                List<Node> nodes = result.getNodes();
                //Log.d(TAG, "startConfirmationActivity: nodes.size(): " + nodes.size());
                if (nodes != null && nodes.size() > 0) {
                    for (Node node : nodes) {
                        //Log.d(TAG, "startConfirmationActivity: Node ID: " + node.getId() + ", Name: " + node.getDisplayName() + ", isNearBy: " + node.isNearby());
                        if (node.isNearby()) {
                            // Send IP camera's current index from wear to mobile,
                            // such that the currently selected IP camera will be opened in mobile app
                            Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(),
                                    START_MAIN_ACT_PATH, intToByteArray(currentCamViewIndex));
                            //Log.d(TAG, "startConfirmationActivity: Message sent to mobile app");
                        }
                    }
                } else {
                    Log.d(TAG, "startConfirmationActivity: Fail to send message to mobile app, no connected node");
                }
            }
        }).start();
    }

    public static final byte[] intToByteArray(int value) {
        return new byte[] {
                (byte)(value >>> 24),
                (byte)(value >>> 16),
                (byte)(value >>> 8),
                (byte)value};
    }

    class TimeoutRunner implements Runnable {
        @Override
        public void run() {
            // Wait (sleep) for 10 seconds (10,000 milliseconds)
            try {
                Thread.sleep(TIME_OUT_PERIOD);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // If the display image status is still NOT OK yet at this point and
            // this thread is the most recently created one
            if (!isDisplayImageOK && Thread.currentThread().getId() == currentThreadId) {
                Log.d(TAG, "TimeoutRunner: Fail to display image in 10 seconds");
                // Display an error message
                displayErrorMessage();
            } else {
                Log.d(TAG, "TimeoutRunner: Pass, image is displayed within 10 seconds");
            }
        }
    }
}