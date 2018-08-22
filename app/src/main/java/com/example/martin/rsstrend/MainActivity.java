package com.example.martin.rsstrend;


import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.estimote.sdk.SystemRequirementsChecker;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity{

    private TextView rssiTextView;
    private TextView ellapsedTimeTextView;
    private EditText fileNameEditText;
    private Button startButton;
    private ProgressBar scanningProgressBar;

    public static final String LOG_TAG = MainActivity.class.getSimpleName();

    static final int REQUEST_CODE_ENABLE_BLE = 1001;
    private static final byte EDDYSTONE_URL_FRAME_TYPE = 0x10;
    private static final byte DATA_PREAMBLE = (byte) 0xAA;
    private static final ParcelUuid EDDYSTONE_SERVICE_UUID = ParcelUuid.fromString("0000FEAA-0000-1000-8000-00805F9B34FB");

    private FileOutputStream fileOutputStream;
    private FileOutputStream stepFileOutputStream;

    private static long seconds = 0;
    private static String fileName = "";
    private static boolean isScanning = false;

    private Handler handler;
    private Runnable runnable;

    private static final ScanFilter EDDYSTONE_SCAN_FILTER = new ScanFilter.Builder()
            .setServiceUuid(EDDYSTONE_SERVICE_UUID)
            .build();


    private final List<ScanFilter> SCAN_FILTERS = buildScanFilters();

    private static List<ScanFilter> buildScanFilters() {
        List<ScanFilter> scanFilters = new ArrayList<>();
        scanFilters.add(EDDYSTONE_SCAN_FILTER);
        return scanFilters;
    }

    private static final int MY_PERMISSION_REQUEST_READ_CONTACTS = 1;

    private ScanSettings SCAN_SETTINGS =
            new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setReportDelay(0)
                    .build();

    private BluetoothManager btManager;
    private BluetoothAdapter btAdapter;
    private BluetoothLeScanner bleScanner;
    private Button stepNoteButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rssiTextView = findViewById(R.id.rssi_textview);
        ellapsedTimeTextView = findViewById(R.id.ellapsed_time_textview);
        startButton = findViewById(R.id.start_button);
        scanningProgressBar = findViewById(R.id.progress_bar);
        fileNameEditText = findViewById(R.id.fileName_editText);
        scanningProgressBar.setVisibility(View.GONE);
        stepNoteButton = findViewById(R.id.step_note_button);

        stepNoteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                long time = seconds;
                fileNameEditText.setHint("Enter point note");
                fileNameEditText.setEnabled(true);
                String note = time + "\t" +fileNameEditText.getText().toString()+"\n";
                try{
                    stepFileOutputStream.write(note.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });


        handler = new Handler();

        btManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        bleScanner = btAdapter.getBluetoothLeScanner();

        if(btAdapter == null || bleScanner == null){
            Toast.makeText(this,"Either bluetooth or BLE not supported!",Toast.LENGTH_SHORT);
            finish();
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSION_REQUEST_READ_CONTACTS);
        }

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isScanning){
                    stopScan();
                }else{
                    startScan();
                }
            }
        });

        fileNameEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if(keyEvent.getAction() == KeyEvent.ACTION_DOWN && i == KeyEvent.KEYCODE_ENTER){
                    startScan();
                    return true;
                }else{
                    return false;
                }

            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        SystemRequirementsChecker.checkWithDefaultDialogs(this);
    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            ScanRecord scanRecord = result.getScanRecord();

            int rssi = result.getRssi();
            String record = seconds + "\t" +rssi+"\n";
            rssiTextView.setText(rssi+"");

            try{
                fileOutputStream.write(record.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }


        }
    };

    private void startScan(){
        if(fileNameEditText.getText().toString().trim() != null && !fileNameEditText.getText().toString().trim().isEmpty()){
            fileName = fileNameEditText.getText().toString().trim()+".txt";
            String stepNoteFN = fileNameEditText.getText().toString()+"_stepNote"+".txt";
            try {
                fileOutputStream = openFileOutput(fileName,MODE_APPEND);
                stepFileOutputStream = openFileOutput(stepNoteFN,MODE_APPEND);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

           bleScanner.startScan(SCAN_FILTERS,SCAN_SETTINGS, scanCallback);
            startTimer();
            scanningProgressBar.setVisibility(View.VISIBLE);
            startButton.setText("STOP");
            fileNameEditText.setEnabled(false);
            fileNameEditText.setText("");
            isScanning = true;
        }else{
            Toast.makeText(this,"Please enter file name",Toast.LENGTH_SHORT).show();
            return;
        }
    }

    private void startTimer(){
        runnable = new Runnable() {
            @Override
            public void run() {
                seconds+=100;
                int minutes = (int) (seconds/60000);
                int sec = (int)(seconds/1000)%60;
                String time = String.format("%02d",minutes)+":"+String.format("%02d",sec);
                ellapsedTimeTextView.setText(time);
                handler.postDelayed(this,100);
            }
        };
        handler.postDelayed(runnable,100);
    }

    private void stopTimer(){
        handler.removeCallbacks(runnable);
    }

    private void stopScan(){
        bleScanner.stopScan(scanCallback);
        stopTimer();
        scanningProgressBar.setVisibility(View.GONE);
        startButton.setText("START");
        fileNameEditText.setEnabled(true);
        try{
            fileOutputStream.close();
            stepFileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        isScanning = false;
    }
}