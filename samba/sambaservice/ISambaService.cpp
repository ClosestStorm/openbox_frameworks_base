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

#include <stdint.h>
#include <sys/types.h>
#include <sys/mount.h>

#include <binder/Parcel.h>
#include <binder/IMemory.h>
#include <utils/Errors.h>  // for status_t

#include <SambaService.h>  // for status_t

namespace android {

enum {
    CREATE_URL = IBinder::FIRST_CALL_TRANSACTION,
	SAMBA_MOUNT,
	SAMBA_UMOUNT
};

class BpSambaService: public BpInterface<ISambaService>
{
public:
    BpSambaService(const sp<IBinder>& impl)
        : BpInterface<ISambaService>(impl)
    {
    }

	int vbser_mount(char *sourcedir, char *target,
									char *filesystemtype, unsigned int flags, 
									char *options){
        Parcel data, reply;
        data.writeInterfaceToken(ISambaService::getInterfaceDescriptor());
        data.writeCString(sourcedir);
        data.writeCString(target);
        data.writeCString(filesystemtype);
        data.writeInt32(flags);
        data.writeCString(options);
        remote()->transact(SAMBA_MOUNT, data, &reply);
        return reply.readInt32();
    }

	int vbser_umount(char *mountpoint){
        Parcel data, reply;
        data.writeInterfaceToken(ISambaService::getInterfaceDescriptor());
		data.writeCString(mountpoint);
        remote()->transact(SAMBA_UMOUNT, data, &reply);
        return reply.readInt32();
	}

};

IMPLEMENT_META_INTERFACE(SambaService, "com.allwinner.ISambaService");

// ----------------------------------------------------------------------

status_t BnSambaService::onTransact(
    uint32_t code, const Parcel& data, Parcel* reply, uint32_t flags)
{
    switch(code) {
        case SAMBA_MOUNT: {
            CHECK_INTERFACE(ISamba, data, reply);
			char *sourcedir = (char *)data.readCString();
			char *target = (char*)data.readCString();
			char *filesystemtype = (char *)data.readCString();
			unsigned int flag = data.readInt32();
			char *options = (char *)data.readCString();
			int ret = vbser_mount(sourcedir, target, filesystemtype, flag, options);
            reply->writeInt32(ret);
            return NO_ERROR;
        } break;
		case SAMBA_UMOUNT: {
            CHECK_INTERFACE(ISamba, data, reply);
			char *mountpoint = (char *)data.readCString();
            int ret = vbser_umount(mountpoint);
            reply->writeInt32(ret);
            return NO_ERROR;
		} break;

        default:
            return BBinder::onTransact(code, data, reply, flags);
    }
}

// ----------------------------------------------------------------------------

}; // namespace android
