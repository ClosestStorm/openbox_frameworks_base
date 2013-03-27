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

#ifndef ANDROID_MEDIAPLAYERSERVICE_H
#define ANDROID_MEDIAPLAYERSERVICE_H

#include <utils/Log.h>
#include <utils/threads.h>
#include <utils/List.h>
#include <utils/Errors.h>
#include <utils/KeyedVector.h>
#include <utils/String8.h>
#include <utils/Vector.h>

#include <ISambaService.h>


namespace android {


class SambaService : public BnSambaService
{
	public:
		static  void                instantiate();
		virtual int vbser_mount(char *sourcedir, char *target,
				char *filesystemtype, unsigned int flags, 
				char *options);

		virtual int vbser_umount(char *mountpoint);
	private:
		SambaService();
		virtual ~SambaService();
};

// ----------------------------------------------------------------------------

}; // namespace android

#endif // ANDROID_MEDIAPLAYERSERVICE_H
