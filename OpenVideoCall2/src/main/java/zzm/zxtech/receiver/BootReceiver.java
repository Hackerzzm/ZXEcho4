package zzm.zxtech.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import zzm.zxtech.signal.SignalActivity;

/**
 * Created by 92010 on 2018/1/5.
 */

public class BootReceiver extends BroadcastReceiver {
  @Override public void onReceive(Context context, Intent intent) {
    if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {     // boot
      Intent intent2 = new Intent(context, SignalActivity.class);
      //          intent2.setAction("android.intent.action.MAIN");
      //          intent2.addCategory("android.intent.category.LAUNCHER");
      intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      context.startActivity(intent2);
    }
  }
}
