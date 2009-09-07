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
package org.tof.util;

import skiba.util.Simply;
import android.os.SystemClock;
import android.util.Log;

public class AvgElapsedMeter {
	
	public AvgElapsedMeter(String what,int measureCount) {
		m_what=what;
		m_measureCount=measureCount;
	}
	
	public void begin() {
		m_startTime=SystemClock.uptimeMillis();
	}
	
	public void end() {
		m_totalTime+=Simply.elapsedUptimeMillis(m_startTime);
		m_startTime=SystemClock.uptimeMillis();
		m_measure++;
		if (m_measure==m_measureCount) {
			Log.e("TOF",
				m_what+": "+m_totalTime+" total, "+
				(float)m_totalTime/m_measureCount+" average");
			m_measure=0;
			m_totalTime=0;
		}
	}
	
	///////////////////////////////////////////// implementation
	
	private String m_what;
	private int m_measureCount;
	private int m_measure;
	private long m_startTime;
	private long m_totalTime;
}
