package com.xsez.nxtimelapse;

import android.app.Activity;
import android.bluetooth.*;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.*;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;
import android.content.Intent;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;


public class MyActivity extends Activity {

    //INIT
    BluetoothAdapter localAdapter;
    BluetoothSocket socket_nxt;

    boolean buttonLeftDown = false;
    boolean buttonRightDown = false;


    boolean autoConnect = false;
    boolean useDrive = false;
    boolean useCamera = false;
    String macAdress = "";
    int driveMotors = 0;
    int cameraMotor = 0;
    int driveSpeed = 0;
    int cameraSpeed = 0;

    double totalRunTime=0;
    double totalDriveSteps=0;
    double totalCameraSteps=0;

    double timeStarted = 0;


    boolean connected = false;
    boolean taskRunning = false;
    int steps = 1;
    int currentStep = 0;
    int stepDriveStep = 0;
    int driveCurrentTacho = 0;
    int cameraCurrentTacho = 0;
    int stepDriveCurrentStep = 0;
    //DATA ARRAY (1. Column [0]: ; 2. Column [1]: ; 3. Column [2]: ; 4. Column [3]: ; 5. Column [4]: ; 6. Column [5]: driveTachoDiffAbs; 7. Column [6]: cameraTachoDiffAbs)
    int taskData[][] = new int[20][7];
    float battery = 0;


    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            TextView tvTimeLeft = (TextView)findViewById(R.id.textViewTimeLeft);
            TextView tvCurrentStep = (TextView)findViewById(R.id.textViewCurrentStep);
            TextView tvDriveTacho = (TextView)findViewById(R.id.textViewDriveTacho);
            TextView tvCameraTacho = (TextView)findViewById(R.id.textViewCameraTacho);
            TextView tvBattery = (TextView)findViewById(R.id.textViewBattery);
            EditText etStep1Tacho = (EditText)findViewById(R.id.editTextStep1DriveTacho);
            EditText etStep1Time = (EditText)findViewById(R.id.editTextStep1Time);
            ProgressBar progressBar = (ProgressBar)findViewById(R.id.progressBar);
            float progress;
            double time;
            time = System.currentTimeMillis()-timeStarted;

            if (taskRunning==true) {
                progress = (((float)time / (float)totalRunTime) *100);
                progressBar.setProgress((int)(progress));

                tvTimeLeft.setText(Double.toString((time)/1000) + "/" + Double.toString(totalRunTime/1000));
                tvCurrentStep.setText(Integer.toString(currentStep) + "/" + Integer.toString(steps));
                tvDriveTacho.setText(Integer.toString(driveCurrentTacho) + "/" + Double.toString(totalDriveSteps));
                tvCameraTacho.setText(Integer.toString(cameraCurrentTacho));
                tvBattery.setText(Double.toString(roundDown2((double)battery)));
            } else {
                progress = 0;
                progressBar.setProgress((int)(progress));

                tvTimeLeft.setText("");
                tvCurrentStep.setText("");
                tvDriveTacho.setText("");
                tvCameraTacho.setText(Integer.toString(cameraCurrentTacho));
                tvBattery.setText(Double.toString(roundDown2((double)battery)));
            }


        }
    };





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        //Activate Spinners
        String[] items = {"s", "min", "h"};
        final Spinner spinnerStep1Time = (Spinner)findViewById(R.id.spinnerStep1Time);

        ArrayAdapter<String> spinnerMenuList = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items);
        spinnerMenuList.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStep1Time.setAdapter(spinnerMenuList);


        //Make UI Invisible
        //Whole ScrollView
        View scrollView = (ScrollView)findViewById(R.id.scrollView);
        scrollView.setVisibility(View.GONE);
        Button buttonRemoveStep = (Button)findViewById(R.id.buttonRemoveStep);
        buttonRemoveStep.setVisibility(View.INVISIBLE);

        View.OnTouchListener touchDriveLeft = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch ( motionEvent.getAction() ) {
                    case MotionEvent.ACTION_DOWN: {

                        driveForward( 400);


                    return true;
                    }
                    case MotionEvent.ACTION_UP: {
                        driveStop();
                        EditText etStep1DriveTacho = (EditText) findViewById(R.id.editTextStep1DriveTacho);
                        etStep1DriveTacho.setText(Integer.toString(getTachocount((byte) 0)));
                    }
                    return true;
                }
                return false;
            }
        };
        View.OnTouchListener touchDriveRight = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch ( motionEvent.getAction() ) {
                    case MotionEvent.ACTION_DOWN: {
                        if (buttonRightDown == false) {
                            driveBackward( 400);
                            buttonRightDown = true;
                        }
                    return true;
                    }
                    case MotionEvent.ACTION_UP: {
                        driveStop();
                        EditText etStep1DriveTacho = (EditText)findViewById(R.id.editTextStep1DriveTacho);
                        etStep1DriveTacho.setText(Integer.toString(getTachocount((byte)0)));
                        buttonRightDown = false;
                    }
                    return true;
                }
                return false;
            }
        };

        Button buttonStep1DriveLeft = (Button)findViewById(R.id.buttonStep1DriveLeft);
        Button buttonStep1DriveRight = (Button)findViewById(R.id.buttonStep1DriveRight);
        buttonStep1DriveLeft.setOnTouchListener(touchDriveLeft);
        buttonStep1DriveRight.setOnTouchListener(touchDriveRight);

        SharedPreferences sharedPref = this.getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);



        autoConnect = sharedPref.getBoolean("auto_connect", false);
        useDrive = sharedPref.getBoolean("use_drive", true);
        useCamera = sharedPref.getBoolean("use_camera", false);
        macAdress = sharedPref.getString("mac_address", "");
        driveMotors = sharedPref.getInt("drive_motors", 0);
        cameraMotor = sharedPref.getInt("camera_motor", 0);
        driveSpeed = sharedPref.getInt("drive_speed", 0);
        cameraSpeed = sharedPref.getInt("camera_speed", 0);



        //Enable Bluetooth
        enableBT();

        //tvCurrentStep.setText(stepDriveCurrentStep);

    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        if (connected == true) {
            MenuItem connectMenu = menu.findItem(R.id.action_connect);
            connectMenu.setTitle("Disconnect");
        } else {
            MenuItem connectMenu = menu.findItem(R.id.action_connect);
            connectMenu.setTitle("Connect");
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity2.class);
            startActivity(intent);
        } else if (id == R.id.action_connect) {
            connect();
        } else if (id == R.id.action_about) {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }



    public void leftButton (View view) {
        motorForward((byte)0, 200);
        motorForward((byte)1, 200);
    }

    public void rightButton (View view) {
        motorBackward((byte) 0, 200);
        motorBackward((byte) 1, 200);
    }

    public void startButton (View view) {
        readTextBoxes();
        sendTask();
        startTask();

    }

    public void addStep(View view) {
        Resources res = getResources();
        String name = getPackageName();

        LinearLayout.LayoutParams layoutParams3 = new TableRow.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, (float).3);
        LinearLayout.LayoutParams layoutParams4 = new TableRow.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, (float).4);
        LinearLayout.LayoutParams layoutParamsFull = new TableRow.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, (float)1);

        String[] spinnerItems = {"s", "min", "h"};

        int index = (steps*4);
        steps++;

        TableLayout table = (TableLayout) findViewById( R.id.tableLayout);
        int numRows = table.getChildCount();
        //Row1
        TableRow row1 = new TableRow(this);
        TextView textViewRow1 = new TextView(this);
        textViewRow1.setText("Step " + steps + " - Time:");


        textViewRow1.setLayoutParams(layoutParams3);

        EditText editTextRow1 = new EditText(this);
        editTextRow1.setText(Integer.toString(60));
        int idTime = 0;
        idTime = res.getIdentifier("editTextStep" + Integer.toString(steps) + "Time", "id", name);
        editTextRow1.setId(idTime);
        layoutParams3.weight = (float).4;
        editTextRow1.setLayoutParams(layoutParams4);

        Spinner spinnerRow1 = new Spinner(this);
        int idSpinner = 0;
        idSpinner = res.getIdentifier("spinnerStep" + Integer.toString(steps) + "Time", "id", name);
        spinnerRow1.setId(idSpinner);
        ArrayAdapter<String> spinnerMenuList = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, spinnerItems);
        spinnerMenuList.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRow1.setAdapter(spinnerMenuList);
        layoutParams3.weight = (float).3;
        spinnerRow1.setLayoutParams(layoutParams3);

        row1.addView(textViewRow1);
        row1.addView(editTextRow1, 1);
        row1.addView(spinnerRow1, 2);
        table.addView( row1, index);

        //Row2
        TableRow row2 = new TableRow(this);
        TextView textViewRow2 = new TextView(this);
        textViewRow2.setText("Step " + steps + " - Drive:");
        textViewRow2.setLayoutParams(layoutParams3);

        EditText editTextRow2 = new EditText(this);
        editTextRow2.setText(Integer.toString(steps*200));
        int idDriveTacho = 0;
        idDriveTacho = res.getIdentifier("editTextStep" + Integer.toString(steps) + "DriveTacho", "id", name);
        editTextRow2.setId(idDriveTacho);
        editTextRow2.setLayoutParams(layoutParams4);

        TextView textView2Row2 = new TextView(this);
        textView2Row2.setText("Tachocount");
        textView2Row2.setLayoutParams(layoutParams3);

        row2.addView(textViewRow2);
        row2.addView(editTextRow2, 1);
        row2.addView(textView2Row2, 2);
        table.addView( row2, index+1);
        //Row3
        /*TableRow row3 = new TableRow(this);
        Button button1Row3 = new Button(this);
        button1Row3.setText("Left");
        Button button2Row3 = new Button(this);
        button2Row3.setText("Right");
        row3.addView(button1Row3);
        row3.addView(button2Row3, 1);
        table.addView( row3, index+2);*/
        //Row4
        TableRow row4 = new TableRow(this);
        TextView textViewRow4 = new TextView(this);
        textViewRow4.setText("Step " + steps + " - Camera:");
        textViewRow4.setLayoutParams(layoutParams3);

        EditText editTextRow4 = new EditText(this);
        editTextRow4.setText(Integer.toString(0));
        int idCameraTacho = 0;
        idCameraTacho = res.getIdentifier("editTextStep" + Integer.toString(steps) + "CameraTacho", "id", name);
        editTextRow4.setId(idCameraTacho);
        editTextRow4.setLayoutParams(layoutParams4);

        TextView textView2Row4 = new TextView(this);
        textView2Row4.setText("Tachocount");
        textView2Row4.setLayoutParams(layoutParams3);

        row4.addView(textViewRow4);
        row4.addView(editTextRow4, 1);
        row4.addView(textView2Row4, 2);
        table.addView( row4, index+2);
        //Row5
        /*TableRow row5 = new TableRow(this);
        Button button1Row5 = new Button(this);
        button1Row5.setText("Left");
        Button button2Row5 = new Button(this);
        button2Row5.setText("Right");
        row5.addView(button1Row5);
        row5.addView(button2Row5, 1);
        table.addView( row5, index+4);*/
        //Row6
        TableRow row6 = new TableRow(this);
        //Space spaceRow6 = new Space(this);
        ProgressBar progressBarRow6 = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        row6.addView(progressBarRow6);
        table.addView(row6, index+3);
        TableRow.LayoutParams params = (TableRow.LayoutParams)progressBarRow6.getLayoutParams();
        params.span = 3;
        params.weight = 1;
        progressBarRow6.setLayoutParams(params);


        Button buttonRemoveStep = (Button)findViewById(R.id.buttonRemoveStep);
        buttonRemoveStep.setVisibility(View.VISIBLE);


    }

    public void removeStep(View view) {
        TableLayout table = (TableLayout) findViewById( R.id.tableLayout);
        steps--;
        int index = (steps*4);
        table.removeViews(index, 4);
        if (steps == 1) {
            Button buttonRemoveStep = (Button)findViewById(R.id.buttonRemoveStep);
            buttonRemoveStep.setVisibility(View.INVISIBLE);
        }
    }

    public void enableBT(){
        localAdapter=BluetoothAdapter.getDefaultAdapter();
        //If Bluetooth not enable then do it
        if(localAdapter.isEnabled()==false){
            localAdapter.enable();
            while(!(localAdapter.isEnabled())){

            }
        } else {
        }

    }

    public void readTextBoxes () {
        int lastDriveTacho = 0;
        int lastCameraTacho = 0;
        totalRunTime = 0;
        totalCameraSteps = 0;
        totalDriveSteps = 0;
        for (int i = 0; i < steps; i++) {
            Resources res = getResources();
            String name = getPackageName();
            int idDriveTacho = 0;
            idDriveTacho = res.getIdentifier("editTextStep" + Integer.toString(i+1) + "DriveTacho", "id", name);
            int idCameraTacho = 0;
            idCameraTacho = res.getIdentifier("editTextStep" + Integer.toString(i+1) + "CameraTacho", "id", name);
            int idTime = 0;
            idTime = res.getIdentifier("editTextStep" + Integer.toString(i+1) + "Time", "id", name);
            int idSpinner = 0;
            idSpinner = res.getIdentifier("spinnerStep" + Integer.toString(i+1) + "Time", "id", name);
            EditText etStepDriveTacho = (EditText)findViewById(idDriveTacho);
            EditText etStepCameraTacho = (EditText)findViewById(idCameraTacho);
            EditText etStepTime = (EditText)findViewById(idTime);
            Spinner spinner = (Spinner)findViewById(idSpinner);
            int driveDelay = 0;
            int cameraDelay = 0;
            int time = 0;
            int driveTacho = 0;
            int cameraTacho = 0;
            int driveTachoDiffAbs = 0;
            int cameraTachoDiffAbs = 0;

            time = Integer.parseInt(etStepTime.getText().toString()) * 1000;
            if (spinner.getSelectedItemId() == 1) {
                time = time * 60;
            } else if (spinner.getSelectedItemId() == 2) {
                time = time * 60 * 60;
            }

            driveTacho = Integer.parseInt(etStepDriveTacho.getText().toString());
            driveTachoDiffAbs = Math.abs(driveTacho - lastDriveTacho);
            if (driveTachoDiffAbs == 0) {
                driveDelay = time;
            } else {
                driveDelay = time / driveTachoDiffAbs ;
            }

            cameraTacho = Integer.parseInt(etStepCameraTacho.getText().toString());
            cameraTachoDiffAbs = Math.abs(cameraTacho - lastCameraTacho);
            if (cameraTachoDiffAbs == 0) {
                cameraDelay = time;
            } else {
                cameraDelay = time / cameraTachoDiffAbs ;
            }

            taskData[i][0]= driveTacho;
            taskData[i][1]= driveDelay;
            taskData[i][2]= cameraTacho;
            taskData[i][3]= cameraDelay;
            taskData[i][4] = time;
            taskData[i][5] = driveTachoDiffAbs;
            taskData[i][6] = cameraTachoDiffAbs;
            totalRunTime += time;
            totalDriveSteps += driveTachoDiffAbs;
            totalCameraSteps += cameraTachoDiffAbs;

            lastDriveTacho = driveTacho;
            lastCameraTacho = cameraTacho;

        }
    }

    //NXT BT Commands
    //connect to NXT
    public void connect() {
        if (connected == false) {
            final String nxtMac = "00:16:53:17:26:D6";
            BluetoothDevice nxt = localAdapter.getRemoteDevice(nxtMac);
            try {
                socket_nxt = nxt.createRfcommSocketToServiceRecord(UUID
                        .fromString("00001101-0000-1000-8000-00805F9B34FB"));
                socket_nxt.connect();
                connected = true;

                View scrollView = (ScrollView)findViewById(R.id.scrollView);
                scrollView.setVisibility(View.VISIBLE);
                View buttonRemoveStep = (Button)findViewById(R.id.buttonRemoveStep);
                //buttonRemoveStep.setEnabled(false);

                handler.postDelayed(stepStatusRunnable, 1000);
                Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();

            } catch (IOException e) {
                connected = false;
                Toast.makeText(getApplicationContext(), "Could not connect\nNXT turned on?", Toast.LENGTH_SHORT).show();
            }

        } else {

            try {
                DataOutputStream out = new DataOutputStream(socket_nxt.getOutputStream());
                out.writeByte(-1);
                socket_nxt.close();
                connected = false;
                View scrollView = (ScrollView)findViewById(R.id.scrollView);
                scrollView.setVisibility(View.INVISIBLE);
                handler.removeCallbacks(stepStatusRunnable);
                Toast.makeText(getApplicationContext(), "Disconnected", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {

            }
        }
    }

    public int getTachocount(byte motor) {
        byte tachoByte = 10;

        try {
            DataOutputStream out = new DataOutputStream(socket_nxt.getOutputStream());
            out.writeByte(tachoByte);
            out.writeByte(motor);
            out.flush();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        int tachocount = 0;
        try {
            DataInputStream in = new DataInputStream(socket_nxt.getInputStream());
            tachocount = in.readInt();


        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return tachocount;
    }

    public void rotateTo(byte motor, int tacho) {
        byte rotateToByte = 11;

        try {
            DataOutputStream out = new DataOutputStream(socket_nxt.getOutputStream());
            out.writeByte(rotateToByte);
            out.writeByte(motor);
            out.writeInt(tacho);
            out.flush();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        byte success;
        try {
            DataInputStream in = new DataInputStream(socket_nxt.getInputStream());
            success = in.readByte();
            if (success == 1) {
                Toast.makeText(getApplicationContext(), "Rotating to: " + tacho, Toast.LENGTH_SHORT).show();
            }


        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void sendTask() {
        byte sendTaskByte = 20;
        try {
            DataOutputStream out = new DataOutputStream(socket_nxt.getOutputStream());
            out.writeByte(sendTaskByte);
            out.writeInt(steps);

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        for (int i = 0; i <= steps; i++) {
            try {
                DataOutputStream out = new DataOutputStream(socket_nxt.getOutputStream());
                out.writeInt(taskData[i][0]);
                out.writeInt(taskData[i][1]);
                out.writeInt(taskData[i][2]);
                out.writeInt(taskData[i][3]);

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        try {
            DataOutputStream out = new DataOutputStream(socket_nxt.getOutputStream());
            out.flush();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        byte success;
        try {
            DataInputStream in = new DataInputStream(socket_nxt.getInputStream());
            success = in.readByte();
            if (success == 1) {
                Toast.makeText(getApplicationContext(), "Task sent successfully", Toast.LENGTH_SHORT).show();
            }


        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void startTask() {
        byte startTaskByte = 21;
        try {
            DataOutputStream out = new DataOutputStream(socket_nxt.getOutputStream());
            out.writeByte(startTaskByte);
            out.flush();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        byte success;
        try {
            DataInputStream in = new DataInputStream(socket_nxt.getInputStream());
            success = in.readByte();
            if (success == 1) {
                Toast.makeText(getApplicationContext(), "Task sent successfully", Toast.LENGTH_SHORT).show();
            }


        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        timeStarted = System.currentTimeMillis();
        taskRunning = true;
    }

    public boolean getStepStatus() {
        byte stepStatusByte = 2;
        boolean active = false;
        try {
            DataOutputStream out = new DataOutputStream(socket_nxt.getOutputStream());
            out.writeByte(stepStatusByte);
            out.flush();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            DataInputStream in = new DataInputStream(socket_nxt.getInputStream());
            active = in.readBoolean();
            currentStep = in.readInt();
            if (active == true) {
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return active;
    }

    public float getBattery() {
        float battery = 0;
        byte batteryByte = 5;
        try {
            DataOutputStream out = new DataOutputStream(socket_nxt.getOutputStream());
            out.writeByte(batteryByte);
            out.flush();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            DataInputStream in = new DataInputStream(socket_nxt.getInputStream());
            battery = in.readFloat();

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return battery;
    }

    public boolean motorForward(byte motor, int speed) {
        byte motorForwardByte = 50;
        boolean success = false;
        try {
            DataOutputStream out = new DataOutputStream(socket_nxt.getOutputStream());
            out.writeByte(motorForwardByte);
            out.writeByte(motor);
            out.writeInt(speed);
            out.flush();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            DataInputStream in = new DataInputStream(socket_nxt.getInputStream());
            success = in.readBoolean();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return success;
    }

    public void motorBackward(byte motor, int speed) {
        byte motorBackwardByte = 51;
        try {
            DataOutputStream out = new DataOutputStream(socket_nxt.getOutputStream());
            out.writeByte(motorBackwardByte);
            out.writeByte(motor);
            out.writeInt(speed);
            out.flush();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            DataInputStream in = new DataInputStream(socket_nxt.getInputStream());
            in.readBoolean();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void motorStop(byte motor) {
        byte motorStopByte = 52;
        try {
            DataOutputStream out = new DataOutputStream(socket_nxt.getOutputStream());
            out.writeByte(motorStopByte);
            out.writeByte(motor);
            out.flush();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            DataInputStream in = new DataInputStream(socket_nxt.getInputStream());
            in.readBoolean();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public boolean driveForward (int speed) {
        byte driveForwardByte = 53;
        boolean success = false;
        try {
            DataOutputStream out = new DataOutputStream(socket_nxt.getOutputStream());
            out.writeByte(driveForwardByte);
            out.writeInt(speed);
            out.flush();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            DataInputStream in = new DataInputStream(socket_nxt.getInputStream());
            success = in.readBoolean();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return success;
    }

    public boolean driveBackward (int speed) {
        byte driveBackwardByte = 54;
        boolean success = false;
        try {
            DataOutputStream out = new DataOutputStream(socket_nxt.getOutputStream());
            out.writeByte(driveBackwardByte);
            out.writeInt(speed);
            out.flush();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            DataInputStream in = new DataInputStream(socket_nxt.getInputStream());
            success = in.readBoolean();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return success;
    }

    public boolean driveStop () {
        byte driveStopByte = 55;
        boolean success = false;
        try {
            DataOutputStream out = new DataOutputStream(socket_nxt.getOutputStream());
            out.writeByte(driveStopByte);
            out.flush();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            DataInputStream in = new DataInputStream(socket_nxt.getInputStream());
            success = in.readBoolean();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return success;
    }




    public static double roundDown2(double d) {
        return (long) (d * 1e2) / 1e2;
    }

    //Timed Thread

    private Runnable stepStatusRunnable = new Runnable() {
        @Override
        public void run() {
      /* do what you need to do */
            //Toast.makeText(getApplicationContext(), "Starting Timer", Toast.LENGTH_SHORT).show();
            boolean active;
            active = getStepStatus();
            battery = getBattery();
            byte motorByte = 0;
            int tacho = 0;
            driveCurrentTacho = getTachocount(motorByte);

            handler.sendEmptyMessage(0);
      /* and here comes the "trick" */

            handler.postDelayed(this, 1000);

        }
    };




}
