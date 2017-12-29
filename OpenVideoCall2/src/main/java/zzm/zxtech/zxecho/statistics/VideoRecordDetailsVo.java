package zzm.zxtech.zxecho.statistics;

/**
 * Created by 92010 on 2017/12/28.
 */

public class VideoRecordDetailsVo {
  private String videoRecordDetailsId;
  private String userId;    // 手机端是维保工的ID、web端是当前登录用户ID、板子填TerminalId
  private String userIp;    // 呼叫方IP
  private String type;      // 0 -&gt;维保工，1-&gt;web , 2 -&gt; terminal
  private String callType;      // 通话类型：0-&gt;语音，1-&gt;视频（呼叫方接入方式）
  private String beCalledId;  //被呼叫的ID

  public String getVideoRecordDetailsId() {
    return videoRecordDetailsId;
  }

  public void setVideoRecordDetailsId(String videoRecordDetailsId) {
    this.videoRecordDetailsId = videoRecordDetailsId;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getUserIp() {
    return userIp;
  }

  public void setUserIp(String userIp) {
    this.userIp = userIp;
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

  public String getBeCalledId() {
    return beCalledId;
  }

  public void setBeCalledId(String beCalledId) {
    this.beCalledId = beCalledId;
  }
}
