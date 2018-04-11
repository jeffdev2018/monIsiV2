package com.monisi.jeff.monisi;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BeaconActivity extends AppCompatActivity {

    private final static String TAG = "BLEDiscovery";

    //constants
    private final static String MAJORS_STORAGE_FILE = "last_majorList_scanned.txt";
    private final static String UUID_STRING = "B5B182C7-EAB1-4988-AA99-B5C1517008D9";
    private final static String FILENAME_PREFIX = "ibeacon_data_";
    private final static String FILENAME_POSTFIX = ".txt";

    private final static int REQUEST_ENABLE_BLE = 100;
    private final static int REQUEST_FINE_LOCATION = 101;
    private static final char hexArray[] = "0123456789ABCDEF".toCharArray();

    //scan modes setting
    private static int scanMode = ScanSettings.SCAN_MODE_LOW_LATENCY;
    private static int newScanMode = ScanSettings.SCAN_MODE_LOW_LATENCY;

    //variables
    List<iBeacon> iBeaconList;  //list of beacon discovered during scanning
    private long preTime;
    private ListView mainListView;  //reference to UI list view
    //used to update the list of beacons
    ArrayAdapter<String> adapter;   //UI
    //used to update the list of beacons

    private FloatingActionButton startButton;   //reference to start button
    private FloatingActionButton stopButton;    //reference to stop button
    private Button requestInputButton;          //reference to set major button
    private Button modeButton;                  //reference to set mode button
    private TextView majorTextView;             //reference to a text view
    //used to update user-input major numbers
    //bluetooth stuffs
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private ScanSettings scanSettings;
    private List<ScanFilter> scanFilters;
    private List<Integer> majorList;
    private Handler mHandler;
    private BluetoothGatt mGatt;
    private boolean scanEnable = false;
    private boolean mScanning = false;
    //end bluetooth stuffs

    private int beaconIndex;    //index of each beacon entry in the log
    private String fileName;    //log file
    private HandlerFileReadWrite fileHandler;    //handler to readwrite object

    Context context;    //current app context

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beacon);

        Log.i(TAG, "ON CREATE");

        //get current context
        context = getApplicationContext();

        //check if the device supports BLE Features
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_LONG).show();
            finish();
        }

        //get bluetooth adapter
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            mBluetoothAdapter = bluetoothManager.getAdapter();
        }


        //initialize objects
        fileHandler = new HandlerFileReadWrite(context);
        fileName = FILENAME_PREFIX;
        beaconIndex = 0;

        /*get reference to view and button objects*/
        //get reference to list view
        mainListView = (ListView) findViewById(R.id.mainListView);

        //get reference to start button
        //and set the the tasks on <start> button
        startButton = (FloatingActionButton) findViewById(R.id.startButton);
       startButton.setVisibility(View.VISIBLE);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (scanEnable) {
                    //disable setMajor + mode buttons
                    requestInputButton.setAlpha(0.5f);
                    requestInputButton.setClickable(false);
                    modeButton.setAlpha(0.5f);
                    modeButton.setClickable(false);

                    //hide start button
                    startButton.setVisibility(View.GONE);

                    //show stop button
                    stopButton.setVisibility(View.VISIBLE);

                    /*start scanning*/
                    //get blescanner object
                    //TODO: do thing during onCreate?
                    mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
                    scanSettings = new ScanSettings.Builder()
                            .setScanMode(scanMode)
                            .build();
                    scanFilters = new ArrayList<ScanFilter>();

                    //get the current time
                    //IS NOT USED AT THE MOMENT
                    preTime = System.currentTimeMillis();

                    //create a new file to store logged data
                    fileName = fileName + new SimpleDateFormat("yyyy_dd_MM_HH_mm_ss", Locale.US).format(new Date()) + FILENAME_POSTFIX;
                    fileHandler.open(fileName, HandlerFileReadWrite.fileOperator.OPEN_WRITE);
                    fileHandler.writeLine("Index\tmajorList\tRSSI");

                    //clear beacon list
                    iBeaconList = new ArrayList<iBeacon>();

                    //clear UI list
                    adapter.clear();
                    adapter.notifyDataSetChanged();

                    //start to scan
                    startScan(true);
                }
            }
        });


        //get reference to start button
        //and set the task on <stop> button
        stopButton = (FloatingActionButton) findViewById(R.id.stopButton);
        stopButton.setVisibility(View.GONE);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (scanEnable) {

                    //enable setMajor + mode buttons
                    requestInputButton.setAlpha(1.0f);
                    requestInputButton.setClickable(true);
                    modeButton.setAlpha(1.0f);
                    modeButton.setClickable(true);

                    //show start button
                    startButton.setVisibility(View.VISIBLE);

                    //hide stop button
                    stopButton.setVisibility(View.GONE);

                    //get ble object
                    //TODO: do this during onCreate?
                    mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
                    scanSettings = new ScanSettings.Builder()
                            .setScanMode(scanMode)//scanMode here
                            //.setReportDelay(1000)
                            .build();
                    scanFilters = new ArrayList<ScanFilter>();
                    preTime = System.currentTimeMillis();

                    //clear beacon list
                    //necessary?
                    iBeaconList = new ArrayList<iBeacon>();

                    //stop the scan
                    startScan(false);

                    //write summary to file
                    fileHandler.writeLine("*******");
                    String filteredmajorList = "";
                    for (int major : majorList) filteredmajorList = filteredmajorList + major + " ";
                    if (filteredmajorList.equals("")) filteredmajorList = "no";
                    fileHandler.writeLine("filters: " + filteredmajorList);
                    String mode;
                    if (scanMode == ScanSettings.SCAN_MODE_LOW_LATENCY) mode = "fast";
                    else if (scanMode == ScanSettings.SCAN_MODE_LOW_POWER) mode = "slow";
                    else mode = "balanced";
                    fileHandler.writeLine("scan mode: " + mode);
                    fileHandler.writeLine("sample count: " + beaconIndex);
                    fileHandler.close();
                    fileName = FILENAME_PREFIX;
                    beaconIndex = 0;
                }
            }
        });

        //get reference to <setMajor> button
        //and set the tasks on <setMajor> button
        //these tasks are not essential
        requestInputButton = (Button) findViewById(R.id.requestInputButton);
        requestInputButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LayoutInflater factory = LayoutInflater.from(BeaconActivity.this);
                final View layoutView = factory.inflate(R.layout.request_input, null);
                final AlertDialog.Builder builder = new AlertDialog.Builder(BeaconActivity.this);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        majorList = new ArrayList<>();
                        EditText inputText = layoutView.findViewById(R.id.inputBeacon);
                        String text = inputText.getText().toString();
                        Resources res = getResources();
                        String majorText = res.getString(R.string.major_monitor_text, "");
                        if (text.equals("")) {
                            majorList = new ArrayList<>();
                            majorTextView.setText(majorText);
                            return;
                        }
                        //filter string input
                        String filtered = text.replaceAll("[^0-9\\s,]", "");

                        String token[] = filtered.split("[,\\s+]");
                        for (String s : token) {
                            if (!s.equals("")) {
                                try {
                                    int major = (Integer.parseInt(s));
                                    if (!majorList.contains(major)) {
                                        majorList.add(Integer.parseInt(s));
                                    }
                                } catch (NumberFormatException e) {
                                    //
                                    Log.i(TAG, "illegal string: [" + s + "]");
                                }
                            }
                        }

                        StringBuilder stringBuilder = new StringBuilder();
                        for (Integer in : majorList) {
                            stringBuilder.append(in);
                            stringBuilder.append(" ");
                        }
                        majorText = res.getString(R.string.major_monitor_text, stringBuilder);
                        Log.i(TAG, "INTs: " + stringBuilder.toString());

                        majorTextView.setText(majorText);

                    }
                });
                final AlertDialog layoutDialog = builder.create();
                layoutDialog.setView(layoutView);
                layoutDialog.show();
            }
        });

        //get reference to <setMode> button
        //and set the tasks on <setMode> button
        //these tasks are not essential
        modeButton = (Button) findViewById(R.id.modeButton);
        modeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LayoutInflater factory = LayoutInflater.from(BeaconActivity.this);
                final View layoutView = factory.inflate(R.layout.choose_mode, null);
                final AlertDialog.Builder builder = new AlertDialog.Builder(BeaconActivity.this);

                RadioGroup radioGroup = layoutView.findViewById(R.id.radioGroup);

                //set radioButton to previous value
                switch (scanMode) {
                    case ScanSettings.SCAN_MODE_LOW_LATENCY: {
                        ((RadioButton) radioGroup.findViewById(R.id.fastRadioButton)).setChecked(true);
                        break;
                    }
                    case ScanSettings.SCAN_MODE_LOW_POWER: {
                        ((RadioButton) radioGroup.findViewById(R.id.slowRadioButton)).setChecked(true);
                        break;
                    }
                    case ScanSettings.SCAN_MODE_BALANCED: {
                        ((RadioButton) radioGroup.findViewById(R.id.balanceRadioButton)).setChecked(true);
                        break;
                    }
                }

                radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(RadioGroup radioGroup, int i) {
                        Log.i(TAG, "checked " + i);
                        switch (i) {
                            case R.id.fastRadioButton: {
                                Log.i(TAG, "FAST SELECED");
                                newScanMode = ScanSettings.SCAN_MODE_LOW_LATENCY;
                                break;
                            }
                            case R.id.slowRadioButton: {
                                Log.i(TAG, "SLOW SELECED");
                                newScanMode = ScanSettings.SCAN_MODE_LOW_POWER;
                                break;
                            }
                            case R.id.balanceRadioButton: {
                                Log.i(TAG, "BALANCE SELECED");
                                newScanMode = ScanSettings.SCAN_MODE_BALANCED;
                                break;
                            }
                        }
                    }
                });
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        scanMode = newScanMode;
                    }
                });
                final AlertDialog layoutDialog = builder.create();
                layoutDialog.setView(layoutView);
                layoutDialog.show();
            }
        });


        //get reference to major list text view
        //set to empty by default
        majorTextView = (TextView) findViewById(R.id.majorsTextView);
        Resources res = getResources();
        String majorText = res.getString(R.string.major_monitor_text, "");
        majorTextView.setText(majorText);

        //
        mHandler = new Handler();

        //list of user-input majors
        //this list will be set in setMode button tasks
        //or it will be set according to the last major values
        majorList = new ArrayList<>();

        //UI handling stuffs
        //set the list adapter of list view
        adapter = new ArrayAdapter<String>(context, R.layout.listview_text);
        mainListView.setAdapter(adapter);


        //get majors from last scan
        StringBuilder stringBuilder = new StringBuilder();

        HandlerFileReadWrite reader = new HandlerFileReadWrite(context);
        reader.open(MAJORS_STORAGE_FILE);
        String line = reader.readLine();
        while (line != null) {
            try {
                majorList.add(Integer.parseInt(line));
                stringBuilder.append(line);
                stringBuilder.append(" ");
            } catch (NumberFormatException e) {
                //
            }
            line = reader.readLine();
        }
        reader.close();
        majorTextView.setText(getResources().getString(R.string.major_monitor_text, stringBuilder));
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onResume() {
        super.onResume();

        //check sd card
        checkSDCar();

        //request device to turn on bluetooth
        //request additional permission
        //note: for API > 23 (Android 6.0 and above) -> must make sure to request "access location"
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BLE);
        } else {
            if (!(checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION);
            } else
                scanEnable = true;
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mScanning) {
            stopButton.performClick();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "ON STOP");
        HandlerFileReadWrite writer = new HandlerFileReadWrite(context);
        writer.open(MAJORS_STORAGE_FILE, HandlerFileReadWrite.fileOperator.OPEN_WRITE);
        for (int i=0; i<majorList.size(); i++) {
            writer.writeLine(majorList.get(i).toString());
        }
        writer.close();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_FINE_LOCATION:
            case REQUEST_ENABLE_BLE: {
                if (resultCode == RESULT_OK) {
                    scanEnable = true;
                } else {
                    finish();
                }
                break;
            }
        }
    }


    //callback method for the scan
    //most IMPORTANT class
    //beacon discovered is delivered and processed here
    private ScanCallback mBleScanCallback = new ScanCallback() {
        @Override

        //this method is called when a beacon is detected
        //beacon advertising result (data) is stored in result
        //we just look there and parse the data
        public void onScanResult(int callbackType, ScanResult result) {

            BluetoothDevice btDevice = result.getDevice();

            if (result.getScanRecord() == null)
                return;

            //get the raw advertising data
            byte raw[] = result.getScanRecord().getBytes();

            //get and parse the UUID of the beacon
            byte uuidBytes[] = new byte[16];
            System.arraycopy(raw, 9, uuidBytes, 0, 16);
            String hexString = bytesToHex(uuidBytes);
            String uuid = hexString.substring(0, 8) + "-"
                    + hexString.substring(8, 12) + "-"
                    + hexString.substring(12, 16) + "-"
                    + hexString.substring(16, 20) + "-"
                    + hexString.substring(20, 32);

            //filter the beacon based on UUID
            if (!uuid.equals(UUID_STRING)) {
                //uuid is not match
                return;
            }

            //parse major data
            int major = (raw[25] & 0xFF) * 0x100 + (raw[26] & 0xFF);

            //parse mijor data
            int minor = (raw[27] & 0xFF) * 0x100 + (raw[28] & 0xFF);

            Log.i(TAG, "Device Discoverd: " + btDevice.getName() + " rssi: " + result.getRssi()
                    + "Major: " + major + " UUID: " + uuid);
            Log.i(TAG, "MODE: " + scanMode);

            //create new beacon class with all the data so far
            iBeacon beacon = new iBeacon(uuid, major, minor, result.getRssi(), 0);

            //update to beacon list
            //(to display on UI for now)
            for (int i = 0; i < iBeaconList.size(); i++) {
                if (iBeaconList.get(i).getMajor() == major) {
                    iBeaconList.remove(i);
                }
            }

            //filter the beacon based on user-input majors
            if (majorList.isEmpty()) {
                //major list is empty
                //monitor all
                iBeaconList.add(beacon);
                //write beacon to file
                fileHandler.writeLine("" + beaconIndex + "\t" + beacon.toLoggedData());
                beaconIndex++;
            } else {
                if (majorList.contains(major)) {
                    iBeaconList.add(beacon);
                    //write beacon to file
                    fileHandler.writeLine("" + beaconIndex + "\t" + beacon.toLoggedData());
                    beaconIndex++;
                }
            }

            //method to update UI
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.clear();
                    if (iBeaconList.isEmpty()) {
                        adapter.add("No Beacon Found.");
                    } else {
                        for (int i = 0; i < iBeaconList.size(); i++) {
                            adapter.add(iBeaconList.get(i).toString());
                        }
                    }
                    adapter.notifyDataSetChanged();
                }
            });

        }

        //use this method to wait for a list of beacon instead of just one
        //currently not implemented
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            //
            Log.i(TAG, "ON BATCH: " + results.size());
        }

        //this method is called when something when wrong during scan
        //TODO: maybe handling error here
        @Override
        public void onScanFailed(int errorCode) {
            //super.onScanFailed(errorCode);
            Log.e(TAG, "Error Code: " + errorCode);
        }
    };

    //method to start or stop the scan
    private void startScan(boolean enable) {
        if (enable) {
            /*mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetoothLeScanner.stopScan(mBleScanCallback);
                }
            }, SCAN_PERIOD);*/
            mScanning = true;
            mBluetoothLeScanner.startScan(scanFilters, scanSettings, mBleScanCallback);
            Log.i(TAG, "Start scanning");
        } else {
            mScanning = false;
            mBluetoothLeScanner.stopScan(mBleScanCallback);
            Log.i(TAG, "Stop scanning");
        }
    }

    //method to check for sd card
    private boolean checkSDCar() {

        File fileDir[] = getExternalMediaDirs();
        Toast toast = Toast.makeText(context, "", Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);

        if (fileDir.length > 1) {
            toast.setText(R.string.sd_card_yes_message);
            toast.show();
            return true;
        } else {
            toast.setText(R.string.sd_card_no_message);
            toast.show();
            return false;
        }
    }

    //method to parse a byte array to equivalent hex array
    private String bytesToHex(byte byteArray[]) {
        char hexRaw[] = new char[byteArray.length * 2];
        for (int i = 0; i < byteArray.length; i++) {
            int val = byteArray[i] & 0xFF;
            hexRaw[i * 2] = hexArray[val >>> 4];
            hexRaw[i * 2 + 1] = hexArray[val & 0x0F];
        }
        return new String(hexRaw);
    }
}
