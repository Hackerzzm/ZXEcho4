package zzm.zxtech.com.agorademo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.agora.AgoraAPI;
import io.agora.AgoraAPIOnlySignal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
  public static final String appID = "88c6cea1c8aa408b892d9d632a4d206b";
  public static final String certificate = "73f5d0b232b94cf58fd9391cc019f1ff";
  private static final boolean enableMediaCertificate = true;
  private static final boolean enableMedia = false;
  private static int my_uid = 0;//onLoginSuccess时记录自己的uid
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
  private boolean m_iscalling = false;
  private boolean m_isjoin = false;
  private AgoraAPIOnlySignal m_agoraAPI;

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

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.signal);
    ButterKnife.bind(this);
    initViews();
  }

  private void initViews() {
    m_agoraAPI = AgoraAPIOnlySignal.getInstance(this, appID);
    m_agoraAPI.callbackSet(new AgoraAPI.CallBack() {
      // TODO: 2017/12/20 监听回调
      //https://docs.agora.io/cn/2.0.2/addons/Signaling/API%20Reference/signal_android?platform=Android
      @Override public void onLoginSuccess(int uid, int fd) {
        //登录成功回调
        super.onLoginSuccess(uid, fd);
        Log.e(TAG, "onLoginSuccess \nfd = " + fd + "\nuid = " + uid);
      }

      @Override public void onLoginFailed(int ecode) {
        //登录失败回调
        super.onLoginFailed(ecode);
        Log.e(TAG, "onLoginFailed ecode = " + ecode);
      }

      @Override public void onLogout(int ecode) {
        super.onLogout(ecode);
        Log.e(TAG, "onLogout ecode = " + ecode);
      }

      @Override public void onMessageSendSuccess(String messageID) {
        //Message发送成功回调
        super.onMessageSendSuccess(messageID);
        Log.e(TAG, "onMessageSendSuccess");
      }

      @Override public void onMessageSendError(String messageID, int ecode) {
        //Message发送失败回调
        super.onMessageSendError(messageID, ecode);
        Log.e(TAG, "onMessageSendError");
      }

      @Override public void onMessageInstantReceive(String account, int uid, String msg) {
        //收到Message回调
        super.onMessageInstantReceive(account, uid, msg);
        Log.e(TAG, "onMessageInstantReceive \naccount =" + account + "\nmsg = " + msg);
      }

      @Override public void onChannelJoined(String channelID) {
        //加入频道成功回调
        super.onChannelJoined(channelID);
        Log.e(TAG, "onChannelJoined channelID = " + channelID);
      }

      @Override public void onChannelJoinFailed(String channelID, int ecode) {
        //加入频道失败回调
        super.onChannelJoinFailed(channelID, ecode);
        Log.e(TAG, "onChannelJoinFailed \nchannelID = " + channelID + "\necode = " + ecode);
      }

      @Override public void onChannelUserJoined(String account, int uid) {
        //有其他用户加入时回调
        super.onChannelUserJoined(account, uid);
        Log.e(TAG, "onChannelUserJoined \naccount = " + account + "\nuid = " + uid);
      }

      @Override public void onChannelUserLeaved(String account, int uid) {
        //有其他用户离开时回调
        super.onChannelUserLeaved(account, uid);
        Log.e(TAG, "onChannelUserLeaved \naccount = " + account + "\nuid = " + uid);
      }

      @Override public void onChannelQueryUserNumResult(String channelID, int ecode, int num) {
        //查询指定频道的用户数量成功后回调
        super.onChannelQueryUserNumResult(channelID, ecode, num);
        Log.e(TAG, "onChannelQueryUserNumResult \nchannelID = " + channelID + "\necode = " + ecode + "\nnum=" + num);
      }

      @Override public void onQueryUserStatusResult(String name, String status) {
        super.onQueryUserStatusResult(name, status);
        Log.e(TAG, "onQueryUserStatusResult \nname = " + name + "\nstatus = " + status);
      }

      @Override public void onMessageChannelReceive(String channelID, String account, int uid, String msg) {
        super.onMessageChannelReceive(channelID, account, uid, msg);
        Log.e(TAG, "onMessageChannelReceive \nchannelID = " + channelID + "\naccount = " + account + "\nuid = " + uid + "\nmsg = " + msg);
      }
    });
  }

  // R.id.buttonSendMsg
  @OnClick({
      R.id.buttonLogin, R.id.buttonSwitch, R.id.buttonInstMsg, R.id.buttonJoin, R.id.buttonEnv, R.id.buttonQueryUserStatus,
      R.id.buttonChannelQueryUserNum, R.id.buttonCall
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
        if (m_iscalling) {
          m_iscalling = false;
          buttonCall.setText("Call");
          m_agoraAPI.channelInviteEnd(channelName2, peer, 0);
          doLeave();
        } else {
          m_iscalling = true;
          buttonCall.setText("End");
          // TODO: 2017/12/20 channelInviteUser;channelInviteUser2;channelInviteDTMF;channelInviteAccept;channelInviteRefuse;channelInviteEnd;
          m_agoraAPI.channelInvitePhone3(channelName2, peer, src, "{\"sip_header:myheader\":\"gogo\"}");// (int)Long.parseLong(peerUid))
          doJoin();
        }
        break;
      /*case R.id.buttonSendMsg:
        if (!isLogin) {
          return;
        }
        String channelName1 = editTextChannelName.getText().toString().trim();
        m_agoraAPI.messageChannelSend(channelName1, "hello world ", "");
        break;*/
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
        Button btn = findViewById(R.id.buttonLogin);
        btn.setText("Login");
      }
    });
  }

  private void set_state_login() {
    isLogin = true;
    runOnUiThread(new Runnable() {
      @Override public void run() {
        Button btn = findViewById(R.id.buttonLogin);
        btn.setText("Logout");
      }
    });
  }

  private void doJoin() {
    runOnUiThread(new Runnable() {
      @Override public void run() {
        final Button btn = findViewById(R.id.buttonJoin);
        m_isjoin = true;
        btn.setText("Leave");
        String channelName = editTextChannelName.getText().toString().trim();
        Log.e(TAG, "Join channel " + channelName);
        m_agoraAPI.channelJoin(channelName);
        // TODO: 2017/12/20 enableMedia的用处？
        /*String key = appID;
        if (enableMediaCertificate) {
          int tsWrong = (int) (new Date().getTime() / 1000);
          int ts = (int) (System.currentTimeMillis() / 1000);
          int r = new Random().nextInt();
          long uid = my_uid;
          int expiredTs = 0;
          try {
            key = DynamicKey4.generateMediaChannelKey(appID, certificate, channelName, ts, r, uid,
                expiredTs);
          } catch (Exception e) {
            e.printStackTrace();
          }
          Log.e(TAG,"media key : " + key);
        }*/
      }
    });
  }

  private void doLeave() {
    runOnUiThread(new Runnable() {
      @Override public void run() {
        final Button btn = findViewById(R.id.buttonJoin);
        m_isjoin = false;
        btn.setText("Join");
        String channelName = editTextChannelName.getText().toString().trim();
        m_agoraAPI.channelLeave(channelName);
      }
    });
  }
}
