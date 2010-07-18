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
package org.tof;

import org.tof.R;
import org.tof.ui.ActivityBase;
import org.tof.ui.UIHelpers;
import org.tof.util.MathHelpers;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;

public class SettingsActivity extends ActivityBase implements SeekBar.OnSeekBarChangeListener {
	
	protected void onCreate(Bundle savedState) {
		super.onCreate(savedState);
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.settings);
		usePageFlipper(savedState);
		
		for (int i=0;i!=Config.COUNTOF_VOLUMES;++i) {
			setupVolumeControl(i);
		}
	}
	
	protected void onResume() {
		super.onResume();
		loadValues();
		if (getCurrentPage()==PAGE_MAIN) {
			UIHelpers.animateHeadAndBody(this,R.id.layout);
		}
	}
	
	protected void onPause() {
		super.onPause();
		storeValues();
		Config.store(this);		
	}
	
//	protected boolean onBackKeyDown() {
//		UISoundEffects.playOutSound();
//		if (getCurrentPage()==PAGE_ADVANCED) {
//			flipToPage(PAGE_MAIN,true);
//			return true;
//		}
//		return false;
//	}

	public boolean onPrepareOptionsMenu(Menu menu) {
		flipToPage(PAGE_ADVANCED,true);
		return true;
	}
	
	private void loadValues() {
		loadBasicValues();
		loadAdvancedValues();
	}

	private void storeValues() {
		storeBasicValues();
		storeAdvancedValues();
	}
	
	private void resetValues() {
		Config.reset();
		loadValues();
	}
	
	/////////////////////////////////////////////////////// advanced page
	
	private void loadAdvancedValues() {
		if (!m_advancedPageInitialized) {
			return;
		}
		putFloatValue(R.id.earlyPickMargin,Config.getEarlyPickMargin());
		putFloatValue(R.id.latePickMargin,Config.getLatePickMargin());
		putFloatValue(R.id.repickMargin,Config.getRepickMargin());
		putIntegerValue(R.id.minNotesDistance,Config.getMinNotesDistance());
		
		putIntegerValue(R.id.targetFPS,Config.getTargetFPS());
		putIntegerValue(R.id.touchHandlerSleep,Config.getTouchHandlerSleep());
		putBooleanValue(R.id.showDebugInfo,Config.showDebugInfo());
	}
	
	private void storeAdvancedValues() {
		if (!m_advancedPageInitialized) {
			return;
		}
		Config.setEarlyPickMargin(getPositiveFloatValue(
			R.id.earlyPickMargin,Config.DEFAULT_EARLY_PICK_MARGIN));
		Config.setLatePickMargin(getPositiveFloatValue(
			R.id.latePickMargin,Config.DEFAULT_LATE_PICK_MARGIN));
		Config.setRepickMargin(getPositiveFloatValue(
			R.id.repickMargin,Config.DEFAULT_REPICK_MARGIN));
		Config.setMinNotesDistance(getPositiveIntegerValue(
			R.id.minNotesDistance,Config.DEFAULT_MIN_NOTES_DISTANCE));

		Config.setTargetFPS(getPositiveIntegerValue(
			R.id.targetFPS,Config.DEFAULT_TARGET_FPS));
		Config.setTouchHandlerSleep(getPositiveIntegerValue(
			R.id.touchHandlerSleep,Config.DEFAULT_TOUCH_HANDLER_SLEEP));
		
		Config.showDebugInfo(getBooleanValue(R.id.showDebugInfo));
	}
	
	protected void doPageAction(int page,int action) {
		if (page==PAGE_ADVANCED && action==PAGEACTION_INITIALIZE) {
			findViewById(R.id.reset_settings).setOnClickListener(
				new View.OnClickListener() {
					public void onClick(View view) {
						resetValues();
					}
				}
			);
			m_advancedPageInitialized=true;
			loadAdvancedValues();
		}
	}
	
	/////////////////////////////////////////////////////// main page
	
	private void loadBasicValues() {
		loadVolumeValues();
		putIntegerValue(R.id.notesDelay,Config.getNotesDelay());
		putIntegerValue(R.id.songCacheLength,Config.getSongCacheLength());
	}
	
	private void storeBasicValues() {
		storeVolumeValues();
		Config.setNotesDelay(getPositiveIntegerValue(
			R.id.notesDelay,Config.DEFAULT_NOTES_DELAY));
		Config.setSongCacheLength(getPositiveIntegerValue(
			R.id.songCacheLength,Config.DEFAULT_SONG_CACHE_LENGTH));
	}
	
	/////////////////////////////////////////////////////// volume
	
	private void loadVolumeValues() {
		for (int i=0;i!=Config.COUNTOF_VOLUMES;++i) {
			float volume=Config.getAbsoluteVolume(i);
			getVolumeControl(i).setProgress(volumeToProgress(volume));
			updateVolumeLabel(i,volume);
		}
	}
	
	private void storeVolumeValues() {
		for (int i=0;i!=Config.COUNTOF_VOLUMES;++i) {
			SeekBar control=getVolumeControl(i);
			Config.setAbsoluteVolume(i,progressToVolume(control.getProgress()));
		}
	}
	
	private void setupVolumeControl(int volumeIndex) {
		SeekBar control=getVolumeControl(volumeIndex);
		control.setOnSeekBarChangeListener(this);
		control.setMax(MathHelpers.round(MAX_VOLUME/VOLUME_STEP));
	}
	
	private void updateVolumeLabel(int volumeIndex,float volume) {
		int labelID=VOLUME_CONTROL_IDS[volumeIndex][1];
		int formatID=VOLUME_CONTROL_IDS[volumeIndex][2];
		UIHelpers.setText(this,labelID,getString(formatID,formatVolume(volume)));
	}
	
	private SeekBar getVolumeControl(int volumeIndex) {
		return (SeekBar)findViewById(VOLUME_CONTROL_IDS[volumeIndex][0]);
	}
	
	public void onProgressChanged(SeekBar seekBar,int progress,boolean fromUser) {
		for (int i=0;i!=Config.COUNTOF_VOLUMES;++i) {
			if (VOLUME_CONTROL_IDS[i][0]==seekBar.getId()) {
				updateVolumeLabel(i,progressToVolume(progress));
			}
		}
	}
	public void onStartTrackingTouch(SeekBar seekBar) {
	}
	public void onStopTrackingTouch(SeekBar seekBar) {
	}

	private static float progressToVolume(int progress) {
		return (float)progress*VOLUME_STEP/MAX_VOLUME;
	}
	private static int volumeToProgress(float volume) {
		return MathHelpers.round(volume*MAX_VOLUME/VOLUME_STEP);
	}
	
	private static String formatVolume(float volume) {
		volume*=MAX_VOLUME;
		return "";//String.format("%.1f/%d",volume,MAX_VOLUME);
	}
	
	/////////////////////////////////////////////////////// helpers
	
	private void putFloatValue(int viewID,float value) {
		TextView view=(TextView)findViewById(viewID);
		view.setText(Float.toString(value));
	}
	private float getPositiveFloatValue(int viewID,float defaultValue) {
		TextView view=(TextView)findViewById(viewID);
		try {
			float value=Float.parseFloat(view.getText().toString());
			return (value<0)?defaultValue:value;
		}
		catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	private void putIntegerValue(int viewID,int value) {
		TextView view=(TextView)findViewById(viewID);
		view.setText(Integer.toString(value));
	}
	private int getPositiveIntegerValue(int viewID,int defaultValue) {
		TextView view=(TextView)findViewById(viewID);
		try {
			int value=Integer.parseInt(view.getText().toString());
			return (value<0)?defaultValue:value;
		}
		catch (NumberFormatException e) {
			return defaultValue;
		}
	}
	
	private void putBooleanValue(int viewID,boolean value) {
		CheckBox view=(CheckBox)findViewById(viewID);
		view.setChecked(value);
	}

	private boolean getBooleanValue(int viewID) {
		CheckBox view=(CheckBox)findViewById(viewID);
		return view.isChecked();
	}
	
	/////////////////////////////////////////////////////// data
	
	private boolean m_advancedPageInitialized;
	
	private static final int PAGE_ADVANCED=1;
	
	private static final int MAX_VOLUME=10;
	private static final float VOLUME_STEP=0.5f;

	private static final int[][] VOLUME_CONTROL_IDS;
	
	static {
		VOLUME_CONTROL_IDS=new int[Config.COUNTOF_VOLUMES][];
		VOLUME_CONTROL_IDS[Config.VOLUME_MENU]=new int[]{
			R.id.menuVolume,R.id.menuVolumeLabel,R.string.menu_volume_fmt
		};
		VOLUME_CONTROL_IDS[Config.VOLUME_SONG]=new int[]{
			R.id.songVolume,R.id.songVolumeLabel,R.string.song_volume_fmt
		};
		VOLUME_CONTROL_IDS[Config.VOLUME_GUITAR]=new int[]{
			R.id.guitarVolume,R.id.guitarVolumeLabel,R.string.guitar_volume_fmt
		};
		VOLUME_CONTROL_IDS[Config.VOLUME_SCREWUP]=new int[]{
			R.id.screwupVolume,R.id.screwupVolumeLabel,R.string.screwup_volume_fmt
		};
	};
}
