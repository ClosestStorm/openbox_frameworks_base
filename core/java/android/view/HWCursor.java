/*
 * Copyright (C) 2007 The Android Open Source Project
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

package android.view;

import java.io.IOException;
import android.os.ServiceManager;
import android.graphics.*;

/**
 * Class that provides access to some of the power management functions.
 *
 * {@hide}
 */
public class HWCursor
{
    private int 		mHWCursorSaveCount;
	private Canvas		mHWCursorCanvas;
	private int         mDisplayNo;
    private native int  nativeHwCursorInit(int displayno);
	private native int  nativeHwCursorShow(int displayno);
	private native int  nativeHwCursorHide(int displayno);
	private native int  nativeHwCursorSetPosition(int displayno,int posx,int posy);
	private native int  nativeHwCursorGetPositionX(int displayno);
	private native int  nativeHwCursorGetPositionY(int displayno);
	private native Canvas nativeHwCursorLockCanvas(int displayno);
	private native int    nativeHwCursorUnLockCanvasAndPost(Canvas canvas);/*
     * We use a class initializer to allow the native code to cache some
     * field offsets.
     */
    native private static void nativeClassInit();
    static { nativeClassInit(); }
	
    // can't instantiate this class
    public HWCursor(int displayno)
    {
        mDisplayNo      = displayno;
        
        mHWCursorCanvas = new Canvas();
        
        nativeHwCursorInit(displayno);
    }

	public int Show()
	{
		return nativeHwCursorShow(mDisplayNo);
	}

	public int Hide()
	{
		return nativeHwCursorHide(mDisplayNo);
	}

	public int SetPosition(int posX,int posY)
	{
		return nativeHwCursorSetPosition(mDisplayNo,posX,posY);
	}

	public int GetPositionX()
	{
		return nativeHwCursorGetPositionX(mDisplayNo);
	}

	public int GetPositionY()
	{
		return nativeHwCursorGetPositionY(mDisplayNo);
	}

	public Canvas LockCanvas()
	{
		return nativeHwCursorLockCanvas(mDisplayNo);
	}

	public int UnlockCanvasAndPost(Canvas canvas)
	{
		return nativeHwCursorUnLockCanvasAndPost(canvas);
	}
}