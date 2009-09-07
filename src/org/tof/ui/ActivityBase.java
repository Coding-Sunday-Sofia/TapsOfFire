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
import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;

public class ActivityBase extends Activity {

	protected void onCreate(Bundle savedState) {
		super.onCreate(savedState);
        CrashHandler.attachToCurrentThread(this);
        Config.load(this);
		UISoundEffects.load(this);
	}
	
	protected void onResume() {
		super.onResume();
		Config.load(this);
	}
	
	protected void onDestroy() {
		super.onDestroy();
	}
	
	protected void onSaveInstanceState(Bundle state) {
		super.onSaveInstanceState(state);
	}
	
	protected void onRestoreInstanceState(Bundle savedState) {
		super.onRestoreInstanceState(savedState);
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
	
	/////////////////////////////////// callbacks
	
	protected boolean onBackKeyDown() {
		return false;
	}
	
	protected void onMasterVolumeAdjusted() {
	}

	/////////////////////////////////// constants
	
	public static final String 
		KEY_ACTIVITY_STATE	="org.tof.Activity:state";
	
	///////////////////////////////////////////// implementation
	
	private void adjustMasterVolume(float adjust) {
		float volume=Config.getMasterVolume();
		Config.setMasterVolume(volume+adjust,this);
		onMasterVolumeAdjusted();
//		Log.e("TOF","Master volume adjusted to: "+Config.getMasterVolume());
//		for (int i=0;i!=Config.COUNTOF_VOLUMES;++i) {
//			Log.e("TOF","Config.scaledVolume "+i+": "+Config.getScaledVolume(i));
//		}
	}
}
