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
import android.os.Bundle;
import android.view.View;
import android.widget.ViewFlipper;

public class HelpActivity extends ActivityBase implements View.OnClickListener {
	
	protected void onCreate(Bundle savedState) {
		super.onCreate(savedState);
		setContentView(R.layout.help);
		
		m_pageFlipper=(ViewFlipper)findViewById(R.id.flipper);
		
		findViewById(R.id.how_to_play).setOnClickListener(this);
		findViewById(R.id.get_songs).setOnClickListener(this);
		findViewById(R.id.misc).setOnClickListener(this);
		findViewById(R.id.about).setOnClickListener(this);
		
		if (savedState!=null) {
			switchToPage(savedState.getInt(KEY_ACTIVITY_STATE,PAGE_MAIN),true);
		}
	}
	
	protected void onSaveInstanceState(Bundle state) {
		super.onSaveInstanceState(state);
		state.putInt(KEY_ACTIVITY_STATE,getCurrentPage());
	}
	
	protected void onResume() {
		super.onResume();
		if (getCurrentPage()==PAGE_MAIN) {
			UIHelpers.animateHeadAndBody(this,R.id.layout);
		}
	}
	
	protected boolean onBackKeyDown() {
		UISoundEffects.playOutSound();
		if (getCurrentPage()!=PAGE_MAIN) {
			switchToPage(PAGE_MAIN,true);
			return true;
		}
		return false;
	}
	
	///////////////////////////////////////////// pages
	
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.how_to_play:
				switchToPage(PAGE_HOW_TO_PLAY,true);
				break;
			case R.id.get_songs:
				switchToPage(PAGE_GET_SONGS,true);
				break;
			case R.id.misc:
				switchToPage(PAGE_MISC,true);
				break;
			case R.id.about:
				switchToPage(PAGE_ABOUT,true);
				break;
		}
	}
	
	private int getCurrentPage() {
		return m_pageFlipper.getDisplayedChild();
	}
	
	private void switchToPage(int page,boolean animate) {
		if (page==getCurrentPage()) {
			return;
		}
		View viewView=m_pageFlipper.getChildAt(page);
		if (viewView.getVisibility()!=View.VISIBLE) {
			viewView.setVisibility(View.VISIBLE);
		}
		UIHelpers.flipToChild(m_pageFlipper,page,animate);
	}
	
	///////////////////////////////////////////// data

	private ViewFlipper m_pageFlipper;
	
	private static final int
		PAGE_MAIN			=0,
		PAGE_HOW_TO_PLAY	=1,
		PAGE_GET_SONGS		=2,
		PAGE_MISC			=3,
		PAGE_ABOUT			=4;
}
