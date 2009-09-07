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
import org.tof.ui.UISoundEffects;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.ViewFlipper;

public class MainMenuActivity extends ActivityBase implements View.OnClickListener {
	
	protected void onCreate(Bundle savedState) {
		super.onCreate(savedState);
		setContentView(R.layout.main_menu);
		
		m_pageFlipper=(ViewFlipper)findViewById(R.id.flipper);
		
		findViewById(R.id.play).setOnClickListener(this);
		findViewById(R.id.settings).setOnClickListener(this);
		findViewById(R.id.help).setOnClickListener(this);
		findViewById(R.id.adc).setOnClickListener(this);
		
		if (savedState!=null) {
			int page=savedState.getInt(KEY_ACTIVITY_STATE,PAGE_MAIN);
			showPage(page,false);
		}
		
		m_player=MediaPlayer.create(this,R.raw.menu);
		if (m_player!=null) {
			m_player.setLooping(true);
		}
	}
	
	protected void onSaveInstanceState(Bundle state) {
		super.onSaveInstanceState(state);
		state.putInt(KEY_ACTIVITY_STATE,getCurrentPage());		
	}
	
	protected void onPause() {
		super.onPause();
		if (m_player!=null) {
			m_player.pause();
		}
	}
	
	protected void onResume() {
		super.onResume();
		if (m_player!=null) {
			float volume=Config.getScaledVolume(Config.VOLUME_MENU);
			m_player.setVolume(volume,volume);
			m_player.start();
		}
		if (getCurrentPage()==PAGE_WELCOME &&
			!checkUpdateFirstTime(false))
		{
			showPage(PAGE_MAIN,false);
		}
		if (getCurrentPage()==PAGE_MAIN) {
			startAnimation();
		}
	}
	
	protected void onMasterVolumeAdjusted() {
		if (m_player!=null) {
			float volume=Config.getScaledVolume(Config.VOLUME_MENU);
			m_player.setVolume(volume,volume);
		}
	}
	
	///////////////////////////////////////////// logic
	
	public boolean onBackKeyDown() {
		UISoundEffects.playOutSound();
		if (getCurrentPage()!=PAGE_MAIN) {
			showPage(PAGE_MAIN,true);
			return true;
		}
		return false;
	}
	
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.play_first_time:
				checkUpdateFirstTime(true);
			case R.id.play:
				if (checkUpdateFirstTime(false)) {
					showPage(PAGE_WELCOME,true);
				} else {
					startActivity(new Intent(this,SongBrowserActivity.class));
				}
				break;
			case R.id.settings:
				startActivity(new Intent(this,SettingsActivity.class));
				break;
			case R.id.help:
				startActivity(new Intent(this,HelpActivity.class));
				break;
			case R.id.adc:
				showPage(PAGE_ADC,true);
				break;
		}
	}
	
	private void startAnimation() {
		UIHelpers.startViewAnimation(
			this,
			R.id.logo,R.anim.logo_in);
		
		int offset=UIHelpers.getInteger(
			this,
			R.integer.anim_logo_in_duration);
		int delay=UIHelpers.getInteger(
			this,
			R.integer.anim_button_delay);
		UIHelpers.startViewAnimation(
			this,
			R.id.play,R.anim.button_in,
			offset);
		UIHelpers.startViewAnimation(
			this,
			R.id.settings,R.anim.button_in,
			offset+delay);
		UIHelpers.startViewAnimation(
			this,
			R.id.help,R.anim.button_in,
			offset+delay*2);
	}
	
	///////////////////////////////////////////// pages
	
	private int getCurrentPage() {
		return m_pageFlipper.getDisplayedChild();
	}
	
	private void showPage(int page,boolean animate) {
		if (getCurrentPage()==page) {
			return;
		}
		View pageView=m_pageFlipper.getChildAt(page);
		if (pageView.getVisibility()!=View.INVISIBLE) {
			pageView.setVisibility(View.VISIBLE);
			if (page==PAGE_WELCOME) {
				findViewById(R.id.play_first_time).setOnClickListener(this);
			}
		}
		UIHelpers.flipToChild(m_pageFlipper,page,animate);
	}

	private boolean checkUpdateFirstTime(boolean update) {
		SharedPreferences preferences=getPreferences(0);
		if (update) {
			SharedPreferences.Editor editor=preferences.edit();
			editor.putBoolean("firstTime",false);
			editor.commit();
			return false;
		} else {
			return preferences.getBoolean("firstTime",true);
		}
	}
	
	private void resetFirstTime() {
		SharedPreferences preferences=getPreferences(0);
		SharedPreferences.Editor editor=preferences.edit();
		editor.clear();
		editor.commit();
	}
	
	///////////////////////////////////////////// data
	
	private MediaPlayer m_player;
	private ViewFlipper m_pageFlipper;
	
	private static final int
		PAGE_MAIN		=0,
		PAGE_WELCOME	=1,
		PAGE_ADC		=2;
}
