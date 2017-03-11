package com.ardic.iot.myandroidthingsproject;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;

import com.ardic.android.iotignite.callbacks.ConnectionCallback;
import com.ardic.android.iotignite.enumerations.NodeType;
import com.ardic.android.iotignite.enumerations.ThingCategory;
import com.ardic.android.iotignite.enumerations.ThingDataType;
import com.ardic.android.iotignite.exceptions.UnsupportedVersionException;
import com.ardic.android.iotignite.listeners.NodeListener;
import com.ardic.android.iotignite.listeners.ThingListener;
import com.ardic.android.iotignite.nodes.IotIgniteManager;
import com.ardic.android.iotignite.nodes.Node;
import com.ardic.android.iotignite.things.Thing;
import com.ardic.android.iotignite.things.ThingActionData;
import com.ardic.android.iotignite.things.ThingData;
import com.ardic.android.iotignite.things.ThingType;
import com.google.android.things.contrib.driver.button.Button;
import com.google.android.things.contrib.driver.button.ButtonInputDriver;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;

public class IotIgniteHandler implements ConnectionCallback, NodeListener, ThingListener {

    private static final String TAG = IotIgniteHandler.class.getSimpleName();

    private static final String ON_DESTROY_MSG = "Application Destroyed";

    // Static singleton instance
    private static IotIgniteHandler INSTANCE = null;
    private static final long IGNITE_RECONNECT_INTERVAL = 10000L;

    private static final String NODE_ID = "My Example Node";
    private static final String THING_ID = "My Example Thing";


    private IotIgniteManager mIotIgniteManager;
    private boolean igniteConnected = false;
    private Context appContext;
    private Handler igniteWatchdog = new Handler();

    private Node mySampleNode;
    private Thing mySampleThing;

    private ThingType sampleThingType = new ThingType(
            /** Define Type of your Thing */
            "My Sample Thing Type",

            /** Set your things vendor. It's usefull if you are using real sensors
             * This is important for seperating same sensor which different vendors.
             * For example accelerometer sensor produced by Bosch data sampling is
             * different than Samsung's.*/
            "My Sample Vendor",

            /** Set your things data type.
             * IoT-Ignite works with data which type you have selected.
             */
            ThingDataType.INTEGER
    );

    private Runnable igniteWatchdogRunnable = new Runnable() {
        @Override
        public void run() {

            if (!igniteConnected) {
                rebuildIgnite();
                igniteWatchdog.postDelayed(this, IGNITE_RECONNECT_INTERVAL);
                Log.e(TAG, "Ignite is not connected trying to reconnect...");
            } else {
                Log.e(TAG, "Ignite is already connected");
            }
        }
    };

    /**
     * Android Things based things and nodes.
     */

    private static final String ANDROID_THINGS_NODE_ID = "Android Things Node";
    private static final String LED_THING_ID = "Led";
    private static final String BTN_THING_ID = "Button";

    private Node androidThingsNode;
    private Thing mLedThing;
    private Thing mButtonThing;

    private ThingType mLedThingType = new ThingType("LED", "Raspberry Pi 3 GPIO", ThingDataType.INTEGER);
    private ThingType mBtnThingType = new ThingType("BUTTON", "Raspberry Pi 3 GPIO", ThingDataType.INTEGER);

    private PeripheralManagerService mPeripheralManagerService = new PeripheralManagerService();
    private Gpio mLedGpio;
    private ButtonInputDriver mButtonInputDriver;
    private static final String LED_PIN = "BCM21";
    private static final String BTN_PIN = "BCM6";


    private IotIgniteHandler(Context context) {
        this.appContext = context;
    }

    public static synchronized IotIgniteHandler getInstance(Context appContext) {

        if (INSTANCE == null) {
            INSTANCE = new IotIgniteHandler(appContext);
        }
        return INSTANCE;

    }


    public void start() {
        startIgniteWatchdog();
    }

    @Override
    public void onConnected() {
        Log.i(TAG, "Ignite Connected");
        // cancel watchdog //
        igniteWatchdog.removeCallbacks(igniteWatchdogRunnable);
        igniteConnected = true;

        Log.i(TAG, "Creating Node : " + NODE_ID);

        mySampleNode = IotIgniteManager.NodeFactory.createNode(
                /*Unique ID of Node*/
                NODE_ID,
                /* Node label could be unique or not.*/
                NODE_ID,
                /*Node Type is definition for node. If your node is really a physical device you can set as it is.
                * Supported Node Types
                * GENERIC : Default node type. If you dont want to do type based things this will works for you
                * RASPBERRY_PI: Defines node as a Raspberry Pi.
                * If your node is Raspberry Pi and you're going to do RasPi specific things choose this one. (RaspiCam etc.)
                * ARDUINO_YUN:  Defines node as Arduino Yun. Use it for Arduino Yun specific things. (Bridge etc.)*/
                NodeType.GENERIC,
                /** Reserved for later uses. Pass null for now.*/
                null,
                /*Node Listener : Callback for node unregistration.Nodes can be unregistered from enterprise.iot-ignite.com remotely.
                * If your node is unregistered from there -- not your code -- you will receive callback here. */
                this
        );


        /**
         * Check node object and register it.
         */
        registerNodeAndSetConnectionOnline(mySampleNode);

        if (mySampleNode != null && mySampleNode.isRegistered()) {

            mySampleThing = mySampleNode.createThing(

                    /*Thing ID : Must be unique*/
                    THING_ID,

                    /*Define your thing type here. Use ThingType object.
                    * Thing Type objects give information about what type of sensor/actuator you are using.*/
                    sampleThingType,

                    /** You can categorize your thing. EXTERNAL, BUILTIN or UNDEFINED */
                    ThingCategory.EXTERNAL,

                    /**If your thing going to to same action for example opening something or triggering relay,
                     * Set this true. When set it true your things can receive action messages over listener callback.
                     * Otwervise if your thing is only generating data. Set this false.*/
                    true,

                    /** Thing Listener : Callback for thing objects. Listener has three callbacks:
                     * - onConfigurationReceived() : Occurs when configuration setted by IoT-Ignite.
                     * - onActionReceived(): If your thing set as actuator action message will handle here.
                     * - onThingUnregistered(): If your thing unregister from IoT-Ignite you will receive this callback.*/
                    this,

                    /** Reserved for later uses. Pass null for now. */
                    null
            );

            registerThingAndSetConnectionOnline(mySampleNode, mySampleThing);
        }


        // TODO: Register android things node here.

        androidThingsNode = IotIgniteManager.NodeFactory.createNode(
                ANDROID_THINGS_NODE_ID,
                ANDROID_THINGS_NODE_ID,
                NodeType.GENERIC,
                null,
                this
        );

        registerNodeAndSetConnectionOnline(androidThingsNode);



        if (androidThingsNode != null && androidThingsNode.isRegistered()) {

            mLedThing = androidThingsNode.createThing(LED_THING_ID, mLedThingType, ThingCategory.BUILTIN, true, this, null);

            registerThingAndSetConnectionOnline(androidThingsNode, mLedThing);

            if (mLedThing.isRegistered()) {


                try {
                    mLedGpio = mPeripheralManagerService.openGpio(LED_PIN);
                    mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
                } catch (IOException e) {
                    Log.e(TAG, "IOException :" + e);
                }
            }


            //TODO:  Register button thing object here.

            mButtonThing = androidThingsNode.createThing(
                    BTN_THING_ID,
                    mBtnThingType,
                    ThingCategory.BUILTIN,
                    false,
                    this,
                    null);

            registerThingAndSetConnectionOnline(androidThingsNode, mButtonThing);


            // TODO: Use button input driver and register it to space key.

            if(mButtonThing.isRegistered()){

                try {
                    mButtonInputDriver = new ButtonInputDriver(BTN_PIN, Button.LogicState.PRESSED_WHEN_LOW, KeyEvent.KEYCODE_SPACE);
                    mButtonInputDriver.register();
                } catch (IOException e) {
                    Log.i(TAG,"IOException :" + e);
                }

            }





        }


    }

    @Override
    public void onDisconnected() {
        Log.i(TAG, "Ignite Disconnected");
        // start watchdog again here.
        igniteConnected = false;
        startIgniteWatchdog();
    }

    /**
     * Connect to iot ignite
     */

    private void rebuildIgnite() {
        try {
            mIotIgniteManager = new IotIgniteManager.Builder()
                    .setConnectionListener(this)
                    .setContext(appContext)
                    .build();
        } catch (UnsupportedVersionException e) {
            Log.e(TAG, "UnsupportedVersionException : " + e);
        }
    }

    /**
     * remove previous callback and setup new watchdog
     */

    private void startIgniteWatchdog() {
        igniteWatchdog.removeCallbacks(igniteWatchdogRunnable);
        igniteWatchdog.postDelayed(igniteWatchdogRunnable, IGNITE_RECONNECT_INTERVAL);

    }

    @Override
    public void onNodeUnregistered(String s) {

    }

    /**
     * Set all things and nodes connection to offline.
     * When the application close or destroyed.
     */


    public void shutdown() {

        setNodeConnection(mySampleNode, false, ON_DESTROY_MSG);
        setThingConnection(mySampleThing, false, ON_DESTROY_MSG);
        setNodeConnection(androidThingsNode, false, ON_DESTROY_MSG);
        setThingConnection(mButtonThing, false, ON_DESTROY_MSG);
        setThingConnection(mLedThing, false, ON_DESTROY_MSG);

        if (mButtonInputDriver != null) {

            mButtonInputDriver.unregister();

            try {
                mButtonInputDriver.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing Button driver", e);
            } finally {
                mButtonInputDriver = null;
            }
        }

        if (mLedGpio != null) {

            try {
                mLedGpio.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing led gpio", e);
            } finally {
                mLedGpio = null;
            }
        }


    }

    @Override
    public void onConfigurationReceived(Thing thing) {

        /**
         * Thing configuration messages will be handled here.
         * For example data reading frequency or custom configuration may be in the incoming thing object.
         */

    }

    @Override
    public void onActionReceived(String s, String s1, ThingActionData thingActionData) {

        /**
         * Thing action message will be handled here. Call thingActionData.getMessage()
         */

    }

    @Override
    public void onThingUnregistered(String s, String s1) {

        /**
         * If your thing object unregistered from outside world you will receive this
         * information callback.
         */
    }

    public void setLed(boolean state) {

        if (mLedGpio != null) {
            try {
                mLedGpio.setValue(state);

                if (mLedThing != null && mLedThing.isRegistered()) {
                    ThingData ledData = new ThingData();
                    ledData.addData(state ? 1 : 0);
                    mLedThing.setThingData(ledData);
                    mLedThing.sendData(ledData);
                }
            } catch (IOException e) {
                Log.e(TAG, "IOException : " + e);
            }
        }
    }

    private void registerNodeAndSetConnectionOnline(Node mNode) {

        if (mNode != null) {
            if (mNode.isRegistered() || mNode.register()) {
                mNode.setConnected(true, "");
            }
        }
    }

    private void registerThingAndSetConnectionOnline(Node mNode, Thing mThing) {

        if (mNode != null && mNode.isRegistered() && mThing != null) {

            if (mThing.isRegistered() || mThing.register()) {
                mThing.setConnected(true, "");
            }
        }
    }

    private void setThingConnection(Thing mThing, boolean state, String explanation) {
        if (mThing != null) {
            mThing.setConnected(state, explanation);
        }

    }

    private void setNodeConnection(Node mNode, boolean state, String explanation) {
        if (mNode != null) {
            mNode.setConnected(state, explanation);
        }
    }


    public void setButtonState(boolean state){

        if(mButtonThing != null && mButtonThing.isRegistered()){

            ThingData mBtnThingData = new ThingData();

            mBtnThingData.addData(state ? 1 : 0);
            mButtonThing.setThingData(mBtnThingData);
            if(mButtonThing.sendData(mBtnThingData)){
                Log.i(TAG,"Button data sent successfully");
            }
        }
    }

    public  IotIgniteManager getIgniteManager(){
        return this.mIotIgniteManager;
    }

}
