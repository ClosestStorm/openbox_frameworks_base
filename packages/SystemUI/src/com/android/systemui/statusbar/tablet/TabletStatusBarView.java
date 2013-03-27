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

package com.android.systemui.statusbar.tablet;

import com.android.systemui.R;
import com.android.systemui.statusbar.policy.KeyButtonView;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Slog;
import android.view.View;
import android.view.MotionEvent;
import android.widget.FrameLayout;

public class TabletStatusBarView extends FrameLayout {
    private Handler mHandler;
	private boolean displayLock;

    private final int MAX_PANELS = 5;
    private final View[] mIgnoreChildren = new View[MAX_PANELS];
    private final View[] mPanels = new View[MAX_PANELS];
    private final int[] mPos = new int[2];

    public TabletStatusBarView(Context context) {
        super(context);
    }

    public TabletStatusBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            if (TabletStatusBar.DEBUG) {
                Slog.d(TabletStatusBar.TAG, "TabletStatusBarView intercepting touch event: " + ev);
            }
            mHandler.removeMessages(TabletStatusBar.MSG_CLOSE_NOTIFICATION_PANEL);
            mHandler.sendEmptyMessage(TabletStatusBar.MSG_CLOSE_NOTIFICATION_PANEL);
            mHandler.removeMessages(TabletStatusBar.MSG_CLOSE_RECENTS_PANEL);
            mHandler.sendEmptyMessage(TabletStatusBar.MSG_CLOSE_RECENTS_PANEL);
            mHandler.removeMessages(TabletStatusBar.MSG_CLOSE_INPUT_METHODS_PANEL);
            mHandler.sendEmptyMessage(TabletStatusBar.MSG_CLOSE_INPUT_METHODS_PANEL);
            mHandler.removeMessages(TabletStatusBar.MSG_STOP_TICKER);
            mHandler.sendEmptyMessage(TabletStatusBar.MSG_STOP_TICKER);

            for (int i=0; i < mPanels.length; i++) {
                if (mPanels[i] != null && mPanels[i].getVisibility() == View.VISIBLE) {
                    if (eventInside(mIgnoreChildren[i], ev)) {
                        if (TabletStatusBar.DEBUG) {
                            Slog.d(TabletStatusBar.TAG,
                                    "TabletStatusBarView eating event for view: "
                                    + mIgnoreChildren[i]);
                        }
                        return true;
                    }
                }
            }
        }
        if (TabletStatusBar.DEBUG) {
            Slog.d(TabletStatusBar.TAG, "TabletStatusBarView not intercepting event");
        }
        return super.onInterceptTouchEvent(ev);
    }
	
	@Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh){
		KeyButtonView volume_up=(KeyButtonView)findViewById(R.id.volume_up);
		KeyButtonView volume_down=(KeyButtonView)findViewById(R.id.volume_down);
		KeyButtonView menu =(KeyButtonView)findViewById(R.id.menu);
		View navigationArea=(View)findViewById(R.id.navigationArea);
		View mNotificationArea = (View)findViewById(R.id.notificationArea);
		if(displayLock){
            if(w<navigationArea.getWidth()+mNotificationArea.getWidth()){
			volume_up.setVisibility(View.GONE);
			volume_down.setVisibility(View.GONE);
			menu.setVisibility(View.GONE);
		    }else{
		     volume_up.setVisibility(View.VISIBLE);
			 volume_down.setVisibility(View.VISIBLE);
			 menu.setVisibility(View.VISIBLE);
		    }
		}
		
	}
    private boolean eventInside(View v, MotionEvent ev) {
        // assume that x and y are window coords because we are.
        final int x = (int)ev.getX();
        final int y = (int)ev.getY();

        final int[] p = mPos;
        v.getLocationInWindow(p);

        final int l = p[0];
        final int t = p[1];
        final int r = p[0] + v.getWidth();
        final int b = p[1] + v.getHeight();

        return x >= l && x < r && y >= t && y < b;
    }

    public void setHandler(Handler h) {
        mHandler = h;
    }
    public void setShowVolume(boolean show,Context mContext){
        displayLock=show;
		Configuration config = mContext.getResources().getConfiguration();
		KeyButtonView volume_up=(KeyButtonView)findViewById(R.id.volume_up);
		KeyButtonView volume_down=(KeyButtonView)findViewById(R.id.volume_down);
		KeyButtonView menu =(KeyButtonView)findViewById(R.id.menu);
		if(config.screenWidthDp>480&&show){
		   volume_up.setVisibility(View.VISIBLE);
		   volume_down.setVisibility(View.VISIBLE);
		   menu.setVisibility(View.VISIBLE);
		}else{
		   volume_up.setVisibility(View.GONE);
		   volume_down.setVisibility(View.GONE);
		   menu.setVisibility(View.GONE);
		}
	}
    /**
     * Let the status bar know that if you tap on ignore while panel is showing, don't do anything.
     * 
     * Debounces taps on, say, a popup's trigger when the popup is already showing.
     */
    public void setIgnoreChildren(int index, View ignore, View panel) {
        mIgnoreChildren[index] = ignore;
        mPanels[index] = panel;
    }
}
