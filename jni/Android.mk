LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := textlink
LOCAL_SRC_FILES := textlink.c htmltotxt.c prctotxt.c

include $(BUILD_SHARED_LIBRARY)
