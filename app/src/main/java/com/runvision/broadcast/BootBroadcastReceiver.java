package com.runvision.broadcast;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.runvision.g68a_sn.LoginActivity;
import com.runvision.g68a_sn.MainActivity;

public class BootBroadcastReceiver extends BroadcastReceiver {
    static final String ACTION = "android.intent.action.BOOT_COMPLETED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ACTION)) {
            Intent mainActivityIntent = new Intent(context, LoginActivity.class);
            mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(mainActivityIntent);
        }
    }

}
