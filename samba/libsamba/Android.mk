BASE_PATH := $(call my-dir)
LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_ARM_MODE := arm

LOCAL_SRC_FILES := \
	source/libsmb/libsmbclient.c \
	source/lib/charcnv.c \
	source/lib/pwd_grp.c \
	source/lib/charset.c \
	source/lib/debug.c \
	source/lib/fault.c \
	source/lib/getsmbpass.c \
	source/lib/interface.c \
	source/lib/kanji.c \
	source/lib/md4.c \
	source/lib/interfaces.c \
	source/lib/pidfile.c \
	source/lib/replace.c \
	source/lib/signal.c \
	source/lib/system.c \
	source/lib/sendfile.c \
	source/lib/time.c \
	source/lib/ufc.c \
	source/lib/genrand.c \
	source/lib/username.c \
	source/lib/util_getent.c \
	source/lib/access.c \
	source/lib/smbrun.c \
	source/lib/bitmap.c \
	source/lib/crc32.c \
	source/lib/snprintf.c \
	source/lib/wins_srv.c \
	source/lib/util_str.c \
	source/lib/util_sid.c \
	source/lib/util_unistr.c \
	source/lib/util_file.c \
	source/lib/util.c \
	source/lib/util_sock.c \
	source/lib/util_sec.c \
	source/smbd/ssl.c \
	source/lib/talloc.c \
	source/lib/hash.c \
	source/lib/substitute.c \
	source/lib/fsusage.c \
	source/lib/ms_fnmatch.c \
	source/lib/select.c \
	source/lib/error.c \
	source/lib/messages.c \
	source/lib/pam_errors.c \
	source/nsswitch/wb_client.c \
	source/nsswitch/wb_common.c \
	source/tdb/tdb.c \
	source/tdb/spinlock.c \
	source/tdb/tdbutil.c \
	source/libsmb/clientgen.c \
	source/libsmb/cliconnect.c \
	source/libsmb/clifile.c \
	source/libsmb/clirap.c \
	source/libsmb/clierror.c \
	source/libsmb/climessage.c \
	source/libsmb/clireadwrite.c \
	source/libsmb/clilist.c \
	source/libsmb/cliprint.c \
	source/libsmb/clitrans.c \
	source/libsmb/clisecdesc.c \
	source/libsmb/clidgram.c \
	source/libsmb/namequery.c \
	source/libsmb/nmblib.c \
	source/libsmb/clistr.c \
	source/libsmb/nterr.c \
	source/libsmb/smbdes.c \
	source/libsmb/smbencrypt.c \
	source/libsmb/smberr.c \
	source/libsmb/credentials.c \
	source/libsmb/pwd_cache.c \
	source/libsmb/clioplock.c \
	source/libsmb/errormap.c \
	source/libsmb/doserr.c \
	source/libsmb/passchange.c \
	source/libsmb/unexpected.c \
	source/rpc_parse/parse_prs.c \
	source/rpc_parse/parse_sec.c \
	source/rpc_parse/parse_misc.c \
	source/libsmb/namecache.c \
	source/param/loadparm.c \
	source/param/params.c \
	source/ubiqx/ubi_BinTree.c \
	source/ubiqx/ubi_Cache.c \
	source/ubiqx/ubi_SplayTree.c \
	source/ubiqx/ubi_dLinkList.c \
	source/ubiqx/ubi_sLinkList.c \
	source/ubiqx/debugparse.c

prefix=/data/data/samba
PRIVATEDIR = ${prefix}/private
PASSWD_PROGRAM = /usr/bin/passwd
SMB_PASSWD_FILE = $(PRIVATEDIR)/smbpasswd
TDB_PASSWD_FILE = $(PRIVATEDIR)/smbpasswd.tdb
VARDIR = ${prefix}/var
LOGFILEBASE = $(VARDIR)
CONFIGFILE = $(prefix)/smb.conf
LIBDIR = /system/lib
SWATDIR = ${prefix}/swat
SBINDIR = /system/bin
BINDIR = /system/bin
LOCKDIR = ${VARDIR}/locks
CODEPAGEDIR = $(LIBDIR)/codepages
PIDDIR = $(VARDIR)/locks

PASSWD_FLAGS = -DPASSWD_PROGRAM=\"$(PASSWD_PROGRAM)\" -DSMB_PASSWD_FILE=\"$(SMB_PASSWD_FILE)\" -DTDB_PASSWD_FILE=\"$(TDB_PASSWD_FILE)\"
FLAGS1 =  $(CPPFLAGS) -DLOGFILEBASE=\"$(LOGFILEBASE)\"
FLAGS2 = -DCONFIGFILE=\"$(CONFIGFILE)\" -DLMHOSTSFILE=\"$(LMHOSTSFILE)\"  
FLAGS3 = -DSWATDIR=\"$(SWATDIR)\" -DSBINDIR=\"$(SBINDIR)\" -DLOCKDIR=\"$(LOCKDIR)\" -DCODEPAGEDIR=\"$(CODEPAGEDIR)\"
FLAGS4 = -DDRIVERFILE=\"$(DRIVERFILE)\" -DBINDIR=\"$(BINDIR)\" -DPIDDIR=\"$(PIDDIR)\" -DLIBDIR=\"$(LIBDIR)\"
FLAGS5 = $(FLAGS1) $(FLAGS2) $(FLAGS3) $(FLAGS4) -DHAVE_INCLUDES_H
FLAGS  =  $(FLAGS5) $(PASSWD_FLAGS)


#LOCAL_CFLAGS += -O3 -fstrict-aliasing -fprefetch-loop-arrays
#LOCAL_CFLAGS += -march=armv6j

LOCAL_CFLAGS += $(FLAGS)

#LOCAL_CPPFLAGS=-D_LARGEFILE64_SOURCE -D_FILE_OFFSET_BITS=64 -D_GNU_SOURCE 

LOCAL_MODULE:= libsmbclient

LOCAL_MODULE_TAGS := optional

LOCAL_SHARED_LIBRARIES := libdl liblog

#LOCAL_STATIC_LIBRARIES := \
#	libft2 \
#	libpng \
#	libgif

LOCAL_C_INCLUDES += \
	$(KERNEL_HEADERS) \
	$(LOCAL_PATH)/source \
	$(LOCAL_PATH)/source/popt \
	$(LOCAL_PATH)/source/tdb \
	$(LOCAL_PATH)/source/aparser \
	$(LOCAL_PATH)/source/pam_smbpass \
	$(LOCAL_PATH)/source/include \
	$(LOCAL_PATH)/source/rpcclient \
	$(LOCAL_PATH)/source/smbwrapper \
	$(LOCAL_PATH)/source/web/po \
	$(LOCAL_PATH)/source/nsswitch \
	$(LOCAL_PATH)/source/ubiqx \
	$(LOCAL_PATH)/source/popt \

LOCAL_LDLIBS += -lpthread
LOCAL_PRELINK_MODULE := false
#LOCAL_LDLIBS += -ldl -lnsl -lcrypt

include $(BUILD_SHARED_LIBRARY)
