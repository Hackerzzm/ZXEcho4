package io.agora.openlive.model;

public class EngineConfig {
  public int mClientRole;

  public int mVideoProfile;

  public int mUid;

  public String mChannel;

  EngineConfig() {
  }

  public void reset() {
    mChannel = null;
  }
}