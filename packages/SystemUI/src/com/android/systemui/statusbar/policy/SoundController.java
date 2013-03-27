package com.android.systemui.statusbar.policy;

import java.util.ArrayList;

import com.android.systemui.R;
import com.google.android.mms.pdu.NotificationInd;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.SystemService;
import android.util.Log;
import com.android.server.AudioDeviceManagerObserver;
import android.widget.Toast;
import android.os.Message;
import android.os.Handler;

/**
 * 管理音频设备的热插拔,当音频输出设备插入时以通知方式提示用户是否需要设置从该通道输出(可选择多通道输出),当输入
 * 设备插入时,直接自动切换至该通道输入(只能单通道输入).
 * @author Chenjd
 * @since 2012-07-16
 * @email chenjd@allwinnertech.com
 */
public class SoundController extends BroadcastReceiver{

	public Context mContext;
	private static final String TAG = SoundController.class.getSimpleName();
	private static final int notificationIdForIn = 20120716;
	private static final int notificationIdForOut = 20120904;
	private AudioManager mAudioManager;
	private ArrayList<String> mAudioOutputChannels = null;
	private ArrayList<String> mAudioInputChannels = null;
	private ArrayList<String> mAudioOutputActivedChannels = null;
	private boolean justOne = true;
	public SoundController(Context context){
		mContext = context;
		mAudioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_AUDIO_PLUG_IN_OUT);

		/* for display mode change */
		filter.addAction(Intent.ACTION_HDMISTATUS_CHANGED);
		filter.addAction(Intent.ACTION_TVDACSTATUS_CHANGED);

		mContext.registerReceiver(this, filter);

		mAudioOutputChannels = mAudioManager.getAudioDevices(AudioManager.AUDIO_OUTPUT_TYPE);
		mAudioInputChannels = mAudioManager.getAudioDevices(AudioManager.AUDIO_INPUT_TYPE);

		//init audio input channels
		ArrayList<String> ls = mAudioManager.getAudioDevices(AudioManager.AUDIO_INPUT_TYPE);
		int i = 0;
		if(ls != null){
			for(i = 0; i < ls.size(); i++){
				String st = ls.get(i);
				if(st.contains("USB")){
					ArrayList<String> list = new ArrayList<String>();
					list.add(st);
					mAudioManager.setAudioDeviceActive(list, AudioManager.AUDIO_INPUT_ACTIVE);
					break;
				}
			}
		}
		if(i == ls.size()){
			//use codec for default
			ArrayList<String> lst = new ArrayList<String>();
			lst.add(AudioManager.AUDIO_NAME_CODEC);
			mAudioManager.setAudioDeviceActive(lst, AudioManager.AUDIO_INPUT_ACTIVE);
		}





	}

	private Handler mHandler = null;

	@Override
	public void onReceive(Context context, Intent intent) {
		final String action = intent.getAction();
		if (action.equals(Intent.ACTION_HDMISTATUS_CHANGED) || action.equals(Intent.ACTION_TVDACSTATUS_CHANGED))
        {
            try{
				Thread.currentThread().sleep(500); //wait display and audio mode change
			}catch(Exception e) {};
			mAudioOutputActivedChannels = mAudioManager.getActiveAudioDevices(AudioManager.AUDIO_OUTPUT_ACTIVE);
			log("save default channels is: " + mAudioOutputActivedChannels);
			//init audio output channels
			if(justOne){
				ArrayList<String> list = mAudioManager.getAudioDevices(AudioManager.AUDIO_OUTPUT_TYPE);
				if(list != null){
					for(String st:list){
						if(st.contains("USB")){
							log("the USB audio-out is cool boot");
							list.clear();
							list.add(st);
							mAudioManager.setAudioDeviceActive(list,AudioManager.AUDIO_OUTPUT_ACTIVE);
							justOne = false;
							break;
						}
					}
				}
			}
			return;
        }

		if(mHandler == null){
			mHandler = new Handler(){
				@Override
				public void handleMessage(Message msg){
					String message = (String)msg.obj;
					toastMessage(message);
				}
			};
		}

		Log.d(TAG,"On Audio device plug in/out receive");
		Bundle bundle = intent.getExtras();
		final int state = bundle.getInt(AudioDeviceManagerObserver.AUDIO_STATE);
		final String name = bundle.getString(AudioDeviceManagerObserver.AUDIO_NAME);
		final int type = bundle.getInt(AudioDeviceManagerObserver.AUDIO_TYPE);

		Thread thread = new Thread(new Runnable() {

			@Override
			public void run() {
				mAudioOutputChannels = mAudioManager.getAudioDevices(AudioManager.AUDIO_OUTPUT_TYPE);
				mAudioInputChannels = mAudioManager.getAudioDevices(AudioManager.AUDIO_INPUT_TYPE);

				String mng = "name: " + name + "  state: " + state + "  type: " + type;
				String title = null;
				String message = null;
				log(mng);
				switch(state){
				case AudioDeviceManagerObserver.PLUG_IN:
					switch(type){
					case AudioDeviceManagerObserver.AUDIO_INPUT_TYPE:
						//auto change to this audio-in channel
						log("audio input plug in");
						ArrayList<String> audio_in = new ArrayList<String>();
						audio_in.add(name);
						mAudioManager.setAudioDeviceActive(audio_in, AudioManager.AUDIO_INPUT_ACTIVE);

						title = mContext.getResources().getString(R.string.audio_in_plug_in_title);
						message = mContext.getResources().getString(R.string.audio_plug_in_message);
						toastPlugInNotification(title, message, false);
						break;
					case AudioDeviceManagerObserver.AUDIO_OUTPUT_TYPE:
						log("audio output plug in");
						ArrayList<String> audio_out = new ArrayList<String>();
						audio_out.add(name);
						mAudioManager.setAudioDeviceActive(audio_out,AudioManager.AUDIO_OUTPUT_ACTIVE);

						title = mContext.getResources().getString(R.string.audio_out_plug_in_title);
						message = mContext.getResources().getString(R.string.audio_plug_in_message);
						toastPlugInNotification(title, message, true);
						break;
					}
					break;
				case AudioDeviceManagerObserver.PLUG_OUT:
					switch(type){
					case AudioDeviceManagerObserver.AUDIO_INPUT_TYPE:
						log("audio input plug out");
						title = mContext.getResources().getString(R.string.audio_in_plug_out_title);
						message = mContext.getResources().getString(R.string.audio_plug_out_message);
						ArrayList<String> actived = mAudioManager.getActiveAudioDevices(AudioManager.AUDIO_INPUT_ACTIVE);
						if(actived == null || actived.size() == 0 || actived.contains(name)){
							ArrayList<String> list = new ArrayList<String>();
							list.add(AudioManager.AUDIO_NAME_CODEC);
							mAudioManager.setAudioDeviceActive(list, AudioManager.AUDIO_INPUT_ACTIVE);
						}
						toastPlugOutNotification(title, message, false);
						break;
					case AudioDeviceManagerObserver.AUDIO_OUTPUT_TYPE:
						log("audio output plug out");

						log("audio pre output type:" + mAudioOutputActivedChannels);
						mAudioManager.setAudioDeviceActive(mAudioOutputActivedChannels,AudioManager.AUDIO_OUTPUT_ACTIVE);

						title = mContext.getResources().getString(R.string.audio_out_plug_out_title);
						message = mContext.getResources().getString(R.string.audio_plug_out_message);
						toastPlugOutNotification(title, message, true);
						break;
					}
					break;
				}
			}
		});
		thread.start();
	}

	private void log(String mng) {
		Log.d(TAG,mng);
	}

	private void toastPlugInNotification(String title,String mng,boolean isAudioOut) {
		Notification notification = new Notification(com.android.internal.R.drawable.stat_sys_data_usb,
				title,System.currentTimeMillis());
		String contentTitle = title;
		String contextText = mng;
		ComponentName cm = new ComponentName("com.android.settings", "com.android.settings.Settings$SoundSettingsActivity");
		Intent notificationIntent = new Intent();
		notificationIntent.setComponent(cm);
		notificationIntent.setAction("com.android.settings.SOUND_SETTINGS");

		PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0, notificationIntent, 0);
		notification.setLatestEventInfo(mContext, contentTitle, contextText, contentIntent);

		notification.defaults &= ~Notification.DEFAULT_SOUND;
		notification.flags = Notification.FLAG_AUTO_CANCEL;

		NotificationManager notificationManager = (NotificationManager) mContext
			.getSystemService(Context.NOTIFICATION_SERVICE);

		notificationManager.notify(getId(isAudioOut), notification);

		handleToastMessage(mng);
	}

	private int getId(boolean isAudioOut){
		if(isAudioOut){
			return notificationIdForOut;
		}else{
			return notificationIdForIn;
		}
	}

	private void toastPlugOutNotification(String title,String mng, boolean isAudioOut){
		/*
		Notification notification = new Notification(com.android.internal.R.drawable.stat_sys_data_usb,
				title,System.currentTimeMillis());
		String contentTitle = title;
		String contextText = mng;
		notification.setLatestEventInfo(mContext, contentTitle, contextText, null);
		notification.defaults &= ~Notification.DEFAULT_SOUND;
		NotificationManager notificationManager = (NotificationManager) mContext
			.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(getId(isAudioOut), notification);*/

		NotificationManager notificationManager = (NotificationManager) mContext
			.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(getId(isAudioOut));
		handleToastMessage(mng);
	}

	private void toastPlugInNotification(String title, boolean isAudioOut){
		Notification notification = new Notification(com.android.internal.R.drawable.stat_sys_data_usb,
				title,System.currentTimeMillis());
		String contentTitle = title;
		String contextText = title;
		notification.setLatestEventInfo(mContext, contentTitle, contextText, null);

		notification.defaults &= ~Notification.DEFAULT_SOUND;
		notification.flags = Notification.FLAG_AUTO_CANCEL;

		NotificationManager notificationManager = (NotificationManager) mContext
			.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(getId(isAudioOut), notification);

		handleToastMessage(title);
	}

	private void handleToastMessage(String message){
		if(mHandler == null) return;
		Message mng = mHandler.obtainMessage();
		mng.obj = message;
		mHandler.sendMessage(mng);
	}

	private void toastMessage(String message){
		Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
	}
}