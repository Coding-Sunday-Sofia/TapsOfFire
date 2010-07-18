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
import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.ViewFlipper;

public class UIHelpers {
	
	public static void makeFullscreen(Activity activity) {
        activity.requestWindowFeature(Window.FEATURE_NO_TITLE);
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
	}
	
	/////////////////////////////////// resources
	
	public static int getColor(Context context,int id) {
		return context.getResources().getColor(id);
	}
	public static int getInteger(Context context,int id) {
		return context.getResources().getInteger(id);
	}
	public static String getString(Context context,int id) { 
		return context.getResources().getString(id);
	}
	public static String getString(Context context,int id,Object...arguments) { 
		return context.getResources().getString(id,arguments);
	}
	public static Typeface getTypeface(Context context,String fontPath) {
		return Typeface.createFromAsset(context.getAssets(),fontPath);
	}
	
	/////////////////////////////////// view
	
	public static void setViewVisibility(Activity activity,int viewID,int visibility) {
		View view=activity.findViewById(viewID);
		if (view!=null) {
			view.setVisibility(visibility);
		}
	}
	
	public static void setText(Activity activity,int viewID,String text) {
		TextView textView=(TextView)activity.findViewById(viewID);
		if (textView!=null) {
			textView.setText(text);
		}
	}
	public static void setText(View view,int viewID,String text) {
		TextView textView=(TextView)view.findViewById(viewID);
		if (textView!=null) {
			textView.setText(text);
		}
	}
	
	/////////////////////////////////// animation
	
	public static int startViewAnimation(View parentView,int viewID,int animationID) {
		return startViewAnimation(parentView,viewID,animationID,0);
	}
	public static int startViewAnimation(View parentView,int viewID,int animationID,int offset) {
		View view=parentView.findViewById(viewID);
		if (view==null) {
			return 0;
		}
		Animation animation=view.getAnimation();
		if (animation==null) {
			animation=AnimationUtils.loadAnimation(
				parentView.getContext(),
				animationID);
		}
		animation.setStartOffset(offset);
		view.startAnimation(animation);
		return offset+(int)animation.getDuration();
	}
	
	public static int startViewAnimation(Activity activity,int viewID,int animationID) {
		return startViewAnimation(activity,viewID,animationID,0);
	}
	public static int startViewAnimation(Activity activity,int viewID,int animationID,int offset) {
		View view=activity.findViewById(viewID);
		if (view==null) {
			return 0;
		}
		Animation animation=AnimationUtils.loadAnimation(activity,animationID);
		offset+=animation.getStartOffset();
		animation.setStartOffset(offset);
		view.startAnimation(animation);
		return offset+(int)animation.getDuration();
	}
	
	public static void animateBody(View parentView,boolean in,int offset,int...ids) { 
		int delay=getInteger(
			parentView.getContext(),
			R.integer.anim_button_delay);
		for (int id: ids) {
			startViewAnimation(
				parentView,
				id,
				in?R.anim.button_in:R.anim.button_out,
				offset);
			offset+=delay;
		}
	}
	
	public static void animateHeadAndBody(Activity activity,int layoutID) {
		animateHeadAndBody(activity,layoutID,false);
	}
		
	public static void animateHeadAndBody(Activity activity,int layoutID,boolean forceLoadAnimations) {
		ViewGroup layout=(ViewGroup)activity.findViewById(layoutID);
		int childCount=layout.getChildCount();
		int animationOffset=0;
		int child=0;
		Animation animation;
		{
			View head=layout.getChildAt(0);
			if (head.getId()==R.id.head) {
				child=1;
				animation=head.getAnimation();
				if (animation==null || forceLoadAnimations) {
					animation=AnimationUtils.loadAnimation(activity,R.anim.head_in);
				}
				animationOffset=(int)animation.getDuration();
				head.startAnimation(animation);
			}
		}
		int delay=UIHelpers.getInteger(activity,R.integer.anim_body_delay);
		int delayDecay=UIHelpers.getInteger(activity,R.integer.anim_body_delay_decay);
		for (;child!=childCount;++child) {
			View view=layout.getChildAt(child);
			animation=view.getAnimation();
			if (animation==null || forceLoadAnimations) {
				animation=AnimationUtils.loadAnimation(activity,R.anim.body_in);
			}
			animation.setStartOffset(animationOffset);
			view.startAnimation(animation);
			animationOffset+=delay;
			delay=Math.max(0,delay-delayDecay);
		}
	}
	
	public static void flipToChild(Activity activity,int flipperID,int child,boolean animate) {
		ViewFlipper flipper=(ViewFlipper)activity.findViewById(flipperID);
		Animation in=null;
		Animation out=null;
		if (!animate) {
			in=flipper.getInAnimation();
			out=flipper.getOutAnimation();
			flipper.setInAnimation(null);
			flipper.setOutAnimation(null);
		}
		flipper.getChildAt(child).setVisibility(View.VISIBLE);
		flipper.setDisplayedChild(child);
		if (!animate) {
			flipper.setInAnimation(in);
			flipper.setOutAnimation(out);
		}
	}

	public static void flipToChild(ViewFlipper flipper,int child,boolean animate) {
		Animation in=null;
		Animation out=null;
		if (!animate) {
			in=flipper.getInAnimation();
			out=flipper.getOutAnimation();
			flipper.setInAnimation(null);
			flipper.setOutAnimation(null);
		}
		flipper.setDisplayedChild(child);
		if (!animate) {
			flipper.setInAnimation(in);
			flipper.setOutAnimation(out);
		}
	}
}
