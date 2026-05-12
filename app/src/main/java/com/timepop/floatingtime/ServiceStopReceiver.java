package com.timepop.floatingtime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ServiceStopReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("com.timepop.floatingtime.SERVICE_STOPPED".equals(intent.getAction())) {
            Intent launchIntent = new Intent(context, MainActivity.class);
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            launchIntent.putExtra("service_running", false);
            context.startActivity(launchIntent);
        }
    }
}
