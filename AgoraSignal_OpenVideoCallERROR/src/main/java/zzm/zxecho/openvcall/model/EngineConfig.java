package zzm.zxecho.openvcall.model;

public class EngineConfig {
  public int mVideoProfile;

  public int mUid;

  public String mChannel;

  EngineConfig() {
  }

  public void reset() {
    mChannel = null;
  }
}
