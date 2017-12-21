package zzm.zxtech.zxecho;

import android.app.Application;
import zzm.zxtech.zxecho.model.CurrentUserSettings;
import zzm.zxtech.zxecho.model.WorkerThread;

public class AGApplication extends Application {

  public static final CurrentUserSettings mVideoSettings = new CurrentUserSettings();
  private WorkerThread mWorkerThread;

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
