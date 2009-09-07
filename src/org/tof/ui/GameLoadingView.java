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
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.RelativeLayout;

public class GameLoadingView extends RelativeLayout {

	public GameLoadingView(Context context) {
		super(context);
	}
	
	public GameLoadingView(Context context,AttributeSet attributes) {
		super(context,attributes);
	}
	
	public GameLoadingView(Context context,AttributeSet attributes,int style) {
		super(context,attributes,style);
	}

	public void hide() {
		if (getVisibility()!=View.VISIBLE) {
			return;
		}
		UIHelpers.startViewAnimation(this,R.id.icon,R.anim.loading_out);
		UIHelpers.startViewAnimation(this,R.id.text,R.anim.loading_out);
		if (m_animation==null) {
			m_animation=new AlphaAnimation(1,0);
			m_animation.setDuration(getHideDuratioin(getContext()));
		}
        startAnimation(m_animation);
        setVisibility(View.GONE);
	}
	
	public void show() {
		setVisibility(View.VISIBLE);
	}
	
	/////////////////////////////////// statics

	public static int getHideDuratioin(Context context) {
		return UIHelpers.getInteger(
			context,
			R.integer.anim_loading_out_duration);
	}

	///////////////////////////////////////////// implementation

    /////////////////////////////////// data
	
	private Animation m_animation;
}
