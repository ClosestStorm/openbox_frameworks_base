package com.android.server.googleremote;

import android.content.Context;
import android.os.AsyncTask;
import android.provider.Settings;

public class GoogleRemoteService {
	private boolean DEBUG = true;
	private static final String TAG	= "GoogleRemoteService";
	private static final int ANYMOTE_SERVER_PORT = 9000;
	private Context mContext;
	private KeyStoreManager mKeyStoreManager;
	private BroadcastRspThread mBroadcastRspThread;
	private AnymoteSever mAnymoteSever;
	private ParingServer mParingServer;
	
	public GoogleRemoteService(Context context)
	{
		mContext = context;
		mKeyStoreManager = new KeyStoreManager(mContext);
		mBroadcastRspThread = new BroadcastRspThread(ANYMOTE_SERVER_PORT);
		mAnymoteSever = new AnymoteSever(ANYMOTE_SERVER_PORT, mKeyStoreManager);
		mParingServer = new ParingServer(this,mContext, ANYMOTE_SERVER_PORT + 1, mKeyStoreManager);
	}
	
	
	public void startServer()
	{
		if (!mKeyStoreManager.hasServerIdentityAlias()){
			new KeystoreInitializerTask(getUniqueId()).execute(mKeyStoreManager);
		} else {
			mBroadcastRspThread.start();
			mAnymoteSever.start();
			mParingServer.start();
		}
	}
	
	public void restartAnymote()
	{
		mAnymoteSever.cancle();
		mAnymoteSever = new AnymoteSever(ANYMOTE_SERVER_PORT, mKeyStoreManager);
		mAnymoteSever.start();
	}
	
	private String getUniqueId() {
	    String id = Settings.Secure.getString(mContext.getContentResolver(),
	        Settings.Secure.ANDROID_ID);
	    // null ANDROID_ID is possible on emulator
	    return id != null ? id : "emulator";
	  }
	
	class KeystoreInitializerTask extends AsyncTask<KeyStoreManager, Void, Void>
	{
		
		private final String id;

	    public KeystoreInitializerTask(String id) {
	      this.id = id;
	    }

		@Override
		protected Void doInBackground(KeyStoreManager... keyStoreManagers) {
			keyStoreManagers[0].initializeKeyStore(id);
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			startServer();
		}
		
		
	}
	
}
