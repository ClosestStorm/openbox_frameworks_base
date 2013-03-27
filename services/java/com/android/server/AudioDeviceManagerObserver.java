/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.server;

import android.app.ActivityManagerNative;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.UEventObserver;
import android.util.Slog;
import android.media.AudioManager;
import android.util.Log;
import android.os.Bundle;


import java.io.FileReader;
import java.io.FileNotFoundException;
import com.allwinner.SecureFile;

/**
 * AudioDeviceManagerObserver monitors for audio devices on the main board.
 */
public class AudioDeviceManagerObserver extends UEventObserver {
    private static final String TAG = AudioDeviceManagerObserver.class.getSimpleName();
    private static final boolean LOG = true;

	private static final int MAX_AUDIO_DEVICES		= 16;
	public static final String AUDIO_TYPE = "audioType";
	public static final int AUDIO_INPUT_TYPE	= 0;
	public static final int AUDIO_OUTPUT_TYPE 	= 1;

	public static final String AUDIO_STATE = "audioState";
	public static final int PLUG_IN				= 1;
	public static final int PLUG_OUT				= 0;

	public static final String AUDIO_NAME = "audioName";

	private static final String uAudioDevicesPath	= "/sys/class/sound/";
	private static final String uEventSubsystem		= "SUBSYSTEM=sound";
	private static final String uEventDevPath		= "DEVPATH";
	private static final String uEventDevName		= "DEVNAME";
	private static final String uEventAction		= "ACTION";
	private static final String uPcmDev				= "snd/pcm";
	private static final String uAudioInType = "c";
	private static final String uAudioOutType = "p";

	private int mUsbAudioCnt = 0;

    private final Context mContext;
    private final WakeLock mWakeLock;  // held while there is a pending route change

	/* use id,devName,androidName to identify a audio dev, the id is the key(one id to one dev)*/
	/*a device = {id,devNameForIn,devNameForOut,androidName}*/
	private String mAudioNameMap[][] = {
		{"audiocodec",	"unknown",	"unknown",	"AUDIO_CODEC"},
		{"sndhdmi",		"unknown",	"unknown",	"AUDIO_HDMI"},
		{"sndspdif",	"unknown",	"unknown",	"AUDIO_SPDIF"},
		{"unknown",		"unknown",	"unknown",	"unknown"},
		{"unknown",		"unknown",	"unknown",	"unknown"},
		{"unknown",		"unknown",	"unknown",	"unknown"},
		{"unknown",		"unknown",	"unknown",	"unknown"},
		{"unknown",		"unknown",	"unknown",	"unknown"},
		{"unknown",		"unknown",	"unknown",	"unknown"},
	};

    public AudioDeviceManagerObserver(Context context) {
        mContext = context;
        PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AudioDeviceManagerObserver");
        mWakeLock.setReferenceCounted(false);

		Log.d(TAG, "AudioDeviceManagerObserver construct");

        context.registerReceiver(new BootCompletedReceiver(),
            new IntentFilter(Intent.ACTION_BOOT_COMPLETED), null, null);
    }

    private final class BootCompletedReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent) {
			// At any given time accessories could be inserted
			// one on the board, one on the dock and one on HDMI:
			// observe three UEVENTs
			init();  // set initial status

			startObserving(uEventSubsystem);
		}
  	}

    private synchronized final void init() {
        char[] buffer = new char[1024];

		String name_linux = String.format("unknown");
		String name_android = String.format("unknown");

        Log.v(TAG, "AudioDeviceManagerObserver init()");

        for (int card = 0; card < MAX_AUDIO_DEVICES; card++) {
            try {
				String newCard = String.format("/sys/class/sound/card%d/",card);
				String newCardId = newCard + "id";
				Log.d(TAG, "AudioDeviceManagerObserver: newCardId: " + newCardId);

				FileReader file = new FileReader(newCardId);
				int len = file.read(buffer, 0, 1024);
                file.close();

				name_linux = new String(buffer, 0, len).trim();


				if (len > 0)
				{
					SecureFile cardDir = new SecureFile(newCard);
					String[] list = cardDir.list();
					for(String name:list){
						if(name.startsWith("pcm")){
							int length = name.length();
							String ext = name.substring(length - 1, length);
							Log.d(TAG,"AudioDeviceManagerObserver: devName: " + name);
							if(ext.equalsIgnoreCase(uAudioInType)){
								name_android = findNameMap(name_linux, "snd/" + name, true);
							}
							else if(ext.equalsIgnoreCase(uAudioOutType)){
								name_android = findNameMap(name_linux, "snd/" + name, false);
							}
						}
					}
					Log.d(TAG, "AudioDeviceManagerObserver: name_linux: " + name_linux + ", name_android: " + name_android);
				}

            } catch (FileNotFoundException e) {
                Log.v(TAG, "This kernel does not have sound card" + card);
				break;
            } catch (Exception e) {
                Log.e(TAG, "" , e);
            }
        }
    }

	/** when the id is not null,it will find mapped androidName by id,
	 *if the devName is not null,its device name will be set to devName,else not change the device name.*/
	private String findNameMap(String id,String devName,boolean audioIn) {

		Log.d(TAG, "~~~~~~~~ AudioDeviceManagerObserver: findNameMap: id: " + id);
		String out = null;
		if(id != null){
			if(devName == null){
				devName = "unknown";
			}
			for (int index = 0; index < MAX_AUDIO_DEVICES; index++) {
				if (mAudioNameMap[index][0].equals("unknown")) {
					mAudioNameMap[index][0] = id;
					if(audioIn){
						mAudioNameMap[index][1] = devName;
					}else{
						mAudioNameMap[index][2] = devName;
					}
					mAudioNameMap[index][3] = String.format("AUDIO_USB%d", mUsbAudioCnt++);

					out = mAudioNameMap[index][3];

					// Log.d(TAG, "xxxx index: " + index + " in: " + mAudioNameMap[index][0] + " out:" + mAudioNameMap[index][1]);
					break;
				}

				if (mAudioNameMap[index][0].equals(id)) {
					if(audioIn){
						mAudioNameMap[index][1] = devName;
					}else{
						mAudioNameMap[index][2] = devName;
					}

					out = mAudioNameMap[index][3];
					// Log.d(TAG, "qqqq index: " + index + " in: " + mAudioNameMap[index][0] + " out:" + mAudioNameMap[index][1]);
					break;
				}
			}
		}

		return out;
	}

	/** find mapped androidName by devName,if the reset is true,its devName will reset to 'unknown' when find it */
	private String findNameMap(String devName,boolean audioIn,boolean reset){
		Log.d(TAG,"~~~~~~~~ AudioDeviceManagerObserver: findNameMap: devName: " + devName);
		String out = null;
		if(devName != null && !devName.equals("unknown")){
			for(int index = 0; index < MAX_AUDIO_DEVICES; index++) {
				if(audioIn && mAudioNameMap[index][1].equals(devName)){
					out = mAudioNameMap[index][3];
					if(reset){
						mAudioNameMap[index][1] = "unknown";
					}
					break;
				}else if(!audioIn && mAudioNameMap[index][2].equals(devName)){
					out = mAudioNameMap[index][3];
					if(reset){
						mAudioNameMap[index][2] = "unknown";
					}
					break;
				}
			}
		}
		return out;
	}

    @Override
    public void onUEvent(UEventObserver.UEvent event) {
        Log.d(TAG, "Audio device change: " + event.toString());

        try {
			String devPath = event.get(uEventDevPath);
			String devName = event.get(uEventDevName);
			String action = event.get(uEventAction);
			String devCardPath;
			String sndName;
			String sndNameMap = "unknown";
			String audioType = null;

			Log.d(TAG, "action: " + action + " devName: " + devName + " devPath: " + devPath);

			if ((devName != null) && (devName.substring(0, 7).equals(uPcmDev.substring(0, 7)))) {
				char[] buffer = new char[64];
				int index = devPath.lastIndexOf("/");
				devCardPath = devPath.substring(0, index);
				devCardPath = devCardPath + "/id";
				int length = devName.length();
				audioType = devName.substring(length - 1, length);
				if(action.equals("add")){
					int cnt = 10;
					while((cnt-- != 0)) {
						try {
							FileReader file = new FileReader("sys" + devCardPath);
		               	 	int len = file.read(buffer, 0, 64);
		                	file.close();

							if (len > 0)
							{
								sndName = new String(buffer, 0, len).trim();
								if(audioType.equalsIgnoreCase(uAudioInType)){
									sndNameMap = findNameMap(sndName, devName, true);
									updateState(sndNameMap, AUDIO_INPUT_TYPE, PLUG_IN);
								}else if(audioType.equalsIgnoreCase(uAudioOutType)){
									sndNameMap = findNameMap(sndName,devName, false);
									updateState(sndNameMap, AUDIO_OUTPUT_TYPE, PLUG_IN);
								}


								break;
							}

						} catch (FileNotFoundException e) {
							if (cnt == 0) {
								Slog.e(TAG, "can not read card id");
								return ;
							}
							try {
								Slog.w(TAG, "read card id, wait for a moment ......");
								Thread.sleep(10);
							} catch (Exception e0) {
								Slog.e(TAG, "" , e0);
							}
						} catch (Exception e) {
							Slog.e(TAG, "" , e);
						}
					}
				}
				else if(action.equals("remove")){
					try{
						if(audioType.equalsIgnoreCase(uAudioInType)){
							sndNameMap = findNameMap(devName, true, true);
							updateState(sndNameMap, AUDIO_INPUT_TYPE, PLUG_OUT);
						}else if(audioType.equalsIgnoreCase(uAudioOutType)){
							sndNameMap = findNameMap(devName, false, true);
							updateState(sndNameMap, AUDIO_OUTPUT_TYPE, PLUG_OUT);
						}
					}catch(Exception e){
						Slog.e(TAG,"",e);
					}
				}

			}
        } catch (NumberFormatException e) {
            Slog.e(TAG, "Could not parse switch state from event " + event);
        }
    }

    private synchronized final void updateState(String name, int type,int state) {
		Log.d(TAG, "name: " + name + ", state: " + state + ", type: " + type);
		Intent intent;

		intent = new Intent(Intent.ACTION_AUDIO_PLUG_IN_OUT);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
		Bundle bundle = new Bundle();
		bundle.putInt(AUDIO_STATE,state);
		bundle.putString(AUDIO_NAME,name);
		bundle.putInt(AUDIO_TYPE,type);
		intent.putExtras(bundle);
        ActivityManagerNative.broadcastStickyIntent(intent, null);
    }

}
