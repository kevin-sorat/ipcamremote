package com.ksorat.foscamremote;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.ksorat.ipcamremote.httpCommand.IPCamCommands;
import com.ksorat.ipcamremote.httpCommand.IPCamType;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Date;
import java.util.StringTokenizer;

/**
 * Created by kevin_sorat on 5/25/15.
 */
public class IPCamWearDataLayerListenerService extends WearableListenerService {
    public static final String FILENAME = "foscam_remote_";
    public static final String FILENAME_CURRENT_CAM = "foscam_remote_current_cam";
    public static final String FILENAME_IP_CAM_TYPE = "ip_cam_remote_ip_cam_type";
    public static final String FILENAME_ANDROID_NOTIFICATION = "ip_cam_remote_android_notification";
    private static final String TAG = "IPCam:DataLayerListener";
    private static final String MESSAGE = "Received an exception";

    // Constants for Wear
    private static final String RETRIEVE_PICT = "com.soratsoft.ipcamremote.wear.retrieve.pict";
    private static final String SEND_PICT = "com.soratsoft.ipcamremote.wear.send.pict";
    private static final String SEND_TIME = "com.soratsoft.ipcamremote.wear.send.time";
    private static final String SEND_CAM_INDEX = "com.soratsoft.ipcamremote.wear.send.camIndex";
    private static final String SEND_CAM_NAME = "com.soratsoft.ipcamremote.wear.send.camName";
    private static final String TIME_PATH = "/time";
    private static final String IMAGE_PATH = "/image";
    private static final String START_MAIN_ACT_PATH = "/startMainActivity";

    private GoogleApiClient mGoogleApiClient;
    private String serverURL = ""; // "http://ksorat.no-ip.org:85";
    private String admin = "";
    private String password = "";
    private String snapshotURL = "";


    @Override
    public void onCreate() {
        super.onCreate();

        initGoogleApiClient();
        initializeIPCamData();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        String nodeId = messageEvent.getSourceNodeId();
        //Log.d(TAG, "onMessageReceived, nodeId: " + nodeId);
        // Receive the message from wear
        if (messageEvent.getPath().equals(START_MAIN_ACT_PATH)) {
            //Log.d(TAG, "onMessageReceived, path: " + START_MAIN_ACT_PATH);

            // Get current camera index from wear app
            int currentCamIndex = fromByteArray(messageEvent.getData());
            //Log.d(TAG, "onMessageReceived: currentCamIndex: " + currentCamIndex);

            // Save the current camera index to local storage, so the main activity can read,
            // by using the existing code
            saveCurrentCameraIndex(currentCamIndex);

            // Check to see if main activity is currently running in foreground
            // If it is NOT running in foreground
            if (!IPCamRemoteApplication.isMainActivityVisible()) {
                // Start main activity with extra param set to false
                Intent startIntent = new Intent(getApplicationContext(), MainActivity.class);
                startIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                //startIntent.putExtra("isResumeFromWear", false);
                IPCamRemoteApplication.setIsResumeFromWear(false);
                startActivity(startIntent);
            // Else main activity is running in foreground
            } else {
                // Start main activity with extra param set to true
                Intent startIntent = new Intent(getApplicationContext(), MainActivity.class);
                startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                //startIntent.putExtra("isResumeFromWear", true);
                IPCamRemoteApplication.setIsResumeFromWear(true);
                startActivity(startIntent);
            }
        }
    }

    int fromByteArray(byte[] bytes) {
        return bytes[0] << 24 | (bytes[1] & 0xFF) << 16 | (bytes[2] & 0xFF) << 8 | (bytes[3] & 0xFF);
    }

    private void saveCurrentCameraIndex(int currentCamIndex) {
        String tempCamIndexStr = Integer.toString(currentCamIndex);

        // Save the data to a file
        try {
            FileOutputStream fos = openFileOutput(FILENAME_CURRENT_CAM,
                    Context.MODE_PRIVATE);
            fos.write(tempCamIndexStr.getBytes());
            fos.close();
        } catch (Exception e) {
            Log.e(TAG, MESSAGE, e);
        }
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        //Log.d(TAG, "onDataChanged: " + dataEventBuffer);
        for (DataEvent event : dataEventBuffer) {
            if (event.getType() == DataEvent.TYPE_CHANGED && event.getDataItem().getUri().getPath().equals(TIME_PATH)) {
                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                Long time = dataMapItem.getDataMap().getLong(SEND_TIME);
                int cameraIndex = dataMapItem.getDataMap().getInt(SEND_CAM_INDEX);
                Log.d(TAG, "onDataChanged: Time is: " + time + ", cameraIndex: " + cameraIndex);

                // TODO: Perform checkings to make sure that there is a selected camera and the connection is valid

                Bitmap bitmap = null;
                String cameraName = null;
                // Load/reload current cam index
                if (loadCameraData(cameraIndex)) {
                    // Retrieve screen shot of the the current IP camera
                    bitmap = loadImageFromUrl(snapshotURL);
                    cameraName = getCameraName();
                }
                putBitmapDataRequest(bitmap, cameraIndex, cameraName);
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                // DataItem deleted
            }
        }
    }

    private void initGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();
        //Wearable.MessageApi.addListener(mGoogleApiClient, this);
    }

    private void initializeIPCamData() {
        IPCamData data = IPCamData.getInstance();
        if (data.getServerURL() == null || data.getServerURL().equals("")) {
            loadDataFromInternalStorage();
        }
    }

    /*
    private void loadCurrentCameraData() {
        IPCamData data = IPCamData.getInstance();
        if (data.getServerURL() != null && !data.getServerURL().equals("")) {
            int currentIndex = loadCurrentCameraIndex();
            data.setCurrentIPCamIndex(currentIndex);
            generateURLStrings();
        }
    }
    */

    private boolean loadCameraData(int cameraIndex) {
        //Log.d(TAG, "loadCameraData");
        IPCamData data = IPCamData.getInstance();
        data.setCurrentIPCamIndex(cameraIndex);
        //Log.d(TAG, "serverURL: " + data.getServerURL());
        if (data.getServerURL() != null && !data.getServerURL().equals("")) {
            generateURLStrings();
            return true;
        } else {
            Log.e(TAG, "Error in loadCameraData: serverURL is null or empty.");
            return false;
        }
    }

    private String getCameraName() {
        IPCamData data = IPCamData.getInstance();
        return data.getCameraName();
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

    private static Asset createAssetFromBitmap(Bitmap bitmap) {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 25, byteStream);
        return Asset.createFromBytes(byteStream.toByteArray());
    }

    private void putBitmapDataRequest(Bitmap bitmap, int cameraIndex, String cameraName) {
        Asset asset = null;
        PutDataMapRequest dataMap = PutDataMapRequest.create(IMAGE_PATH);
        dataMap.getDataMap().putLong(SEND_TIME, new Date().getTime());

        if (bitmap == null) {
            Log.d(TAG, "putBitmapDataRequest: bitmap is null!");
        } else {
            asset = createAssetFromBitmap(bitmap);
        }

        // Send back image data
        dataMap.getDataMap().putAsset(SEND_PICT, asset);
        // Also send back IP camera's index and name
        dataMap.getDataMap().putInt(SEND_CAM_INDEX, cameraIndex);
        dataMap.getDataMap().putString(SEND_CAM_NAME, cameraName);

        PutDataRequest request = dataMap.asPutDataRequest();
        //PendingResult<DataApi.DataItemResult> pendingResult =
        Wearable.DataApi.putDataItem(mGoogleApiClient, request);

        /*
        pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(final DataApi.DataItemResult result) {
                if (result.getStatus().isSuccess()) {
                    Log.d(TAG, "onResult: Success");
                } else {
                    Log.d(TAG, "onResult: Fail");
                }
            }
        });
        */
    }

    /*
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
    */

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
                        data.setServerURL(tokenizer.nextToken());
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
    }

    private void generateURLStrings() {
        IPCamData data = IPCamData.getInstance();
        // If public camera is selected
        if (data.getPortNumberOrPublic().equals(IPCamera.PUBLIC)) {
            // Do nothing
            // Else use foscam URLs
        } else {
            serverURL = data.getServerURL() + ":" + data.getPortNumberOrPublic();
            admin = data.getUserName();
            password = data.getPassword();
            Object[] args = { serverURL, admin, password };

            IPCamCommands commands = data.getIPCamCommands();
            snapshotURL = MessageFormat.format(commands.snapshotURLTemp, args);
        }
    }
}
