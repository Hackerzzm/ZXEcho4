package com.example.myapp1;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;
import io.agora.AgoraAPI;
import io.agora.AgoraAPIOnlySignal;
import io.agora.IAgoraAPI;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Random;
import zzm.zxecho.media.DynamicKey4;

public class ActivityLogin extends Activity {
  public static final String appID = "88c6cea1c8aa408b892d9d632a4d206b";
  public static final String certificate = "73f5d0b232b94cf58fd9391cc019f1ff";
  public static final boolean enableMediaCertificate = true;

  private static final boolean enableMedia = false;
  int msgid = 0;
  boolean isLogin = false;
  boolean m_iscalling = false;
  boolean m_isjoin = false;
  boolean m_env_dbg = false;
  SurfaceView mLocalView = null;
  int my_uid = 0;
  //    boolean isChecked(int id){
  //        CheckBox x = (CheckBox) findViewById(id);
  //        return x.isChecked();
  //    }
  private AgoraAPIOnlySignal m_agoraAPI;
  private RtcEngine m_agoraMedia;
  private int video_peer_uid = 0;
  private SurfaceView remoteView;

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

  String getEditTextValue(int id) {
    EditText x = (EditText) findViewById(id);
    return x.getText().toString();
  }

  void setEditTextValue(final int id, final String v) {
    runOnUiThread(new Runnable() {
      @Override public void run() {
        EditText x = (EditText) findViewById(id);
        x.getText().clear();
        x.getText().append(v);
      }
    });
  }

  void doJoin() {

    runOnUiThread(new Runnable() {
      @Override public void run() {
        final Button btn = (Button) findViewById(R.id.buttonJoin);
        m_isjoin = true;
        btn.setText("Leave");

        String channelName = getEditTextValue(R.id.editTextChannelName);
        log("Join channel " + channelName);
        m_agoraAPI.channelJoin(channelName);

        CheckBox x = (CheckBox) findViewById(R.id.checkBoxVideo);
        if (enableMedia) {
          if (x.isChecked()) {
            /*m_agoraMedia.enableVideo();
            FrameLayout localViewContainer = (FrameLayout) findViewById(R.id.video1);
            mLocalView = m_agoraMedia.CreateRendererView(getApplicationContext());
            localViewContainer.addView(mLocalView,
                new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            m_agoraMedia.setupLocalVideo(new VideoCanvas(mLocalView));
            m_agoraMedia.startPreview();*/
          } else {
            m_agoraMedia.disableVideo();
          }
        }

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
          log("media key : " + key);
        }
        if (enableMedia) m_agoraMedia.joinChannel(key, channelName, "", my_uid);
      }
    });
  }

  void doLeave() {
    runOnUiThread(new Runnable() {
      @Override public void run() {
        final Button btn = (Button) findViewById(R.id.buttonJoin);
        m_isjoin = false;
        btn.setText("Join");
        video_peer_uid = 0;
        if (remoteView != null) {
          FrameLayout remoteViewContainer = (FrameLayout) findViewById(R.id.video2);
          remoteViewContainer.removeAllViews();
          remoteView = null;
        }
        String channelName = getEditTextValue(R.id.editTextChannelName);
        log("Leave channel " + channelName);
        m_agoraAPI.channelLeave(channelName);
        if (enableMedia) m_agoraMedia.leaveChannel();
      }
    });
  }

  public String calcToken(String appID, String certificate, String account, long expiredTime) {
    // Token = 1:appID:expiredTime:sign
    // Token = 1:appID:expiredTime:md5(account + vendorID + certificate + expiredTime)

    String sign = md5hex((account + appID + certificate + expiredTime).getBytes());
    return "1:" + appID + ":" + expiredTime + ":" + sign;
  }

  /**
   * Called when the activity is first created.
   */
  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.login);

    //        setEditTextValue(R.id.editTextUID, "22");
    //        setEditTextValue(R.id.editTextCallUID, "0");

    {
      final Button btn = (Button) findViewById(R.id.buttonEnv);

      btn.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View arg0) {
          m_env_dbg = !m_env_dbg;

          log("env : " + (m_env_dbg ? "DBG" : "PROD"));

          m_agoraAPI.dbg("lbss", m_env_dbg ? "lbs.d.agorabeckon.com" : "lbs.sig.agora.io");
        }
      });
    }

    {
      final Button btn = (Button) findViewById(R.id.buttonLogin);

      btn.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View arg0) {

          if (appID.equals("")) {
            Toast.makeText(getApplicationContext(), "Please set appID in ActivityLogin.java", Toast.LENGTH_SHORT).show();
            return;
          }

          // dbg
          if (isLogin) {
            set_state_logout();

            log("Logout");
            m_agoraAPI.logout();
          } else {
            set_state_login();

            //String appID = getEditTextValue(R.id.editTextVendorID);
            String account = getEditTextValue(R.id.editTextName);
            log("Login : appID=" + appID + ", account=" + account);
            long expiredTimeWrong = new Date().getTime() / 1000 + 3600;
            long expiredTime = System.currentTimeMillis() / 1000 + 3600;
            String token = calcToken(appID, certificate, account, expiredTime);
            //                        m_agoraAPI.login(appID, account, token, 0, "");
            m_agoraAPI.login2(appID, account, token, 0, "", 60, 5);
          }
        }
      });
    }

    {
      Button btn = (Button) findViewById(R.id.buttonInstMsg);

      btn.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View arg0) {
          if (!isLogin) return;

          String peer = getEditTextValue(R.id.editTextCallUser);

          m_agoraAPI.messageInstantSend(peer, 0, "hello world " + (++msgid), "");

          //                    m_agoraAPI.channelLeave(channelName);
        }
      });
    }

    {
      Button btn = (Button) findViewById(R.id.buttonSendMsg);

      btn.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View arg0) {

          if (!isLogin) return;

          String channelName = getEditTextValue(R.id.editTextChannelName);

          m_agoraAPI.messageChannelSend(channelName, "hello world " + (++msgid), "");

          //                    m_agoraAPI.channelLeave(channelName);
        }
      });
    }

    {
      final Button btn = (Button) findViewById(R.id.buttonCallPhone);

      btn.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View arg0) {

          String channelName = getEditTextValue(R.id.editTextChannelName);
          String peer = getEditTextValue(R.id.editDst);
          String src = getEditTextValue(R.id.editSrc);

          if (m_iscalling) {
            m_iscalling = false;
            btn.setText("Call");

            log("endcall");
            m_agoraAPI.channelInviteEnd(channelName, peer, 0);
            doLeave();
          } else {
            m_iscalling = true;
            btn.setText("Bye");

            set_state_incall();
            doJoin();
            //                    String peerUid = getEditTextValue(R.id.editTextCallUID);

            log("InvitePhone " + src + " -> " + peer + " join " + channelName);
            m_agoraAPI.channelInvitePhone3(channelName, peer, src, "{\"sip_header:myheader\":\"gogo\"}");// (int)Long.parseLong(peerUid))
          }
        }
      });
    }

    {
      final Button btn = (Button) findViewById(R.id.buttonCall);

      btn.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View arg0) {

          if (!isLogin) return;

          //                    log("is_online" + m_agoraAPI.isOnline());
          String peerName = getEditTextValue(R.id.editTextCallUser);
          String channelName = getEditTextValue(R.id.editTextChannelName);

          if (m_iscalling) {
            set_state_notincall();

            log("End ");
            m_agoraAPI.channelInviteEnd(channelName, peerName, 0);
            doLeave();
          } else {
            set_state_incall();

            doJoin();
            log("Invite " + peerName + "" + " join " + channelName);
            m_agoraAPI.channelInviteUser(channelName, peerName, 0);// (int)Long.parseLong(peerUid));
          }
        }
      });
    }

    {
      final Button btn = (Button) findViewById(R.id.buttonJoin);

      btn.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View arg0) {

          if (!isLogin) return;

          if (m_isjoin) {
            doLeave();
          } else {
            doJoin();
          }
        }
      });
    }

    {
      Button btn = (Button) findViewById(R.id.buttonSwitch);
      btn.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View arg0) {
          String a1 = getEditTextValue(R.id.editTextName);
          String a2 = getEditTextValue(R.id.editTextCallUser);

          setEditTextValue(R.id.editTextName, a2);
          setEditTextValue(R.id.editTextCallUser, a1);
        }
      });
    }

    {
      CheckBox x = (CheckBox) findViewById(R.id.checkBoxSpeaker);
      x.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
        @Override public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
          if (enableMedia) m_agoraMedia.setEnableSpeakerphone(b);
        }
      });
    }

    {
      CheckBox x = (CheckBox) findViewById(R.id.checkBoxMute);
      x.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
        @Override public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
          if (enableMedia) m_agoraMedia.muteLocalAudioStream(b);
        }
      });
    }

    for (int i = 0; i < 1; i++) {
      final int fi = i;
      //String appID = getEditTextValue(R.id.editTextVendorID);
      m_agoraAPI = AgoraAPIOnlySignal.getInstance(this, appID);
      if (enableMedia) {
        try {
          m_agoraMedia = RtcEngine.create(this, appID, new IRtcEngineEventHandler() {
            @Override public void onFirstRemoteVideoDecoded(final int uid, int width, int height, int elapsed) {

              if (video_peer_uid == uid) {
                return;
              }

              video_peer_uid = uid;
              if (remoteView == null) {
                FrameLayout remoteViewContainer = (FrameLayout) findViewById(R.id.video2);
                remoteView = RtcEngine.CreateRendererView(getApplicationContext());
                remoteViewContainer.addView(remoteView,
                    new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
              }

              int successCode = m_agoraMedia.setupRemoteVideo(new VideoCanvas(remoteView, VideoCanvas.RENDER_MODE_ADAPTIVE, uid));

              if (successCode < 0) {
                new android.os.Handler().postDelayed(new Runnable() {
                  @Override public void run() {
                    m_agoraMedia.setupRemoteVideo(new VideoCanvas(remoteView, VideoCanvas.RENDER_MODE_ADAPTIVE, uid));
                    remoteView.invalidate();
                  }
                }, 500);
              }
            }
          });
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      m_agoraAPI.callbackSet(new AgoraAPI.CallBack() {
        //                int buf[] = new int[1000*1000*1];
        @Override public void onReconnecting(int nretry) {
          System.out.println(fi);
        }
      });

      m_agoraAPI.callbackSet(new AgoraAPI.CallBack() {
        @Override public void onLoginSuccess(int uid, int fd) {
          Log.i("sdk2", "login successfully");
          log("Login successfully");

          my_uid = uid;

          //                if (isChecked(R.id.checkBoxCall)) {
          //
          //                }

          //                m_agoraAPI.setBackground(99);
          //                m_agoraAPI.messageDTMFSend();
        }

        @Override public void onLoginFailed(int ecode) {
          Log.i("sdk2", "Login failed " + ecode);
          log("Login failed " + ecode);

          set_state_notincall();
          set_state_logout();
          //                    doLeave();
        }

        @Override public void onLogout(int ecode) {
          set_state_logout();
          set_state_notincall();

          if (m_isjoin) doLeave();

          if (ecode == IAgoraAPI.ECODE_LOGOUT_E_USER) {
            log("Logout successfully ");
          } else {
            log("Logout on " + ecode);
          }
        }

        @Override public void onLog(String txt) {
          //                ActivityLogin.this.log(txt);
          Log.i("sdk2", txt);
        }

        @Override public void onChannelJoined(String chanID) {
          log("Join channel " + chanID + " successfully"); // + " docall " + doCall);
          //                    if (doCall) {
          //                        doCall = false;
          //
          //
          //                        if (callNum){
          //                            String peer = getEditTextValue(R.id.editDst);
          //                            String src = getEditTextValue(R.id.editSrc);
          //                            //                    String peerUid = getEditTextValue(R.id.editTextCallUID);
          //
          //                            log("InvitePhone " + src + " -> " + peer + " join " + chanID);
          ////                            m_agoraAPI.channelInvitePhone2(chanID, peer, src);// (int)Long.parseLong(peerUid));
          //                            m_agoraAPI.channelInvitePhone3(chanID, peer, src, "{\"sip_header:myheader\":\"gogo\"}");// (int)Long.parseLong(peerUid));
          //                        }else{
          //                            String peerName = getEditTextValue(R.id.editTextCallUser);
          //                            //                    String peerUid = getEditTextValue(R.id.editTextCallUID);
          //
          //                            log("Invite " + peerName + ""  + " join " + chanID);
          //                            m_agoraAPI.channelInviteUser(chanID, peerName, 0);// (int)Long.parseLong(peerUid));
          //                        }
          //                    }
        }

        @Override public void onInviteReceived(String channleID, String account, int uid, String extra) {
          log("Received Invitation from " + account + ":" + uid + " to join " + channleID + " extra:" + extra);
          m_agoraAPI.channelInviteAccept(channleID, account, uid);

          setEditTextValue(R.id.editTextCallUser, account);
          setEditTextValue(R.id.editTextChannelName, channleID);

          doJoin();
          set_state_incall();
        }

        @Override public void onChannelUserJoined(String account, int uid) {
          log(account + ":" + (uid & 0xffffffffl) + " joined");
        }

        @Override public void onChannelJoinFailed(String chanID, int ecode) {
          log("Join " + chanID + " failed : ecode " + ecode);
        }

        @Override public void onChannelUserLeaved(String account, int uid) {
          log(account + ":" + (uid & 0xffffffffl) + " leaved");
        }

        @Override public void onChannelUserList(String[] accounts, int[] uids) {
          log("Channel user list:");
          for (int i = 0; i < accounts.length; i++) {
            long uid = uids[i] & 0xffffffffl;
            log(accounts[i] + ":" + (uid & 0xffffffffl));
          }
        }

        @Override public void onInviteMsg(String channelID, String account, int uid, String msgType, String msgData, String extra) {
          log("Received msg from " + account + ":" + (uid & 0xffffffffl) + " : " + msgType + " : " + msgData);
        }

        @Override public void onInviteReceivedByPeer(String channleID, String account, int uid) {
          log("Invitation received by " + account + ":" + (uid & 0xffffffffl));
        }

        @Override public void onInviteEndByPeer(String channelID, String account, int uid, String extra) {
          log("Invitation end by " + account + ":" + (uid & 0xffffffffl));

          doLeave();

          set_state_notincall();
        }

        @Override public void onInviteEndByMyself(String channelID, String account, int uid) {
          log("Invitation end bymyself " + account + ":" + (uid & 0xffffffffl));

          doLeave();

          set_state_notincall();
        }

        @Override public void onInviteAcceptedByPeer(String channleID, String account, int uid, String extra) {
          log("Invitation accepted by " + account + ":" + (uid & 0xffffffffl));
        }

        @Override public void onMessageChannelReceive(String channelID, String account, int uid, String msg) {
          log("recv channel msg " + channelID + " " + account + " : " + msg);
        }

        @Override public void onMessageInstantReceive(String account, int uid, String msg) {
          log("recv inst msg " + account + " : " + (uid & 0xffffffffl) + " : " + msg);
        }
      });
    }

    if (enableMedia) {
      m_agoraMedia.enableAudioVolumeIndication(1000, 3);
      m_agoraMedia.setParameters("{\"rtc.log_filter\":32783}");//0x800f, log to console
      m_agoraMedia.setLogFilter(32783);
    }


        /*
        m_agoraAPI.setMediaCB(new IRtcEngineEventHandlerEx() {
            @Override
            public void onAudioVolumeIndication(AudioVolumeInfo[] speakers, int totalVolume) {

            }
        });
        */
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

  private void set_state_incall() {
    m_iscalling = true;
    runOnUiThread(new Runnable() {
      @Override public void run() {
        Button btn = (Button) findViewById(R.id.buttonCall);
        btn.setText("End");
      }
    });
  }

  private void set_state_notincall() {
    m_iscalling = false;
    runOnUiThread(new Runnable() {
      @Override public void run() {
        Button btn = (Button) findViewById(R.id.buttonCall);
        btn.setText("Call");
      }
    });
  }

  public void log(final String s) {
    super.runOnUiThread(new Runnable() {
      @Override public void run() {
        EditText x = (EditText) findViewById(R.id.editTextLog);
        x.getText().append(s + "\n");
        x.scrollTo(0, 1000000000);
      }
    });
  }
}
