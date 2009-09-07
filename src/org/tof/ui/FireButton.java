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

import java.util.Random;
import org.tof.R;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

class FireButton extends TextButton implements View.OnClickListener {

	public FireButton(Context context) {
		super(context);
		setup();
	}

	public FireButton(Context context,AttributeSet attrs) {
		super(context,attrs);
		setup();
	}

	public FireButton(Context context,AttributeSet attrs,int defStyle) {
		super(context,attrs,defStyle);
		setup();
	}
	
	public void setOnClickListener(View.OnClickListener listener) {
		m_clickListener=listener;
	}
	
	public void setAnimation(Animation animation) {
		endClick();
		super.setAnimation(animation);
	}
	
	public void startAnimation(Animation animation) {
		endClick();
		super.startAnimation(animation);
	}
	
	public void clearAnimation() {
		endClick();
		super.clearAnimation();
	}
	
	///////////////////////////////////////////// implementation
	
//	protected void onDraw(Canvas canvas) {
//		m_fireShaderMatrix.preTranslate(0.3f,-0.5f);
//		m_fireShader.setLocalMatrix(m_fireShaderMatrix);
//		super.onDraw(canvas);
//		postInvalidate();
//	}

	public void onClick(View view) {
		UISoundEffects.playInSound();
		Animation currentAnimation=getAnimation();
		if (!m_clicking &&
			currentAnimation!=null && !currentAnimation.hasEnded())
		{
			click();
		} else {
			startClick();
		}
	}
	
	public void onAnimationEnd() {
		endClick();
	}
	
	private void startClick() {
		super.startAnimation(m_animation);
		m_clicking=true;
	}
	
	private void endClick() {
		if (m_clicking) {
			click();
			super.setAnimation(null);
		}
	}
	
	private void click() {
		m_clicking=false;
		if (m_clickListener!=null) {
			m_clickListener.onClick(this);
		}
	}
	
	private void setup() {
		super.setOnClickListener(this);
		m_animation=AnimationUtils.loadAnimation(getContext(),R.anim.squash);
		
//		Drawable drawable=getResources().getDrawable(R.drawable.fire8);
//		Bitmap fireBitmap=((BitmapDrawable)drawable).getBitmap();
//		fireBitmap=Bitmap.createBitmap(fireBitmap);
//		m_fireShader=new BitmapShader(
//			fireBitmap,
//			Shader.TileMode.MIRROR,Shader.TileMode.MIRROR);
//		m_fireShaderMatrix=new Matrix();
//		m_fireShaderMatrix.postTranslate(new Random().nextInt(100),0);
//		getPaint().setShader(m_fireShader);
	}
	
	/////////////////////////////////// data
	
	private View.OnClickListener m_clickListener;
	private Animation m_animation;
	private boolean m_clicking;
	
	private BitmapShader m_fireShader;
	private Matrix m_fireShaderMatrix;
	
}
