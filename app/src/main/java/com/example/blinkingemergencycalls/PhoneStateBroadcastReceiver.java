package com.example.blinkingemergencycalls;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

public class PhoneStateBroadcastReceiver extends BroadcastReceiver {

    private static final int CONTINUERIGING = 1;
    private static final int ENDCALL = 2;
    private static final String TAG = "neuroskyTest";
    Context mContext;
    String incoming_number;
    private int prev_state;
    private boolean bBroadcast = false;
    private String strCallState;

    public Bundle extras;
    public Intent i;

    private static final String MSG_KEY = "BROADCASTKEY";
    private static final String INDENTIFY_KEY = "MESSAGE";
    private static final String PHONE_NUMBER = "PHONENUMBER";
    public TelephonyManager telephony;
    public CustomPhoneStateListener customPhoneListener;
    private static String prevPhoneNumber = null;

    @SuppressLint("LongLogTag")
    @Override
    public void onReceive(Context context, Intent intent) {


        Log.d(TAG, "__________GCM Broadcast");

        extras = intent.getExtras();
        i = new Intent(MSG_KEY);
        // Data you need to pass to activity


        telephony = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE); //TelephonyManager object
        customPhoneListener = new CustomPhoneStateListener();
        telephony.listen(customPhoneListener, PhoneStateListener.LISTEN_CALL_STATE); //Register our listener with TelephonyManager

        Bundle bundle = intent.getExtras();
        String phoneNr = bundle.getString("incoming_number");
        mContext = context;

    }
    public class CustomPhoneStateListener extends PhoneStateListener {

        private static final String TAG = "neuroskyTest";

        @SuppressLint("LongLogTag")
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {

            if (incomingNumber != null && incomingNumber.length() > 0)
                incoming_number = incomingNumber;

            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    strCallState = "CALL_STATE_RINGING";
                    Log.d(TAG, "CALL_STATE_RINGING");
                    prev_state = state;
                    break;

                case TelephonyManager.CALL_STATE_OFFHOOK:
                        //Activate loudspeaker
                        try {
                            Thread.sleep(500); // Delay 0,5 seconds to handle better turning on loudspeaker
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
                        audioManager.setMode(AudioManager.MODE_IN_CALL);
                        audioManager.setSpeakerphoneOn(false);
                        audioManager.setSpeakerphoneOn(true);
                        audioManager.setWiredHeadsetOn(true);
                    break;

                case TelephonyManager.CALL_STATE_IDLE:
                    //strCallState = "CALL_STATE_IDLE";
//                    if (callFromOffHook) {
//                        //callFromOffHook = false;
//                        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
//                        audioManager.setMode(AudioManager.MODE_NORMAL);
//                        telephony.listen(customPhoneListener, PhoneStateListener.LISTEN_NONE);
//                        i.putExtra(INDENTIFY_KEY, ENDCALL);
//                        i.putExtra(PHONE_NUMBER,incoming_number);
//                        Log.d(TAG,"aaaa");
//                    }
                    //else callFromOffHook = false;


                    Log.d(TAG, "CALL_STATE_IDLE==>" + incoming_number);
                    if(incoming_number == null) {

                        telephony.listen(customPhoneListener,PhoneStateListener.LISTEN_NONE);
                        try {
                            Thread.sleep(500); // Delay 0,5 seconds to handle better turning on loudspeaker
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        i.putExtra(INDENTIFY_KEY, ENDCALL);
                        i.putExtra(PHONE_NUMBER,prevPhoneNumber);
                        mContext.sendBroadcast(i);


                        AudioManager audioManager1 = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
                        audioManager1.setMode(AudioManager.MODE_NORMAL);
                        Log.d(TAG,"11111");
                    }
                    else{
                        prevPhoneNumber = incoming_number;
                    }
                    break;
            }
        }
    }
}

