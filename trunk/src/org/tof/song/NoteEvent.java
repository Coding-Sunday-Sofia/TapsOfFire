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

public class NoteEvent extends Event {
	
	public NoteEvent(int string,float time,float endTime) {
		super(time,endTime);
		m_string=string;
	}
	
	public int getString() {
		return m_string;
	}
	
	/////////////////////////////////// runtime
	
	/* This really should not be here, as NoteEvent is meant 
	 * to represent notes in song file, and therefore be not 
	 * modifiable.
	 * 
	 * I will certainly refactor this out of here.
	 */
	
	public void makeIntact() {
		state=STATE_INTACT;
		unpickPosition=0;
	}
	
	public boolean isIntact() {
		return state==STATE_INTACT;
	}
	
	public boolean isPicked() {
		return state==STATE_PICKED || state==STATE_REPICKED;
	}
	public void pick() {
		if (state==STATE_UNPICKED) {
			state=STATE_REPICKED;
		} else {
			state=STATE_PICKED;
		}
	}
	public boolean isRepicked() {
		return state==STATE_REPICKED;
	}
	public boolean isUnpicked() {
		return state==STATE_UNPICKED;
	}
	public int getUnpickPosition() {
		return unpickPosition;
	}
	public void unpick(int position) {
		state=STATE_UNPICKED;
		unpickPosition=position;
	}
	
	public boolean isPickEnded() {
		return state==STATE_PICK_ENDED;
	}
	public void endPick() {
		state=STATE_PICK_ENDED;
	}
	
	public boolean isMissed() {
		return state==STATE_MISSED;
	}
	public void setMissed() {
		state=STATE_MISSED;
	}
	
	public int state=STATE_INTACT;
	public int unpickPosition=0;
	
	public static final int
		STATE_INTACT		=0,
		STATE_PICKED		=1,
		STATE_REPICKED		=2,
		STATE_UNPICKED		=3,
		STATE_PICK_ENDED	=4,
		STATE_MISSED		=5;
	
	///////////////////////////////////////////// implementation
	
	/*package*/ void setString(int string) {
		m_string=string;
	}
	
	/////////////////////////////////// data
	
	private int m_string;
}
