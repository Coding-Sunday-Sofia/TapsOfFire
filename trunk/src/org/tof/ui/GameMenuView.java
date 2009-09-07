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

import org.tof.R;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class GameMenuView extends LinearLayout implements OnClickListener {

	public GameMenuView(Context context) {
		super(context);
	}
	
	public GameMenuView(Context context,AttributeSet attributes) {
		super(context,attributes);
	}
	
	public void show() {
		if (getVisibility()!=View.VISIBLE) {
			animate(true);
		}
	}
	
	public void hide() {
		if (getVisibility()!=View.GONE) {
			animate(false);
		}
	}
	
	/////////////////////////////////// callback
	
	public static interface Callback {
		public void onGameMenuResume();
		public void onGameMenuRestart();
	}
	
	public void setCallback(Callback callback) {
		m_callback=callback;
	}
	
	///////////////////////////////////////////// implementation

	protected void onFinishInflate() {
		super.onFinishInflate();
		findViewById(R.id.resume).setOnClickListener(this);
		findViewById(R.id.restart).setOnClickListener(this);
	}
	
	public void onClick(View view) {
		if (m_callback==null) {
			return;
		}
		switch (view.getId()) {
			case R.id.resume:
			{
				m_callback.onGameMenuResume();
				break;
			}
			case R.id.restart:
			{
				m_callback.onGameMenuRestart();
				break;
			}
			default: return;
		}
	}
	
	private void animate(boolean in) {
		Animation animation=AnimationUtils.loadAnimation(
			getContext(),
			in?R.anim.game_menu_in:R.anim.game_menu_out);
		startAnimation(animation);
		setVisibility(in?View.VISIBLE:View.GONE);
		UIHelpers.animateBody(this,in,0,R.id.resume,R.id.restart);
	}
	
	/////////////////////////////////// data
	
	private Callback m_callback;
	
}
