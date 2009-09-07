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
import android.graphics.drawable.Drawable;

public class SpriteUtil {

	public static SpriteRegion createSprite(GL10 gl,Drawable drawable,int width,int height) {
		Bitmap bitmap=Bitmap.createBitmap(
			MathHelpers.roundUpPower2(width),
			MathHelpers.roundUpPower2(height),
			Bitmap.Config.ARGB_4444);
		{
			bitmap.eraseColor(0x00000000);
			Canvas canvas=new Canvas(bitmap);
			drawable.setBounds(0,0,width,height);
			drawable.draw(canvas);
		}
		Sprite sprite=new Sprite(gl,bitmap,true);
		return new SpriteRegion(sprite,0,0,width,height);
	}
	
}
