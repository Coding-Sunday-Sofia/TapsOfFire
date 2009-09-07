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
import org.tof.R;
import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

public class UISoundEffects {
	
	public static void load(Context context) {
		if (m_pool!=null) {
			return;
		}
		m_pool=new SoundPool(2,AudioManager.STREAM_MUSIC,0);
		m_inSound=m_pool.load(context,R.raw.in,1);
		m_outSound=m_pool.load(context,R.raw.out,1);
	}
	
	public static void destroy() {
		if (m_pool!=null) {
			m_pool.release();
			m_pool=null;
		}
	}
	
	public static void playInSound() {
		if (m_pool==null) {
			return;
		}
		float volume=Config.getScaledVolume(Config.VOLUME_MENU);
		m_pool.play(m_inSound,volume,volume,1,0,1);
	}

	public static void playOutSound() {
		if (m_pool==null) {
			return;
		}
		float volume=Config.getScaledVolume(Config.VOLUME_MENU);
		if (volume!=0) {
			m_pool.play(m_outSound,volume,volume,1,0,1);
		}
	}
	
	///////////////////////////////////////////// implementation
	
	private static SoundPool m_pool;
	private static int m_inSound;
	private static int m_outSound;
}
