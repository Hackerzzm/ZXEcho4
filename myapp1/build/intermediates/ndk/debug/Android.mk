LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := libagorasdk2
LOCAL_LDFLAGS := -Wl,--build-id
LOCAL_SRC_FILES := \
	D:\AndroidStudioProjects\ZXEcho4\myapp1\src\main\jni\agora_sdk2_jni.cpp \
	D:\AndroidStudioProjects\ZXEcho4\myapp1\src\main\jni\Android.mk \
	D:\AndroidStudioProjects\ZXEcho4\myapp1\src\main\jni\Android.mk.a \
	D:\AndroidStudioProjects\ZXEcho4\myapp1\src\main\jni\Android.mk.bak \
	D:\AndroidStudioProjects\ZXEcho4\myapp1\src\main\jni\Android.mk.so \
	D:\AndroidStudioProjects\ZXEcho4\myapp1\src\main\jni\Application.mk \
	D:\AndroidStudioProjects\ZXEcho4\myapp1\src\main\jni\libuv.mk \
	D:\AndroidStudioProjects\ZXEcho4\myapp1\src\main\jni\test.cpp \

LOCAL_C_INCLUDES += D:\AndroidStudioProjects\ZXEcho4\myapp1\src\main\jni
LOCAL_C_INCLUDES += D:\AndroidStudioProjects\ZXEcho4\myapp1\src\debug\jni

include $(BUILD_SHARED_LIBRARY)
