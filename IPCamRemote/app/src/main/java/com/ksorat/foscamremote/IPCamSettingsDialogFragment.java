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
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Toast;

public class IPCamSettingsDialogFragment extends DialogFragment {

	private static IPCamSettingsDialogFragment f;
	private View dialogView;

	public static IPCamSettingsDialogFragment getInstance() {
		if (f == null) {
			f = new IPCamSettingsDialogFragment();
		}
		return f;
	}

	// Cannot make this private, Android will crash when changing the
	// orientation.
	public IPCamSettingsDialogFragment() {
		super();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setStyle(DialogFragment.STYLE_NORMAL,
				android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		// Inflate the layout to use as dialog or embedded fragment
		dialogView = inflater.inflate(R.layout.dialog_foscam_settings,
				container, false);

		RadioButton publicCamRadioButton = (RadioButton) dialogView
				.findViewById(R.id.publicCamRadioButton);
		publicCamRadioButton
				.setOnCheckedChangeListener(new publicCamCheckBoxCheckedChangeListener());

		Button okButton = (Button) dialogView.findViewById(R.id.okButton);
		okButton.setOnClickListener(new okButtonClickListener());

		Button cancelButton = (Button) dialogView
				.findViewById(R.id.cancelButton);
		cancelButton.setOnClickListener(new cancelButtonClickListener());

		Button clearButton = (Button) dialogView.findViewById(R.id.clearButton);
		clearButton.setOnClickListener(new clearButtonClickListener());

		IPCamData data = IPCamData.getInstance();
		if (data.getServerURL() != null && data.getServerURL().equals("")) {
			data.setServerURL("http://");
		}
		
		String[] tempArray = getResources().getStringArray(
				R.array.public_cam_array);
		
		// Create an array of public cam names
		String[] publicNames = new String[tempArray.length];
		// Create an array of public cam (IPCamera objects)
		IPCamera[] publicCams = new IPCamera[tempArray.length];
		
		for (int i=0 ; i<tempArray.length ; i++) {
			// Must use double slashes before the pipe char for split
			String[] tokens = tempArray[i].split("\\|");
			if (tokens.length == 4) {
				publicCams[i] = new IPCamera();
				publicCams[i].serverURL = tokens[0] + ":" + tokens[1] + tokens[2];
				publicCams[i].portNumberOrPublic = IPCamera.PUBLIC;
				publicCams[i].cameraName = tokens[3];
				publicNames[i] = tokens[3];
			}
		}
		data.availablePublicCameras = publicCams;
		
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(IPCamSettingsDialogFragment.this.getDialog().getContext(),
				android.R.layout.simple_spinner_item, publicNames);
		// Specify the layout to use when the list of choices appears
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		// Apply the adapter to the spinner
		Spinner publicCamSpinner = (Spinner) dialogView.findViewById(R.id.publicCamSpinner);
		publicCamSpinner.setAdapter(adapter);

		/*
		// TODO: Hide the H.264 camera radio button for now
		RadioButton h264CamRadioButton = (RadioButton) dialogView
				.findViewById(R.id.h264RadioButton);
		h264CamRadioButton.setVisibility(View.GONE);
		*/
		
		return dialogView;
	}

	@Override
	public void onStart() {
		super.onStart();

		// Populate the UI using the persistent data
		EditText nameEditText = (EditText) dialogView
				.findViewById(R.id.nameEditText);
		EditText addressEditText = (EditText) dialogView
				.findViewById(R.id.addressEditText);
		EditText portNumEditText = (EditText) dialogView
				.findViewById(R.id.portNumEditText);
		EditText userNameEditText = (EditText) dialogView
				.findViewById(R.id.userNameEditText);
		EditText passwordEditText = (EditText) dialogView
				.findViewById(R.id.passwordEditText);

		IPCamData data = IPCamData.getInstance();
		nameEditText.setText(data.getCameraName());
		addressEditText.setText(data.getServerURL());
		portNumEditText.setText(data.getPortNumberOrPublic());
		userNameEditText.setText(data.getUserName());
		passwordEditText.setText(data.getPassword());

		if (data.getPortNumberOrPublic().equals(IPCamera.PUBLIC)) {
			RadioButton publicCamRadioButton = (RadioButton) dialogView.findViewById(R.id.publicCamRadioButton);
			publicCamRadioButton.setChecked(true);
			
			Spinner publicCamSpinner = (Spinner) dialogView.findViewById(R.id.publicCamSpinner);
			IPCamera[] avaiPublicCams = data.availablePublicCameras;
			for (int i=0 ; i<avaiPublicCams.length ; i++) {
				if (avaiPublicCams[i].cameraName.equals(data.getCameraName())) {
					publicCamSpinner.setSelection(i);
					break;
				}
			}
			
		} else if (data.getIPCamType() == IPCamType.MJPEG) {
			RadioButton mjpegCamRadioButton = (RadioButton) dialogView.findViewById(R.id.mjpegRadioButton);
			mjpegCamRadioButton.setChecked(true);
		} else if (data.getIPCamType() == IPCamType.H264) {
			RadioButton h264CamRadioButton = (RadioButton) dialogView.findViewById(R.id.h264RadioButton);
			h264CamRadioButton.setChecked(true);
		}
		disableEnableControls();
	}

	private class okButtonClickListener implements OnClickListener {

		public void onClick(View arg0) {

			EditText nameEditText = (EditText) dialogView
					.findViewById(R.id.nameEditText);
			EditText addressEditText = (EditText) dialogView
					.findViewById(R.id.addressEditText);
			EditText portNumEditText = (EditText) dialogView
					.findViewById(R.id.portNumEditText);
			EditText userNameEditText = (EditText) dialogView
					.findViewById(R.id.userNameEditText);
			EditText passwordEditText = (EditText) dialogView
					.findViewById(R.id.passwordEditText);
			
			RadioButton mjpegCamRadioButton = (RadioButton) dialogView
					.findViewById(R.id.mjpegRadioButton);
			RadioButton h264CamRadioButton = (RadioButton) dialogView
					.findViewById(R.id.h264RadioButton);
			RadioButton publicCamRadioButton = (RadioButton) dialogView
					.findViewById(R.id.publicCamRadioButton);
			Spinner publicCamSpinner = (Spinner) dialogView
					.findViewById(R.id.publicCamSpinner);
			
			// Public IP camera was selected
			if (publicCamRadioButton.isChecked() && 
					publicCamSpinner.getSelectedItemPosition() != Spinner.INVALID_POSITION) {
				
				String publicCameraName = publicCamSpinner.getSelectedItem().toString();
				String serverURL = addressEditText.getText().toString();
				String portNumber = portNumEditText.getText().toString();
				
				if (validateDuplicateCamera(serverURL, portNumber, publicCameraName)) {
					// Save the data to application data and internal storage
					IPCamData data = IPCamData.getInstance();
					int selectedIndex = publicCamSpinner.getSelectedItemPosition();
					IPCamera selectedCam = data.availablePublicCameras[selectedIndex];
					data.setCameraName(selectedCam.cameraName);
					data.setServerURL(selectedCam.serverURL);
					data.setPortNumberOrPublic(selectedCam.portNumberOrPublic);
					data.setUserName(selectedCam.userName);
					data.setPassword(selectedCam.password);
					data.setIPCamType(IPCamType.PUBLIC);
					data.setScreenMode(VideoView.SIZE_4_3);
					
					// Erase entries in the user's IP camera section
					nameEditText.setText("");
					addressEditText.setText("");
					portNumEditText.setText("");
					userNameEditText.setText("");
					passwordEditText.setText("");
					
					// Update the main activity's view
					// Invoke the handler method from the main activity
					((MainActivity) IPCamSettingsDialogFragment.this.getActivity())
							.okButtonClickConnectionSettingsHandler();
					if (IPCamSettingsDialogFragment.this.getDialog() != null)
						IPCamSettingsDialogFragment.this.getDialog().dismiss();
				}
			// Else non-public IP camera was selected
			} else {
				String cameraName = nameEditText.getText().toString();
				String serverURL = addressEditText.getText().toString();
				String portNumber = portNumEditText.getText().toString();
				String userName = userNameEditText.getText().toString();
				String password = passwordEditText.getText().toString().trim();
	
				// Validate the data
				// If the validation passes
				if (validateInputData(cameraName, serverURL, portNumber, userName,
						password) && validateDuplicateCamera(serverURL, portNumber, "")) {
					
					// Append to the front of server URL if protocol does not exist
					if (!serverURL.contains("http://") && !serverURL.contains("https://")) {
						serverURL = "http://" + serverURL;
					}
	
					// Save the data to application data and internal storage
					IPCamData data = IPCamData.getInstance();
					data.setCameraName(cameraName);
					data.setServerURL(serverURL);
					data.setPortNumberOrPublic(portNumber);
					data.setUserName(userName);
					data.setPassword(password);
					data.setScreenMode(VideoView.SIZE_4_3);
					
					// Save the IP camera type
					if (mjpegCamRadioButton.isChecked()) {
						data.setIPCamType(IPCamType.MJPEG);
					} else if (h264CamRadioButton.isChecked()) {
						data.setIPCamType(IPCamType.H264);
					} else {
						data.setIPCamType(IPCamType.MJPEG);
					}
	
					// Update the main activity's view
					// Invoke the handler method from the main activity
					((MainActivity) IPCamSettingsDialogFragment.this.getActivity())
							.okButtonClickConnectionSettingsHandler();
					if (IPCamSettingsDialogFragment.this.getDialog() != null)
						IPCamSettingsDialogFragment.this.getDialog().dismiss();
				}
			}
		}
	}

	private boolean validateInputData(String cameraName, String serverURL,
			String portNumber, String userName, String password) {

		if (cameraName == null || cameraName.equals("")) {
			Toast toast = Toast.makeText(IPCamSettingsDialogFragment.this.getDialog().getContext(), 
					R.string.camera_name_empty, Toast.LENGTH_LONG);
			toast.show();
			return false;
		} else if (serverURL == null || serverURL.equals("")) {
			Toast toast = Toast.makeText(IPCamSettingsDialogFragment.this.getDialog().getContext(), 
					R.string.url_empty, Toast.LENGTH_LONG);
			toast.show();
			return false;
		} else if (portNumber == null || portNumber.equals("")) {
			Toast toast = Toast.makeText(IPCamSettingsDialogFragment.this.getDialog().getContext(), 
					R.string.port_number_empty, Toast.LENGTH_LONG);
			toast.show();
			return false;
		}
		/*
		 * } else if (userName == null || userName.equals("")) { Toast toast =
		 * Toast
		 * .makeText(IPCamSettingsDialogFragment.this.getDialog().getContext(),
		 * R.string.user_name_empty, Toast.LENGTH_LONG); toast.show(); return
		 * false; }
		 */
		/*
		 * } else if (password == null || password.equals("")) { Toast toast =
		 * Toast
		 * .makeText(IPCamSettingsDialogFragment.this.getDialog().getContext(),
		 * "Your password field cannot be empty.", Toast.LENGTH_LONG);
		 * toast.show(); return false; }
		 */
		return true;
	}

	private boolean validateDuplicateCamera(String serverURL, String portNumber, String publicCameraName) {
		IPCamData data = IPCamData.getInstance();
		IPCamera[] foscamArray = data.getIPCamArray();

		for (int i = 0; i < foscamArray.length; i++) {
			if (foscamArray[i] != null && i != data.getCurrentIPCamIndex()) {
				if (foscamArray[i].portNumberOrPublic.equals(IPCamera.PUBLIC)) {
					if (!foscamArray[i].cameraName.equals("") && 
							foscamArray[i].cameraName.equals(publicCameraName)) {
						int stringId = R.string.duplicate_public_ipcam;
						Toast toast = Toast.makeText(
								IPCamSettingsDialogFragment.this.getDialog().getContext(), stringId,
								Toast.LENGTH_LONG);
						toast.show();
						// There is a duplicate
						return false;
					}
				} else if (!serverURL.equals("") && serverURL.equals(foscamArray[i].serverURL) && 
						portNumber.equals(foscamArray[i].portNumberOrPublic)) {
					int stringId = R.string.duplicate_ipcam;
					Toast toast = Toast.makeText(
							IPCamSettingsDialogFragment.this.getDialog().getContext(), stringId,
							Toast.LENGTH_LONG);
					toast.show();
					// There is a duplicate
					return false;
				}
			}
		}
		// There are no duplicate
		return true;
	}

	private void disableEnableControls() {
		RadioButton publicCamRadioButton = (RadioButton) dialogView
				.findViewById(R.id.publicCamRadioButton);
		Spinner publicIPCamSpinner = (Spinner) dialogView
				.findViewById(R.id.publicCamSpinner);
		
		EditText nameEditText = (EditText) dialogView
				.findViewById(R.id.nameEditText);
		EditText addressEditText = (EditText) dialogView
				.findViewById(R.id.addressEditText);
		EditText portNumEditText = (EditText) dialogView
				.findViewById(R.id.portNumEditText);
		EditText userNameEditText = (EditText) dialogView
				.findViewById(R.id.userNameEditText);
		EditText passwordEditText = (EditText) dialogView
				.findViewById(R.id.passwordEditText);
		
		// If public cam checkbox is checked
		if (publicCamRadioButton.isChecked()) {
			// Enable public cam spinner
			publicIPCamSpinner.setEnabled(true);
			// Disable UI controls for user's IP camera
			nameEditText.setEnabled(false);
			addressEditText.setEnabled(false);
			portNumEditText.setEnabled(false);
			userNameEditText.setEnabled(false);
			passwordEditText.setEnabled(false);
			
			nameEditText.setText("");
			addressEditText.setText("");
			portNumEditText.setText("");
			userNameEditText.setText("");
			passwordEditText.setText("");
			
			publicIPCamSpinner.setFocusable(true);
			publicIPCamSpinner.setFocusableInTouchMode(true);
			publicIPCamSpinner.requestFocus();
		// Else use user's IP camera
		} else {
			// Disable public cam spinner
			publicIPCamSpinner.setEnabled(false);
			// Enable UI controls for user's IP camera
			nameEditText.setEnabled(true);
			addressEditText.setEnabled(true);
			portNumEditText.setEnabled(true);
			userNameEditText.setEnabled(true);
			passwordEditText.setEnabled(true);
			
			nameEditText.setFocusable(true);
			nameEditText.setFocusableInTouchMode(true);
			nameEditText.requestFocus();
		}
	}

	private class cancelButtonClickListener implements OnClickListener {

		public void onClick(View arg0) {
			IPCamSettingsDialogFragment.this.getDialog().cancel();
		}
	}

	private class clearButtonClickListener implements OnClickListener {

		public void onClick(View arg0) {
			RadioButton mjpegCamRadioButton = (RadioButton) dialogView
					.findViewById(R.id.mjpegRadioButton);
			EditText nameEditText = (EditText) dialogView
					.findViewById(R.id.nameEditText);
			EditText addressEditText = (EditText) dialogView
					.findViewById(R.id.addressEditText);
			EditText portNumEditText = (EditText) dialogView
					.findViewById(R.id.portNumEditText);
			EditText userNameEditText = (EditText) dialogView
					.findViewById(R.id.userNameEditText);
			EditText passwordEditText = (EditText) dialogView
					.findViewById(R.id.passwordEditText);

			nameEditText.setText("");
			addressEditText.setText("");
			portNumEditText.setText("");
			userNameEditText.setText("");
			passwordEditText.setText("");
			mjpegCamRadioButton.setChecked(true);

			String cameraName = nameEditText.getText().toString();
			String serverURL = addressEditText.getText().toString();
			String portNumber = portNumEditText.getText().toString();
			String userName = userNameEditText.getText().toString();
			String password = passwordEditText.getText().toString().trim();
			
			// Save the data to application data and internal storage
			IPCamData data = IPCamData.getInstance();
			data.setCameraName(cameraName);
			data.setServerURL(serverURL);
			data.setPortNumberOrPublic(portNumber);
			data.setUserName(userName);
			data.setPassword(password);
			data.setIPCamType(IPCamType.MJPEG);
			data.setScreenMode(VideoView.SIZE_4_3);

			// Update the main activity's view
			// Invoke the handler method from the main activity
			((MainActivity) IPCamSettingsDialogFragment.this.getActivity())
					.clearButtonClickConnectionSettingsHandler();
			IPCamSettingsDialogFragment.this.getDialog().dismiss();
		}
	}

	private class publicCamCheckBoxCheckedChangeListener implements
			OnCheckedChangeListener {

		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			
			disableEnableControls();
		}

	}
}
