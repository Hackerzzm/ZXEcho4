package zzm.zxtech.signal;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * Created by 92010 on 2017/12/29.
 */

public class MonitorService extends Service {
  @Nullable @Override public IBinder onBind(Intent intent) {
    return null;
  }
}
