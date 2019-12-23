package com.example.blinkingemergencycalls;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.CallLog;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.neurosky.AlgoSdk.NskAlgoDataType;
import com.neurosky.AlgoSdk.NskAlgoSdk;
import com.neurosky.AlgoSdk.NskAlgoType;
import com.neurosky.connection.ConnectionStates;
import com.neurosky.connection.TgStreamHandler;
import com.neurosky.connection.TgStreamReader;
import com.neurosky.connection.DataType.MindDataType;

import java.lang.reflect.Method;
import java.util.Date;


public class MainActivity extends AppCompatActivity {
    final int REQUEST_ENABLE_BT = 1;
    final String TAG = "neuroskyTest";
    private static final int REQUEST_CALL = 1;
    private static final String MSG_KEY = "BROADCASTKEY";
    private static final String INDENTIFY_KEY = "MESSAGE";
    private static final String PHONE_NUMBER = "PHONENUMBER";
    private static final int TOTALCOUNT = 3;
    private static int Flag = 0; // 0:init  1:continue to call    2:end
    private int order = 0;
    // COMM SDK handles
    private TgStreamReader tgStreamReader;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean bEnable = true; //Dial state

    // internal variables
    private boolean bInited = false;
    private boolean bRunning = false;
    private NskAlgoType currentSelectedAlgo;

    // canned data variables
    private short raw_data[] = {0};
    private int raw_data_index = 0;
    private float output_data[];
    private int output_data_count = 0;
    private int raw_data_sec_len = 85;
    private int blinkCount = 0;
    private NskAlgoSdk nskAlgoSdk;
    public String prevNumber = null;

    private TextView tvBlinkCount, tvDialStatus;


    private static GlobalVariable primaryValue = new GlobalVariable();

    public BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Bundle b = intent.getExtras();
            Flag = b.getInt(INDENTIFY_KEY);
            String phonenumber = b.getString(PHONE_NUMBER);
            String phone = primaryValue.phoneNumbers.get(order % primaryValue.phoneNumbers.size()).replaceAll("\\D", "");

            Log.d(TAG, phonenumber + ":" + phone);

            if(phonenumber.contains(phone)) {
                if (Flag == 2) {
                    if (getCallLog() == true) {
                        Flag = 2;
                        choosetheWay(Flag);
                        //prevNumber = null;
                    } else {
                        Flag = 1;
                        if ((order + 1) / primaryValue.phoneNumbers.size() >= TOTALCOUNT)
                            Flag = 2;
                        Log.d(TAG, String.valueOf(order) + ":" + String.valueOf(primaryValue.phoneNumbers.size()));
                        //showToast(String.valueOf(order)+":"+String.valueOf(primaryValue.phoneNumbers.size()),Toast.LENGTH_SHORT);
                        Log.d(TAG, String.valueOf(order));
                        choosetheWay(Flag);
                        //prevNumber = phonenumber;
                    }
                }
            }
            else Log.d(TAG,"differnet");
        }
    };
    private void choosetheWay(int Flag){
        Log.d(TAG,"chooseTheWay:"+String.valueOf(Flag));
        if(Flag == 1) // Must call other number
        {
            bEnable = false;
            order++;
            DialToPhone(order);
        }
        if(Flag == 2) // End call
        {
            blinkCount = 0;
            order = 0;
            Flag = 0;
            bEnable = true;
            //();
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        registerReceiver(broadcastReceiver, new IntentFilter(MSG_KEY));


        primaryValue = SplashSettings.primaryValue;
        showToast(String.valueOf(primaryValue.getBlinkCount()), Toast.LENGTH_SHORT);
        getPermission();

        tvBlinkCount = (TextView) findViewById(R.id.showBlinkCount);
        tvDialStatus = (TextView) findViewById(R.id.dialStation);

        tvBlinkCount.setText("Now Count of Blink:" + String.valueOf(primaryValue.blinkCount));

        nskAlgoSdk = new NskAlgoSdk();

        if (checkBlueToothAdapter()) {
            checkHeadSet();
            setAlgos();
            startDetect();
            countBlink();
        } else {
            showToast("Check Bluetooth adpater and run application again", Toast.LENGTH_SHORT);
        }
        //DialToPhone(order);
    }
    private void getPermission(){
        if (!hasPhoneContactsPermission(Manifest.permission.READ_CALL_LOG)) {
            requestPermission(Manifest.permission.READ_CALL_LOG);
        }
        if(!hasPhoneContactsPermission(Manifest.permission.MODIFY_AUDIO_SETTINGS)){
            requestPermission(Manifest.permission.MODIFY_AUDIO_SETTINGS);
        }
        if(!hasPhoneContactsPermission(Manifest.permission.RECORD_AUDIO)){
            requestPermission(Manifest.permission.RECORD_AUDIO);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.settings_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                startSplashSettings();
                return true;
            default:
                return false;

        }
    }

    private void startSplashSettings() {
        showToast("startSplashSettings", Toast.LENGTH_SHORT);
        primaryValue = null;
        SharedPreferences mPrefs;
        SharedPreferences.Editor mEditor;
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mEditor = mPrefs.edit();
        mEditor.putBoolean("bFirst", false);
        mEditor.commit();
        Intent mMainActivity = new Intent(MainActivity.this, SplashSettings.class);
        startActivity(mMainActivity);
        finish();
    }

    private void startDetect() {
        if (bRunning == false) {
            nskAlgoSdk.NskAlgoStart(false);
        } else {
            nskAlgoSdk.NskAlgoPause();
        }
    }

    private void countBlink() {
        nskAlgoSdk.setOnEyeBlinkDetectionListener(new NskAlgoSdk.OnEyeBlinkDetectionListener() {
            @Override
            public void onEyeBlinkDetect(int strength) {
                //Log.d(TAG, "NskAlgoEyeBlinkDetectionListener: Eye blink detected: " + strength);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG,"blink");
                        if(bEnable) {
                            blinkCount++;
                            tvBlinkCount.setText("Now Count of Blink:" + String.valueOf(blinkCount));
                            if(blinkCount == primaryValue.getBlinkCount()){
                                DialToPhone(order);
                                bEnable = false;
                            }
                        }
                    }
                });
            }
        });
    }


    private void checkHeadSet() {
        output_data_count = 0;
        output_data = null;

        raw_data = new short[512];
        raw_data_index = 0;

        // Example of constructor public TgStreamReader(BluetoothAdapter ba, TgStreamHandler tgStreamHandler)
        tgStreamReader = new TgStreamReader(mBluetoothAdapter, callBack);

        if (tgStreamReader != null && tgStreamReader.isBTConnected()) {

            // Prepare for connecting
            tgStreamReader.stop();
            tgStreamReader.close();
        }

        // (4) Demo of  using connect() and start() to replace connectAndStart(),
        // please call start() when the state is changed to STATE_CONNECTED
        tgStreamReader.connect();
    }

    private void setAlgos() {
        int algoTypes = 0;
        currentSelectedAlgo = NskAlgoType.NSK_ALGO_TYPE_INVALID;
        algoTypes += NskAlgoType.NSK_ALGO_TYPE_BLINK.value;
        if (algoTypes == 0) {
            showDialog("Please select at least one algorithm");
        } else {
            if (bInited) {
                nskAlgoSdk.NskAlgoUninit();
                bInited = false;
            }
            int ret = nskAlgoSdk.NskAlgoInit(algoTypes, getFilesDir().getAbsolutePath());
            if (ret == 0) {
                bInited = true;
            }

            Log.d(TAG, "NSK_ALGO_Init() " + ret);
            String sdkVersion = "SDK ver.: " + nskAlgoSdk.NskAlgoSdkVersion();

            if ((algoTypes & NskAlgoType.NSK_ALGO_TYPE_ATT.value) != 0) {
                sdkVersion += "\nATT ver.: " + nskAlgoSdk.NskAlgoAlgoVersion(NskAlgoType.NSK_ALGO_TYPE_ATT.value);
            }
            if ((algoTypes & NskAlgoType.NSK_ALGO_TYPE_MED.value) != 0) {
                sdkVersion += "\nMED ver.: " + nskAlgoSdk.NskAlgoAlgoVersion(NskAlgoType.NSK_ALGO_TYPE_MED.value);
            }
            if ((algoTypes & NskAlgoType.NSK_ALGO_TYPE_BLINK.value) != 0) {
                sdkVersion += "\nBlink ver.: " + nskAlgoSdk.NskAlgoAlgoVersion(NskAlgoType.NSK_ALGO_TYPE_BLINK.value);
            }
            if ((algoTypes & NskAlgoType.NSK_ALGO_TYPE_BP.value) != 0) {
                sdkVersion += "\nEEG Bandpower ver.: " + nskAlgoSdk.NskAlgoAlgoVersion(NskAlgoType.NSK_ALGO_TYPE_BP.value);
            }
            showToast(sdkVersion, Toast.LENGTH_LONG);
        }
    }

    private boolean checkBlueToothAdapter() {

        try {
            // (1) Make sure that the device supports Bluetooth and Bluetooth is on
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
                showToast(
                        "Please enable your Bluetooth and re-run this program !",
                        Toast.LENGTH_LONG);
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                //finish();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "error:" + e.getMessage());
            return false;
        }
        return true;
    }
    private void requestPermission(String permission)
    {
        String requestPermissionArray[] = {permission};
        ActivityCompat.requestPermissions(this, requestPermissionArray, 1);
    }
    private boolean hasPhoneContactsPermission(String permission)
    {
        boolean ret = false;

        // If android sdk version is bigger than 23 the need to check run time permission.
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

            // return phone read contacts permission grant status.
            int hasPermission = ContextCompat.checkSelfPermission(getApplicationContext(), permission);
            // If permission is granted then return true.
            if (hasPermission == PackageManager.PERMISSION_GRANTED) {
                ret = true;
            }
        }else
        {
            ret = true;
        }
        return ret;
    }
    private boolean getCallLog() {
        Cursor mCursor = getContentResolver().query(CallLog.Calls.CONTENT_URI, null
                , null, null, null);
        int number = mCursor.getColumnIndex(CallLog.Calls.NUMBER);
        int duration = mCursor.getColumnIndex(CallLog.Calls.DURATION);
        int type = mCursor.getColumnIndex(CallLog.Calls.TYPE);
        int date = mCursor.getColumnIndex(CallLog.Calls.DATE);

        //StringBuilder sb = new StringBuilder();\
        mCursor.moveToNext();
        int callDuration = 0;
        String primaryDate1 = mCursor.getString(date);
        callDuration = mCursor.getInt(duration);

        while (mCursor.moveToNext()) {
            String pNumber = mCursor.getString(number);

            String phone = primaryValue.phoneNumbers.get(order % primaryValue.phoneNumbers.size()).replaceAll("\\D","");

            if (pNumber.contains(phone)){
                String currentDate = mCursor.getString(date);
                if(currentDate.compareTo(primaryDate1) > 0) {
                    primaryDate1 = currentDate;
                    callDuration = mCursor.getInt(duration);
                }
            }
        }
        //Log.d("neuroskyTest","CallDuration:" + callDuration);
        if(callDuration != 0)
            return true;
        else return false;
    }
    private void DialToPhone(int order) {

        if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,new String[] {Manifest.permission.CALL_PHONE},REQUEST_CALL);
        }
        else {
            //String telPhoneNumber = primaryValue.phoneNumbers.get(


            tvDialStatus.setText(primaryValue.phoneNumbers.get(order % primaryValue.phoneNumbers.size()));
            String telPhoneNumber = primaryValue.phoneNumbers.get(order % primaryValue.phoneNumbers.size());
            String dial = "tel:" + telPhoneNumber;

            Log.d(TAG,dial);

            setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);

            //Log.d("neuroskyTest","order:" + String.valueOf(order));
            startActivity(new Intent(Intent.ACTION_CALL,Uri.parse(dial)));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CALL)
        {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                //DialToPhone();
            }
        }
    }

    private TgStreamHandler callBack = new TgStreamHandler() {
        @Override
        public void onDataReceived(int datatype, int data, Object obj) {
            switch (datatype) {
                case MindDataType.CODE_ATTENTION:
                    short attValue[] = {(short) data};
                    nskAlgoSdk.NskAlgoDataStream(NskAlgoDataType.NSK_ALGO_DATA_TYPE_ATT.value, attValue, 1);
                    break;
                case MindDataType.CODE_MEDITATION:
                    short medValue[] = {(short) data};
                    nskAlgoSdk.NskAlgoDataStream(NskAlgoDataType.NSK_ALGO_DATA_TYPE_MED.value, medValue, 1);
                    break;
                case MindDataType.CODE_POOR_SIGNAL:
                    short pqValue[] = {(short) data};
                    nskAlgoSdk.NskAlgoDataStream(NskAlgoDataType.NSK_ALGO_DATA_TYPE_PQ.value, pqValue, 1);
                    break;
                case MindDataType.CODE_RAW:
                    raw_data[raw_data_index++] = (short) data;
                    if (raw_data_index == 512) {
                        nskAlgoSdk.NskAlgoDataStream(NskAlgoDataType.NSK_ALGO_DATA_TYPE_EEG.value, raw_data, raw_data_index);
                        raw_data_index = 0;
                    }
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onStatesChanged(int connectionStates) {
            Log.d(TAG, "connectionStates change to: " + connectionStates);
            switch (connectionStates) {
                case ConnectionStates.STATE_CONNECTING:
                    // Do something when connecting
                    break;
                case ConnectionStates.STATE_CONNECTED:
                    // Do something when connected
                    tgStreamReader.start();
                    showToast( "Connected", Toast.LENGTH_SHORT);
                    break;
                case ConnectionStates.STATE_WORKING:
                    // Do something when working

                    //(9) demo of recording raw data , stop() will call stopRecordRawData,
                    //or you can add a button to control it.
                    //You can change the save path by calling setRecordStreamFilePath(String filePath) before startRecordRawData
                    //tgStreamReader.startRecordRawData();

                    MainActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                        }

                    });

                    break;
                case ConnectionStates.STATE_GET_DATA_TIME_OUT:
                    // Do something when getting data timeout

                    //(9) demo of recording raw data, exception handling
                    //tgStreamReader.stopRecordRawData();

                    showToast("Get data time out!", Toast.LENGTH_SHORT);

                    if (tgStreamReader != null && tgStreamReader.isBTConnected()) {
                        tgStreamReader.stop();
                        tgStreamReader.close();
                    }

                    break;
                case ConnectionStates.STATE_STOPPED:
                    // Do something when stopped
                    // We have to call tgStreamReader.stop() and tgStreamReader.close() much more than
                    // tgStreamReader.connectAndstart(), because we have to prepare for that.

                    break;
                case ConnectionStates.STATE_DISCONNECTED:
                    // Do something when disconnected
                    break;
                case ConnectionStates.STATE_ERROR:
                    // Do something when you get error message
                    break;
                case ConnectionStates.STATE_FAILED:
                    break;
            }
        }

        @Override
        public void onChecksumFail(byte[] bytes, int i, int i1) {

        }

        @Override
        public void onRecordFail(int i) {

        }
    };
    public void showToast(final String msg, final int timeStyle) {
        MainActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getApplicationContext(), msg, timeStyle).show();
            }
        });
    }
    private void showDialog (String message) {
        new AlertDialog.Builder(this)
                .setTitle("")
                .setMessage(message)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with delete
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }
}
