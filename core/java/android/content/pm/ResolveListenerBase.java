/*
 * Copyright (C) 2006 The Android Open Source Project
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

package android.content.pm;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.StrictMode;
import android.util.EventLog;
import android.util.Log;
import android.util.LogPrinter;
import android.util.Slog;
import java.util.ArrayList;
import android.content.Context;
import android.content.pm.IResolveListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.app.AppGlobals;


/**
 * This manages the execution of the main thread in an
 * application process, scheduling and executing activities,
 * broadcasts, and other operations on it as the activity
 * manager requests.
 *
 * {@hide}
 */
public abstract class ResolveListenerBase 
{
    /** @hide */
    public static final String TAG = "ResolveListenerBase";
    private IResolveListener.Stub mResolveListener = new ResolveListener();
	private IntentFilter	mFilter;
    
    protected abstract boolean onInterceptResolveIntent(Intent intent,String resolvedType);
    
    public ResolveListenerBase(IntentFilter filter)
    {
    	mFilter			= filter;
		try 
        {
            AppGlobals.getPackageManager().registerResolveListener(mResolveListener);
        } 
        catch (RemoteException ex) 
        {
        }	
    }

	public IntentFilter getIntentFilter()
	{
		return mFilter;
	}
	
    private class ResolveListener extends IResolveListener.Stub
    {
        public final IntentFilter getResolveIntentFilter()
        {
            return  getIntentFilter();
        }
        
        public final boolean onInterceptResolve(Intent intent,String resolvedType)
        {
            return onInterceptResolveIntent(intent,resolvedType);
        }
    }

	public void exitResolveListener()
	{
		try 
        {
            AppGlobals.getPackageManager().unregisterResolveListener(mResolveListener);
        } 
        catch (RemoteException ex) 
        {

        }
	}
}

