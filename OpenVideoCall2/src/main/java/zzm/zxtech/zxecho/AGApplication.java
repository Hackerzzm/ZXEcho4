package zzm.zxtech.zxecho;

import android.app.Application;
import android.content.Context;
import zzm.zxtech.zxecho.model.CurrentUserSettings;
import zzm.zxtech.zxecho.model.WorkerThread;

public class AGApplication extends Application {

  public static final CurrentUserSettings mVideoSettings = new CurrentUserSettings();
  private static Context applicationCtx;
  private WorkerThread mWorkerThread;

  public static Context getContext() {
    return applicationCtx;
  }

  @Override public void onCreate() {
    super.onCreate();
    applicationCtx = AGApplication.this.getApplicationContext();
  }

  public synchronized void initWorkerThread() {
    if (mWorkerThread == null) {
      mWorkerThread = new WorkerThread(getApplicationContext());
      mWorkerThread.start();

      mWorkerThread.waitForReady();
    }
  }

  public synchronized WorkerThread getWorkerThread() {
    return mWorkerThread;
  }

  public synchronized void deInitWorkerThread() {
    mWorkerThread.exit();
    try {
      mWorkerThread.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    mWorkerThread = null;
  }
}
