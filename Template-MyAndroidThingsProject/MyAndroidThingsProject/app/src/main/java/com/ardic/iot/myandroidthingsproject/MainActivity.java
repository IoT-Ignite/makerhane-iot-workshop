package com.ardic.iot.myandroidthingsproject;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;

import com.ardic.android.iot.hwnodeapptemplate.base.BaseWifiNodeDevice;
import com.ardic.android.iot.hwnodeapptemplate.listener.CompatibilityListener;
import com.ardic.android.iot.hwnodeapptemplate.listener.ThingEventListener;
import com.ardic.android.iot.hwnodeapptemplate.listener.WifiNodeManagerListener;
import com.ardic.android.iot.hwnodeapptemplate.manager.GenericWifiNodeManager;
import com.ardic.android.iot.hwnodeapptemplate.node.GenericWifiNodeDevice;
import com.ardic.android.iot.hwnodeapptemplate.service.WifiNodeService;
import com.ardic.android.iotignite.exceptions.UnsupportedVersionException;
import com.ardic.android.iotignite.things.ThingConfiguration;
import com.ardic.android.iotignite.things.ThingData;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MainActivity extends Activity implements CompatibilityListener,WifiNodeManagerListener {

    public static final String TYPE = "DYNAMIC NODE - DHT11 SENSOR";
    private static final String TAG ="Sample IoT-Ignite App";
    private IotIgniteHandler mIotIgniteHandler;
    private boolean lastState=false;
    private GenericWifiNodeManager espManager;
    private List<BaseWifiNodeDevice> espNodeList = new CopyOnWriteArrayList<>();
    private ThingEventListener mEspThingEventListener = new ThingEventListener() {
        @Override
        public void onDataReceived(String s, String s1, ThingData thingData) {

            Log.i(TAG, "onDataReceived [" + s + "][" + s1 + "][" + thingData.getDataList() + "]");

        }

        @Override
        public void onConnectionStateChanged(String s, boolean b) {
            Log.i(TAG, "onConnectionStateChanged [" + s + "][" + b + "]");

        }

        @Override
        public void onActionReceived(String s, String s1, String s2) {

            Log.i(TAG, "onActionReceived [" + s + "][" + s1 + "][" + s2 + "]");

        }

        @Override
        public void onConfigReceived(String s, String s1, ThingConfiguration thingConfiguration) {

            Log.i(TAG, "onConfigReceived [" + s + "][" + s1 + "][" + thingConfiguration.getDataReadingFrequency() + "]");

        }

        @Override
        public void onUnknownMessageReceived(String s, String s1) {

            Log.i(TAG, "onUnknownMessageReceived [" + s + "][" + s1 + "]");


        }

        @Override
        public void onNodeUnregistered(String s) {
            Log.i(TAG, "onNodeUnregistered [" + s + "]");



        }

        @Override
        public void onThingUnregistered(String s, String s1) {

            Log.i(TAG, "onThingUnregistered [" + s + "][" + s1 + "]");


        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /**
         * Set IgniteHandler and start.
         */
        mIotIgniteHandler = IotIgniteHandler.getInstance(getApplicationContext());
        mIotIgniteHandler.start();


        startService(new Intent(this, WifiNodeService.class));
        WifiNodeService.setCompatibilityListener(this);
        initEspDeviceAndNodeManager();


    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_SPACE) {
            // Turn on the LED
            if(!lastState) {
                mIotIgniteHandler.setLed(true);
                mIotIgniteHandler.setButtonState(true);
                lastState = true;
            }
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_SPACE) {


            if (lastState) {
                // Turn off the LED
                mIotIgniteHandler.setLed(false);
                mIotIgniteHandler.setButtonState(false);
                lastState = false;
            }
            return true;
        }

        return super.onKeyUp(keyCode, event);
    }


    @Override
    protected void onDestroy() {

        if(mIotIgniteHandler != null){
            mIotIgniteHandler.shutdown();
        }

        stopService(new Intent(this, WifiNodeService.class));

        super.onDestroy();


    }

    @Override
    public void onUnsupportedVersionExceptionReceived(UnsupportedVersionException e) {
        Log.e(TAG, "Ignite onUnsupportedVersionExceptionReceived :  " + e);
    }


    private void initEspDeviceAndNodeManager() {
        espManager = GenericWifiNodeManager.getInstance(getApplicationContext());
        //   espManager.setIgniteManager(IotIgniteHandler.getInstance(getApplicationContext()).getIgniteManager());

        espManager.addWifiNodeManagerListener(this);

        for (GenericWifiNodeDevice dvc : espManager.getWifiNodeDeviceList()) {
            checkAndUpdateDeviceList(dvc);
        }

    }

    @Override
    public void onWifiNodeDeviceAdded(BaseWifiNodeDevice baseWifiNodeDevice) {
        checkAndUpdateDeviceList(baseWifiNodeDevice);

    }

    @Override
    public void onIgniteConnectionChanged(boolean b) {

        Log.i(TAG,"IGNITE CONNECTION : " + b );

    }

    private void checkAndUpdateDeviceList(BaseWifiNodeDevice device) {


        if (TYPE.equals(device.getWifiNodeDevice().getNodeType())) {

            if (!espNodeList.contains(device)) {
                Log.i(TAG, "New node found adding to list.");
                espNodeList.add(device);
                device.addThingEventListener(mEspThingEventListener);
            } else {
                Log.i(TAG, "New node already in list.Updating...");
                if (espNodeList.indexOf(device) != -1) {
                    Log.i(TAG, "New node already in list.Removing...");
                    espNodeList.remove(espNodeList.indexOf(device));
                }
                espNodeList.add(device);
                device.removeThingEventListener(mEspThingEventListener);
                device.addThingEventListener(mEspThingEventListener);
            }
        }
    }


}
