LOCAL_PATH := $(call my-dir)

#====================================== libvorbisidec

include $(CLEAR_VARS)

LOCAL_SRC_FILES = \
	Tremor/bitwise.c \
	Tremor/codebook.c \
	Tremor/dsp.c \
	Tremor/floor0.c \
	Tremor/floor1.c \
	Tremor/floor_lookup.c \
	Tremor/framing.c \
	Tremor/info.c \
	Tremor/mapping0.c \
	Tremor/mdct.c \
	Tremor/misc.c \
	Tremor/res012.c \
	Tremor/vorbisfile.c

LOCAL_CFLAGS+= -O2 -fsigned-char

ifeq ($(TARGET_ARCH),arm)
LOCAL_CFLAGS+= -D_ARM_ASSEM_
endif
	
zLOCAL_C_INCLUDES:= \
	$(LOCAL_PATH)/Tremor

LOCAL_ARM_MODE := arm

LOCAL_MODULE := libvorbisidec

include $(BUILD_STATIC_LIBRARY)

#====================================== tof

include $(CLEAR_VARS)

LOCAL_MODULE           := tof

LOCAL_CFLAGS           := -I$(LOCAL_PATH)/Tremor
LOCAL_C_INCLUDES       := $(LOCAL_PATH)/Tremor
LOCAL_SRC_FILES        := VorbisDecoder.cpp

LOCAL_STATIC_LIBRARIES := libvorbisidec

include $(BUILD_SHARED_LIBRARY)
