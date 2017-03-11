package com.ardic.iot.myandroidthingsproject.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.ardic.iot.myandroidthingsproject.MainActivity;

/**
 * Created by yavuz.erzurumlu on 2/20/17.
 */

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = BootReceiver.class.getSimpleName();
    @Override
    public void onReceive(Context context, Intent intent) {

        Log.i(TAG,"Boot broadcast received!! Starting application");
        Intent applicationIntent = new Intent(context, MainActivity.class);
        context.startActivity(applicationIntent);

    }
}


