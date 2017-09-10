package com.ksorat.foscamremote;

import com.ksorat.foscamremote.R;
import com.ksorat.ipcamremote.httpCommand.IPCamType;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;

public class ScreenSettingsDialogFragment extends DialogFragment {
	private static ScreenSettingsDialogFragment f;
	private View dialogView;
	
	public static ScreenSettingsDialogFragment getInstance() {
		if (f == null) {
			f = new ScreenSettingsDialogFragment();
		}
        return f;
    }
	
	// Cannot make this private, Android will crash when changing the orientation.
	public ScreenSettingsDialogFragment() {
		super();
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	
    	// Inflate the layout to use as dialog or embedded fragment
    	dialogView = inflater.inflate(R.layout.screen_settings, container, false);
    	
    	Button okButton = (Button)dialogView.findViewById(R.id.okButton);
    	okButton.setOnClickListener(new okButtonClickListener());
    	
    	Button cancelButton = (Button)dialogView.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new cancelButtonClickListener());
        
        return dialogView;
    }
    
    @Override
    public void onStart() {
    	super.onStart();
    	// Populate the UI using the persistent data
    	IPCamData data = IPCamData.getInstance();
    	CheckBox flipVerticalCheckBox = (CheckBox) dialogView.findViewById(R.id.verticalInvertCheckBox);
    	CheckBox mirrorCheckBox = (CheckBox) dialogView.findViewById(R.id.mirrorCheckBox);
    	CheckBox keepScreenOnCheckBox = (CheckBox) dialogView.findViewById(R.id.keepScreenOnCheckBox);
    	CheckBox keepSpeakerOnCheckBox = (CheckBox) dialogView.findViewById(R.id.keepSpeakerOnCheckBox);
    	CheckBox lowResCheckBox = (CheckBox) dialogView.findViewById(R.id.lowResCheckBox);
    	CheckBox wideScreenCheckBox = (CheckBox) dialogView.findViewById(R.id.wideScreenCheckBox);
    	
    	CheckBox flipVerticalControlCheckBox = (CheckBox) dialogView.findViewById(R.id.verticalInvertControlCheckBox);
		CheckBox mirrorControlCheckBox = (CheckBox) dialogView.findViewById(R.id.mirrorControlCheckBox);
    	
    	int flipValue = data.getFlip();
    	if (flipValue == 0) {
    		flipVerticalCheckBox.setChecked(false);
    		mirrorCheckBox.setChecked(false);
    	} else if (flipValue == 1) {
    		flipVerticalCheckBox.setChecked(true);
    		mirrorCheckBox.setChecked(false);
    	} else if (flipValue == 2) {
    		flipVerticalCheckBox.setChecked(false);
    		mirrorCheckBox.setChecked(true);
    	} else if (flipValue == 3) {
    		flipVerticalCheckBox.setChecked(true);
    		mirrorCheckBox.setChecked(true);
    	}
    	
    	int flipControlValue = data.getFlipControl();
    	if (flipControlValue == 0) {
    		flipVerticalControlCheckBox.setChecked(false);
    		mirrorControlCheckBox.setChecked(false);
    	} else if (flipControlValue == 1) {
    		flipVerticalControlCheckBox.setChecked(true);
    		mirrorControlCheckBox.setChecked(false);
    	} else if (flipControlValue == 2) {
    		flipVerticalControlCheckBox.setChecked(false);
    		mirrorControlCheckBox.setChecked(true);
    	} else if (flipControlValue == 3) {
    		flipVerticalControlCheckBox.setChecked(true);
    		mirrorControlCheckBox.setChecked(true);
    	}
    	
    	keepScreenOnCheckBox.setChecked(data.keepScreenOn);
    	keepSpeakerOnCheckBox.setChecked(data.keepSpeakerOn);
    	lowResCheckBox.setChecked(data.isLowRes);
    	wideScreenCheckBox.setChecked(data.getScreenMode() == VideoView.SIZE_16_9);
    	
    	// Hide low res checkbox and keep speacker on checkbox if the current camera is H264
    	if (data.getIPCamType() == IPCamType.H264) {
    		keepSpeakerOnCheckBox.setVisibility(View.GONE);
    		lowResCheckBox.setVisibility(View.GONE);
    		wideScreenCheckBox.setVisibility(View.VISIBLE);
    	// Hide wide screen checkbox if the current camera is MJPEG
    	} else {
    		keepSpeakerOnCheckBox.setVisibility(View.VISIBLE);
    		lowResCheckBox.setVisibility(View.VISIBLE);
    		wideScreenCheckBox.setVisibility(View.GONE);
    	}
	}
    
    private class okButtonClickListener implements OnClickListener {

		public void onClick(View arg0) {
			// Save the flip vertical setting in the persistent data
			IPCamData data = IPCamData.getInstance();
			CheckBox flipVerticalCheckBox = (CheckBox) dialogView.findViewById(R.id.verticalInvertCheckBox);
			CheckBox mirrorCheckBox = (CheckBox) dialogView.findViewById(R.id.mirrorCheckBox);
			CheckBox keepScreenOnCheckBox = (CheckBox) dialogView.findViewById(R.id.keepScreenOnCheckBox);
			CheckBox keepSpeakerOnCheckBox = (CheckBox) dialogView.findViewById(R.id.keepSpeakerOnCheckBox);
			CheckBox flipVerticalControlCheckBox = (CheckBox) dialogView.findViewById(R.id.verticalInvertControlCheckBox);
			CheckBox mirrorControlCheckBox = (CheckBox) dialogView.findViewById(R.id.mirrorControlCheckBox);
			CheckBox lowResCheckBox = (CheckBox) dialogView.findViewById(R.id.lowResCheckBox);
			CheckBox wideScreenCheckBox = (CheckBox) dialogView.findViewById(R.id.wideScreenCheckBox);
			
			if (!flipVerticalCheckBox.isChecked() && !mirrorCheckBox.isChecked()) {
				data.setFlip(0);
			} else if (flipVerticalCheckBox.isChecked() && !mirrorCheckBox.isChecked()) {
				data.setFlip(1);
			} else if (!flipVerticalCheckBox.isChecked() && mirrorCheckBox.isChecked()) {
				data.setFlip(2);
			} else if (flipVerticalCheckBox.isChecked() && mirrorCheckBox.isChecked()) {
				data.setFlip(3);
			}
			
			if (!flipVerticalControlCheckBox.isChecked() && !mirrorControlCheckBox.isChecked()) {
				data.setFlipControl(0);
			} else if (flipVerticalControlCheckBox.isChecked() && !mirrorControlCheckBox.isChecked()) {
				data.setFlipControl(1);
			} else if (!flipVerticalControlCheckBox.isChecked() && mirrorControlCheckBox.isChecked()) {
				data.setFlipControl(2);
			} else if (flipVerticalControlCheckBox.isChecked() && mirrorControlCheckBox.isChecked()) {
				data.setFlipControl(3);
			}
			
			data.keepScreenOn = keepScreenOnCheckBox.isChecked();
			data.keepSpeakerOn = keepSpeakerOnCheckBox.isChecked();
			data.isLowRes = lowResCheckBox.isChecked();
			if (wideScreenCheckBox.isChecked()) {
				data.setScreenMode(VideoView.SIZE_16_9);
			} else {
				data.setScreenMode(VideoView.SIZE_4_3);
			}
			
			// Invoke the handler method from the main activity
			((MainActivity)ScreenSettingsDialogFragment.this.getActivity()).okButtonClickDisplaySettingsHandler();
			ScreenSettingsDialogFragment.this.getDialog().dismiss();
		}
    }
 
    private class cancelButtonClickListener implements OnClickListener {

		public void onClick(View arg0) {
			ScreenSettingsDialogFragment.this.getDialog().cancel();
		}
    }
}
