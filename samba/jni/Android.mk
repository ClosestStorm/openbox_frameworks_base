LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_PRELINK_MODULE := false
LOCAL_SRC_FILES:= \
    com_allwinner_smb.cpp \
	../sambaservice/ISambaService.cpp

LOCAL_C_INCLUDES += \
    $(JNI_H_INCLUDE) \
	frameworks/base/samba/sambaservice

LOCAL_SHARED_LIBRARIES := \
	libcutils \
	libnativehelper \
	libsmbclient \
	libbinder \
	libutils 

LOCAL_CFLAGS += -O3 -g
LOCAL_LDLIBS += -llog -lm

LOCAL_MODULE := libsmbjni
LOCAL_MODULE_TAGS := optional
include $(BUILD_SHARED_LIBRARY)

