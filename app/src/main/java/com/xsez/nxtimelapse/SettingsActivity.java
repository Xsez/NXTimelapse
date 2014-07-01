package com.xsez.nxtimelapse;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import com.xsez.nxtimelapse.R;
import android.content.*;
import android.widget.CheckBox;
import android.widget.EditText;


public class SettingsActivity extends Activity {

    boolean autoConnect = false;
    boolean useDrive = false;
    boolean useCamera = false;
    String macAdress = "";
    int driveMotors = 0;
    int cameraMotor = 0;
    int driveSpeed = 0;
    int cameraSpeed = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Context context = this;
        SharedPreferences sharedPref = context.getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);



        autoConnect = sharedPref.getBoolean("auto_connect", false);
        useDrive = sharedPref.getBoolean("use_drive", true);
        useCamera = sharedPref.getBoolean("use_camera", false);
        macAdress = sharedPref.getString("mac_address", "");
        driveMotors = sharedPref.getInt("drive_motors", 0);
        cameraMotor = sharedPref.getInt("camera_motor", 0);
        driveSpeed = sharedPref.getInt("drive_speed", 0);
        cameraSpeed = sharedPref.getInt("camera_speed", 0);


        EditText etMacAddress = (EditText)findViewById(R.id.editTextMacAddress);
        etMacAddress.setText(macAdress);
        CheckBox cbAutoConnect = (CheckBox)findViewById(R.id.checkBoxAutoConnect);
        cbAutoConnect.setChecked(autoConnect);
        CheckBox cbUseDrive = (CheckBox)findViewById(R.id.checkBoxUseDrive);
        cbUseDrive.setChecked(useDrive);
        CheckBox cbUseCamera = (CheckBox)findViewById(R.id.checkBoxUseCamera);
        cbUseCamera.setChecked(useCamera);
    }

    public void onPause() {
        super.onPause();
        EditText etMacAddress = (EditText)findViewById(R.id.editTextMacAddress);
        macAdress = etMacAddress.getText().toString();
        CheckBox cbAutoConnect = (CheckBox)findViewById(R.id.checkBoxAutoConnect);
        autoConnect = cbAutoConnect.isChecked();
        CheckBox cbUseDrive = (CheckBox)findViewById(R.id.checkBoxUseDrive);
        useDrive = cbUseDrive.isChecked();
        CheckBox cbUseCamera = (CheckBox)findViewById(R.id.checkBoxUseCamera);
        useCamera = cbUseCamera.isChecked();

        SharedPreferences sharedPref = this.getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean("auto_connect", autoConnect);
        editor.putBoolean("use_drive", useDrive);
        editor.putBoolean("use_camera", useCamera);
        editor.putString("mac_address", macAdress);
        editor.putInt("drive_motors", driveMotors);
        editor.putInt("camera_motor", cameraMotor);
        editor.putInt("drive_speed", driveSpeed);
        editor.putInt("camera_speed", cameraSpeed);
        editor.commit();
    }

    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my_activity2, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }*/
}
