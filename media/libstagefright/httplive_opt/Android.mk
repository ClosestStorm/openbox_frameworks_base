include frameworks/base/media/CedarX-Projects/Config.mk

ifeq ($(CEDARX_DEBUG_FRAMEWORK),Y)
include frameworks/base/media/CedarX-Projects/CedarX/overlay/httplive/Android.mk
endif
