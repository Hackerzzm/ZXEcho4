package zzm.zxtech.signal;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import butterknife.BindView;
import butterknife.OnClick;
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
import zzm.zxtech.zxecho.R;
import zzm.zxtech.zxecho.model.ConstantApp;
import zzm.zxtech.zxecho.statistics.VideoRecordDetailsVo;
import zzm.zxtech.zxecho.statistics.VideoRecordVo;
import zzm.zxtech.zxecho.ui.BaseActivity;
import zzm.zxtech.zxecho.ui.ChatActivity;

public class SignalActivity extends BaseActivity {
  //public static final String appID = "88c6cea1c8aa408b892d9d632a4d206b";
  //public static final String certificate = "73f5d0b232b94cf58fd9391cc019f1ff";

  public static final String appID = "2f6ef44c25f644d3a38146d35a449335";
  public static final String certificate = "192be39be9b64c8f8393cb2f8225e5ba";
  private static final boolean enableMediaCertificate = true;
  private static final boolean enableMedia = false;
  private static SignalActivity instant;
  private static int my_uid = 0;//onLoginSuccess时记录自己的uid
  private static boolean m_iscalling = false;//记录是否在通话中
  private final String TAG = "zzm debug!!!";
  @BindView(R.id.buttonLogin) Button buttonLogin;
  @BindView(R.id.editTextName) EditText editTextName;//用户1
  @BindView(R.id.buttonSwitch) Button buttonSwitch;
  @BindView(R.id.buttonCall) Button buttonCall;
  @BindView(R.id.editTextCallUser) EditText editTextCallUser;//用户2
  @BindView(R.id.buttonInstMsg) Button buttonInstMsg;
  @BindView(R.id.buttonJoin) Button buttonJoin;
  @BindView(R.id.editTextChannelName) EditText editTextChannelName;//频道
  //@BindView(R.id.buttonSendMsg) Button buttonSendMsg;
  @BindView(R.id.checkBoxSpeaker) CheckBox checkBoxSpeaker;
  @BindView(R.id.checkBoxMute) CheckBox checkBoxMute;
  @BindView(R.id.checkBoxVideo) CheckBox checkBoxVideo;
  @BindView(R.id.buttonEnv) Button buttonEnv;
  @BindView(R.id.buttonQueryUserStatus) Button buttonQueryUserStatus;
  @BindView(R.id.buttonChannelQueryUserNum) Button buttonChannelQueryUserNum;
  @BindView(R.id.buttonMessageChannelSend) Button buttonMessageChannelSend;
  private boolean isLogin = false;
  private boolean m_isjoin = false;//记录是否加入信令通道
  private AgoraAPIOnlySignal m_agoraAPI;
  private String terminal_id = "";
  private String channelName = "";
  private boolean is_being_called = false;//只用于在查询频道用户数是用来判断是否能通话的
  private String whoCalledAccount = "";
  private int whoCalledUid = 0;
  private OkHttpClient client;
  private String videoRecordId = "";
  private String videoRecordDetailsId = "";

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
    if (instant != null) {
      instant.doLeave();
    }
  }

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.signal);
    /*ButterKnife.bind(this);
    OkhttpClientInit();
    initViews();
    instant = this;*/

    Intent intent = new Intent(this, SignalService.class);
    startService(intent);
    finish();
    try {
      Thread.sleep(5000);
      finish();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  @Override protected void initUIandEvent() {
  }

  @Override protected void deInitUIandEvent() {
  }

  private void initViews() {
    //terminal_id = "746b001";//ConstantUtil.getId(null)
    terminal_id = ConstantUtil.getId(null);
    channelName = "channel_" + terminal_id;
    Log.e(TAG, "get terminal_id = " + terminal_id);
    m_agoraAPI = AgoraAPIOnlySignal.getInstance(this, appID);
    m_agoraAPI.callbackSet(new AgoraAPI.CallBack() {
      //https://docs.agora.io/cn/2.0.2/addons/Signaling/API%20Reference/signal_android?platform=Android
      //登录成功回调
      @Override public void onLoginSuccess(int uid, int fd) {
        //登录成功回调
        super.onLoginSuccess(uid, fd);
        my_uid = uid;
        m_agoraAPI.setAttr("uid", my_uid + "");
        Log.e(TAG, "onLoginSuccess \nfd = " + fd + "\nuid = " + uid);
      }

      //登录失败回调
      @Override public void onLoginFailed(int ecode) {
        super.onLoginFailed(ecode);
        Log.e(TAG, "onLoginFailed ecode = " + ecode);
      }

      //退出登录回调
      @Override public void onLogout(int ecode) {
        super.onLogout(ecode);
        Log.e(TAG, "onLogout ecode = " + ecode);
      }

      //Message发送成功回调
      @Override public void onMessageSendSuccess(String messageID) {
        super.onMessageSendSuccess(messageID);
        Log.e(TAG, "onMessageSendSuccess");
      }

      @Override public void onInviteRefusedByPeer(String channelID, String account, int uid, String extra) {
        super.onInviteRefusedByPeer(channelID, account, uid, extra);
        Log.e(TAG, "onInviteRefusedByPeer");
      }

      //Message发送失败回调
      @Override public void onMessageSendError(String messageID, int ecode) {
        super.onMessageSendError(messageID, ecode);
        Log.e(TAG, "onMessageSendError");
      }

      //收到Message回调
      @Override public void onMessageInstantReceive(String account, int uid, String msg) {
        super.onMessageInstantReceive(account, uid, msg);
        Log.e(TAG, "onMessageInstantReceive \naccount =" + account + "\nmsg = " + msg);
      }

      //加入频道回调
      @Override public void onChannelJoined(String channelID) {
        super.onChannelJoined(channelID);
        Log.e(TAG, "onChannelJoined channelID = " + channelID);
      }

      //加入频道失败回调
      @Override public void onChannelJoinFailed(String channelID, int ecode) {
        super.onChannelJoinFailed(channelID, ecode);
        Log.e(TAG, "onChannelJoinFailed \nchannelID = " + channelID + "\necode = " + ecode);
      }

      //有其他用户加入时回调
      @Override public void onChannelUserJoined(String account, int uid) {
        super.onChannelUserJoined(account, uid);
        Log.e(TAG, "onChannelUserJoined \naccount = " + account + "\nuid = " + uid);
      }

      //有其他用户离开时回调
      @Override public void onChannelUserLeaved(String account, int uid) {
        super.onChannelUserLeaved(account, uid);
        Log.e(TAG, "onChannelUserLeaved \naccount = " + account + "\nuid = " + uid);
        //m_agoraAPI.channelQueryUserNum(channelName);
        m_agoraAPI.channelQueryUserNum(editTextChannelName.getText().toString().trim());
      }

      //查询指定频道的用户数量成功后回调
      @Override public void onChannelQueryUserNumResult(String channelID, int ecode, int num) {
        super.onChannelQueryUserNumResult(channelID, ecode, num);
        Log.e(TAG, "onChannelQueryUserNumResult \nchannelID = " + channelID + "\necode = " + ecode + "\nnum=" + num);
        if (is_being_called) {
          if (num < 6) {
            try {
              Thread.sleep(2000);
              Log.e(TAG, "I accept！");
              m_agoraAPI.channelInviteAccept(channelID, whoCalledAccount, whoCalledUid);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          } else {
            m_agoraAPI.channelInviteRefuse(channelID, terminal_id, my_uid, "{msg:\"人数过多\"}");
          }
          is_being_called = false;
        } else {
          if (num < 2) {
            Log.e(TAG, "onChannelQueryUserNumResult 没其他人在这个频道了");
            doLeave();
          }
        }
      }

      //用户状态查询回调
      @Override public void onQueryUserStatusResult(String name, String status) {
        super.onQueryUserStatusResult(name, status);
        Log.e(TAG, "onQueryUserStatusResult \nname = " + name + "\nstatus = " + status);
      }

      //收到频道消息回调
      @Override public void onMessageChannelReceive(String channelID, String account, int uid, String msg) {
        super.onMessageChannelReceive(channelID, account, uid, msg);
        Log.e(TAG, "onMessageChannelReceive \nchannelID = " + channelID + "\naccount = " + account + "\nuid = " + uid + "\nmsg = " + msg);
      }

      //出错回调
      @Override public void onError(String name, int ecode, String desc) {
        super.onError(name, ecode, desc);
        Log.e(TAG, "onError \nname = " + name + "\necode = " + ecode + "\ndesc = " + desc);
      }

      //重连成功回调
      @Override public void onReconnected(int fd) {
        super.onReconnected(fd);
        Log.e(TAG, "onReconnected fd = " + fd);
      }

      //连接丢失回调
      @Override public void onReconnecting(int nretry) {
        super.onReconnecting(nretry);
        Log.e(TAG, "onReconnecting nretry = " + nretry);
      }

      @Override public void onInviteReceived(String channelID, String account, int uid, String extra) {
        super.onInviteReceived(channelID, account, uid, extra);
        Log.e(TAG, "onInviteReceived m_iscalling = " + m_iscalling);
        if (m_iscalling) {//正在通话中
          whoCalledAccount = account;
          whoCalledUid = uid;
          is_being_called = true;
          m_agoraAPI.channelQueryUserNum("channel_" + terminal_id);
        } else {//不在通话
          doJoin();
          saveVideoRecordPost();
          try {
            Thread.sleep(2000);
            Log.e(TAG, "I accept！");
            m_agoraAPI.channelInviteAccept(channelID, account, uid);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }

      @Override public void onInviteAcceptedByPeer(String channelID, String account, int uid, String extra) {
        super.onInviteAcceptedByPeer(channelID, account, uid, extra);
        Log.e(TAG, "onInviteAcceptedByPeer\n channelID = " + channelID + "\naccount = " + account + "\nuid = " + uid + "\nextra = " + extra);
        doJoin();
      }
    });
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
        runOnUiThread(new Runnable() {
          @Override public void run() {
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
        runOnUiThread(new Runnable() {
          @Override public void run() {
            Log.e("zzm debug!!!", resultStr);
          }
        });
      }
    });
  }

  @OnClick({
      R.id.buttonLogin, R.id.buttonSwitch, R.id.buttonInstMsg, R.id.buttonJoin, R.id.buttonEnv, R.id.buttonQueryUserStatus,
      R.id.buttonChannelQueryUserNum, R.id.buttonCall, R.id.buttonMessageChannelSend
  }) public void onViewClicked(View view) {
    switch (view.getId()) {
      case R.id.buttonLogin:
        //登录、登出
        if (isLogin) {
          set_state_logout();
          m_agoraAPI.logout();
        } else {
          set_state_login();
          String account = editTextName.getText().toString().trim();
          Log.e(TAG, "Login : appID=" + appID + ", account=" + account);
          long expiredTimeWrong = new Date().getTime() / 1000 + 3600;
          long expiredTime = System.currentTimeMillis() / 1000 + 3600;
          String token = calcToken(appID, certificate, account, expiredTime);
          //m_agoraAPI.login(appID, account, token, 0, "");
          m_agoraAPI.login2(appID, account, token, 0, "", 60, 5);
        }
        break;
      case R.id.buttonSwitch:
        String temp = editTextCallUser.getText().toString().trim();
        editTextCallUser.setText(editTextName.getText().toString().trim());
        editTextName.setText(temp);
        break;
      case R.id.buttonInstMsg:
        //发送点对点消息
        if (!isLogin) {
          return;
        }
        m_agoraAPI.messageInstantSend(editTextCallUser.getText().toString().trim(), 0, "hello world ", "");
        break;
      case R.id.buttonJoin:
        if (!isLogin) {
          return;
        }
        if (m_isjoin) {
          doLeave();
        } else {
          doJoin();
        }
        break;
      case R.id.buttonCall:
        String channelName2 = editTextChannelName.getText().toString().trim();
        String peer = editTextCallUser.getText().toString().trim();
        String src = editTextName.getText().toString().trim();
        if (buttonCall.getText().toString().equals("End")) {
          buttonCall.setText("Call");
          m_agoraAPI.channelInviteEnd(channelName2, peer, 0);
          doLeave();
        } else {
          buttonCall.setText("End");
          // TODO: 2017/12/20 channelInviteUser;channelInviteUser2;channelInviteDTMF;channelInviteAccept;channelInviteRefuse;channelInviteEnd;
          //m_agoraAPI.channelInvitePhone3(channelName2, peer, src, "{\"sip_header:myheader\":\"gogo\"}");// (int)Long.parseLong(peerUid))
          m_agoraAPI.channelInviteUser2(channelName2, peer, "{\"sip_header:myheader\":\"gogo\"}");
        }
        break;
      case R.id.buttonMessageChannelSend:
        if (!isLogin) {
          return;
        }
        Gson gson = new Gson();
        Map<String, Object> msg = new HashMap<>();
        msg.put("code", 105);
        msg.put("from", terminal_id);
        Map<String, Object> content = new HashMap();
        content.put("msg", terminal_id + " is about to leave");
        msg.put("content", content);
        Log.e(TAG, gson.toJson(msg));
        m_agoraAPI.messageChannelSend(channelName, gson.toJson(msg), "");
        break;
      case R.id.buttonEnv:
        break;
      case R.id.buttonQueryUserStatus:
        m_agoraAPI.queryUserStatus(editTextName.getText().toString().trim());
        break;
      case R.id.buttonChannelQueryUserNum:
        m_agoraAPI.channelQueryUserNum(editTextChannelName.getText().toString().trim());
        break;
    }
  }

  public String calcToken(String appID, String certificate, String account, long expiredTime) {
    // Token = 1:appID:expiredTime:sign
    // Token = 1:appID:expiredTime:md5(account + vendorID + certificate + expiredTime)
    String sign = md5hex((account + appID + certificate + expiredTime).getBytes());
    return "1:" + appID + ":" + expiredTime + ":" + sign;
  }

  private void set_state_logout() {
    isLogin = false;
    runOnUiThread(new Runnable() {
      @Override public void run() {
        Button btn = (Button) findViewById(R.id.buttonLogin);
        btn.setText("Login");
      }
    });
  }

  private void set_state_login() {
    isLogin = true;
    runOnUiThread(new Runnable() {
      @Override public void run() {
        Button btn = (Button) findViewById(R.id.buttonLogin);
        btn.setText("Logout");
      }
    });
  }

  private void doJoin() {
    if (m_iscalling) {
      Log.e(TAG, "已经在通话中不执行doJoin");
      return;
    } else {
      Log.e(TAG, "不在通话中，执行doJoin");
    }
    m_iscalling = true;
    runOnUiThread(new Runnable() {
      @Override public void run() {
        final Button btn = (Button) findViewById(R.id.buttonJoin);
        m_isjoin = true;
        btn.setText("Leave");
        String channelName = editTextChannelName.getText().toString().trim();
        Log.e(TAG, "Join channel " + channelName);
        m_agoraAPI.channelJoin(channelName);

        vSettings().mChannelName = channelName;
        vSettings().mEncryptionKey = "";
        vSettings().mEncryptionModeIndex = 0;
        Intent i = new Intent(SignalActivity.this, ChatActivity.class);
        i.putExtra(ConstantApp.ACTION_KEY_CHANNEL_NAME, channelName);
        i.putExtra(ConstantApp.ACTION_KEY_ENCRYPTION_KEY, "");
        i.putExtra(ConstantApp.ACTION_KEY_ENCRYPTION_MODE, "");
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
      }
    });
  }

  private void doLeave() {
    Log.e(TAG, "at doLeave");
    m_iscalling = false;
    runOnUiThread(new Runnable() {
      @Override public void run() {
        final Button btn = (Button) findViewById(R.id.buttonJoin);
        m_isjoin = false;
        btn.setText("Join");
        final Button btn2 = (Button) findViewById(R.id.buttonCall);
        btn2.setText("Call");
        String channelName = editTextChannelName.getText().toString().trim();
        m_agoraAPI.channelLeave(channelName);
        ChatActivity.ChatLeave();
        updateVideoRecordPost();
      }
    });
  }
}
