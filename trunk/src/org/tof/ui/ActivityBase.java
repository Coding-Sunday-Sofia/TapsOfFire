/*
 * Taps of Fire
 * Copyright (C) 2009 Dmitry Skiba
 * 
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
*/
package org.tof.ui;

import org.tof.Config;
import org.tof.CrashHandler;
import org.tof.R;
import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.Window;
import android.widget.ViewFlipper;

public class ActivityBase extends Activity {

	protected void onCreate(Bundle savedState) {
		super.onCreate(savedState);
        CrashHandler.attachToCurrentThread(this);
        Config.load(this);
		UISoundEffects.load(this);
	}
	
	protected void onSaveInstanceState(Bundle state) {
		super.onSaveInstanceState(state);
		savePageFlipper(state);
	}
	
	protected void onPause() {
		super.onPause();
		if (isUsingPageFlipper()) {
			doPageAction(getCurrentPage(),PAGEACTION_PAUSE);
		}
	}
	
	protected void onResume() {
		super.onResume();
		Config.load(this);
		if (isUsingPageFlipper()) {
			doPageAction(getCurrentPage(),PAGEACTION_RESUME);
		}
	}
	
	protected void onDestroy() {
		super.onDestroy();
		if (isUsingPageFlipper()) {
			doPageAction(getCurrentPage(),PAGEACTION_STOP);
		}
	}
	
	public boolean onKeyDown(int keyCode,KeyEvent event) {
		if (keyCode==KeyEvent.KEYCODE_VOLUME_DOWN) {
			adjustMasterVolume(-0.05f);
			return true;
		}
		if (keyCode==KeyEvent.KEYCODE_VOLUME_UP) {
			adjustMasterVolume(+0.05f);
			return true;
		}
		if (keyCode==KeyEvent.KEYCODE_BACK && event.getRepeatCount()==0) {
			if (onBackKeyDown()) {
				return true;
			}
		}
		return super.onKeyDown(keyCode,event);
	}
	
	///////////////////////////////////////////////////////////////// callbacks
	
	protected boolean onBackKeyDown() {
		UISoundEffects.playOutSound();
		if (!isUsingPageFlipper()) {
			return false;
		}
		return !onBackToMainPage();
	}
	
	protected void onMasterVolumeAdjusted() {
	}
	
	protected boolean onBackToMainPage() {
		if (getCurrentPage()==PAGE_MAIN) {
			return true;
		}
		flipToPage(PAGE_MAIN,true);
		return false;
	}

	protected void doPageAction(int page,int action) {
	}
	
	protected View onCreateMenuView() {
		return null;
	}
	
	protected void onMenuItemClick(int id) {
	}
	
	///////////////////////////////////////////////////////////////// pages
	
	protected void usePageFlipper(Bundle savedState) {
		m_pageFlipper=(ViewFlipper)findViewById(R.id.page_flipper);
		if (savedState!=null) {
			int page=savedState.getInt(KEY_ACTIVITY_PAGE,PAGE_MAIN);
			flipToPage(page,false);
		}
	}
	
	protected boolean isUsingPageFlipper() {
		return m_pageFlipper!=null;
	}
	
	protected int getCurrentPage() {
		return m_pageFlipper.getDisplayedChild();
	}
	
	protected void flipToPage(int page,boolean animate) {
		if (page==getCurrentPage()) {
			return;
		}
		m_pageFlipper.getChildAt(page).setVisibility(View.VISIBLE);
		doPageAction(page,PAGEACTION_INITIALIZE);
		doPageAction(getCurrentPage(),PAGEACTION_STOP);
		UIHelpers.flipToChild(m_pageFlipper,page,animate);
		doPageAction(page,PAGEACTION_START);
	}
	
	
	protected static final int 
		PAGE_MAIN				=0;
	
	protected static final int
		PAGEACTION_INITIALIZE	=0,
		PAGEACTION_START		=1,
		PAGEACTION_STOP			=2,
		PAGEACTION_PAUSE		=3,
		PAGEACTION_RESUME		=4;
	
	///////////////////////////////////////////////////////////////// menu
	
	public View onCreatePanelView(int feature) {
		if (feature==Window.FEATURE_OPTIONS_PANEL) {
			View view=onCreateMenuView();
			if (view==null) {
				return null;
			}
			view.setLayoutParams(new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.FILL_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT
			));
			View.OnClickListener clickListener=new View.OnClickListener() { 
				public void onClick(View view) {
					closeOptionsMenu();
					onMenuItemClick(view.getId());	
				}
			};
			setOnClickListener(view,clickListener);
			return view;
		}
		return null;
	}
	

	///////////////////////////////////////////////////////////////// constants
	
	protected static final String 
		KEY_ACTIVITY_			="org.tof.Activity:",
		KEY_ACTIVITY_PAGE		=KEY_ACTIVITY_+"page",
		KEY_ACTIVITY_STATE		=KEY_ACTIVITY_+"state";
	
	///////////////////////////////////////////////////////////////// implementation
	
	private void adjustMasterVolume(float adjust) {
		float volume=Config.getMasterVolume();
		Config.setMasterVolume(volume+adjust,this);
		onMasterVolumeAdjusted();
	}

	private static void setOnClickListener(View viewOrGroup,View.OnClickListener listener) {
		if (viewOrGroup instanceof ViewGroup) {
			ViewGroup group=(ViewGroup)viewOrGroup;
			for (int i=0;i!=group.getChildCount();++i) {
				group.getChildAt(i).setOnClickListener(listener);
			}
		} else {
			viewOrGroup.setOnClickListener(listener);
		}
	}

	private void savePageFlipper(Bundle state) {
		if (isUsingPageFlipper()) {
			state.putInt(KEY_ACTIVITY_PAGE,getCurrentPage());
		}
	}
	
	private ViewFlipper m_pageFlipper;
}
