package zzm.zxtech.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import zzm.zxtech.zxecho.AGApplication;

/**
 * Created by 92010 on 2017/12/23.
 */

public class ConstantUtil {
  public static final String APP_PREFERENCE_FILE = "ZXTABLET_PREFERENCE";
  public static final String JAVA_STATISTICS_URL = "http://172.16.10.58:8080/SmartElevatorGuard/f/mobile/applogin";
  public static String getId(Context ctx) {
    int MODE = Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE;
    String id = "1111111";
    if (ctx == null) {
      ctx = AGApplication.getContext();
    }
    Context firstAppContext = null;
    try {
      firstAppContext = ctx.createPackageContext("com.zxtech.zzm.zxtablet", Context.CONTEXT_IGNORE_SECURITY);
      SharedPreferences sharedPreferences = firstAppContext.getSharedPreferences(APP_PREFERENCE_FILE, MODE);
      id = sharedPreferences.getString("ID", "1111111");
    } catch (PackageManager.NameNotFoundException e) {
      e.printStackTrace();
    }
    return id;
  }
}
