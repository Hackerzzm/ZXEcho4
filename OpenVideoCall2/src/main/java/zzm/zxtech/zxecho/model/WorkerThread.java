package zzm.zxtech.zxecho.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceView;
import io.agora.rtc.Constants;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;
import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zzm.zxtech.propeller.Constant;
import zzm.zxtech.zxecho.R;

public class WorkerThread extends Thread {
  private final static Logger log = LoggerFactory.getLogger(WorkerThread.class);
  private static final int ACTION_WORKER_THREAD_QUIT = 0X1010; // quit this thread
  private static final int ACTION_WORKER_JOIN_CHANNEL = 0X2010;
  private static final int ACTION_WORKER_LEAVE_CHANNEL = 0X2011;
  private static final int ACTION_WORKER_CONFIG_ENGINE = 0X2012;
  private static final int ACTION_WORKER_PREVIEW = 0X2014;
  private final Context mContext;
  private final MyEngineEventHandler mEngineEventHandler;
  private WorkerThreadHandler mWorkerHandler;

  private boolean mReady;
  private RtcEngine mRtcEngine;
  private EngineConfig mEngineConfig;

  public WorkerThread(Context context) {
    this.mContext = context;

    this.mEngineConfig = new EngineConfig();
    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
    this.mEngineConfig.mUid = pref.getInt(ConstantApp.PrefManager.PREF_PROPERTY_UID, 0);

    this.mEngineEventHandler = new MyEngineEventHandler(mContext, this.mEngineConfig);
  }

  public static String getDeviceID(Context context) {
    // XXX according to the API docs, this value may change after factory reset
    // use Android id as device id
    return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
  }

  public final void waitForReady() {
    while (!mReady) {
      try {
        Thread.sleep(20);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      log.debug("wait for " + WorkerThread.class.getSimpleName());
    }
  }

  @Override public void run() {
    log.trace("start to run");
    Looper.prepare();

    mWorkerHandler = new WorkerThreadHandler(this);

    ensureRtcEngineReadyLock();

    mReady = true;

    // enter thread looper
    Looper.loop();
  }

  public final void enablePreProcessor() {
  }

  public final void setPreParameters(float lightness, int smoothness) {
    Constant.PRP_DEFAULT_LIGHTNESS = lightness;
    Constant.PRP_DEFAULT_SMOOTHNESS = smoothness;
  }

  public final void disablePreProcessor() {
  }

  public final void joinChannel(final String key, final String channel, int uid) {
    if (Thread.currentThread() != this) {
      log.warn("joinChannel() - worker thread asynchronously " + channel + " " + uid);
      Message envelop = new Message();
      envelop.what = ACTION_WORKER_JOIN_CHANNEL;
      envelop.obj = new String[] { channel, key };
      envelop.arg1 = uid;
      mWorkerHandler.sendMessage(envelop);
      return;
    }

    ensureRtcEngineReadyLock();
    // TODO: 2017/12/23
    Log.e("zzm debug!!!", "WorkerThread uid = " + uid);
    mRtcEngine.joinChannel(key, channel, "OpenVCall", uid);

    mEngineConfig.mChannel = channel;

    enablePreProcessor();
    log.debug("joinChannel " + channel + " " + uid);
  }

  public final void leaveChannel(String channel) {
    if (Thread.currentThread() != this) {
      log.warn("leaveChannel() - worker thread asynchronously " + channel);
      Message envelop = new Message();
      envelop.what = ACTION_WORKER_LEAVE_CHANNEL;
      envelop.obj = channel;
      mWorkerHandler.sendMessage(envelop);
      return;
    }

    if (mRtcEngine != null) {
      mRtcEngine.leaveChannel();
      mRtcEngine.enableVideo();
    }

    disablePreProcessor();

    mEngineConfig.reset();
    log.debug("leaveChannel " + channel);
  }

  public final EngineConfig getEngineConfig() {
    return mEngineConfig;
  }

  public final void configEngine(int vProfile, String encryptionKey, String encryptionMode) {
    if (Thread.currentThread() != this) {
      log.warn("configEngine() - worker thread asynchronously " + vProfile + " " + encryptionMode);
      Message envelop = new Message();
      envelop.what = ACTION_WORKER_CONFIG_ENGINE;
      envelop.obj = new Object[] { vProfile, encryptionKey, encryptionMode };
      mWorkerHandler.sendMessage(envelop);
      return;
    }

    ensureRtcEngineReadyLock();
    mEngineConfig.mVideoProfile = vProfile;

    if (!TextUtils.isEmpty(encryptionKey)) {
      mRtcEngine.setEncryptionMode(encryptionMode);

      mRtcEngine.setEncryptionSecret(encryptionKey);
    }

    mRtcEngine.setVideoProfile(mEngineConfig.mVideoProfile, false);

    log.debug("configEngine " + mEngineConfig.mVideoProfile + " " + encryptionMode);
  }

  public final void preview(boolean start, SurfaceView view, int uid) {
    if (Thread.currentThread() != this) {
      log.warn("preview() - worker thread asynchronously " + start + " " + view + " " + (uid & 0XFFFFFFFFL));
      Message envelop = new Message();
      envelop.what = ACTION_WORKER_PREVIEW;
      envelop.obj = new Object[] { start, view, uid };
      mWorkerHandler.sendMessage(envelop);
      return;
    }

    ensureRtcEngineReadyLock();
    if (start) {
      mRtcEngine.switchCamera();
      //mRtcEngine.setParameters("{\"che.video.captureFormatNV21\": true}");

      mRtcEngine.setupLocalVideo(new VideoCanvas(view, VideoCanvas.RENDER_MODE_HIDDEN, uid));
      mRtcEngine.startPreview();
    } else {
      mRtcEngine.stopPreview();
    }
  }

  private RtcEngine ensureRtcEngineReadyLock() {
    if (mRtcEngine == null) {
      String appId = mContext.getString(R.string.private_app_id);
      if (TextUtils.isEmpty(appId)) {
        throw new RuntimeException("NEED TO use your App ID, get your own ID at https://dashboard.agora.io/");
      }
      try {
        mRtcEngine = RtcEngine.create(mContext, appId, mEngineEventHandler.mRtcEventHandler);
      } catch (Exception e) {
        log.error(Log.getStackTraceString(e));
        throw new RuntimeException("NEED TO check rtc sdk init fatal error\n" + Log.getStackTraceString(e));
      }
      if (mRtcEngine == null) {
        Log.e("zzm debug!!!", "mRtcEngine = null");
      }
      mRtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION);
      mRtcEngine.enableVideo();
      mRtcEngine.enableAudioVolumeIndication(200, 3); // 200 ms
      mRtcEngine.setLogFile(Environment.getExternalStorageDirectory() + File.separator + mContext.getPackageName() + "/log/agora-rtc.log");
    }
    return mRtcEngine;
  }

  public MyEngineEventHandler eventHandler() {
    return mEngineEventHandler;
  }

  public RtcEngine getRtcEngine() {
    return mRtcEngine;
  }

  /**
   * call this method to exit
   * should ONLY call this method when this thread is running
   */
  public final void exit() {
    if (Thread.currentThread() != this) {
      log.warn("exit() - exit app thread asynchronously");
      mWorkerHandler.sendEmptyMessage(ACTION_WORKER_THREAD_QUIT);
      return;
    }

    mReady = false;

    // TODO should remove all pending(read) messages

    log.debug("exit() > start");

    // exit thread looper
    Looper.myLooper().quit();

    mWorkerHandler.release();

    log.debug("exit() > end");
  }

  private static final class WorkerThreadHandler extends Handler {

    private WorkerThread mWorkerThread;

    WorkerThreadHandler(WorkerThread thread) {
      this.mWorkerThread = thread;
    }

    public void release() {
      mWorkerThread = null;
    }

    @Override public void handleMessage(Message msg) {
      if (this.mWorkerThread == null) {
        log.warn("handler is already released! " + msg.what);
        return;
      }

      switch (msg.what) {
        case ACTION_WORKER_THREAD_QUIT:
          mWorkerThread.exit();
          break;
        case ACTION_WORKER_JOIN_CHANNEL:
          String[] data = (String[]) msg.obj;
          mWorkerThread.joinChannel(data[1], data[0], msg.arg1);
          break;
        case ACTION_WORKER_LEAVE_CHANNEL:
          String channel = (String) msg.obj;
          mWorkerThread.leaveChannel(channel);
          break;
        case ACTION_WORKER_CONFIG_ENGINE:
          Object[] configData = (Object[]) msg.obj;
          mWorkerThread.configEngine((int) configData[0], (String) configData[1], (String) configData[2]);
          break;
        case ACTION_WORKER_PREVIEW:
          Object[] previewData = (Object[]) msg.obj;
          mWorkerThread.preview((boolean) previewData[0], (SurfaceView) previewData[1], (int) previewData[2]);
          break;
      }
    }
  }
}
