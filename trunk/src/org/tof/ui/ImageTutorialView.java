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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

public class ImageTutorialView
	extends ViewSwitcher
	implements ViewSwitcher.ViewFactory, View.OnClickListener
{

	public ImageTutorialView(Context context) {
		this(context,null);
	}

	public ImageTutorialView(Context context,AttributeSet attributes) {
		super(context,attributes);
		setInAnimation(getContext(),R.anim.push_left_in);
		setOutAnimation(getContext(),R.anim.push_left_out);
		setFactory(this);
		m_position=-1;
		advance();
	}

	///////////////////////////////////////////// 
	
	public View makeView() {
		LayoutInflater inflater=(LayoutInflater)getContext().
			getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view=inflater.inflate(R.layout.image_tutorial_item,null);
		view.findViewById(R.id.image).setOnClickListener(this);
        view.setLayoutParams(new ImageSwitcher.LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.FILL_PARENT));
		return view;
	}
	
	public void onClick(View view) {
		advance();
	}
	
	private void advance() {
		m_position++;
		if (m_position>=TUTORIAL_LENGTH) {
			m_position=0;
		}
		View view=getNextView();
		setup(view);
		showNext();
	}
	
	private void setup(View view) {
		ImageView image=(ImageView)view.findViewById(R.id.image);
		image.setImageResource(TUTORIAL[m_position*2]);
	
		TextView text=(TextView)view.findViewById(R.id.text);
		text.setText(TUTORIAL[m_position*2+1]);
	}
	
	/////////////////////////////////// data
	
	private int m_position;
	
	private static final int[] TUTORIAL=new int[]{
		R.drawable.tutorial_1,R.string.tutorial_1,
		R.drawable.tutorial_2,R.string.tutorial_2,
		R.drawable.tutorial_3,R.string.tutorial_3,
		R.drawable.tutorial_4,R.string.tutorial_4,
		R.drawable.tutorial_5,R.string.tutorial_5,
		R.drawable.tutorial_6,R.string.tutorial_6,
		R.drawable.tutorial_7,R.string.tutorial_7,
		R.drawable.tutorial_8,R.string.tutorial_8,
	};
	
	private static final int TUTORIAL_LENGTH=8;
}
