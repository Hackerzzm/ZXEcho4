package zzm.zxtech.zxecho.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewParent;
import android.view.ViewStub;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zzm.zxtech.propeller.Constant;
import zzm.zxtech.propeller.UserStatusData;
import zzm.zxtech.propeller.VideoInfoData;
import zzm.zxtech.propeller.preprocessing.VideoPreProcessing;
import zzm.zxtech.propeller.ui.RtlLinearLayoutManager;
import zzm.zxtech.signal.SignalActivity;
import zzm.zxtech.zxecho.model.AGEventHandler;
import zzm.zxtech.zxecho.model.ConstantApp;
import zzm.zxtech.zxecho.model.Message;
import zzm.zxtech.zxecho.model.User;

public class ChatActivity extends BaseActivity implements AGEventHandler {

  public static final int LAYOUT_TYPE_DEFAULT = 0;
  public static final int LAYOUT_TYPE_SMALL = 1;
  private final static Logger log = LoggerFactory.getLogger(ChatActivity.class);
  private static ChatActivity instant;
  // should only be modified under UI thread
  private final HashMap<Integer, SurfaceView> mUidsList = new HashMap<>(); // uid = 0 || uid == EngineConfig.mUid
  private final String TAG = "zzm debug!!!";
  public int mLayoutType = LAYOUT_TYPE_DEFAULT;
  private GridVideoViewContainer mGridVideoViewContainer;
  private RelativeLayout mSmallVideoViewDock;
  private volatile boolean mVideoMuted = false;
  private volatile boolean mAudioMuted = false;
  private volatile int mAudioRouting = -1; // Default
  private InChannelMessageListAdapter mMsgAdapter;
  private ArrayList<Message> mMsgList;
  private int mDataStreamId;
  private VideoPreProcessing mVideoPreProcessing;
  private SmallVideoViewAdapter mSmallVideoViewAdapter;

  public static void ChatLeave() {
    if (instant != null) {
      instant.finish();
    }
  }

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(zzm.zxtech.zxecho.R.layout.activity_chat);
    instant = this;
    checkSelfPermissions();
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    return false;
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    return false;
  }

  @Override protected void initUIandEvent() {
    event().addEventHandler(this);
    Intent i = getIntent();
    String channelName = i.getStringExtra(ConstantApp.ACTION_KEY_CHANNEL_NAME);
    final String encryptionKey = i.getStringExtra(ConstantApp.ACTION_KEY_ENCRYPTION_KEY);
    final String encryptionMode = i.getStringExtra(ConstantApp.ACTION_KEY_ENCRYPTION_MODE);
    String key = i.getStringExtra("key");
    int uid = i.getIntExtra("uid", 0);
    doConfigEngine(encryptionKey, encryptionMode);

    mGridVideoViewContainer = (GridVideoViewContainer) findViewById(zzm.zxtech.zxecho.R.id.grid_video_view_container);
    mGridVideoViewContainer.setItemEventHandler(new VideoViewEventListener() {
      @Override public void onItemDoubleClick(View v, Object item) {
        log.debug("onItemDoubleClick " + v + " " + item + " " + mLayoutType);
        if (mUidsList.size() < 2) {
          return;
        }
        UserStatusData user = (UserStatusData) item;
        int uid = (user.mUid == 0) ? config().mUid : user.mUid;
        //if (mLayoutType == LAYOUT_TYPE_DEFAULT && mUidsList.size() != 1) {
        Log.e("zzm debug!!!", "switchTosmall 1");
          switchToSmallVideoView(uid);
        /*} else {
          Log.e("zzm debug!!!", "switchTodefault 1");
          switchToDefaultVideoView();
        }*/
      }
    });

    SurfaceView surfaceV = RtcEngine.CreateRendererView(getApplicationContext());
    rtcEngine().setupLocalVideo(new VideoCanvas(surfaceV, VideoCanvas.RENDER_MODE_ADAPTIVE, 0));
    surfaceV.setZOrderOnTop(false);
    surfaceV.setZOrderMediaOverlay(false);

    mUidsList.put(0, surfaceV); // get first surface view

    mGridVideoViewContainer.initViewContainer(this, 0, mUidsList); // first is now full view
    worker().preview(true, surfaceV, 0);
    config().mUid = uid;

    worker().joinChannel(key, channelName, uid);

    TextView textChannelName = (TextView) findViewById(zzm.zxtech.zxecho.R.id.channel_name);
    textChannelName.setText(channelName);

    optional();

    LinearLayout bottomContainer = (LinearLayout) findViewById(zzm.zxtech.zxecho.R.id.bottom_container);
    FrameLayout.MarginLayoutParams fmp = (FrameLayout.MarginLayoutParams) bottomContainer.getLayoutParams();
    fmp.bottomMargin = virtualKeyHeight() + 16;

    initMessageList();
  }

  public void onClickHideIME(View view) {
    log.debug("onClickHideIME " + view);
    closeIME(findViewById(zzm.zxtech.zxecho.R.id.msg_content));
    findViewById(zzm.zxtech.zxecho.R.id.msg_input_container).setVisibility(View.GONE);
    findViewById(zzm.zxtech.zxecho.R.id.bottom_action_end_call).setVisibility(View.VISIBLE);
    findViewById(zzm.zxtech.zxecho.R.id.bottom_action_container).setVisibility(View.VISIBLE);
  }

  private void initMessageList() {
    mMsgList = new ArrayList<>();
    RecyclerView msgListView = (RecyclerView) findViewById(zzm.zxtech.zxecho.R.id.msg_list);

    mMsgAdapter = new InChannelMessageListAdapter(this, mMsgList);
    mMsgAdapter.setHasStableIds(true);
    msgListView.setAdapter(mMsgAdapter);
    msgListView.setLayoutManager(new LinearLayoutManager(getApplicationContext(), RecyclerView.VERTICAL, false));
    msgListView.addItemDecoration(new MessageListDecoration());
  }

  private void notifyMessageChanged(Message msg) {
    mMsgList.add(msg);

    int MAX_MESSAGE_COUNT = 16;

    if (mMsgList.size() > MAX_MESSAGE_COUNT) {
      int toRemove = mMsgList.size() - MAX_MESSAGE_COUNT;
      for (int i = 0; i < toRemove; i++) {
        mMsgList.remove(i);
      }
    }

    mMsgAdapter.notifyDataSetChanged();
  }

  private void sendChannelMsg(String msgStr) {
    RtcEngine rtcEngine = rtcEngine();
    if (mDataStreamId <= 0) {
      mDataStreamId = rtcEngine.createDataStream(true, true); // boolean reliable, boolean ordered
    }

    if (mDataStreamId < 0) {
      String errorMsg = "Create data stream error happened " + mDataStreamId;
      log.warn(errorMsg);
      showLongToast(errorMsg);
      return;
    }

    byte[] encodedMsg;
    try {
      encodedMsg = msgStr.getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      encodedMsg = msgStr.getBytes();
    }

    rtcEngine.sendStreamMessage(mDataStreamId, encodedMsg);
  }

  private void optional() {
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

    setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
  }

  private void optionalDestroy() {
  }

  private int getVideoProfileIndex() {
    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    int profileIndex = pref.getInt(ConstantApp.PrefManager.PREF_PROPERTY_PROFILE_IDX, ConstantApp.DEFAULT_PROFILE_IDX);
    if (profileIndex > ConstantApp.VIDEO_PROFILES.length - 1) {
      profileIndex = ConstantApp.DEFAULT_PROFILE_IDX;

      // save the new value
      SharedPreferences.Editor editor = pref.edit();
      editor.putInt(ConstantApp.PrefManager.PREF_PROPERTY_PROFILE_IDX, profileIndex);
      editor.apply();
    }
    //return profileIndex;
    return 2;
  }

  private void doConfigEngine(String encryptionKey, String encryptionMode) {
    int vProfile = ConstantApp.VIDEO_PROFILES[getVideoProfileIndex()];

    worker().configEngine(vProfile, encryptionKey, encryptionMode);
  }

  public void onBtn0Clicked(View view) {
    log.info("onBtn0Clicked " + view + " " + mVideoMuted + " " + mAudioMuted);
    showMessageEditContainer();
  }

  private void showMessageEditContainer() {
    findViewById(zzm.zxtech.zxecho.R.id.bottom_action_container).setVisibility(View.GONE);
    findViewById(zzm.zxtech.zxecho.R.id.bottom_action_end_call).setVisibility(View.GONE);
    findViewById(zzm.zxtech.zxecho.R.id.msg_input_container).setVisibility(View.VISIBLE);

    EditText edit = (EditText) findViewById(zzm.zxtech.zxecho.R.id.msg_content);

    edit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
      @Override public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_SEND || (event != null
            && event.getAction() == KeyEvent.ACTION_DOWN
            && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
          String msgStr = v.getText().toString();
          if (TextUtils.isEmpty(msgStr)) {
            return false;
          }
          sendChannelMsg(msgStr);

          v.setText("");

          Message msg = new Message(Message.MSG_TYPE_TEXT, new User(config().mUid, String.valueOf(config().mUid)), msgStr);
          notifyMessageChanged(msg);

          return true;
        }
        return false;
      }
    });

    openIME(edit);
  }

  public void onCustomizedFunctionClicked(View view) {
    log.info("onCustomizedFunctionClicked " + view + " " + mVideoMuted + " " + mAudioMuted + " " + mAudioRouting);
    if (mVideoMuted) {
      onSwitchSpeakerClicked();
    } else {
      onSwitchCameraClicked();
    }
  }

  private void onSwitchCameraClicked() {
    RtcEngine rtcEngine = rtcEngine();
    rtcEngine.switchCamera();
  }

  private void onSwitchSpeakerClicked() {
    RtcEngine rtcEngine = rtcEngine();
    rtcEngine.setEnableSpeakerphone(mAudioRouting != 3);
  }

  @Override protected void deInitUIandEvent() {
    optionalDestroy();
    doLeaveChannel();
    event().removeEventHandler(this);
    mUidsList.clear();
  }

  private void doLeaveChannel() {
    worker().leaveChannel(config().mChannel);
    worker().preview(false, null, 0);
  }

  public void onEndCallClicked(View view) {
    log.info("onEndCallClicked " + view);
    SignalActivity.SignalLeave();
    finish();
  }

  public void onBtnNClicked(View view) {
    if (mVideoPreProcessing == null) {
      mVideoPreProcessing = new VideoPreProcessing();
    }

    ImageView iv = (ImageView) view;
    Object showing = view.getTag();
    if (showing != null && (Boolean) showing) {
      mVideoPreProcessing.enablePreProcessing(false);
      iv.setTag(null);
      iv.clearColorFilter();
    } else {
      mVideoPreProcessing.enablePreProcessing(true);
      iv.setTag(true);
      iv.setColorFilter(getResources().getColor(zzm.zxtech.zxecho.R.color.agora_blue), PorterDuff.Mode.MULTIPLY);
    }
  }

  public void onVoiceChatClicked(View view) {
    log.info("onVoiceChatClicked " + view + " " + mUidsList.size() + " video_status: " + mVideoMuted + " audio_status: " + mAudioMuted);
    if (mUidsList.size() == 0) {
      return;
    }
    SurfaceView surfaceV = getLocalView();
    ViewParent parent;
    if (surfaceV == null || (parent = surfaceV.getParent()) == null) {
      log.warn("onVoiceChatClicked " + view + " " + surfaceV);
      return;
    }
    RtcEngine rtcEngine = rtcEngine();
    mVideoMuted = !mVideoMuted;
    if (mVideoMuted) {
      rtcEngine.disableVideo();
    } else {
      rtcEngine.enableVideo();
    }
    ImageView iv = (ImageView) view;
    iv.setImageResource(mVideoMuted ? zzm.zxtech.zxecho.R.drawable.btn_video : zzm.zxtech.zxecho.R.drawable.btn_voice);
    hideLocalView(mVideoMuted);
    if (mVideoMuted) {
      resetToVideoDisabledUI();
    } else {
      resetToVideoEnabledUI();
    }
  }

  private SurfaceView getLocalView() {
    for (HashMap.Entry<Integer, SurfaceView> entry : mUidsList.entrySet()) {
      if (entry.getKey() == 0 || entry.getKey() == config().mUid) {
        return entry.getValue();
      }
    }

    return null;
  }

  private void hideLocalView(boolean hide) {
    int uid = config().mUid;
    doHideTargetView(uid, hide);
  }

  private void doHideTargetView(int targetUid, boolean hide) {
    HashMap<Integer, Integer> status = new HashMap<>();
    status.put(targetUid, hide ? UserStatusData.VIDEO_MUTED : UserStatusData.DEFAULT_STATUS);
    if (mLayoutType == LAYOUT_TYPE_DEFAULT) {
      mGridVideoViewContainer.notifyUiChanged(mUidsList, targetUid, status, null);
    } else if (mLayoutType == LAYOUT_TYPE_SMALL) {
      UserStatusData bigBgUser = mGridVideoViewContainer.getItem(0);
      if (bigBgUser.mUid == targetUid) { // big background is target view
        mGridVideoViewContainer.notifyUiChanged(mUidsList, targetUid, status, null);
      } else { // find target view in small video view list
        log.warn("SmallVideoViewAdapter call notifyUiChanged "
            + mUidsList
            + " "
            + (bigBgUser.mUid & 0xFFFFFFFFL)
            + " target: "
            + (targetUid & 0xFFFFFFFFL)
            + "=="
            + targetUid
            + " "
            + status);
        mSmallVideoViewAdapter.notifyUiChanged(mUidsList, bigBgUser.mUid, status, null);
      }
    }
  }

  private void resetToVideoEnabledUI() {
    ImageView iv = (ImageView) findViewById(zzm.zxtech.zxecho.R.id.customized_function_id);
    iv.setImageResource(zzm.zxtech.zxecho.R.drawable.btn_switch_camera);
    iv.clearColorFilter();

    notifyHeadsetPlugged(mAudioRouting);
  }

  private void resetToVideoDisabledUI() {
    ImageView iv = (ImageView) findViewById(zzm.zxtech.zxecho.R.id.customized_function_id);
    iv.setImageResource(zzm.zxtech.zxecho.R.drawable.btn_speaker);
    iv.clearColorFilter();

    notifyHeadsetPlugged(mAudioRouting);
  }

  public void onVoiceMuteClicked(View view) {
    log.info("onVoiceMuteClicked " + view + " " + mUidsList.size() + " video_status: " + mVideoMuted + " audio_status: " + mAudioMuted);
    if (mUidsList.size() == 0) {
      return;
    }

    RtcEngine rtcEngine = rtcEngine();
    rtcEngine.muteLocalAudioStream(mAudioMuted = !mAudioMuted);

    ImageView iv = (ImageView) view;

    if (mAudioMuted) {
      iv.setColorFilter(getResources().getColor(zzm.zxtech.zxecho.R.color.agora_blue), PorterDuff.Mode.MULTIPLY);
    } else {
      iv.clearColorFilter();
    }
  }

  @Override public void onFirstRemoteVideoDecoded(int uid, int width, int height, int elapsed) {
    doRenderRemoteUi(uid);
  }

  private void doRenderRemoteUi(final int uid) {
    runOnUiThread(new Runnable() {
      @Override public void run() {
        if (isFinishing()) {
          return;
        }

        if (mUidsList.containsKey(uid)) {
          return;
        }

        SurfaceView surfaceV = RtcEngine.CreateRendererView(getApplicationContext());
        Log.e(TAG, "mUidsList.put uid = " + uid);
        mUidsList.put(uid, surfaceV);

        boolean useDefaultLayout = mLayoutType == LAYOUT_TYPE_DEFAULT && mUidsList.size() != 2;

        surfaceV.setZOrderOnTop(!useDefaultLayout);
        surfaceV.setZOrderMediaOverlay(!useDefaultLayout);

        rtcEngine().setupRemoteVideo(new VideoCanvas(surfaceV, VideoCanvas.RENDER_MODE_ADAPTIVE, uid));

        /*if (useDefaultLayout) {
          log.debug("doRenderRemoteUi LAYOUT_TYPE_DEFAULT " + (uid & 0xFFFFFFFFL));
          Log.e("zzm debug!!!", "switchTodefault 2");
          switchToDefaultVideoView();
        } else {*/
          int bigBgUid = mSmallVideoViewAdapter == null ? uid : mSmallVideoViewAdapter.getExceptedUid();
          log.debug("doRenderRemoteUi LAYOUT_TYPE_SMALL " + (uid & 0xFFFFFFFFL) + " " + (bigBgUid & 0xFFFFFFFFL));
          try {
            Log.e("zzm debug!!!", "switchTosmall 2");
            switchToSmallVideoView(bigBgUid);
          } catch (NullPointerException e) {
            Log.e(TAG, "一个空指针是什么鬼");
            e.printStackTrace();
          }
        //}
      }
    });
  }

  @Override public void onJoinChannelSuccess(String channel, final int uid, int elapsed) {
    log.debug("onJoinChannelSuccess " + channel + " " + (uid & 0xFFFFFFFFL) + " " + elapsed);
    Log.e(TAG, "onJoinChannelSuccess " + channel + " " + (uid & 0xFFFFFFFFL) + " " + elapsed);
    runOnUiThread(new Runnable() {
      @Override public void run() {
        if (isFinishing()) {
          return;
        }

        SurfaceView local = mUidsList.remove(0);

        if (local == null) {
          return;
        }

        mUidsList.put(uid, local);
      }
    });
  }

  @Override public void onUserOffline(int uid, int reason) {
    doRemoveRemoteUi(uid);
  }

  @Override public void onExtraCallback(final int type, final Object... data) {

    runOnUiThread(new Runnable() {
      @Override public void run() {
        if (isFinishing()) {
          return;
        }
        doHandleExtraCallback(type, data);
      }
    });
  }

  private void doHandleExtraCallback(int type, Object... data) {
    int peerUid;
    boolean muted;

    switch (type) {
      case AGEventHandler.EVENT_TYPE_ON_USER_AUDIO_MUTED:
        peerUid = (Integer) data[0];
        muted = (boolean) data[1];

        if (mLayoutType == LAYOUT_TYPE_DEFAULT) {
          HashMap<Integer, Integer> status = new HashMap<>();
          status.put(peerUid, muted ? UserStatusData.AUDIO_MUTED : UserStatusData.DEFAULT_STATUS);
          mGridVideoViewContainer.notifyUiChanged(mUidsList, config().mUid, status, null);
        }

        break;

      case AGEventHandler.EVENT_TYPE_ON_USER_VIDEO_MUTED:
        peerUid = (Integer) data[0];
        muted = (boolean) data[1];

        doHideTargetView(peerUid, muted);

        break;

      case AGEventHandler.EVENT_TYPE_ON_USER_VIDEO_STATS:
        IRtcEngineEventHandler.RemoteVideoStats stats = (IRtcEngineEventHandler.RemoteVideoStats) data[0];

        if (Constant.SHOW_VIDEO_INFO) {
          if (mLayoutType == LAYOUT_TYPE_DEFAULT) {
            mGridVideoViewContainer.addVideoInfo(stats.uid,
                new VideoInfoData(stats.width, stats.height, stats.delay, stats.receivedFrameRate, stats.receivedBitrate));
            int uid = config().mUid;
            int profileIndex = getVideoProfileIndex();
            String resolution = getResources().getStringArray(zzm.zxtech.zxecho.R.array.string_array_resolutions)[profileIndex];
            String fps = getResources().getStringArray(zzm.zxtech.zxecho.R.array.string_array_frame_rate)[profileIndex];
            String bitrate = getResources().getStringArray(zzm.zxtech.zxecho.R.array.string_array_bit_rate)[profileIndex];

            String[] rwh = resolution.split("x");
            int width = Integer.valueOf(rwh[0]);
            int height = Integer.valueOf(rwh[1]);

            mGridVideoViewContainer.addVideoInfo(uid,
                new VideoInfoData(width > height ? width : height, width > height ? height : width, 0, Integer.valueOf(fps),
                    Integer.valueOf(bitrate)));
          }
        } else {
          mGridVideoViewContainer.cleanVideoInfo();
        }

        break;

      case AGEventHandler.EVENT_TYPE_ON_SPEAKER_STATS:
        IRtcEngineEventHandler.AudioVolumeInfo[] infos = (IRtcEngineEventHandler.AudioVolumeInfo[]) data[0];

        if (infos.length == 1 && infos[0].uid == 0) { // local guy, ignore it
          break;
        }

        if (mLayoutType == LAYOUT_TYPE_DEFAULT) {
          HashMap<Integer, Integer> volume = new HashMap<>();

          for (IRtcEngineEventHandler.AudioVolumeInfo each : infos) {
            peerUid = each.uid;
            int peerVolume = each.volume;

            if (peerUid == 0) {
              continue;
            }
            volume.put(peerUid, peerVolume);
          }
          mGridVideoViewContainer.notifyUiChanged(mUidsList, config().mUid, null, volume);
        }

        break;

      case AGEventHandler.EVENT_TYPE_ON_APP_ERROR:
        // 2018/1/5  通话过程中掉线
        int subType = (int) data[0];
        if (subType == ConstantApp.AppError.NO_NETWORK_CONNECTION) {
          showLongToast(getString(zzm.zxtech.zxecho.R.string.msg_no_network_connection));
        }
        SignalActivity.SignalLeave();
        finish();
        break;

      case AGEventHandler.EVENT_TYPE_ON_DATA_CHANNEL_MSG:

        peerUid = (Integer) data[0];
        final byte[] content = (byte[]) data[1];
        notifyMessageChanged(new Message(new User(peerUid, String.valueOf(peerUid)), new String(content)));

        break;

      case AGEventHandler.EVENT_TYPE_ON_AGORA_MEDIA_ERROR: {
        int error = (int) data[0];
        String description = (String) data[1];

        notifyMessageChanged(new Message(new User(0, null), error + " " + description));

        break;
      }

      case AGEventHandler.EVENT_TYPE_ON_AUDIO_ROUTE_CHANGED:
        notifyHeadsetPlugged((int) data[0]);

        break;
    }
  }

  private void requestRemoteStreamType(final int currentHostCount) {
    log.debug("requestRemoteStreamType " + currentHostCount);
  }

  private void doRemoveRemoteUi(final int uid) {
    runOnUiThread(new Runnable() {
      @Override public void run() {
        if (isFinishing()) {
          return;
        }

        Object target = mUidsList.remove(uid);
        if (target == null) {
          return;
        }

        int bigBgUid = -1;
        if (mSmallVideoViewAdapter != null) {
          bigBgUid = mSmallVideoViewAdapter.getExceptedUid();
        }

        log.debug("doRemoveRemoteUi " + (uid & 0xFFFFFFFFL) + " " + (bigBgUid & 0xFFFFFFFFL) + " " + mLayoutType);

        if (mLayoutType == LAYOUT_TYPE_DEFAULT || uid == bigBgUid) {
          Log.e("zzm debug!!!", "switchTodefault 3");
          //switchToDefaultVideoView();
        } else {
          Log.e("zzm debug!!!", "switchTosmall 3");
          switchToSmallVideoView(bigBgUid);
        }
      }
    });
  }

  /*private void switchToDefaultVideoView() {
    Log.e("zzm debug!!!", "switchToDefaultVideoView");
    if (mSmallVideoViewDock != null) {
      mSmallVideoViewDock.setVisibility(View.GONE);
    }
    mGridVideoViewContainer.initViewContainer(this, config().mUid, mUidsList);

    mLayoutType = LAYOUT_TYPE_DEFAULT;
  }*/

  private void switchToSmallVideoView(int bigBgUid) {
    Log.e("zzm debug!!!", "switchToSmallVideoView bigBgUid = " + bigBgUid);
    HashMap<Integer, SurfaceView> slice = new HashMap<>(1);
    slice.put(bigBgUid, mUidsList.get(bigBgUid));
    mGridVideoViewContainer.initViewContainer(this, bigBgUid, slice);

    bindToSmallVideoView(bigBgUid);

    mLayoutType = LAYOUT_TYPE_SMALL;

    requestRemoteStreamType(mUidsList.size());
  }

  private void bindToSmallVideoView(int exceptUid) {
    if (mSmallVideoViewDock == null) {
      ViewStub stub = (ViewStub) findViewById(zzm.zxtech.zxecho.R.id.small_video_view_dock);
      mSmallVideoViewDock = (RelativeLayout) stub.inflate();
    }

    boolean twoWayVideoCall = mUidsList.size() == 2;

    RecyclerView recycler = (RecyclerView) findViewById(zzm.zxtech.zxecho.R.id.small_video_view_container);

    boolean create = false;

    if (mSmallVideoViewAdapter == null) {
      create = true;
      mSmallVideoViewAdapter = new SmallVideoViewAdapter(this, config().mUid, exceptUid, mUidsList, new VideoViewEventListener() {
        @Override public void onItemDoubleClick(View v, Object item) {
          Log.e("zzm debug!!!", "switchTodefault 4");
          //switchToDefaultVideoView();
        }
      });
      mSmallVideoViewAdapter.setHasStableIds(true);
    }
    recycler.setHasFixedSize(true);

    log.debug("bindToSmallVideoView " + twoWayVideoCall + " " + (exceptUid & 0xFFFFFFFFL));
    if (twoWayVideoCall) {
      Log.e("zzm debug!!!", "bindToSmallVideoView true");
      recycler.setLayoutManager(new RtlLinearLayoutManager(getApplicationContext(), RtlLinearLayoutManager.HORIZONTAL, false));
    } else {
      Log.e("zzm debug!!!", "bindToSmallVideoView false");
      recycler.setLayoutManager(new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.HORIZONTAL, false));
    }
    recycler.addItemDecoration(new SmallVideoViewDecoration());
    recycler.setAdapter(mSmallVideoViewAdapter);

    recycler.setDrawingCacheEnabled(true);
    recycler.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_AUTO);

    if (!create) {
      mSmallVideoViewAdapter.setLocalUid(config().mUid);
      mSmallVideoViewAdapter.notifyUiChanged(mUidsList, exceptUid, null, null);
    }
    recycler.setVisibility(View.VISIBLE);
    mSmallVideoViewDock.setVisibility(View.VISIBLE);
  }

  public void notifyHeadsetPlugged(final int routing) {
    log.info("notifyHeadsetPlugged " + routing + " " + mVideoMuted);

    mAudioRouting = routing;

    if (!mVideoMuted) {
      return;
    }

    ImageView iv = (ImageView) findViewById(zzm.zxtech.zxecho.R.id.customized_function_id);
    if (mAudioRouting == 3) { // Speakerphone
      iv.setColorFilter(getResources().getColor(zzm.zxtech.zxecho.R.color.agora_blue), PorterDuff.Mode.MULTIPLY);
    } else {
      iv.clearColorFilter();
    }
  }
}
