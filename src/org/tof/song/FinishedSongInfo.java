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
package org.tof.song;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.tof.util.DataStreamHelpers;

public class FinishedSongInfo extends SongInfo {
	
	public FinishedSongInfo(SongInfo info) {
		super(info);
	}
	
	public FinishedSongInfo(DataInput dataIn) throws IOException {
		super(dataIn);
		DataStreamHelpers.checkTag(dataIn,DATA_TAG);
		m_score=dataIn.readInt();
		m_longestStreak=dataIn.readInt();
		m_accuracy=dataIn.readFloat();
	}
	
	public void saveState(DataOutput dataOut) throws IOException {
		super.saveState(dataOut);
		dataOut.writeInt(DATA_TAG);
		dataOut.writeInt(m_score);
		dataOut.writeInt(m_longestStreak);
		dataOut.writeFloat(m_accuracy);
	}
	
	public int getScore() {
		return m_score;
	}
	public void setScore(int score) {
		m_score=score;
	}
	
	public int getLongestStreak() {
		return m_longestStreak;
	}
	public void setLongestStreak(int streak) {
		m_longestStreak=streak;
	}
	
	public float getAccuracy() {
		return m_accuracy;
	}
	public void setAccuracy(float accuracy) {
		m_accuracy=accuracy;
	}
	
	public static final String BUNDLE_KEY="org.tof.FinishedSongInfo";
	
	///////////////////////////////////////////// implementation
	
	private int m_score;
	private int m_longestStreak;
	private float m_accuracy;
	
	private static final int DATA_TAG=0x46534E46;
}
