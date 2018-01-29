package zzm.zxtech.signal;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import com.google.gson.Gson;
import io.agora.AgoraAPI;
import io.agora.AgoraAPIOnlySignal;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;
import zzm.zxtech.util.ConstantUtil;
import zzm.zxtech.util.ShellUtils;
import zzm.zxtech.zxecho.AGApplication;
import zzm.zxtech.zxecho.model.ConstantApp;
import zzm.zxtech.zxecho.statistics.VideoRecordDetailsVo;
import zzm.zxtech.zxecho.statistics.VideoRecordVo;
import zzm.zxtech.zxecho.ui.ChatActivity;

/**
 * Created by 92010 on 2017/12/26.
 */

public class SignalService extends Service {
  public static final String appID = "2f6ef44c25f644d3a38146d35a449335";
  public static final String certificate = "192be39be9b64c8f8393cb2f8225e5ba";
  private static final boolean enableMediaCertificate = true;
  private static final boolean enableMedia = false;
  private static SignalService instant;
  private static int my_uid = 0;//onLoginSuccess时记录自己的uid
  private final String TAG = "zzm debug!!!";
  private boolean isLogin = false;
  private boolean m_isjoin = false;//记录是否加入信令通道
  private boolean m_iscalling = false;//记录是否在通话中
  private AgoraAPIOnlySignal m_agoraAPI;
  private String terminal_id = "";
  private String channelName = "";
  private boolean is_being_called = false;//只用于在查询频道用户数是用来判断是否能通话的
  private String whoCalledAccount = "";
  private int whoCalledUid = 0;
  private String videoRecordId = "";
  private String videoRecordDetailsId = "";
  private OkHttpClient client;
  private long lastHeartBeatTime = 0; //记录上次收到心跳的时间
  private Handler signalHandler = new Handler() {
    @Override public void handleMessage(Message msg) {
      super.handleMessage(msg);
      switch (msg.what) {
        case 1:
          m_isjoin = false;
          m_agoraAPI.channelLeave(channelName);
          ChatActivity.ChatLeave();
          updateVideoRecordPost();
          break;
        case 2:
          m_isjoin = true;
          //String channelName = "channel_" + terminal_id;//editTextChannelName.getText().toString().trim()
          Log.e(TAG, "Join channel " + channelName);
          m_agoraAPI.channelJoin(channelName);
          AGApplication.mVideoSettings.mChannelName = channelName;
          AGApplication.mVideoSettings.mEncryptionKey = "";
          AGApplication.mVideoSettings.mEncryptionModeIndex = 0;
          Intent i = new Intent(SignalService.this, ChatActivity.class);
          i.putExtra(ConstantApp.ACTION_KEY_CHANNEL_NAME, channelName);
          i.putExtra(ConstantApp.ACTION_KEY_ENCRYPTION_KEY, "");
          i.putExtra(ConstantApp.ACTION_KEY_ENCRYPTION_MODE, "");
          i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          Log.e("zzm debug!!!", "SignalActivity uid = " + my_uid);
          i.putExtra("uid", my_uid);
          String key = appID;
          if (enableMediaCertificate) {
            int tsWrong = (int) (new Date().getTime() / 1000);
            int ts = (int) (System.currentTimeMillis() / 1000);
            int r = new Random().nextInt();
            long uid = my_uid;
            int expiredTs = 0;
            try {
              key = DynamicKey4.generateMediaChannelKey(appID, certificate, channelName, ts, r, uid, expiredTs);
            } catch (Exception e) {
              e.printStackTrace();
            }
            Log.e(TAG, "media key : " + key);
          }
          i.putExtra("key", key);
          //getResources().getStringArray(zzm.zxtech.zxecho.R.array.encryption_mode_values)[vSettings().mEncryptionModeIndex]
          startActivity(i);
          break;
        default:
          break;
      }
    }
  };
  private long lastLeaveTime = 0; // 记录上次挂断时间，给他15s释放摄像头的时间，以免有人不停挂断呼叫导致异常

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

  public static void SignalLeave() {
    instant.doLeave();
  }

  @Nullable @Override public IBinder onBind(Intent intent) {
    return null;
  }

  @Override public void onCreate() {
    super.onCreate();
    initViews();
    instant = this;
    doLogin();
    startMonitorThread();
  }

  private void startMonitorThread() {
    Thread t = new Thread() {
      @Override public void run() {
        super.run();
        while (true) {

          try {
            Thread.sleep(60 * 1000);//每60秒检测一次
            if (m_iscalling) {
              if ((System.currentTimeMillis() - lastHeartBeatTime) > 80 * 1000) {
                // TODO: 2018/1/25 通知Java服务器强制踢人
                Log.e(TAG, "检测发现超时80s以上没有心跳，踢人");
                channelBroadcast();
                doLeave();
                kickOffPost();
              } else {
                Log.e(TAG, "每60s检测ok！！！");
              }
            } else {
              Log.e(TAG, "不在通话中，不检测！！！");
            }
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
    };
    t.start();
  }

  private void channelBroadcast() {
    Gson gson = new Gson();
    Map<String, Object> msg = new HashMap<>();
    msg.put("code", 105);
    msg.put("from", terminal_id);
    Map<String, Object> content = new HashMap();
    content.put("msg", terminal_id + " is about to leave");
    msg.put("content", content);
    Log.e(TAG, gson.toJson(msg));
    m_agoraAPI.messageChannelSend(channelName, gson.toJson(msg), "");
  }

  private void kickOffPost() {
    RequestBody requestBodyPost = new FormBody.Builder().add("terminalId", terminal_id).build();
    //61.160.96.205:8800/f/mobile/kickOff?terminalId=
    Request requestPost = new Request.Builder().url("http://61.160.96.205:8800/f/mobile/kickOff").post(requestBodyPost).build();
    client.newCall(requestPost).enqueue(new Callback() {
      @Override public void onFailure(Call call, IOException e) {
      }

      @Override public void onResponse(Call call, Response response) throws IOException {
        final String resultStr = response.body().string();
        Log.e("zzm debug!!!", "通知服务器踢人：" + resultStr);
      }
    });
  }

  public String calcToken(String appID, String certificate, String account, long expiredTime) {
    // Token = 1:appID:expiredTime:sign
    // Token = 1:appID:expiredTime:md5(account + vendorID + certificate + expiredTime)
    String sign = md5hex((account + appID + certificate + expiredTime).getBytes());
    return "1:" + appID + ":" + expiredTime + ":" + sign;
  }

  private void initViews() {
    OkhttpClientInit();
    //terminal_id = "746b001";//ConstantUtil.getId(null)
    terminal_id = ConstantUtil.getId(null);
    channelName = "channel_" + terminal_id;
    Log.e(TAG, "get terminal_id = " + terminal_id);
    m_agoraAPI = AgoraAPIOnlySignal.getInstance(this, appID);
    m_agoraAPI.callbackSet(new AgoraAPI.CallBack() {
      // 2017/12/20 监听回调
      //https://docs.agora.io/cn/2.0.2/addons/Signaling/API%20Reference/signal_android?platform=Android
      //登录成功回调
      @Override public void onLoginSuccess(int uid, int fd) {
        //登录成功回调
        super.onLoginSuccess(uid, fd);
        my_uid = uid;
        m_agoraAPI.setAttr("uid", uid + "");
        Log.e(TAG, "SignalService onLoginSuccess \nfd = " + fd + "\nuid = " + uid);
      }

      //登录失败回调
      @Override public void onLoginFailed(int ecode) {
        super.onLoginFailed(ecode);
        Log.e(TAG, "SignalService onLoginFailed ecode = " + ecode);
        doLogin();
      }

      //退出登录回调
      @Override public void onLogout(int ecode) {
        super.onLogout(ecode);
        Log.e(TAG, "SignalService onLogout ecode = " + ecode);
        doLogin();
      }

      //Message发送成功回调
      @Override public void onMessageSendSuccess(String messageID) {
        super.onMessageSendSuccess(messageID);
        Log.e(TAG, "SignalService onMessageSendSuccess");
      }

      //Message发送失败回调
      @Override public void onMessageSendError(String messageID, int ecode) {
        super.onMessageSendError(messageID, ecode);
        Log.e(TAG, "SignalService onMessageSendError");
      }

      //收到Message回调
      @Override public void onMessageInstantReceive(String account, int uid, String msg) {
        super.onMessageInstantReceive(account, uid, msg);
        Log.e(TAG, "SignalService onMessageInstantReceive \naccount =" + account + "\nmsg = " + msg);
        // TODO: 2018/1/24 添加心跳机制，当有用户异常退出时能通知服务器将这个频道票内的所有用户踢出
        lastHeartBeatTime = System.currentTimeMillis();
      }

      //加入频道回调
      @Override public void onChannelJoined(String channelID) {
        super.onChannelJoined(channelID);
        Log.e(TAG, "SignalService onChannelJoined channelID = " + channelID);
      }

      //加入频道失败回调
      @Override public void onChannelJoinFailed(String channelID, int ecode) {
        super.onChannelJoinFailed(channelID, ecode);
        Log.e(TAG, "SignalService onChannelJoinFailed \nchannelID = " + channelID + "\necode = " + ecode);
      }

      //有其他用户加入时回调
      @Override public void onChannelUserJoined(String account, int uid) {
        super.onChannelUserJoined(account, uid);
        Log.e(TAG, "SignalService onChannelUserJoined \naccount = " + account + "\nuid = " + uid);
      }

      //有其他用户离开时回调
      @Override public void onChannelUserLeaved(String account, int uid) {
        super.onChannelUserLeaved(account, uid);
        Log.e(TAG, "SignalService onChannelUserLeaved \naccount = " + account + "\nuid = " + uid);
        m_agoraAPI.channelQueryUserNum(channelName);
      }

      //查询指定频道的用户数量成功后回调
      @Override public void onChannelQueryUserNumResult(String channelID, int ecode, int num) {
        super.onChannelQueryUserNumResult(channelID, ecode, num);
        Log.e(TAG, "SignalService onChannelQueryUserNumResult \nchannelID = " + channelID + "\necode = " + ecode + "\nnum=" + num);
        if (is_being_called) {
          if (num < 6) {
            try {
              Thread.sleep(2000);
              Log.e(TAG, "SignalService I accept！");
              m_agoraAPI.channelInviteAccept(channelID, whoCalledAccount, whoCalledUid);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          } else {
            m_agoraAPI.channelInviteRefuse(channelID, whoCalledAccount, 0, "{\"msg\":\"人数过多\"}");
          }
          is_being_called = false;
        } else {
          if (num < 2) {
            Log.e(TAG, "SignalService onChannelQueryUserNumResult 没其他人在这个频道了");
            doLeave();
          }
        }
      }

      //用户状态查询回调
      @Override public void onQueryUserStatusResult(String name, String status) {
        super.onQueryUserStatusResult(name, status);
        Log.e(TAG, "SignalService onQueryUserStatusResult \nname = " + name + "\nstatus = " + status);
      }

      //收到频道消息回调
      @Override public void onMessageChannelReceive(String channelID, String account, int uid, String msg) {
        super.onMessageChannelReceive(channelID, account, uid, msg);
        Log.e(TAG,
            "SignalService onMessageChannelReceive \nchannelID = " + channelID + "\naccount = " + account + "\nuid = " + uid + "\nmsg = " + msg);
      }

      //出错回调
      @Override public void onError(String name, int ecode, String desc) {
        super.onError(name, ecode, desc);
        Log.e(TAG, "SignalService onError \nname = " + name + "\necode = " + ecode + "\ndesc = " + desc);
        doLeave();
      }

      //重连成功回调
      @Override public void onReconnected(int fd) {
        super.onReconnected(fd);
        Log.e(TAG, "SignalService onReconnected fd = " + fd);
      }

      //连接丢失回调
      @Override public void onReconnecting(int nretry) {
        super.onReconnecting(nretry);
        Log.e(TAG, "SignalService onReconnecting nretry = " + nretry);
        doLeave();
        m_iscalling = false;
        // 2018/1/5 断线重连
        if (nretry > 3) {
          doLogin();
        }
      }

      @Override public void onInviteReceived(String channelID, String account, int uid, String extra) {
        super.onInviteReceived(channelID, account, uid, extra);
        Log.e(TAG, "SignalService onInviteReceived m_iscalling = " + m_iscalling);
        String result = ShellUtils.execCommand("ls /dev", true).successMsg;
        int resultvideonum = result.split("video").length;
        Log.e(TAG, "resultvideonum = " + resultvideonum);
        if (resultvideonum < 3) {
          m_agoraAPI.channelInviteRefuse(channelID, account, 0, "{\"msg\":\"未插摄像头\"}");
        } else {
          Log.e(TAG, "m_iscalling = " + m_iscalling);
          //{"msg":"未插摄像头"}
          if (m_iscalling) {//正在通话中
            whoCalledAccount = account;
            whoCalledUid = uid;
            is_being_called = true;
            m_agoraAPI.channelQueryUserNum(channelName);
          } else {//不在通话
            doJoin();
            saveVideoRecordPost();
            try {
              Thread.sleep(2000);
              Log.e(TAG, "SignalService I accept！");
              m_agoraAPI.channelInviteAccept(channelID, account, uid);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          }
        }
      }

      @Override public void onInviteAcceptedByPeer(String channelID, String account, int uid, String extra) {
        super.onInviteAcceptedByPeer(channelID, account, uid, extra);
        Log.e(TAG,
            "SignalService onInviteAcceptedByPeer\n channelID = " + channelID + "\naccount = " + account + "\nuid = " + uid + "\nextra = " + extra);
        doJoin();
      }
    });
  }

  private void doLogin() {
    m_iscalling = false;
    ChatActivity.ChatLeave();
    long expiredTime = System.currentTimeMillis() / 1000 + 3600;
    String token = calcToken(appID, certificate, terminal_id, expiredTime);
    //m_agoraAPI.login(appID, account, token, 0, "");
    m_agoraAPI.login2(appID, terminal_id, token, 0, "", 60, 5);
  }

  private void doJoin() {
    if (m_iscalling) {
      Log.e(TAG, "SignalService 已经在通话中不执行doJoin");
      return;
    } else {
      Log.e(TAG, "SignalService 不在通话中，执行doJoin");
    }
    lastHeartBeatTime = System.currentTimeMillis();
    //增加停止录像功能
    Log.e("zzm debug!!!", "发送关闭后台录制广播");
    Intent intent = new Intent("com.zxtech.BG_RECORDING_STOP");
    sendBroadcast(intent);
    Intent intent2 = new Intent();
    intent2.setComponent(new ComponentName("com.zxtech.zzm.backgroundrecordingmonitor", "com.zxtech.zzm.backgroundrecordingmonitor.MonitorService"));
    stopService(intent2);
    //等待一段时间，让录制视频关闭，释放摄像头
    try {
      Thread.sleep(3000);
      m_iscalling = true;
      signalHandler.sendEmptyMessage(2);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void doLeave() {
    Log.e(TAG, "SignalService at doLeave");
    //隐式Intent 启动Service
    new MyTask().execute();
    Intent intent = new Intent(Intent.ACTION_MAIN);
    intent.addCategory(Intent.CATEGORY_LAUNCHER);
    ComponentName cn = new ComponentName("com.zxtech.zzm.zxtablet", "com.zxtech.zzm.zxtablet.ui.activity.Main2Activity");
    intent.setComponent(cn);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    startActivity(intent);
    lastLeaveTime = System.currentTimeMillis();
    m_iscalling = false;
    signalHandler.sendEmptyMessage(1);
  }

  private void OkhttpClientInit() {
    client = new OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS).build();
  }

  private void saveVideoRecordPost() {
    VideoRecordVo videoRecordVo = new VideoRecordVo();
    videoRecordVo.setBeCalledId(terminal_id);
    videoRecordVo.setCallType("1");
    videoRecordVo.setType("0");
    VideoRecordDetailsVo videoRecordDetailsVo = new VideoRecordDetailsVo();
    videoRecordDetailsVo.setUserId(terminal_id);
    videoRecordDetailsVo.setUserIp("");
    videoRecordDetailsVo.setType("2");
    videoRecordDetailsVo.setCallType("1");
    RequestBody requestBodyPost = new FormBody.Builder().add("httpCmd", "saveVideoRecord")
        .add("videoRecordVo", new Gson().toJson(videoRecordVo))
        .add("videoRecordDetailsVo", new Gson().toJson(videoRecordDetailsVo))
        .build();
    Request requestPost = new Request.Builder().url(ConstantUtil.JAVA_STATISTICS_URL).post(requestBodyPost).build();
    client.newCall(requestPost).enqueue(new Callback() {
      @Override public void onFailure(Call call, IOException e) {

      }

      @Override public void onResponse(Call call, Response response) throws IOException {
        final String resultStr = response.body().string();
        Log.e("zzm debug!!!", resultStr);
        try {
          JSONObject resultObj = new JSONObject(resultStr);
          JSONObject contentObj = resultObj.getJSONObject("content");
          videoRecordId = contentObj.getString("videoRecordId");
          videoRecordDetailsId = contentObj.getString("videoRecordDetailsId");
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }
    });
  }

  private void updateVideoRecordPost() {
    VideoRecordVo videoRecordVo = new VideoRecordVo();
    videoRecordVo.setVideoRecordId(videoRecordId);
    VideoRecordDetailsVo videoRecordDetailsVo = new VideoRecordDetailsVo();
    videoRecordDetailsVo.setVideoRecordDetailsId(videoRecordDetailsId);
    RequestBody requestBodyPost = new FormBody.Builder().add("httpCmd", "updateVideoRecord")
        .add("videoRecordVo", new Gson().toJson(videoRecordVo))
        .add("videoRecordDetailsVo", new Gson().toJson(videoRecordDetailsVo))
        .build();
    Request requestPost = new Request.Builder().url(ConstantUtil.JAVA_STATISTICS_URL).post(requestBodyPost).build();
    client.newCall(requestPost).enqueue(new Callback() {
      @Override public void onFailure(Call call, IOException e) {

      }

      @Override public void onResponse(Call call, Response response) throws IOException {
        final String resultStr = response.body().string();
        Log.e("zzm debug!!!", resultStr);
      }
    });
  }

  class MyTask extends AsyncTask<Void, Void, Void> {
    @Override protected Void doInBackground(Void... params) {
      Log.e("zzm debug!!!", "linphone 启动 backgroundrecordingmonitor");
      /*// 实例化Intent
      Intent it = new Intent();
      //设置Intent的Action属性
      it.setComponent(new ComponentName("com.zxtech.zxbackgroundrecording","com.zxtech.zxbackgroundrecording.BackgroundVideoRecorder"));//设置一个组件名称  同组件名来启动所需要启动Service
      // 启动Activity
      startService(it);*/
      Intent intent2 = new Intent();
      intent2.setComponent(
          new ComponentName("com.zxtech.zzm.backgroundrecordingmonitor", "com.zxtech.zzm.backgroundrecordingmonitor.MonitorService"));
      startService(intent2);
      return null;
    }
  }
}
