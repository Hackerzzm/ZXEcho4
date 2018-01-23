package zzm.zxtech.util;

import android.hardware.Camera;
import android.os.Build;

/**
 * Created by 92010 on 2018/1/11.
 */

public class CameraDetectUtil {
  private static boolean checkCameraFacing(final int facing) {
    if (getSdkVersion() < Build.VERSION_CODES.GINGERBREAD) {
      return false;
    }
    final int cameraCount = Camera.getNumberOfCameras();
    Camera.CameraInfo info = new Camera.CameraInfo();
    for (int i = 0; i < cameraCount; i++) {
      Camera.getCameraInfo(i, info);
      if (facing == info.facing) {
        return true;
      }
    }
    return false;
  }

  /**
   * 检查设备是否有摄像头
   */
  public static boolean hasCamera() {
    return hasBackFacingCamera() || hasFrontFacingCamera();
  }

  /**
   * 检查设备是否有后置摄像头
   */
  public static boolean hasBackFacingCamera() {
    final int CAMERA_FACING_BACK = 0;
    return checkCameraFacing(CAMERA_FACING_BACK);
  }

  /**
   * 检查设备是否有前置摄像头
   */
  public static boolean hasFrontFacingCamera() {
    final int CAMERA_FACING_BACK = 1;
    return checkCameraFacing(CAMERA_FACING_BACK);
  }

  public static int getSdkVersion() {
    return android.os.Build.VERSION.SDK_INT;
  }
}
