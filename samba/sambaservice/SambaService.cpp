/*
**
** Copyright 2008, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

// Proxy for media player implementations

//#define LOG_NDEBUG 0
#include <android/log.h>
#define LOG_TAG "SambaService"
//#include <utils/Log.h>

#include <sys/types.h>
#include <sys/stat.h>
#include <sys/mount.h>
#include <dirent.h>
#include <unistd.h>
#include <errno.h>

#include <string.h>

#include <cutils/atomic.h>
#include <cutils/properties.h> // for property_get

#include <utils/misc.h>

#include <android_runtime/ActivityManager.h>

#include <binder/IPCThreadState.h>
#include <binder/IServiceManager.h>
#include <binder/MemoryHeapBase.h>
#include <binder/MemoryBase.h>
#include <utils/Errors.h>  // for status_t
#include <utils/String8.h>
#include <utils/SystemClock.h>
#include <utils/Vector.h>
#include <cutils/properties.h>

#include <SambaService.h>

/* desktop Linux needs a little help with gettid() */
#if defined(HAVE_GETTID) && !defined(HAVE_ANDROID_OS)
#define __KERNEL__
# include <linux/unistd.h>
#ifdef _syscall0
_syscall0(pid_t,gettid)
#else
pid_t gettid() { return syscall(__NR_gettid);}
#endif
#undef __KERNEL__
#endif


namespace android {

	void SambaService::instantiate() {
		defaultServiceManager()->addService(
				String16("netshared.smb"), new SambaService());
	}

	SambaService::SambaService() {
		LOGV("SambaService created");
	}

	SambaService::~SambaService()
	{
		LOGV("SambaService destroyed");
	}

	int SambaService::vbser_mount(char *sourcedir, char *target,
									char *filesystemtype, unsigned int flags, 
									char *options){
		int ret = mount(sourcedir, target, filesystemtype, flags, options);
		return (ret == 0) ? ret : errno;
	}

	int SambaService::vbser_umount(char *mountpoint){
		int ret = umount(mountpoint);
		return (ret == 0) ? ret : errno;
	}

}
