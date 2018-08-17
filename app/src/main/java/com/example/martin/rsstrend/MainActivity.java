package com.example.martin.rsstrend;


import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.SystemRequirementsChecker;
import com.estimote.sdk.eddystone.Eddystone;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;


public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private BeaconManager beaconManager;
    BeaconManager.RangingListener rangingListener;
    private Region region;

    private static boolean isRanging = false;

    private EditText fileNameEditText;
    private static Button startButton;
    private static Button stopButton;

    private FileOutputStream fileOutputStream;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        beaconManager = new BeaconManager(this);

        rangingListener = new BeaconManager.RangingListener() {
            @Override
            public void onBeaconsDiscovered(Region region, List<Beacon> list) {
                if(!list.isEmpty()){
                    Beacon gotBeacon = list.get(0);
                    String fileName = fileNameEditText.getText().toString().trim() + ".txt";
                    String address = gotBeacon.getMacAddress().toString();
                    int rssi = gotBeacon.getRssi();


                    try {
                        fileOutputStream = new FileOutputStream(fileName,true);
                        FileWriter fileWriter;

                        try{
                            fileWriter = new FileWriter(fileOutputStream.getFD());
                            fileWriter.write(address+"\t"+rssi+"\n");
                            fileWriter.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }finally {
                            fileOutputStream.getFD().sync();
                            fileOutputStream.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        beaconManager.setRangingListener(rangingListener);

        startButton = (Button) findViewById(R.id.start_button);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(fileNameEditText.getText().toString().trim() == null){
                    Toast.makeText(MainActivity.this,"Enter file name",Toast.LENGTH_SHORT).show();
                    return;
                }
                beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
                    @Override
                    public void onServiceReady() {
                        beaconManager.startRanging(region);
                        isRanging = true;
                    }
                });
            }
        });

        stopButton = (Button) findViewById(R.id.stop_button);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                beaconManager.stopRanging(region);
                isRanging = false;
            }
        });


        region = new Region("ranged region",UUID.fromString(BeaconContract.Beacon3.UUID), BeaconContract.Beacon3.major, BeaconContract.Beacon3.minor);


    }

    static {
        if(isRanging){
            startButton.setClickable(false);
            stopButton.setClickable(true);
        }else{
            startButton.setClickable(true);
            stopButton.setClickable(false);
        }
    }



    @Override
    protected void onResume() {
        super.onResume();
        SystemRequirementsChecker.checkWithDefaultDialogs(this);
    }

    @Override
    protected void onPause() {
        beaconManager.stopRanging(region);
        super.onPause();
    }
}
