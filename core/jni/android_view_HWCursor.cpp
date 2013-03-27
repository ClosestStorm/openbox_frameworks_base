#include "JNIHelp.h"
#include "jni.h"
#include "android_runtime/AndroidRuntime.h"
#include <utils/misc.h>
#include <hardware_legacy/power.h>
#include <sys/reboot.h>
#include <utils/Log.h>
#include <hardware/hardware.h>
#include <hardware/display.h>
#include <ui/PixelFormat.h>
#include <SkCanvas.h>
#include <SkBitmap.h>
#include <SkRegion.h>
#define LOG_TAG "android_view_HWCursor"
#define LOG_NDEBUG 0


namespace android
{
    struct dio_t 
    {
        jfieldID canvas;
        jfieldID saveCount;
    };
    
    static dio_t dio;

    struct dco_t 
    {
        jfieldID surfaceFormat;
    };
    static dco_t dco;

    struct dno_t 
    {
        jfieldID native_canvas;
    };
    static dno_t dno;
    
    hw_module_t* 		disp_module = NULL;
    display_device_t*	disp_device = NULL;
    void*               canvas_addr;

    static void throw_NullPointerException(JNIEnv *env, const char* msg)
    {
        jclass clazz;
        clazz = env->FindClass("java/lang/NullPointerException");
        env->ThrowNew(clazz, msg);
    }
    
    static __attribute__((noinline))
    void doThrow(JNIEnv* env, const char* exc, const char* msg = NULL)
    {
        if (!env->ExceptionOccurred()) {
            jclass npeClazz = env->FindClass(exc);
            env->ThrowNew(npeClazz, msg);
        }
    }
    
    static int hwcursorInit_native(JNIEnv *env, jobject clazz,int displayno)
    {
        int 				err;
	    
	    err = hw_get_module(DISPLAY_HARDWARE_MODULE_ID, (hw_module_t const**)&disp_module);
	    if (err == 0) 
	    {
		    err = display_open(disp_module, &disp_device);
		    if (err == 0) 
		    {
		    	LOGE("Open Display Device Failed!\n");
		    } 
	    }
	    
	    if(disp_device)
		{
			return  disp_device->hwcursorinit(disp_device,displayno);
		}
		
		return  -1;
    }
    
    static int hwcursorShow_native(JNIEnv *env, jobject clazz,int displayno)
	{
		if(disp_device)
		{
			return  disp_device->hwcursorshow(disp_device,displayno);
		}
		
		return  -1;
	}
	
	static int	hwcursorHide_native(JNIEnv *env, jobject clazz,int displayno)
	{
		if(disp_device)
		{
			return  disp_device->hwcursorhide(disp_device,displayno);
		}
		
		return  -1;
	}

    static int  hwcursorSetPosition(int displayno,int posX,int posY)
	{
		if(disp_device)
		{
			return  disp_device->sethwcursorpos(disp_device,displayno,posX,posY);
		}
		
		return  -1;
	}
	
	static int	hwcursorGetPositionX_native(JNIEnv *env, jobject clazz,int displayno)
	{
		if(disp_device)
		{
			return  disp_device->gethwcursorposx(disp_device,displayno);
		}
		
		return  -1;
	}

    static int hwcursorGetPositionY_native(JNIEnv *env, jobject clazz,int displayno)
	{
		if(disp_device)
		{
			return  disp_device->gethwcursorposy(disp_device,displayno);
		}
		
		return  -1;
	}

    static void* hwcursorGetVAddr(int displayno)
	{
		if(disp_device)
		{
			return  (void *)disp_device->hwcursorgetvaddr(disp_device,displayno);
		}
		
		return  NULL;
	}

    static int	hwcursorGetPAddr(int displayno)
	{
		if(disp_device)
		{
			return  disp_device->hwcursorgetpaddr(disp_device,displayno);
		}
		
		return  0;
	}

    static int hwcursorSetPosition_native(JNIEnv *env, jobject clazz,int displayno,int posX,int posY)
	{
		if(disp_device)
		{
            int  displayposX;
            int  displayposY;
            int  width,height;
            int  valid_width,valid_height;
            int  fb_width,fb_height;

            width           = disp_device->getdisplayparameter(disp_device,displayno,DISPLAY_OUTPUT_WIDTH);
            height          = disp_device->getdisplayparameter(disp_device,displayno,DISPLAY_OUTPUT_HEIGHT);
            valid_width     = disp_device->getdisplayparameter(disp_device,displayno,DISPLAY_OUTPUT_VALIDWIDTH);
            valid_height    = disp_device->getdisplayparameter(disp_device,displayno,DISPLAY_OUTPUT_VALIDHEIGHT);
            fb_width        = disp_device->getdisplayparameter(disp_device,displayno,DISPLAY_FBWIDTH);
            fb_height       = disp_device->getdisplayparameter(disp_device,displayno,DISPLAY_FBHEIGHT);

            LOGV("hwcurosrMapDisplayPosition width = %d\n",width);
            LOGV("hwcurosrMapDisplayPosition height = %d\n",height);
            LOGV("hwcurosrMapDisplayPosition valid_width = %d\n",valid_width);
            LOGV("hwcurosrMapDisplayPosition valid_height = %d\n",valid_height);
            LOGV("hwcurosrMapDisplayPosition fb_width = %d\n",fb_width);
            LOGV("hwcurosrMapDisplayPosition fb_height = %d\n",fb_height);
            
            displayposX     = posX * valid_width/fb_width + ((width - valid_width) >> 1);
            displayposY     = posY * valid_height/fb_height + ((height - valid_height) >> 1);
            
            LOGV("hwcurosrMapDisplayPosition displayposX = %d\n",displayposX);
            LOGV("hwcurosrMapDisplayPosition displayposY = %d\n",displayposY);
            LOGV("hwcurosrMapDisplayPosition displayno = %d\n",displayno);
            
			return  disp_device->sethwcursorpos(disp_device,displayno,displayposX,displayposY);
		}
		
		return  -1;
	}
	
	static void hwcursor_canvasswap()
	{
	    unsigned char  tmp;
	    unsigned char  *values;
	    int      count;
	    
	    if(canvas_addr)
	    {
	        values = (unsigned char *)canvas_addr;
	        for(count = 0;count < 128 * 128;count++)
	        {
	            tmp = values[0];
	            values[0] = values[2];
	            values[2] = tmp;
	            
	            values = values + 4;
	        }
	    }
	    
	}
	
	static jobject hwcursorLockCanvas_native(JNIEnv* env, jobject clazz,int displayno)
    {
        void  *vaddr;
        
        canvas_addr  = hwcursorGetVAddr(displayno);
        if(canvas_addr == NULL)
        {
            LOGE("get virtual addr failed!\n");

            return NULL;
        }
        
        LOGV("hwcursorLockCanvas_native canvas_addr = %x\n",(unsigned long)canvas_addr);
        // Associate a SkCanvas object to this surface
        jobject canvas = env->GetObjectField(clazz, dio.canvas);
        LOGV("hwcursorLockCanvas_native01 vaddr = %x\n",(unsigned long)vaddr);
        env->SetIntField(canvas, dco.surfaceFormat, PIXEL_FORMAT_RGBA_8888);
        LOGV("hwcursorLockCanvas_native1 vaddr = %x\n",(unsigned long)vaddr);
        SkCanvas* nativeCanvas = (SkCanvas*)env->GetIntField(canvas, dno.native_canvas);
        SkBitmap bitmap;
        ssize_t bpr = 128 * 4;
        bitmap.setConfig(SkBitmap::kARGB_8888_Config, 128, 128, bpr);
        bitmap.setIsOpaque(false);
        bitmap.setPixels(canvas_addr);
        LOGV("hwcursorLockCanvas_native2 vaddr = %x\n",(unsigned long)vaddr);
        nativeCanvas->setBitmapDevice(bitmap);
      
        int saveCount = nativeCanvas->save();
        LOGV("hwcursorLockCanvas_native3 vaddr = %x\n",(unsigned long)vaddr);
        env->SetIntField(clazz, dio.saveCount, saveCount);

        return canvas;
    }

    static void hwcursorUnLockCanvasAndPost_native(JNIEnv* env, jobject clazz, jobject argCanvas)
    {
        jobject canvas = env->GetObjectField(clazz, dio.canvas);
        if (canvas != argCanvas) 
        {
            doThrow(env, "java/lang/IllegalArgumentException", NULL);
            
            return;
        }
        
        hwcursor_canvasswap();

        // detach the canvas from the surface
        SkCanvas* nativeCanvas = (SkCanvas*)env->GetIntField(canvas, dno.native_canvas);
        int saveCount = env->GetIntField(clazz, dio.saveCount);
        nativeCanvas->restoreToCount(saveCount);
        nativeCanvas->setBitmapDevice(SkBitmap());
        env->SetIntField(clazz, dio.saveCount, 0);
    }
    
    static void nativeClassInit(JNIEnv* env, jclass clazz);

    static JNINativeMethod method_table[] = 
    {
        { "nativeHwCursorInit", "(I)I",(void *)hwcursorInit_native},
        { "nativeHwCursorShow", "(I)I",(void *)hwcursorShow_native},
        { "nativeHwCursorHide", "(I)I",(void *)hwcursorHide_native},
        { "nativeHwCursorSetPosition", "(III)I",(void *)hwcursorSetPosition_native},
        { "nativeHwCursorGetPositionX", "(I)I",(void *)hwcursorGetPositionX_native},
        { "nativeHwCursorGetPositionY", "(I)I",(void *)hwcursorGetPositionY_native},
        { "nativeHwCursorLockCanvas", "(I)Landroid/graphics/Canvas;",(void *)hwcursorLockCanvas_native},
        { "nativeHwCursorUnLockCanvasAndPost", "(Landroid/graphics/Canvas;)I",(void *)hwcursorUnLockCanvasAndPost_native},
        { "nativeClassInit","()V",(void *)nativeClassInit}
    };
    
    void nativeClassInit(JNIEnv* env, jclass clazz)
    {
        LOGD("nativeClassInit0");
        dio.canvas          = env->GetFieldID(clazz, "mHWCursorCanvas", "Landroid/graphics/Canvas;");
        LOGD("nativeClassInit1");
        dio.saveCount       = env->GetFieldID(clazz, "mHWCursorSaveCount", "I");
        LOGD("nativeClassInit2");
        jclass canvas       = env->FindClass("android/graphics/Canvas");
        dno.native_canvas   = env->GetFieldID(canvas, "mNativeCanvas", "I");
        dco.surfaceFormat   = env->GetFieldID(canvas, "mSurfaceFormat", "I");
        LOGD("nativeClassInit3");
    }
    
    int register_android_view_HWCursor(JNIEnv *env)
    {
        return AndroidRuntime::registerNativeMethods(
            env, "android/view/HWCursor",
            method_table, NELEM(method_table));
    }
};