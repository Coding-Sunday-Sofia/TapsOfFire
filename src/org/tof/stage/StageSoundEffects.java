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
package org.tof.stage;

import java.util.Random;
import org.tof.Config;
import org.tof.R;
import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

class StageSoundEffects {
	
	public void load(Context context) {
		if (m_pool!=null) {
			return;
		}
		m_pool=new SoundPool(1,AudioManager.STREAM_MUSIC,0);
		m_screwUpSounds=new int[6];
		m_screwUpSounds[0]=m_pool.load(context,R.raw.screwup1,1);
		m_screwUpSounds[1]=m_pool.load(context,R.raw.screwup2,1);
		m_screwUpSounds[2]=m_pool.load(context,R.raw.screwup3,1);
		m_screwUpSounds[3]=m_pool.load(context,R.raw.screwup4,1);
		m_screwUpSounds[4]=m_pool.load(context,R.raw.screwup5,1);
		m_screwUpSounds[5]=m_pool.load(context,R.raw.screwup6,1);		
	}
	
	public void destroy() {
		if (m_pool==null) {
			return;
		}
		m_pool.release();
		m_pool=null;
		m_screwUpSounds=null;
	}
	
	public void playScrewUpSound() {
		if (m_screwUpSounds==null) {
			return;
		}
		int soundIndex=m_random.nextInt(m_screwUpSounds.length);
		if (soundIndex>=m_screwUpSounds.length) {
			return;
		}
		float volume=Config.getScaledVolume(Config.VOLUME_SCREWUP);
		if (volume!=0) {
			m_pool.play(m_screwUpSounds[soundIndex],volume,volume,1,0,1);
		}
	}
	
	///////////////////////////////////////////// implementation
	
	private SoundPool m_pool;
	private int[] m_screwUpSounds;
	private Random m_random=new Random();
}
