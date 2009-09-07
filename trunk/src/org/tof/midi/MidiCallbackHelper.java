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
package org.tof.midi;

public class MidiCallbackHelper implements MidiReader.Callback {
	
	public void onStart(MidiHeader format)
		throws InvalidMidiDataException
	{
	}
	public void onTrackStart(int track)
		throws InvalidMidiDataException
	{
	}
	public void onEventDeltaTime(int deltaTime)
		throws InvalidMidiDataException
	{
	}
	public void onMidiEvent(int command,int channel,int data1,int data2)
		throws InvalidMidiDataException
	{
		switch (command) {
			case MidiConstant.NOTE_ON:
			{
				if (data2==0) {
					noteOnOff(false,channel,data1,data2);										
				} else {
					noteOnOff(true,channel,data1,data2);
				}
				break;
			}
			case MidiConstant.NOTE_OFF:
			{
				noteOnOff(false,channel,data1,data2);										
				break;
			}
		}
	}
	public void onSysexEvent(byte[] dataBuffer,int dataLength)
		throws InvalidMidiDataException
	{
	}
	public void onMetaEvent(int type,byte[] dataBuffer,int dataLength) 
		throws InvalidMidiDataException
	{
		switch (type) {
			case MidiConstant.TEMPO:
			{
				if (dataLength!=3) {
					throw new InvalidMidiDataException("Invalid tempo event.");
				}
				tempo(
					((dataBuffer[0] & 0xFF)<<16) |
					((dataBuffer[1] & 0xFF)<<8 ) |
					((dataBuffer[2] & 0xFF)    )
				);
				break;
			}
		}
	}
	public void onTrackEnd()
		throws InvalidMidiDataException
	{
	}
	public void onEnd() 
		throws InvalidMidiDataException
	{
	}
	
	/////////////////////////////////// overridables
	
	public void noteOnOff(boolean on,int channel,int note,int velocity)
		throws InvalidMidiDataException
	{
	}
	public void tempo(int value)
		throws InvalidMidiDataException
	{
	}
}
