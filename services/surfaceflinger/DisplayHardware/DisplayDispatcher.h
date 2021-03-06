/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef _UI_DISPLAY_DISPATCHER_H
#define _UI_DISPLAY_DISPATCHER_H

#include <ui/Input.h>
#include <ui/InputTransport.h>
#include <utils/KeyedVector.h>
#include <utils/Vector.h>
#include <utils/threads.h>
#include <utils/Timers.h>
#include <utils/RefBase.h>
#include <utils/String8.h>
#include <utils/Looper.h>
#include <utils/BitSet.h>
#include <hardware/display.h>
#include <hardware/hardware.h>
#include <stddef.h>
#include <unistd.h>
#include <limits.h>
#include <ui/DisplayCommand.h>
#include "DisplaySemaphore.h"

namespace android 
{
	class SurfaceFlinger; 
	
    /* 同显时的帧管理线程 */
    class DisplayDispatcherThread : public Thread 
    {
        public:
            explicit DisplayDispatcherThread(display_device_t*	mDevice,const sp<SurfaceFlinger>& flinger);
            ~DisplayDispatcherThread();
            void                setSrcBuf(int srcfb_id,int srcfb_offset);
            void                signalEvent(int reason);
            void                waitForEvent();
            void 				resetEvent();
            void				setConvertBufId(int bufid);
            void				setConvertBufParam(int handle,int width,int height,int format);
        private:
        	sp<SurfaceFlinger> mFlinger;
            sp<DisplaySemaphore>    mSemaphore;
            int                 mSrcfbid;
            int                 mSrcfboffset;
            int                 mCurfb;             /*定义同显时需要显示副屏需要显示的fb no*/
            int                 mFbOffset;          /*定义当前在副屏上显示的fb中的buffer id*/
            int                 mFrameidx[DISPLAYDISPATCH_MAXBUFNO];  /*每个buffer管理的帧号*/
            void				enqueuebuf(int frameidx);
            virtual bool        threadLoop();
            void 				LooperOnce();
    		display_device_t*	mDispDevice;
    		int                 mWifiDisplayBufHandle;
            int                 mWifiDisplayBufWidth;
            int                 mWifiDisplayBufHeight;
            int                 mWifiDisplayBufFormat;
    		bool				mStartConvert;	
    		int					mConvertBufId;
    		int					mStartReason;
    		int64_t				mStartTime;
    		mutable Mutex       mBufferLock;
    };

    class DisplayDispatcher:public virtual RefBase
    {
        public:
            DisplayDispatcher(const sp<SurfaceFlinger>& flinger);
            ~DisplayDispatcher();
            
            int     			setDispProp(int cmd,int param0,int param1,int param2);
            int     			getDispProp(int cmd,int param0,int param1);
            void    			startSwapBuffer();
            	
        private:   
        	sp<SurfaceFlinger>  mFlinger; 			
            int  				changeDisplayMode(int displayno, int value0,int value1);
			int  				setDisplayParameter(int displayno, int value0,int value1);	
			int  				setDisplayMode(int mode);
			int					openDisplay(int displayno);
			int 				closeDisplay(int displayno);
			int 				getHdmiStatus(void);
			int 				getTvDacStatus(void);
			int 				getDisplayParameter(int displayno, int param);
			int 				setMasterDisplay(int displayno);
			int 				getMasterDisplay();
			int 				getDisplayMode();
			int 				getDisplayCount();
			int					getMaxWidthDisplay();
			int     			getMaxHdmiMode();
			int     			setDisplayBacklightMode(int mode);   
			int					setConvertBufId(int bufid);
			
			/*wifi Display相关的处理*/
			int     			startWifiDisplaySend(int displayno,int mode);
			int     			endWifiDisplaySend(int displayno);
			int     			startWifiDisplayReceive(int displayno,int mode);
			int					endWifiDisplayReceive(int displayno);
            int     			updateSendClient(int mode);
			        			
			bool 				mDisplayOpen0;
	        bool 				mDisplayOpen1;
	        int 				mDisplayMaster;
	        int 				mDisplayMode;
	        int 				mDisplayPixelFormat0;
	        int 				mDisplayPixelFormat1;
	        int 				mDisplayType0;
	        int 				mDisplayType1;
	        int 				mDisplayFormat0;
	        int 				mDisplayFormat1;
            int                 mWifiDisplayBufHandle;
            int                 mWifiDisplayBufWidth;
            int                 mWifiDisplayBufHeight;
            int                 mWifiDisplayBufFormat;
            bool				mStartConvert;
            int					mConvertBufId;
            sp<DisplayDispatcherThread>   mThread;
            display_device_t*	mDevice;	
            
    };

} // namespace android

#endif // _UI_INPUT_DISPATCHER_H
