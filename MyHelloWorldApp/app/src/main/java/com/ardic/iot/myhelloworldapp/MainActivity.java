package com.ardic.iot.myhelloworldapp;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.ardic.android.iotignite.callbacks.ConnectionCallback;
import com.ardic.android.iotignite.enumerations.NodeType;
import com.ardic.android.iotignite.exceptions.UnsupportedVersionException;
import com.ardic.android.iotignite.listeners.NodeListener;
import com.ardic.android.iotignite.nodes.IotIgniteManager;
import com.ardic.android.iotignite.nodes.Node;
import com.google.android.things.contrib.driver.button.Button;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    /**
     * Android Things PIO Service instance
     */
    private PeripheralManagerService mService = new PeripheralManagerService();

    /**
     * Raspberry Pi GPIO Pin which LED connected
     */
    private static final String LED_PIN = "BCM6";

    /**
     * Raspberry Pi GPIO Pin which button connectec.
     */
    private static final String BTN_PIN = "BCM21";

    /**
     * Sample Node ID for defining IoT-Ignite Node
     */
    private static final String NODE_ID = "My Awesome Node";


    /**
     * Gpio instance for controlling led.
     */
    private Gpio mLed;

    /**
     * Gpio instance for controlling button.
     */
    private Button mButton;

    /**
     * Handler for controlling blink operation in a separate thread.
     */
    private Handler mLedHandler = new Handler();

    /**
     * IoT-Ignite Manager instance for connecting.
     */
    private IotIgniteManager mIotIgniteManager;

    /**
     * Sample IoT-Ignite Node instance.
     *
     */
    private Node myNewNode;


    /**
     * Runnable for blinking led.
     */
    private Runnable mLedRunnable = new Runnable() {
        @Override
        public void run() {

            try {

                /**
                 * Set led value to inverse of previous value for blink
                 */
                mLed.setValue(!mLed.getValue());
            } catch (IOException e) {
                Log.i(TAG,"IOException " + e);
            }

            /**
             * Schedule for run again.
             */
            mLedHandler.postDelayed(this,3000);

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        /**
         * List available GPIO pins.
         */
        Log.i(TAG,"GPIO :\n" + mService.getGpioList());


        try {

            /**
             * Get led gpio instance from peripheral manager
             */
            mLed = mService.openGpio(LED_PIN);

            /**
             * Set direction of led out and initial value to low.
             */
            mLed.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
        } catch (IOException e) {
            Log.i(TAG,"IOException " + e);

        }

        /**
         * uncomment here if you want blink led.
         */
        // mLedHandler.post(mLedRunnable);


        try {

            /**
             * Get button pin instance using button driver.
             */
            mButton = new Button(BTN_PIN, Button.LogicState.PRESSED_WHEN_HIGH);
        } catch (IOException e) {
            Log.i(TAG,"IOException " + e);

        }

        /**
         * Register changing button events.
         */
        mButton.setOnButtonEventListener(new Button.OnButtonEventListener() {
            @Override
            public void onButtonEvent(Button button, boolean pressed) {
                try {
                    mLed.setValue(pressed);
                } catch (IOException e) {
                    Log.i(TAG,"IOException " + e);
                }
            }
        });



        try {
            mIotIgniteManager = new IotIgniteManager.Builder()
                    .setContext(getApplicationContext())
                    .setConnectionListener(new ConnectionCallback() {
                        @Override
                        public void onConnected() {

                            Log.i(TAG,"Ignite Connected");

                        }

                        @Override
                        public void onDisconnected() {

                            Log.i(TAG,"Ignite Disconnected");
                        }
                    }).build();
        } catch (UnsupportedVersionException e) {
            Log.i(TAG,"UnsupportedVersionException :" + e);
        }

        myNewNode = IotIgniteManager.NodeFactory.createNode(NODE_ID, NODE_ID, NodeType.GENERIC, null, new NodeListener() {
            @Override
            public void onNodeUnregistered(String s) {

            }
        });

        if(myNewNode != null){

            myNewNode.register();
        }





      /*  mButton.setOnButtonEventListener(new Button.OnButtonEventListener() {
            @Override
            public void onButtonEvent(Button button, boolean pressed) {

            }
        });
        *


         /**
         * Uncomment belowed code if you want to open led directly.
         */


       /* if(mLed != null){
            try {
                mLed.setValue(true);
            } catch (IOException e) {
                Log.i(TAG,"IOException " + e);
            }
        }*/



    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        /**
         * Release GPIO Pins when application destroyed.
         */
        try {
            if(mLed != null) {
                mLed.setValue(false);
                mLed.close();
            }

            if(mButton !=null){
                mButton.close();
            }
        } catch (IOException e) {
            Log.i(TAG,"IOException " + e);
        }

        mService = null;

    }





}
