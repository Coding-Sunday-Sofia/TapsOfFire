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
import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;

class TextButton extends Button {

	public TextButton(Context context) {
		super(context);
		setup();
	}

	public TextButton(Context context,AttributeSet attrs) {
		super(context,attrs);
		setup();
	}

	public TextButton(Context context,AttributeSet attrs,int defStyle) {
		super(context,attrs,defStyle);
		setup();
	}
	
	///////////////////////////////////////////// implementation
	
	private void setup() {
		setTypeface(Config.getDefaultTypeface());
		//setBackgroundDrawable(null);
		//setPadding(0,0,0,0);
	}
}
