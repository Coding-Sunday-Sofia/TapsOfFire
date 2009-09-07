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
package org.tof.gl.sprite;

import javax.microedition.khronos.opengles.GL10;
import org.tof.util.MathHelpers;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;

public class SpriteFontBuilder {
	
	public SpriteFontBuilder() {
		reset();
	}
	
	public void reset() {
		m_typeface=Typeface.DEFAULT;
		m_size=16;
		m_color=0xFF000000;
		m_backgroundColor=0x00000000;
		m_shadowColor=0x00000000;
		resetCharacters();
	}

	public void setTypeface(Typeface typeface) {
		if (typeface!=null) {
			m_typeface=typeface;
		}
	}

	public void setSize(float size) {
		m_size=size;
	}
	
	public void setColor(int color) {
		m_color=color;
	}
	
	public void setBackgroundColor(int color) {
		m_backgroundColor=color;
	}
	
	public void setShadowColor(int color) {
		m_shadowColor=color;
	}
	
	public void resetCharacters() {
		m_characters.delete(0,m_characters.length());
	}
	
	public void addCharacters(String characters) {
		for (int i=0,e=characters.length();i!=e;++i) {
			char ch=characters.charAt(i);
			if (getIndex(ch)==-1) {
				continue;
			}
			boolean found=false;
			for (int j=0;j!=m_characters.length();++j) {
				if (m_characters.charAt(j)==ch) {
					found=true;
					break;
				}
			}
			if (!found) {
				m_characters.append(ch);
			}
		}
	}
	
	public SpriteFont buildFont(GL10 gl) {
		Paint paint=new Paint();
		paint.setAntiAlias(true);
		paint.setTextSize(m_size);
		paint.setTypeface(m_typeface);
		
		Paint.FontMetrics fontMetrics=paint.getFontMetrics();
		float fontHeight=(fontMetrics.descent-fontMetrics.ascent);
		fontHeight+=SHADOW_OFFSET;

		String characters=m_characters.toString();
		float charactersHeight=fontHeight;
		float charactersWidth=paint.measureText(characters);
		float[] characterWidths=new float[characters.length()];
		paint.getTextWidths(characters,characterWidths);
		RectF[] characterRects=new RectF[256];
		{
			float x=0;
			float y=0;
			for (int i=0,e=characters.length();i!=e;++i) {
				int charIndex=getIndex(characters.charAt(i));
				characterRects[charIndex]=new RectF(
					x,y,
					x+characterWidths[i],y+fontHeight
				);
				x+=characterWidths[i];
				x+=SHADOW_OFFSET;
			}
		}
		
		Sprite sprite;
		{
			Bitmap bitmap=Bitmap.createBitmap(
				MathHelpers.roundUpPower2(MathHelpers.round(charactersWidth)),
				MathHelpers.roundUpPower2(MathHelpers.round(charactersHeight)),
				Bitmap.Config.ARGB_4444);
			bitmap.eraseColor(m_backgroundColor);
			Canvas canvas=new Canvas(bitmap);
			if (Color.alpha(m_shadowColor)!=0) {
				paint.setColor(m_shadowColor);
				drawSpacedText(
					canvas,
					SHADOW_OFFSET,-fontMetrics.ascent+SHADOW_OFFSET,
					characters,characterWidths,
					SHADOW_OFFSET,
					paint);
			}
			paint.setColor(m_color);
			drawSpacedText(
				canvas,
				0,-fontMetrics.ascent,
				characters,characterWidths,
				SHADOW_OFFSET,
				paint);
			sprite=new Sprite(gl,bitmap,true);
		}
		
		SpriteFont font=new SpriteFont();
		font.m_sprite=sprite;
		font.m_height=fontHeight+SHADOW_OFFSET;
		font.m_leading=fontMetrics.leading;
		font.m_charRects=characterRects;
		return font;
	}

	public SpriteRegion buildText(GL10 gl,String text) {
		Paint paint=new Paint();
		paint.setAntiAlias(true);
		paint.setTextSize(m_size);
		paint.setTypeface(m_typeface);
		
		Paint.FontMetrics fontMetrics=paint.getFontMetrics();
		float fontHeight=(fontMetrics.descent-fontMetrics.ascent);
		float textWidth=paint.measureText(text);
		
		fontHeight+=SHADOW_OFFSET;
		textWidth+=SHADOW_OFFSET;
		
		Sprite sprite;
		{
			Bitmap bitmap=Bitmap.createBitmap(
				MathHelpers.roundUpPower2(MathHelpers.round(textWidth)),
				MathHelpers.roundUpPower2(MathHelpers.round(fontHeight)),
				Bitmap.Config.ARGB_4444);
			bitmap.eraseColor(m_backgroundColor);
			Canvas canvas=new Canvas(bitmap);
			if (Color.alpha(m_shadowColor)!=0) {
				paint.setColor(m_shadowColor);
				canvas.drawText(
					text,
					SHADOW_OFFSET,-fontMetrics.ascent+SHADOW_OFFSET,
					paint);
			}
			paint.setColor(m_color);
			canvas.drawText(text,0,-fontMetrics.ascent,paint);
			sprite=new Sprite(gl,bitmap,true);
		}
		
		return new SpriteRegion(sprite,0,0,textWidth,fontHeight);
	}
	
	///////////////////////////////////////////// implementation
	
	/*package*/ static int getIndex(char ch) {
		if (ch>256 || ch<0) {
			return -1;
		}
		return (ch & 0xFF);
	}
	
	private static void drawSpacedText(
			Canvas canvas,float x,float y,
			String text,float[] widths,float spacing,Paint paint)
	{
		for (int i=0;i!=text.length();++i) {
			canvas.drawText(text,i,i+1,x,y,paint);
			x+=widths[i];
			x+=spacing;
		}
	}
    
    /////////////////////////////////// data
	
	private Typeface m_typeface;
	private float m_size;
	private int m_color;
	private int m_backgroundColor;
	private int m_shadowColor;
	private StringBuilder m_characters=new StringBuilder();
	
	private static final int SHADOW_OFFSET=1;
}
