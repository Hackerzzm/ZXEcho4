package zzm.zxtech.zxecho.statistics;

/**
 * Created by 92010 on 2017/12/28.
 */

public class VideoRecordVo {
  private String videoRecordId;
  private String beCalledId;    // 被呼叫的ID
  private String type;      // 类型（0-&gt;设备，1-&gt;手机）
  private String callType;      // 通话类型：0-&gt;语音，1-&gt;视频

  public String getVideoRecordId() {
    return videoRecordId;
  }

  public void setVideoRecordId(String videoRecordId) {
    this.videoRecordId = videoRecordId;
  }

  public String getBeCalledId() {
    return beCalledId;
  }

  public void setBeCalledId(String beCalledId) {
    this.beCalledId = beCalledId;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getCallType() {
    return callType;
  }

  public void setCallType(String callType) {
    this.callType = callType;
  }
}
