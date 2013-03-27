//
// Copyright 2010 The Android Open Source Project
//
// The Display dispatcher.
//
#define LOG_TAG "DisplayDispatcher"

//#define LOG_NDEBUG 0

#include <cutils/log.h>
#include <ui/PowerManager.h>
#include <ui/DisplaySemaphore.h>
#include <ui/DisplayDispatcher.h>
#include <stddef.h>
#include <unistd.h>
#include <errno.h>
#include <limits.h>
#include <cutils/properties.h>


#define INDENT "  "
#define INDENT2 "    "

#define  MAX_FRAMEID                  2147483640

namespace android 
{
	void	DisplayDispatcherThread::enqueuebuf(int frameidx)
	{
		int   i;
		int   tmp;
		
		tmp = mFrameidx[frameidx];
		for(i = frameidx;i > 0;i--)
		{
			mFrameidx[i] = mFrameidx[i - 1];
		}
		
		mFrameidx[0] = tmp;
	}
	
    void DisplayDispatcherThread::setSrcBuf(int srcfb_id,int srcfb_offset)
    {
        mSrcfbid        = srcfb_id;
        mSrcfboffset    = srcfb_offset;
    }

    void DisplayDispatcherThread::signalEvent()
    {
        mSemaphore->up();
    }

    void DisplayDispatcherThread::waitForEvent()
    {
        mSemaphore->down();
    }
    
    void DisplayDispatcherThread::resetEvent()
    {
        mSemaphore->reset();
    }
    
    // --- InputDispatcherThread ---
    void DisplayDispatcherThread::LooperOnce()
    {
        int  writebufid;
        int  showbufid;
        int  ret;
        int  write_index;
        
        waitForEvent();

        mDispDevice->request_modelock(mDispDevice);

		if(mFrameidx[DISPLAYDISPATCH_MAXBUFNO - 1] == mFbOffset)
		{
        	writebufid  = mFrameidx[DISPLAYDISPATCH_MAXBUFNO - 2];

            write_index = DISPLAYDISPATCH_MAXBUFNO - 2;
    	}
    	else
    	{
    		writebufid  = mFrameidx[DISPLAYDISPATCH_MAXBUFNO - 1];

            write_index = DISPLAYDISPATCH_MAXBUFNO - 1;
    	}
    	
        ret = mDispDevice->copysrcfbtodstfb(mDispDevice,mSrcfbid,1 - mSrcfboffset,1 - mSrcfbid,writebufid);
        if(ret != 0)
        {
            mDispDevice->release_modelock(mDispDevice);

            return ;
        }

        enqueuebuf(write_index);

        showbufid = mFrameidx[0];
        
        mFbOffset = showbufid;
        

        mDispDevice->pandisplay(mDispDevice,1 - mSrcfbid,showbufid);

        mDispDevice->release_modelock(mDispDevice); 
    }

    DisplayDispatcherThread::DisplayDispatcherThread(display_device_t*	mDevice) :
            Thread(/*canCallJava*/ true), mDispDevice(mDevice) 
    {
    	for(int i = 0;i < DISPLAYDISPATCH_MAXBUFNO;i++)
    	{
    		mFrameidx[i] 		= i;
    	}
    	
        mSemaphore = new DisplaySemaphore(0);
    }

    DisplayDispatcherThread::~DisplayDispatcherThread() 
    {
        
    }

    bool DisplayDispatcherThread::threadLoop() 
    {
        this->LooperOnce();
        return true;
    }

    DisplayDispatcher::DisplayDispatcher()
    {
        int 				err;
        hw_module_t* 		module;
        status_t 			result;
		char property[PROPERTY_VALUE_MAX];
	    err = hw_get_module(DISPLAY_HARDWARE_MODULE_ID, (hw_module_t const**)&module);
	    if (err == 0) 
	    {            
		    err = display_open(module, &mDevice);
	    } 
        else
        {
            LOGW("hw_get display module Failed!\n");
        }

	   if (property_get("ro.display.switch", property, NULL) > 0) 
       {
	        if (atoi(property) == 1) 
	        {
	            mThread = new DisplayDispatcherThread(mDevice);
		        result = mThread->run("DisplayDispatcher", PRIORITY_HIGHEST);
			    if (result) 
			    {
			        LOGE("Could not start DisplayDispatcher thread due to error %d.", result);
			
			        mThread->requestExit();
			    }
			    
	            LOGD("display dispatcher enabled");
	        }
	        else
	        {
	            LOGW("display dispatcher disable");
	        }
	    }
	    else
	    {
	        LOGW("display dispatcher disable");
	    }
    }

    DisplayDispatcher::~DisplayDispatcher()
    {
    }
	
	int DisplayDispatcher::setDisplayParameter(int displayno, int value0,int value1)
	{
		if(displayno == 0)
		{
			mDisplayType0 	= value0;
			mDisplayFormat0 = value1;
		}
		else
		{
			mDisplayType1 	= value0;
			mDisplayFormat1 = value1;
		}
		
		return  0;
	}
	
	
	int DisplayDispatcher::setDisplayMode(int mode)
	{
		if(mDevice)
		{
			struct display_modepara_t    disp_para;

			disp_para.d0type			= mDisplayType0;
    		disp_para.d1type			= mDisplayType1;
    		disp_para.d0format			= mDisplayFormat0;
    		disp_para.d1format			= mDisplayFormat1;
    		disp_para.d0pixelformat		= 0;
    		disp_para.d1pixelformat		= 0;
    		disp_para.masterdisplay		= 0;

			return  mDevice->setdisplaymode(mDevice,mode,&disp_para);
		}

		return  -1;
	}

    int DisplayDispatcher::setDispProp(int cmd,int param0,int param1,int param2)		
    {
        switch(cmd)
        {
            case  DISPLAY_CMD_SETDISPPARA:
                return  setDisplayParameter(param0,param1,param2);

            case  DISPLAY_CMD_CHANGEDISPMODE:
                return  mDevice->changemode(mDevice,param0,param1,param2);

            case  DISPLAY_CMD_CLOSEDISP:
                return  mDevice->closedisplay(mDevice,param0);

            case  DISPLAY_CMD_OPENDISP:
                return  mDevice->opendisplay(mDevice,param0);

            case  DISPLAY_CMD_GETDISPCOUNT:
                return  mDevice->getdisplaycount(mDevice);

            case DISPLAY_CMD_GETDISPLAYMODE:
                return  mDevice->getdisplaymode(mDevice);

            case DISPLAY_CMD_GETDISPPARA:
                return  mDevice->getdisplayparameter(mDevice,param0,param1);

            case DISPLAY_CMD_GETHDMISTATUS:
                return  mDevice->gethdmistatus(mDevice);

            case DISPLAY_CMD_GETMASTERDISP:
                return  mDevice->getmasterdisplay(mDevice);

            case DISPLAY_CMD_GETMAXHDMIMODE:
                return  mDevice->gethdmimaxmode(mDevice);

            case DISPLAY_CMD_GETMAXWIDTHDISP:
                return  mDevice->getmaxwidthdisplay(mDevice);

            case DISPLAY_CMD_GETTVSTATUS:
                return  mDevice->gettvdacstatus(mDevice);

            case DISPLAY_CMD_SETMASTERDISP:
                return  mDevice->setmasterdisplay(mDevice,param0);

            case DISPLAY_CMD_SETDISPMODE:
                return  setDisplayMode(param0);

			case DISPLAY_CMD_SETBACKLIGHTMODE:
				return  mDevice->setdisplaybacklightmode(mDevice,param0);

		    case DISPLAY_CMD_SETORIENTATION:
		        return mDevice->setOrientation(mDevice,param0);

			case DISPLAY_CMD_ISSUPPORTHDMIMODE:
			    return mDevice->issupporthdmimode(mDevice,param0);

			case DISPLAY_CMD_SETAREAPERCENT:
			    return mDevice->setdisplayareapercent(mDevice,param0,param1);

			case DISPLAY_CMD_GETAREAPERCENT:
			    return mDevice->getdisplayareapercent(mDevice,param0);

			case DISPLAY_CMD_SETBRIGHT:
			    return mDevice->setdisplaybrightness(mDevice,param0,param1);

			case DISPLAY_CMD_GETBRIGHT:
			    return mDevice->getdisplaybrightness(mDevice,param0);

			case DISPLAY_CMD_SETCONTRAST:
			    return mDevice->setdisplaycontrast(mDevice,param0,param1);

			case DISPLAY_CMD_GETCONTRAST:
			    return mDevice->getdisplaycontrast(mDevice,param0);

			case DISPLAY_CMD_SETSATURATION:
			    return mDevice->setdisplaysaturation(mDevice,param0,param1);

			case DISPLAY_CMD_GETSATURATION:
			    return mDevice->getdisplaysaturation(mDevice,param0);

			case DISPLAY_CMD_SETHUE:
			    return mDevice->setdisplayhue(mDevice,param0,param1);

			case DISPLAY_CMD_GETHUE:
                return mDevice->getdisplayhue(mDevice,param0);

            default:
                LOGE("Display Cmd not Support!\n");
                return  -1;
        }
    }

    int DisplayDispatcher::getDispProp(int cmd,int param0,int param1)
    {
        return  0;
    }

    void DisplayDispatcher::startSwapBuffer()
    {
        int    master_bufid;
        int    mode;
        
        mode            = mDevice->getdisplaymode(mDevice);
        if(mode == DISPLAY_MODE_DUALSAME || mode == DISPLAY_MODE_DUALSAME_TWO_VIDEO || mode == DISPLAY_MODE_SINGLE_VAR_BE)
        {
            master_bufid    = mDevice->getdisplaybufid(mDevice,0);

            mThread->setSrcBuf(0,master_bufid);
            mThread->signalEvent();
        }
    }

} // namespace android
