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
import android.os.Bundle;
import android.view.View;

public class HelpActivity extends ActivityBase implements View.OnClickListener {
	
	protected void onCreate(Bundle savedState) {
		super.onCreate(savedState);
		setContentView(R.layout.help);
		usePageFlipper(savedState);

		findViewById(R.id.how_to_play).setOnClickListener(this);
		findViewById(R.id.get_songs).setOnClickListener(this);
		findViewById(R.id.misc).setOnClickListener(this);
		findViewById(R.id.about).setOnClickListener(this);
//		
//		if (savedState!=null) {
//			flipToPage(savedState.getInt(KEY_ACTIVITY_STATE,PAGE_MAIN),true);
//		}
	}
	
	protected void onSaveInstanceState(Bundle state) {
		super.onSaveInstanceState(state);
//		state.putInt(KEY_ACTIVITY_STATE,getCurrentPage());
	}
	
	protected void onResume() {
		super.onResume();
		if (getCurrentPage()==PAGE_MAIN) {
			UIHelpers.animateHeadAndBody(this,R.id.layout);
		}
	}
	
//	protected boolean onBackKeyDown() {
//		UISoundEffects.playOutSound();
//		if (getCurrentPage()!=PAGE_MAIN) {
//			flipToPage(PAGE_MAIN,true);
//			return true;
//		}
//		return false;
//	}
	
	///////////////////////////////////////////// pages
	
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.how_to_play:
				flipToPage(PAGE_HOW_TO_PLAY,true);
				break;
			case R.id.get_songs:
				flipToPage(PAGE_GET_SONGS,true);
				break;
			case R.id.misc:
				flipToPage(PAGE_MISC,true);
				break;
			case R.id.about:
				flipToPage(PAGE_ABOUT,true);
				break;
		}
	}

	private static final int
		PAGE_HOW_TO_PLAY	=1,
		PAGE_GET_SONGS		=2,
		PAGE_MISC			=3,
		PAGE_ABOUT			=4;
}
