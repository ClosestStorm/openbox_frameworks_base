LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	SambaService.cpp \
	ISambaService.cpp \
	main_sambservice.cpp

LOCAL_SHARED_LIBRARIES := \
	libcutils \
	libutils \
	libbinder

LOCAL_C_INCLUDES := \
	$(LOCAL)

LOCAL_MODULE_TAGS := optional

LOCAL_MODULE:= sambaserver

include $(BUILD_EXECUTABLE)
