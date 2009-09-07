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

import org.tof.Config;
import skiba.util.Simply;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;

public class GameFPSTimer {
	
	public GameFPSTimer(int averageCount) {
		m_averageCount=averageCount;
	}
	
	public int getAverageFPS() {
		return m_averageFPS;
	}
	
	public int getMomentaryFPS() {
		return m_momentaryFPS;
	}
	
	public void onBeforeRender() {
		m_totalOutsideTime+=Simply.elapsedUptimeMillis(m_outsideTimeStart);

		m_renderStart=SystemClock.uptimeMillis();
		{
			int frameTime=Simply.elapsedUptimeMillis(m_frameTimeStart);
			int waitTime=(1000/Math.max(1,Config.getTargetFPS()))-frameTime;
			if (waitTime>3) {
				Simply.waitSleep(waitTime-2);
				m_totalWaitTime+=waitTime;
			}
			m_frameTimeStart=SystemClock.uptimeMillis();
		}
		
		long setPriorityTime=SystemClock.uptimeMillis();
		Simply.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
		m_totalPriorityTime+=Simply.elapsedUptimeMillis(setPriorityTime);
	}
	
	public void onAfterRender() {
		long setPriorityTime=SystemClock.uptimeMillis();
		Simply.setThreadPriority(Process.THREAD_PRIORITY_URGENT_DISPLAY);
		m_totalPriorityTime+=Simply.elapsedUptimeMillis(setPriorityTime);
		
		m_momentaryFPS=1000/Simply.elapsedUptimeMillis(m_outsideTimeStart);
	
		m_totalRenderTime+=Simply.elapsedUptimeMillis(m_renderStart);
	
		if (m_renderCounter==0) {
			m_fpsStart=SystemClock.uptimeMillis();
		}
		m_renderCounter++;
		if (m_renderCounter==m_averageCount) {
			m_averageFPS=1000*(m_averageCount-1)/
				Simply.elapsedUptimeMillis(m_fpsStart);
//			Log.e(
//				"FPSTimer",
//				"wait("+m_totalWaitTime/m_averageCount+") "+
//				"render("+m_totalRenderTime/m_averageCount+") "+
//				"outside("+m_totalOutsideTime/m_averageCount+") "+
//				"priority("+m_totalPriorityTime/m_averageCount+") "+
//				"@ "+m_averageFPS+" fps");
			m_totalWaitTime=0;
			m_totalOutsideTime=0;
			m_totalRenderTime=0;
			m_renderCounter=0;
			m_totalPriorityTime=0;
		}
		m_outsideTimeStart=SystemClock.uptimeMillis();
	}
	
	///////////////////////////////////////////// implementation

	private int m_averageCount;

	private int m_averageFPS;
	private int m_momentaryFPS;
	
	private int m_renderCounter=0;
	private long m_totalWaitTime=0;
	private long m_totalRenderTime=0;
	private long m_totalOutsideTime=0;
	private long m_totalPriorityTime=0;
	private long m_fpsStart=0;
	private long m_outsideTimeStart=0;
	private long m_frameTimeStart=0;
	private long m_renderStart=0;
}
