package com.ksorat.foscamremote;

import com.ksorat.foscamremote.R;
import com.ksorat.ipcamremote.httpCommand.IPCamType;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.TextView;

public class NotificationSettingsDialogFragment extends DialogFragment {
	private static NotificationSettingsDialogFragment f;
	private View dialogView;
	
	public static NotificationSettingsDialogFragment getInstance() {
		if (f == null) {
			f = new NotificationSettingsDialogFragment();
		}
        return f;
    }
	
	// Cannot make this private, Android will crash when changing the orientation.
	public NotificationSettingsDialogFragment() {
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
    	dialogView = inflater.inflate(R.layout.notification_settings, container, false);
    	
    	CheckBox motionDetectionCheckBox = (CheckBox) dialogView.findViewById(R.id.motionDetectionCheckBox);
		CheckBox soundDetectionCheckBox = (CheckBox) dialogView.findViewById(R.id.soundDetectionCheckBox);
		CheckBox androidNotificationCheckBox = (CheckBox) dialogView.findViewById(R.id.androidNotificationCheckBox);
    	
    	IPCamData data = IPCamData.getInstance();
    	IPCamType camType = data.getIPCamType();
    	int sensitivityArrayId = 0;
		if (camType == IPCamType.MJPEG) {
			sensitivityArrayId = R.array.sensitivity_array;
			soundDetectionCheckBox.setVisibility(View.VISIBLE);
			androidNotificationCheckBox.setVisibility(View.VISIBLE);
		} else if (camType == IPCamType.H264) {
			sensitivityArrayId = R.array.sensitivity_array_h264;
			// Hide sound detection and Android notification checkboxes because 
			// these features are not available for H264 cameras
			soundDetectionCheckBox.setVisibility(View.GONE);
			androidNotificationCheckBox.setVisibility(View.GONE);
		}
    	
    	Button okButton = (Button)dialogView.findViewById(R.id.okButton);
    	okButton.setOnClickListener(new okButtonClickListener());
    	
    	Button cancelButton = (Button)dialogView.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new CancelButtonClickListener());
        
        Spinner motionSpinner = (Spinner) dialogView.findViewById(R.id.motionSensitivitySpinner);
		// Create an ArrayAdapter using the string array and a default spinner layout
		ArrayAdapter<CharSequence> motionAdapter = ArrayAdapter.createFromResource(dialogView.getContext(),
				sensitivityArrayId, android.R.layout.simple_spinner_item);
		// Specify the layout to use when the list of choices appears
		motionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		// Apply the adapter to the spinner
		motionSpinner.setAdapter(motionAdapter);
		
		Spinner soundSpinner = (Spinner) dialogView.findViewById(R.id.soundSensitivitySpinner);
		if (soundSpinner != null) {
			// Create an ArrayAdapter using the string array and a default spinner layout
			ArrayAdapter<CharSequence> soundAdapter = ArrayAdapter.createFromResource(dialogView.getContext(),
			        R.array.sensitivity_array, android.R.layout.simple_spinner_item);
			// Specify the layout to use when the list of choices appears
			soundAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			// Apply the adapter to the spinner
			soundSpinner.setAdapter(soundAdapter);
		}
		
		//CheckBox emailNotificationCheckBox = (CheckBox) dialogView.findViewById(R.id.emailNotificationCheckBox);
		//emailNotificationCheckBox.setOnCheckedChangeListener(new CheckboxCheckedChangeListener());
		
		//CheckBox androidNotificationCheckBox = (CheckBox) dialogView.findViewById(R.id.androidNotificationCheckBox);
		//androidNotificationCheckBox.setOnCheckedChangeListener(new CheckboxCheckedChangeListener());
		
		motionDetectionCheckBox.setOnCheckedChangeListener(new CheckboxCheckedChangeListener());
		
		if (soundDetectionCheckBox != null) {
			soundDetectionCheckBox.setOnCheckedChangeListener(new CheckboxCheckedChangeListener());
		}
		
		processCheckedChange();
		
        return dialogView;
    }
    
    @Override
    public void onStart() {
    	super.onStart();
    	// Populate the UI using the persistent data
    	IPCamData data = IPCamData.getInstance();
    	
    	CheckBox motionDetectionCheckBox = (CheckBox) dialogView.findViewById(R.id.motionDetectionCheckBox);
    	motionDetectionCheckBox.setChecked(data.isMotionDetection());
    	
    	Spinner motionSensitivitySpinner = (Spinner) dialogView.findViewById(R.id.motionSensitivitySpinner);
    	int motionSensitivity = data.getMotionSensitivity();

    	if (data.getIPCamType() == IPCamType.H264) {
			// H264 sensitivity is a bit strange
			// low = 0, normal = 1, high = 2, lower = 3, lowest = 4
			if (motionSensitivity == 4) motionSensitivity = 0;
			else if (motionSensitivity == 3) motionSensitivity = 1;
			else if (motionSensitivity == 0) motionSensitivity = 2;
			else if (motionSensitivity == 1) motionSensitivity = 3;
			else if (motionSensitivity == 2) motionSensitivity = 4;
    	}
    	
    	motionSensitivitySpinner.setSelection(motionSensitivity);
    	
    	CheckBox soundDetectionCheckBox = (CheckBox) dialogView.findViewById(R.id.soundDetectionCheckBox);
    	if (soundDetectionCheckBox != null) {
    		soundDetectionCheckBox.setChecked(data.isSoundDetection());
    	}
    	
    	Spinner soundSensitivitySpinner = (Spinner) dialogView.findViewById(R.id.soundSensitivitySpinner);
    	if (soundSensitivitySpinner != null) {
	    	int soundSensitivity = data.getSoundSensitivity();
	    	soundSensitivitySpinner.setSelection(soundSensitivity);
    	}
    	
    	CheckBox emailNotificationCheckBox = (CheckBox) dialogView.findViewById(R.id.emailNotificationCheckBox);
    	emailNotificationCheckBox.setChecked(data.isEmailNotification());
    	
    	CheckBox androidNotificationCheckBox = (CheckBox) dialogView.findViewById(R.id.androidNotificationCheckBox);
    	androidNotificationCheckBox.setChecked(data.isAndroidNotification);
	}
    
    private class okButtonClickListener implements OnClickListener {

		public void onClick(View arg0) {
			IPCamData data = IPCamData.getInstance();
			
			// Save the motion detection setting in the persistent data
			CheckBox motionDetectionCheckBox = (CheckBox) dialogView.findViewById(R.id.motionDetectionCheckBox);
			data.setMotionDetection(motionDetectionCheckBox.isChecked());
			
			// Save the motion sensitivity setting in the persistent data
			Spinner motionSensitivitySpinner = (Spinner) dialogView.findViewById(R.id.motionSensitivitySpinner);
	    	data.setMotionSensitivity(motionSensitivitySpinner.getSelectedItemPosition());
			
			// Save the sound detection setting in the persistent data
	    	CheckBox soundDetectionCheckBox = (CheckBox) dialogView.findViewById(R.id.soundDetectionCheckBox);
	    	if (soundDetectionCheckBox != null) {
	    		data.setSoundDetection(soundDetectionCheckBox.isChecked());
	    	}
			
			// Save the sound sensitivity setting in the persistent data
			Spinner soundSensitivitySpinner = (Spinner) dialogView.findViewById(R.id.soundSensitivitySpinner);
			if (soundSensitivitySpinner != null) {
				data.setSoundSensitivity(soundSensitivitySpinner.getSelectedItemPosition());
			}
			
			// Save the email notification setting in the persistent data
			CheckBox emailNotificationCheckBox = (CheckBox) dialogView.findViewById(R.id.emailNotificationCheckBox);
			data.setEmailNotification(emailNotificationCheckBox.isChecked());
			
			// Save the Android notification setting in the persistent data
	    	CheckBox androidNotificationCheckBox = (CheckBox) dialogView.findViewById(R.id.androidNotificationCheckBox);
	    	data.isAndroidNotification = androidNotificationCheckBox.isChecked();
			
			// Invoke the handler method from the main activity
			((MainActivity)NotificationSettingsDialogFragment.this.getActivity()).okButtonClickNotificaionSettingsHandler();
			NotificationSettingsDialogFragment.this.getDialog().dismiss();
		}
    }
    
    private void processCheckedChange() {
    	CheckBox motionDetectionCheckBox = (CheckBox) dialogView.findViewById(R.id.motionDetectionCheckBox);
		CheckBox soundDetectionCheckBox = (CheckBox) dialogView.findViewById(R.id.soundDetectionCheckBox);
		CheckBox androidNotificationCheckBox = (CheckBox) dialogView.findViewById(R.id.androidNotificationCheckBox);
		CheckBox emailNotificationCheckBox = (CheckBox) dialogView.findViewById(R.id.emailNotificationCheckBox);
		
		Spinner motionSensitivitySpinner = (Spinner) dialogView.findViewById(R.id.motionSensitivitySpinner);
		Spinner soundSensitivitySpinner = (Spinner) dialogView.findViewById(R.id.soundSensitivitySpinner);
		TextView motionSensitivityTextView = (TextView) dialogView.findViewById(R.id.motionSensitivityTextView);
		TextView soundSensitivityTextView = (TextView) dialogView.findViewById(R.id.soundSensitivityTextView);
		
		if (motionDetectionCheckBox.isChecked()) {
			motionSensitivitySpinner.setEnabled(true);
			motionSensitivitySpinner.setVisibility(View.VISIBLE);
			motionSensitivityTextView.setVisibility(View.VISIBLE);
		} else {
			motionSensitivitySpinner.setEnabled(false);
			motionSensitivitySpinner.setVisibility(View.GONE);
			motionSensitivityTextView.setVisibility(View.GONE);
		}
		
		if (soundDetectionCheckBox != null) {
			if (soundDetectionCheckBox.isChecked()) {
				soundSensitivitySpinner.setEnabled(true);
				soundSensitivitySpinner.setVisibility(View.VISIBLE);
				soundSensitivityTextView.setVisibility(View.VISIBLE);
			} else {
				soundSensitivitySpinner.setEnabled(false);
				soundSensitivitySpinner.setVisibility(View.GONE);
				soundSensitivityTextView.setVisibility(View.GONE);
			}
		}
		
		// If both motion detection and sound detection are unchecked
		if (!motionDetectionCheckBox.isChecked() && !soundDetectionCheckBox.isChecked()) {
			// Uncheck and disable both android notification and email notification
			androidNotificationCheckBox.setChecked(false);
			androidNotificationCheckBox.setEnabled(false);
			emailNotificationCheckBox.setChecked(false);
			emailNotificationCheckBox.setEnabled(false);
		} else {
			emailNotificationCheckBox.setEnabled(true);
			if (motionDetectionCheckBox.isChecked()) {
				// Enable android notification checkbox only if motion detection is checked
				androidNotificationCheckBox.setEnabled(true);
			} else {
				androidNotificationCheckBox.setEnabled(false);
			}
		}
    }
 
    private class CancelButtonClickListener implements OnClickListener {

		public void onClick(View arg0) {
			NotificationSettingsDialogFragment.this.getDialog().cancel();
		}
    }
    
    private class CheckboxCheckedChangeListener implements OnCheckedChangeListener {

		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			processCheckedChange();
		}
    }
}
