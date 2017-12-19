package io.agora.tutorials1v1vcall;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;
import com.orhanobut.logger.Logger;
import io.agora.AgoraAPI;
import io.agora.AgoraAPIOnlySignal;
import io.agora.media.DynamicKey4;
import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Random;

public class VideoChatViewActivity extends AppCompatActivity {

  private static final String LOG_TAG = VideoChatViewActivity.class.getSimpleName();

  private static final int PERMISSION_REQ_ID_RECORD_AUDIO = 22;
  private static final int PERMISSION_REQ_ID_CAMERA = PERMISSION_REQ_ID_RECORD_AUDIO + 1;
  final String appID = "88c6cea1c8aa408b892d9d632a4d206b";
  final String certificate = "73f5d0b232b94cf58fd9391cc019f1ff";
  private RtcEngine mRtcEngine;// Tutorial Step 1
  private final IRtcEngineEventHandler mRtcEventHandler =
      new IRtcEngineEventHandler() { // Tutorial Step 1
        @Override public void onFirstRemoteVideoDecoded(final int uid, int width, int height,
            int elapsed) { // Tutorial Step 5
          runOnUiThread(new Runnable() {
            @Override public void run() {
              setupRemoteVideo(uid);
            }
          });
        }

        @Override public void onUserOffline(int uid, int reason) { // Tutorial Step 7
          runOnUiThread(new Runnable() {
            @Override public void run() {
              onRemoteUserLeft();
            }
          });
        }

        @Override
        public void onUserMuteVideo(final int uid, final boolean muted) { // Tutorial Step 10
          runOnUiThread(new Runnable() {
            @Override public void run() {
              onRemoteUserVideoMuted(uid, muted);
            }
          });
        }
      };
  private AgoraAPIOnlySignal m_agoraAPI;
  private int my_uid = 0;

  public static String md5hex(byte[] s) {
    MessageDigest messageDigest = null;
    try {
      messageDigest = MessageDigest.getInstance("MD5");
      messageDigest.update(s);
      return hexlify(messageDigest.digest());
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
      return "";
    }
  }

  public static String hexlify(byte[] data) {
    char[] DIGITS_LOWER = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    /**
     * 用于建立十六进制字符的输出的大写字符数组
     */
    char[] DIGITS_UPPER = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    char[] toDigits = DIGITS_LOWER;
    int l = data.length;
    char[] out = new char[l << 1];
    // two characters form the hex value.
    for (int i = 0, j = 0; i < l; i++) {
      out[j++] = toDigits[(0xF0 & data[i]) >>> 4];
      out[j++] = toDigits[0x0F & data[i]];
    }
    return String.valueOf(out);
  }

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_video_chat_view);

    if (checkSelfPermission(Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO)
        && checkSelfPermission(Manifest.permission.CAMERA, PERMISSION_REQ_ID_CAMERA)) {
      initAgoraEngineAndJoinChannel();
    }
    m_agoraAPI = AgoraAPIOnlySignal.getInstance(this, appID);
    initAPI();
    login();
  }

  private void initAgoraEngineAndJoinChannel() {
    initializeAgoraEngine();     // Tutorial Step 1
    setupVideoProfile();         // Tutorial Step 2
    setupLocalVideo();           // Tutorial Step 3
    joinChannel();               // Tutorial Step 4
  }

  public boolean checkSelfPermission(String permission, int requestCode) {
    Log.i(LOG_TAG, "checkSelfPermission " + permission + " " + requestCode);
    if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {

      ActivityCompat.requestPermissions(this, new String[] { permission }, requestCode);
      return false;
    }
    return true;
  }

  @Override public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
      @NonNull int[] grantResults) {
    Log.i(LOG_TAG, "onRequestPermissionsResult " + grantResults[0] + " " + requestCode);

    switch (requestCode) {
      case PERMISSION_REQ_ID_RECORD_AUDIO: {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          checkSelfPermission(Manifest.permission.CAMERA, PERMISSION_REQ_ID_CAMERA);
        } else {
          showLongToast("No permission for " + Manifest.permission.RECORD_AUDIO);
          finish();
        }
        break;
      }
      case PERMISSION_REQ_ID_CAMERA: {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          initAgoraEngineAndJoinChannel();
        } else {
          showLongToast("No permission for " + Manifest.permission.CAMERA);
          finish();
        }
        break;
      }
    }
  }

  public final void showLongToast(final String msg) {
    this.runOnUiThread(new Runnable() {
      @Override public void run() {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
      }
    });
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    leaveChannel();
    RtcEngine.destroy();
    mRtcEngine = null;
  }

  // Tutorial Step 10
  public void onLocalVideoMuteClicked(View view) {
    ImageView iv = (ImageView) view;
    if (iv.isSelected()) {
      iv.setSelected(false);
      iv.clearColorFilter();
    } else {
      iv.setSelected(true);
      iv.setColorFilter(getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.MULTIPLY);
    }

    mRtcEngine.muteLocalVideoStream(iv.isSelected());

    FrameLayout container = findViewById(R.id.local_video_view_container);
    SurfaceView surfaceView = (SurfaceView) container.getChildAt(0);
    surfaceView.setZOrderMediaOverlay(!iv.isSelected());
    surfaceView.setVisibility(iv.isSelected() ? View.GONE : View.VISIBLE);
  }

  // Tutorial Step 9
  public void onLocalAudioMuteClicked(View view) {
    ImageView iv = (ImageView) view;
    if (iv.isSelected()) {
      iv.setSelected(false);
      iv.clearColorFilter();
    } else {
      iv.setSelected(true);
      iv.setColorFilter(getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.MULTIPLY);
    }

    mRtcEngine.muteLocalAudioStream(iv.isSelected());
  }

  // Tutorial Step 8
  public void onSwitchCameraClicked(View view) {
    mRtcEngine.switchCamera();
  }

  // Tutorial Step 6
  public void onEncCallClicked(View view) {
    finish();
  }

  // Tutorial Step 1
  private void initializeAgoraEngine() {
    try {
      mRtcEngine =
          RtcEngine.create(getBaseContext(), getString(R.string.agora_app_id), mRtcEventHandler);
    } catch (Exception e) {
      Log.e(LOG_TAG, Log.getStackTraceString(e));

      throw new RuntimeException(
          "NEED TO check rtc sdk init fatal error\n" + Log.getStackTraceString(e));
    }
  }

  // Tutorial Step 2
  private void setupVideoProfile() {
    mRtcEngine.enableVideo();
    mRtcEngine.setVideoProfile(Constants.VIDEO_PROFILE_360P, false);
    mRtcEngine.switchCamera();
    mRtcEngine.setParameters("{\"che.video.captureFormatNV21\": true}");
    mRtcEngine.enableWebSdkInteroperability(true);
  }

  // Tutorial Step 3
  private void setupLocalVideo() {
    FrameLayout container = findViewById(R.id.local_video_view_container);
    SurfaceView surfaceView = RtcEngine.CreateRendererView(getBaseContext());
    surfaceView.setZOrderMediaOverlay(true);
    container.addView(surfaceView);
    mRtcEngine.setupLocalVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_ADAPTIVE, 0));
  }

  // Tutorial Step 4
  private void joinChannel() {
    String channelName = "demoChannel1";
    int ts = (int) (System.currentTimeMillis() / 1000);
    int r = new Random().nextInt();
    int expiredTs = 0;

    String key = "";
    try {
      key = DynamicKey4.generateMediaChannelKey(appID, certificate, channelName, ts, r, my_uid,
          expiredTs);
      Logger.wtf("key = " + key);
    } catch (Exception e) {
      e.printStackTrace();
    }
    mRtcEngine.joinChannel(key, channelName, "Extra Optional Data",
        0); // if you do not specify the uid, we will generate the uid for you
  }

  // Tutorial Step 5
  private void setupRemoteVideo(int uid) {
    FrameLayout container = findViewById(R.id.remote_video_view_container);

    if (container.getChildCount() >= 1) {
      return;
    }

    SurfaceView surfaceView = RtcEngine.CreateRendererView(getBaseContext());
    container.addView(surfaceView);
    mRtcEngine.setupRemoteVideo(
        new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_ADAPTIVE, uid));

    surfaceView.setTag(uid); // for mark purpose
    View tipMsg = findViewById(R.id.quick_tips_when_use_agora_sdk); // optional UI
    tipMsg.setVisibility(View.GONE);
  }

  // Tutorial Step 6
  private void leaveChannel() {
    mRtcEngine.leaveChannel();
  }

  // Tutorial Step 7
  private void onRemoteUserLeft() {
    FrameLayout container = findViewById(R.id.remote_video_view_container);
    container.removeAllViews();

    View tipMsg = findViewById(R.id.quick_tips_when_use_agora_sdk); // optional UI
    tipMsg.setVisibility(View.VISIBLE);
  }

  // Tutorial Step 10
  private void onRemoteUserVideoMuted(int uid, boolean muted) {
    FrameLayout container = findViewById(R.id.remote_video_view_container);

    SurfaceView surfaceView = (SurfaceView) container.getChildAt(0);

    Object tag = surfaceView.getTag();
    if (tag != null && (Integer) tag == uid) {
      surfaceView.setVisibility(muted ? View.GONE : View.VISIBLE);
    }
  }

  private void login() {
    String account = "zzm";
    Logger.wtf("Login : appID=" + appID + ", account=" + account);
    long expiredTimeWrong = new Date().getTime() / 1000 + 3600;
    long expiredTime = System.currentTimeMillis() / 1000 + 3600;
    String token = calcToken(appID, certificate, account, expiredTime);
    //                        m_agoraAPI.login(appID, account, token, 0, "");
    m_agoraAPI.login2(appID, account, token, 0, "", 60, 5);
  }

  public String calcToken(String appID, String certificate, String account, long expiredTime) {
    // Token = 1:appID:expiredTime:sign
    // Token = 1:appID:expiredTime:md5(account + vendorID + certificate + expiredTime)

    String sign = md5hex((account + appID + certificate + expiredTime).getBytes());
    return "1:" + appID + ":" + expiredTime + ":" + sign;
  }

  private void initAPI() {
    m_agoraAPI.callbackSet(new AgoraAPI.CallBack() {
      @Override public void onLoginSuccess(int uid, int fd) {
        Log.i("sdk2", "login successfully");
        my_uid = uid;
        Log.e("zzm debug!!!", "my_uid = " + my_uid);
      }
    });
  }
}
