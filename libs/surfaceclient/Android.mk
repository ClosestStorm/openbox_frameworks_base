LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES:= \
	ISurfaceClient.cpp 

LOCAL_SHARED_LIBRARIES := \
	libcutils \
	libutils \
	libbinder \
	libhardware \
	libui

LOCAL_MODULE:= libsurfaceclient

include $(BUILD_SHARED_LIBRARY)
