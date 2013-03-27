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

package com.android.systemui.statusbar.policy;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Slog;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.DisplayManager;
import com.android.systemui.statusbar.policy.DisplayHotPlugPolicy;
import android.media.MediaPlayer;
import android.media.AudioSystem;
import com.android.systemui.R;
import android.os.SystemProperties;
import android.provider.Settings;

/* add by Gary. start {{----------------------------------- */
/* 2012-01-29 */
/* add a new function 'getProductName()' in ProducSpec and define some produects' names */
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.Gravity;
import android.view.DispList;
import android.os.Gpio;
import android.media.AudioManager;
import android.provider.Settings;
import android.view.View;
import com.android.internal.allwinner.config.ProductConfig;
import android.os.Handler;
/* add by Gary. end   -----------------------------------}} */

public class DisplayController extends BroadcastReceiver {
    private static final String TAG = "StatusBar.DisplayController";

    private Context mContext;
    private final  DisplayManager mDisplayManager;
	private DisplayHotPlugPolicy  mDispHotPolicy = null;
	private static final boolean SHOW_HDMIPLUG_IN_CALL = true;
    private static final boolean SHOW_TVPLUG_IN_CALL = true;
    /* add by Gary. start {{----------------------------------- */
    /* 2011-12-19 */
    /* handle the plug in and out of the display cables */
    private final boolean         mDeviceHasYpbpr = false;
    private AudioManager          mAudioManager = null;
    private HotPlugToast          mToast = null;
    private boolean               mBootCompleted = false;
    private boolean               mHdmiIsConnected = false;
    private boolean               mCvbsIsConnected = false;
    private boolean               mYpbprIsConnected = false;
    private boolean               mIsStandby = false;
    private String                mChipType = null;
    private Runnable              mSetStandbyState = new Runnable() {
        public void run(){
            mIsStandby = false;
        }
    };
    private Runnable              mSetMutePin = new Runnable() {
        public void run(){
            Slog.d(TAG, "------------------mute on----------------");
            if(ProductConfig.PRODUCT_NAME_APOLLO_MELE.equals(ProductConfig.getProductSpec().getProductName())){
                Gpio.writeGpio('b', 3, 1);
            }
        }
    };
    private final Handler mHandler = new Handler();
    /* add by Gary. end   -----------------------------------}} */

    public DisplayController(Context context) {
        mContext = context;

		mDisplayManager = (DisplayManager) mContext.getSystemService(Context.DISPLAY_SERVICE);

        /* add by Gary. start {{----------------------------------- */
        /* 2012-05-30 */
        /* modify for a10s */	
        mChipType = SystemProperties.get(ProductConfig.CHIP_TYPE);
        if(mChipType == null){
            Slog.w(TAG, "Fail in getting the value of property " + ProductConfig.CHIP_TYPE);
            mChipType = ProductConfig.CHIP_TYPE_DEFAULT;
        }
        if(mChipType.equals(ProductConfig.CHIP_TYPE_A10S))
		    mDispHotPolicy = new EliteHotPlug();
		else
		    mDispHotPolicy = new HomletHotPlug();
        /* add by Gary. end   -----------------------------------}} */
		
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_HDMISTATUS_CHANGED);
		filter.addAction(Intent.ACTION_TVDACSTATUS_CHANGED);
        /* add by Gary. start {{----------------------------------- */
        /* 2011-12-19 */
        /* handle the plug in and out of the display cables */
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_BOOT_COMPLETED);
        /* add by Gary. end   -----------------------------------}} */
        context.registerReceiver(this, filter);
        
        /* add by Gary. start {{----------------------------------- */
        /* 2011-12-19 */
        /* handle the plug in and out of the display cables */
        mAudioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
        if(mAudioManager == null){
            Slog.w(TAG, "mAudioManager is null");
        }
        mToast = new HotPlugToast(mContext);
        /* init display cables' connection status */
        if(!mChipType.equals(ProductConfig.CHIP_TYPE_A10S)){
            if(mDisplayManager.getHdmiHotPlugStatus() != 0)
                mHdmiIsConnected = true;
            int tvStatus = mDisplayManager.getTvHotPlugStatus();
            Slog.d(TAG, "tv connect status = " + tvStatus);
            if(tvStatus == DisplayManager.DISPLAY_TVDAC_CVBS)
                mCvbsIsConnected = true;
            else if(tvStatus == DisplayManager.DISPLAY_TVDAC_YPBPR)
                mYpbprIsConnected = true;
            else if(tvStatus == DisplayManager.DISPLAY_TVDAC_ALL){
                mCvbsIsConnected = true;
                mYpbprIsConnected = true;
            }
        }
        /* add by Gary. end   -----------------------------------}} */
    }

    public void onReceive(Context context, Intent intent) 
    {
        final String action = intent.getAction();
        if (action.equals(Intent.ACTION_HDMISTATUS_CHANGED)) 
        {
            mDispHotPolicy.onHdmiPlugChanged(intent);
        }
        else if(action.equals(Intent.ACTION_TVDACSTATUS_CHANGED))
        {
			mDispHotPolicy.onTvDacPlugChanged(intent);
        }
        /* add by Gary. start {{----------------------------------- */
        /* 2011-12-19 */
        /* handle the plug in and out of the display cables */
        else if (action.equals(Intent.ACTION_BOOT_COMPLETED))
        {
            mBootCompleted = true;
        }
        else if (action.equals(Intent.ACTION_SCREEN_ON)) {
            mHandler.postDelayed(mSetStandbyState, 3000);
//                mHandler.postDelayed(mSetMutePin, 2000);
        }else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
            mHandler.removeCallbacks(mSetStandbyState);
//                mHandler.removeCallbacks(mSetMutePin);
            mIsStandby = true;
//                if(ProductConfig.PRODUCT_NAME_APOLLO_MELE.equals(ProductConfig.getProductSpec().getProductName())){
//                    Gpio.writeGpio('b', 3, 0);
//                }
        }
        /* add by Gary. end   -----------------------------------}} */
    }
        
    private class StatusBarPadHotPlug implements DisplayHotPlugPolicy
    {
        //0:screen0 fix;
        //1:dual same lcd
        //3:screen0 fix,screen1 auto,(same ui,one video on screen1);
        //4:screen0 fix,screen1 auto,(same ui,two video);
        //5:screen0 auto(ui use fe)
        //6:screen0 auto(ui use be)
        //7:screen0 auto(fb var)
        public int mDisplay_mode = 3;

    	StatusBarPadHotPlug()
    	{
    	}
    	
    	private void onHdmiPlugIn(Intent intent) 
		{
			int     maxscreen;
			int     maxhdmimode;
			int     customhdmimode;
			int     hdmi_mode;
			int     AUTO_HDMI_MODE = 0xff;
			int     MIN_HDMI_MODE = 0;
			
	        if (SHOW_HDMIPLUG_IN_CALL) 
			{
	          	Slog.d(TAG,"onHdmiPlugIn Starting!\n");

	            customhdmimode = Settings.System.getInt(mContext.getContentResolver(), Settings.System.HDMI_OUTPUT_MODE, AUTO_HDMI_MODE);
				maxhdmimode	= mDisplayManager.getMaxHdmiMode();
				
	            if (customhdmimode < AUTO_HDMI_MODE) {
					hdmi_mode = customhdmimode;
	            } else {
	                hdmi_mode = maxhdmimode;
	            }

				if(mDisplay_mode == 3)
				{
					mDisplayManager.setDisplayParameter(1,DisplayManager.DISPLAY_OUTPUT_TYPE_HDMI,hdmi_mode);
					mDisplayManager.setDisplayMode(DisplayManager.DISPLAY_MODE_DUALSAME);
                    SystemProperties.set("audio.routing", Integer.toString(AudioSystem.DEVICE_OUT_AUX_DIGITAL));
                    AudioSystem.setParameters("routing="+AudioSystem.DEVICE_OUT_AUX_DIGITAL);
				}
				else if(mDisplay_mode == 4)
				{
					mDisplayManager.setDisplayParameter(1,DisplayManager.DISPLAY_OUTPUT_TYPE_HDMI,hdmi_mode);
					mDisplayManager.setDisplayMode(DisplayManager.DISPLAY_MODE_DUALSAME_TWO_VIDEO);
                    SystemProperties.set("audio.routing", Integer.toString(AudioSystem.DEVICE_OUT_AUX_DIGITAL));
                    AudioSystem.setParameters("routing="+AudioSystem.DEVICE_OUT_AUX_DIGITAL);
				}
				else if(mDisplay_mode == 5)
				{
					mDisplayManager.setDisplayParameter(0,DisplayManager.DISPLAY_OUTPUT_TYPE_HDMI,hdmi_mode);
					mDisplayManager.setDisplayMode(DisplayManager.getDisplayModeForIC());
                    SystemProperties.set("audio.routing", Integer.toString(AudioSystem.DEVICE_OUT_AUX_DIGITAL));
                    AudioSystem.setParameters("routing="+AudioSystem.DEVICE_OUT_AUX_DIGITAL);
				}
				else if(mDisplay_mode == 6)
				{
					mDisplayManager.setDisplayParameter(0,DisplayManager.DISPLAY_OUTPUT_TYPE_HDMI,hdmi_mode);
					mDisplayManager.setDisplayMode(DisplayManager.DISPLAY_MODE_SINGLE_VAR_BE);
                    SystemProperties.set("audio.routing", Integer.toString(AudioSystem.DEVICE_OUT_AUX_DIGITAL));
                    AudioSystem.setParameters("routing="+AudioSystem.DEVICE_OUT_AUX_DIGITAL);
				}
				else if(mDisplay_mode == 7)
				{
					mDisplayManager.setDisplayParameter(0,DisplayManager.DISPLAY_OUTPUT_TYPE_HDMI,hdmi_mode);
					mDisplayManager.setDisplayMode(DisplayManager.DISPLAY_MODE_SINGLE_FB_VAR);
                    SystemProperties.set("audio.routing", Integer.toString(AudioSystem.DEVICE_OUT_AUX_DIGITAL));
                    AudioSystem.setParameters("routing="+AudioSystem.DEVICE_OUT_AUX_DIGITAL);
				}
	        }
	    }
	
		private void onTvDacYPbPrPlugIn(Intent intent)
		{
            if(mDisplay_mode == 3)
            {
                mDisplayManager.setDisplayParameter(1,DisplayManager.DISPLAY_OUTPUT_TYPE_TV,DisplayManager.DISPLAY_TVFORMAT_720P_50HZ);
                mDisplayManager.setDisplayMode(DisplayManager.DISPLAY_MODE_DUALSAME);
            }
            else if(mDisplay_mode == 4)
            {
                mDisplayManager.setDisplayParameter(1,DisplayManager.DISPLAY_OUTPUT_TYPE_TV,DisplayManager.DISPLAY_TVFORMAT_720P_50HZ);
                mDisplayManager.setDisplayMode(DisplayManager.DISPLAY_MODE_DUALSAME_TWO_VIDEO);
            }
            else if(mDisplay_mode == 5)
            {
                mDisplayManager.setDisplayParameter(0,DisplayManager.DISPLAY_OUTPUT_TYPE_TV,DisplayManager.DISPLAY_TVFORMAT_720P_50HZ);
                mDisplayManager.setDisplayMode(DisplayManager.getDisplayModeForIC());
            }
            else if(mDisplay_mode == 6)
            {
                mDisplayManager.setDisplayParameter(0,DisplayManager.DISPLAY_OUTPUT_TYPE_TV,DisplayManager.DISPLAY_TVFORMAT_720P_50HZ);
                mDisplayManager.setDisplayMode(DisplayManager.DISPLAY_MODE_SINGLE_VAR_BE);
            }
		}
		
		private void onTvDacCVBSPlugIn(Intent intent)
		{
            if(mDisplay_mode == 3)
            {
                mDisplayManager.setDisplayParameter(1,DisplayManager.DISPLAY_OUTPUT_TYPE_TV,DisplayManager.DISPLAY_TVFORMAT_NTSC);
                mDisplayManager.setDisplayMode(DisplayManager.DISPLAY_MODE_DUALSAME);
            }
            else if(mDisplay_mode == 4)
            {
                mDisplayManager.setDisplayParameter(1,DisplayManager.DISPLAY_OUTPUT_TYPE_TV,DisplayManager.DISPLAY_TVFORMAT_NTSC);
                mDisplayManager.setDisplayMode(DisplayManager.DISPLAY_MODE_DUALSAME_TWO_VIDEO);
            }
            else if(mDisplay_mode == 5)
            {
                mDisplayManager.setDisplayParameter(0,DisplayManager.DISPLAY_OUTPUT_TYPE_TV,DisplayManager.DISPLAY_TVFORMAT_NTSC);
                mDisplayManager.setDisplayMode(DisplayManager.getDisplayModeForIC());
            }
            else if(mDisplay_mode == 6)
            {
                mDisplayManager.setDisplayParameter(0,DisplayManager.DISPLAY_OUTPUT_TYPE_TV,DisplayManager.DISPLAY_TVFORMAT_NTSC);
                mDisplayManager.setDisplayMode(DisplayManager.DISPLAY_MODE_SINGLE_VAR_BE);
            }
		}
	
		private void onHdmiPlugOut(Intent intent)
		{
			int     maxscreen;
			
			Slog.d(TAG,"onHdmiPlugOut Starting!\n");

			if((mDisplay_mode == 3) || (mDisplay_mode == 4))
			{
			    mDisplayManager.setDisplayParameter(1,DisplayManager.DISPLAY_OUTPUT_TYPE_NONE,0);
    	        mDisplayManager.setDisplayMode(DisplayManager.DISPLAY_MODE_SINGLE);
                SystemProperties.set("audio.routing", Integer.toString(AudioSystem.DEVICE_OUT_SPEAKER));
                AudioSystem.setParameters("routing="+AudioSystem.DEVICE_OUT_SPEAKER);
			}
			else if(mDisplay_mode == 5)
			{
    	      	mDisplayManager.setDisplayParameter(0,DisplayManager.DISPLAY_OUTPUT_TYPE_LCD,0);
    	        mDisplayManager.setDisplayMode(DisplayManager.getDisplayModeForIC());
                SystemProperties.set("audio.routing", Integer.toString(AudioSystem.DEVICE_OUT_SPEAKER));
                AudioSystem.setParameters("routing="+AudioSystem.DEVICE_OUT_SPEAKER);
	        }
	        else if(mDisplay_mode == 6)
			{
    	      	mDisplayManager.setDisplayParameter(0,DisplayManager.DISPLAY_OUTPUT_TYPE_LCD,0);
    	        mDisplayManager.setDisplayMode(DisplayManager.DISPLAY_MODE_SINGLE_VAR_BE);
                SystemProperties.set("audio.routing", Integer.toString(AudioSystem.DEVICE_OUT_SPEAKER));
                AudioSystem.setParameters("routing="+AudioSystem.DEVICE_OUT_SPEAKER);
	        }
	        else if(mDisplay_mode == 7)
			{
    	      	mDisplayManager.setDisplayParameter(0,DisplayManager.DISPLAY_OUTPUT_TYPE_LCD,0);
    	        mDisplayManager.setDisplayMode(DisplayManager.DISPLAY_MODE_SINGLE_FB_VAR);
                SystemProperties.set("audio.routing", Integer.toString(AudioSystem.DEVICE_OUT_SPEAKER));
                AudioSystem.setParameters("routing="+AudioSystem.DEVICE_OUT_SPEAKER);
	        }
		}
	
		private void onTvDacPlugOut(Intent intent)
		{
			Slog.d(TAG,"onTvDacPlugOut Starting!\n");
			
			if((mDisplay_mode == 3) || (mDisplay_mode == 4))
			{
			    mDisplayManager.setDisplayParameter(1,DisplayManager.DISPLAY_OUTPUT_TYPE_NONE,0);
    	        mDisplayManager.setDisplayMode(DisplayManager.DISPLAY_MODE_SINGLE);
			}
			else if(mDisplay_mode == 5)
			{
    	      	mDisplayManager.setDisplayParameter(0,DisplayManager.DISPLAY_OUTPUT_TYPE_LCD,0);
    	        mDisplayManager.setDisplayMode(DisplayManager.getDisplayModeForIC());
	        }
	        else if(mDisplay_mode == 6)
			{
    	      	mDisplayManager.setDisplayParameter(0,DisplayManager.DISPLAY_OUTPUT_TYPE_LCD,0);
    	        mDisplayManager.setDisplayMode(DisplayManager.DISPLAY_MODE_SINGLE_VAR_BE);
	        }
		}
		
		public void onHdmiPlugChanged(Intent intent)
		{
			int   hdmiplug;
			
			hdmiplug = intent.getIntExtra(DisplayManager.EXTRA_HDMISTATUS, 0);
			if(hdmiplug == 1)
			{
				onHdmiPlugIn(intent);
			}
			else
			{
				onHdmiPlugOut(intent);
			}
		}
		
		public void onTvDacPlugChanged(Intent intent)
		{
			int   tvdacplug;
			
			tvdacplug = intent.getIntExtra(DisplayManager.EXTRA_TVSTATUS, 0);
			if(tvdacplug == 1)
			{
				onTvDacYPbPrPlugIn(intent);
			}
			else if(tvdacplug == 2)
			{
				onTvDacCVBSPlugIn(intent);
			}
			else
			{
				onTvDacPlugOut(intent);
			}
		}
    }
    
    /* add by Gary. start {{----------------------------------- */
    /* 2011-12-19 */
    /* handle the plug in and out of the display cables */
    private class HomletHotPlug implements DisplayHotPlugPolicy
    {
    	HomletHotPlug()
    	{
    		
    	}

		public void onHdmiPlugChanged(Intent intent)
		{
		    Slog.d(TAG, "onHdmiPlugChanged and mIsStandby is = " + mIsStandby);
			if(!mIsStandby){
    			int   hdmiplug;
    			
    			hdmiplug = intent.getIntExtra(DisplayManager.EXTRA_HDMISTATUS, 0);
    			if(hdmiplug == 1){
    			    if(mBootCompleted)
    				    onHdmiPlugIn(intent);
    				mHdmiIsConnected = true;
    			}else {
    			    if(mBootCompleted)
        				onHdmiPlugOut(intent);
    				mHdmiIsConnected = false;
    			}
    	    }
		}
		
		public void onTvDacPlugChanged(Intent intent)
		{
		    Slog.d(TAG, "onTvDacPlugChanged and mIsStandby is = " + mIsStandby);
			if(!mIsStandby){
    			int   tvStatus;
    			
    			tvStatus = intent.getIntExtra(DisplayManager.EXTRA_TVSTATUS, 0);
    			Slog.d(TAG, "tvStatus = " + tvStatus);
    			boolean cvbsIsConnected = false;
    			boolean ypbprIsConnected = false;
                if(tvStatus == DisplayManager.DISPLAY_TVDAC_CVBS)
                    cvbsIsConnected = true;
                else if(tvStatus == DisplayManager.DISPLAY_TVDAC_YPBPR)
                    ypbprIsConnected = true;
                else if(tvStatus == DisplayManager.DISPLAY_TVDAC_ALL){
                    cvbsIsConnected = true;
                    ypbprIsConnected = true;
                }
                
                if(mBootCompleted){
                    if(!mCvbsIsConnected && cvbsIsConnected)
        				onTvDacCVBSPlugIn(intent);
                    if(mCvbsIsConnected && !cvbsIsConnected)
        				onTvDacCVBSPlugOut(intent);
                    if(!mYpbprIsConnected && ypbprIsConnected && mDeviceHasYpbpr)
        				onTvDacYPBPRPlugIn(intent);
                    if(mYpbprIsConnected && !ypbprIsConnected && mDeviceHasYpbpr)
        				onTvDacYPBPRPlugOut(intent);
    		    }
    		    
    		    mCvbsIsConnected = cvbsIsConnected;
    		    mYpbprIsConnected  = ypbprIsConnected;
    		}
		}

    	private void onHdmiPlugIn(Intent intent) 
		{
	        if (SHOW_HDMIPLUG_IN_CALL) {
	          	Slog.d(TAG,"onHdmiPlugIn Starting!\n");
	          	int curType = mDisplayManager.getDisplayOutputType(0);
	          	if(curType != DisplayManager.DISPLAY_OUTPUT_TYPE_HDMI){
	          	    mToast.setText(com.android.internal.R.string.hdmi_plug_in);
	          	    mToast.setOnHideListener(mHDMIPlugInToastOnHide);
                    mToast.setDuration(Toast.LENGTH_LONG);
	          	    mToast.show();
                }
	        }
	    }
	
		private void onTvDacVGAPlugIn(Intent intent)
		{
	        if (SHOW_TVPLUG_IN_CALL) {
	          	Slog.d(TAG,"onTvDacVGAPlugIn Starting!\n");
                int hdmiConnect = mDisplayManager.getHdmiHotPlugStatus();
                int tvdacConnect = mDisplayManager.getTvHotPlugStatus();
                if(!mHdmiIsConnected && !mCvbsIsConnected){
              	    switchDisplayOutput(DispList.VGA_DEFAULT_FORMAT.mOutputType, 
              	                        DispList.VGA_DEFAULT_FORMAT.mFormat);
                }else {
	          	    mToast.setText(com.android.internal.R.string.vga_plug_in);
	          	    mToast.setOnHideListener(null);
                    mToast.setDuration(Toast.LENGTH_SHORT);
	          	    mToast.show();
                }
            }
		}
		
		private void onTvDacYPBPRPlugIn(Intent intent)
		{
	        if (SHOW_TVPLUG_IN_CALL) {
	          	Slog.d(TAG,"onTvDacYPBPRPlugIn Starting!\n");
                int hdmiConnect = mDisplayManager.getHdmiHotPlugStatus();
                int tvdacConnect = mDisplayManager.getTvHotPlugStatus();
                if(!mHdmiIsConnected && !mCvbsIsConnected){
              	    switchDisplayOutput(DispList.YPBPR_DEFAULT_FORMAT.mOutputType, 
              	                        DispList.YPBPR_DEFAULT_FORMAT.mFormat);
                }else {
	          	    mToast.setText(com.android.internal.R.string.ypbpr_plug_in);
	          	    mToast.setOnHideListener(null);
                    mToast.setDuration(Toast.LENGTH_SHORT);
	          	    mToast.show();
                }
            }
		}
		
		private void onTvDacCVBSPlugIn(Intent intent)
		{
	        if (SHOW_TVPLUG_IN_CALL) {
	          	Slog.d(TAG,"onCvbsPlugIn Starting!\n");
                int hdmiConnect = mDisplayManager.getHdmiHotPlugStatus();
                int tvdacConnect = mDisplayManager.getTvHotPlugStatus();
                Slog.d(TAG, "hdmiConnect = " + hdmiConnect + "tvdacConnect = " + tvdacConnect);
                if(!mHdmiIsConnected && !mYpbprIsConnected){
              	    switchDisplayOutput(DispList.CVBS_DEFAULT_FORMAT.mOutputType,
              	                        DispList.CVBS_DEFAULT_FORMAT.mFormat);
                }else {
	          	    mToast.setText(com.android.internal.R.string.cvbs_plug_in);
	          	    mToast.setOnHideListener(null);
                    mToast.setDuration(Toast.LENGTH_SHORT);
	          	    mToast.show();
                }
            }
		}
	
		private void onHdmiPlugOut(Intent intent)
		{
	        if (SHOW_HDMIPLUG_IN_CALL) {
	          	Slog.d(TAG,"onHdmiPlugOut Starting!\n");
	          	int curType = mDisplayManager.getDisplayOutputType(0);
	          	if(curType == DisplayManager.DISPLAY_OUTPUT_TYPE_HDMI){
                    DispList.DispFormat format = null;
                    if(mCvbsIsConnected){
                        format = DispList.CVBS_DEFAULT_FORMAT;
                    }else if(mYpbprIsConnected){
                        format = DispList.VGA_DEFAULT_FORMAT;
                    }

                    if(format != null){
                  	    switchDisplayOutput(format.mOutputType, format.mFormat);
                    }
                }else{
	          	    mToast.setText(com.android.internal.R.string.hdmi_plug_out);
	          	    mToast.setOnHideListener(null);
                    mToast.setDuration(Toast.LENGTH_SHORT);
	          	    mToast.show();
	          	}
	        }
		}
	
		private void onTvDacCVBSPlugOut(Intent intent)
		{
	        if (SHOW_TVPLUG_IN_CALL) 
			{
	          	Slog.d(TAG,"onCvbsPlugOut Starting!\n");
          	    mToast.setText(com.android.internal.R.string.cvbs_plug_out);
          	    mToast.setOnHideListener(null);
                mToast.setDuration(Toast.LENGTH_SHORT);
          	    mToast.show();
	        }
		}
		
		private void onTvDacVGAPlugOut(Intent intent)
		{
	        if (SHOW_TVPLUG_IN_CALL) 
			{
	          	Slog.d(TAG,"onVgaPlugOut Starting!\n");
	          	int curType = mDisplayManager.getDisplayOutputType(0);
	          	if(curType != DisplayManager.DISPLAY_OUTPUT_TYPE_VGA){
	          	    mToast.setText(com.android.internal.R.string.vga_plug_out);
	          	    mToast.setOnHideListener(null);
                    mToast.setDuration(Toast.LENGTH_SHORT);
	          	    mToast.show();
	          	}
	        }
		}
		
		private void onTvDacYPBPRPlugOut(Intent intent)
		{
	        if (SHOW_TVPLUG_IN_CALL) 
			{
	          	Slog.d(TAG,"onypbprPlugOut Starting!\n");
          	    mToast.setText(com.android.internal.R.string.ypbpr_plug_out);
          	    mToast.setOnHideListener(null);
                mToast.setDuration(Toast.LENGTH_SHORT);
          	    mToast.show();
	        }
		}
		
        private void switchDisplayOutput(int type, int format){
      	    /* switch display output format */
      	    Slog.d(TAG, "switch diplay to " + DispList.ItemCode2Name(new DispList.DispFormat(type, format)));
      	    //mDisplayManager.setDisplayOutputType(0, type, format);
      	    
            mDisplayManager.setDisplayParameter(0,type,format);
            mDisplayManager.setDisplayMode(DisplayManager.getDisplayModeForIC());
            
      	    /* record the display output format */
            Settings.System.putString(mContext.getContentResolver(), Settings.System.DISPLY_OUTPUT_FORMAT, 
                                      DispList.ItemCode2Name(new DispList.DispFormat(type, format)));

			/* modified by chenjd,chenjd@allwinnertect.com,20120710 start{{--------------------
			 * change display mode */
			ArrayList<String> audioOutputChannels = mAudioManager.getActiveAudioDevices(AudioManager.AUDIO_OUTPUT_ACTIVE);
			if(type == DisplayManager.DISPLAY_OUTPUT_TYPE_HDMI){
				if(!audioOutputChannels.contains(AudioManager.AUDIO_NAME_SPDIF) &&
					!audioOutputChannels.contains(AudioManager.AUDIO_NAME_HDMI)){
					audioOutputChannels.clear();
					audioOutputChannels.add(AudioManager.AUDIO_NAME_HDMI);
					mAudioManager.setAudioDeviceActive(audioOutputChannels,AudioManager.AUDIO_OUTPUT_ACTIVE);
				}
			}else{
				if(!audioOutputChannels.contains(AudioManager.AUDIO_NAME_SPDIF) &&
					!audioOutputChannels.contains(AudioManager.AUDIO_NAME_CODEC)){
					audioOutputChannels.clear();
					audioOutputChannels.add(AudioManager.AUDIO_NAME_CODEC);
					mAudioManager.setAudioDeviceActive(audioOutputChannels,AudioManager.AUDIO_OUTPUT_ACTIVE);
				}
			}
			String st = null;
			for(int i = 0; i < audioOutputChannels.size(); i++){
				if(st == null){
					st = audioOutputChannels.get(i);
				}else{
					st = "," + st;
				}
			}
			Settings.System.putString(mContext.getContentResolver(), Settings.System.AUDIO_OUTPUT_CHANNEL,st);
			/* modified by chendj.end --------------}} */

			/* switch audio output type */
			/*
            int audioOutputChannel = mAudioManager.getAudioOutMode();
            int audioOutputChannelNew = audioOutputChannel;
            if(type == DisplayManager.DISPLAY_OUTPUT_TYPE_HDMI){
                if(audioOutputChannelNew != AudioManager.AUDIO_OUT_CHANNEL_SPDIF)
                    audioOutputChannelNew = AudioManager.AUDIO_OUT_CHANNEL_HDMI;
            }else{
                if(audioOutputChannelNew != AudioManager.AUDIO_OUT_CHANNEL_SPDIF)
                    audioOutputChannelNew = AudioManager.AUDIO_OUT_CHANNEL_CODEC;
            }
            if(audioOutputChannelNew != audioOutputChannel){
                mAudioManager.switchAudioOutMode(audioOutputChannelNew);
                /* record the audio output channel */
            /*    Settings.System.putString(mContext.getContentResolver(), Settings.System.AUDIO_OUTPUT_CHANNEL,
                                          AudioManager.auidoOutputChannelCode2Name(audioOutputChannelNew));

			}*/
        }

        private OnHideListener mHDMIPlugInToastOnHide = new OnHideListener(){
            public void onHide(){
                switchDisplayOutput(DispList.HDMI_DEFAULT_FORMAT.mOutputType, DispList.HDMI_DEFAULT_FORMAT.mFormat);
            }
        };
    }

    interface OnHideListener {
        public void onHide();
    }

    private class HotPlugToast extends Toast{
        
        public HotPlugToast(Context context){
            super(context);
            LayoutInflater inflate = (LayoutInflater)
                    context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View v = inflate.inflate(com.android.internal.R.layout.transient_notification, null);
            
            super.setGravity(Gravity.CENTER, 0, 0 );
            super.setView(v);
            super.setDuration(Toast.LENGTH_LONG);
        }
        
        public void setOnHideListener(OnHideListener listener){
            mOnHideListener = listener;
        }
        
        public void onHide(){
            Slog.d(TAG, "on Hide in HotPlugToast.\n");
            if(mOnHideListener != null)
                mOnHideListener.onHide();
        }
        
        private OnHideListener mOnHideListener = null;
    }
    /* add by Gary. end   -----------------------------------}} */
    
    /* add by Gary. start {{----------------------------------- */
    /* 2012-05-30 */
    /* modify for a10s */	
    private class EliteHotPlug implements DisplayHotPlugPolicy
    {
    	EliteHotPlug()
    	{
    		
    	}

		public void onHdmiPlugChanged(Intent intent)
		{
		    Slog.d(TAG, "EliteHotPlug: onHdmiPlugChanged()");
		}
		
		public void onTvDacPlugChanged(Intent intent)
		{
		    Slog.d(TAG, "EliteHotPlug: onTvDacPlugChanged()");
		}
    }
    /* add by Gary. end   -----------------------------------}} */
}
