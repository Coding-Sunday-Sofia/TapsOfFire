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

public class MidiConstant {
	
	public static final int FILE_HEADER=0x4d546864; // MThd
	public static final int TRACK_HEADER=0x4d54726b; // MTrk

	/**
	 * Tempo-based timing. Resolution is specified in ticks per beat.
	 */
	public static final float PPQ=0.0f;

	/**
	 * 24 frames/second timing. Resolution is specific in ticks per frame.
	 */
	public static final float SMPTE_24=24.0f;

	/**
	 * 25 frames/second timing. Resolution is specific in ticks per frame.
	 */
	public static final float SMPTE_25=25.0f;

	/**
	 * 30 frames/second timing. Resolution is specific in ticks per frame.
	 */
	public static final float SMPTE_30=30.0f;

	/**
	 * 29.97 frames/second timing. Resolution is specific in ticks per frame.
	 */
	public static final float SMPTE_30DROP=29.97f;

	/**
	 * Status byte for Time Code message.
	 */
	public static final int MIDI_TIME_CODE=0xF1;

	/**
	 * Status byte for Song Position Pointer message.
	 */
	public static final int SONG_POSITION_POINTER=0xF2;

	/**
	 * Status byte for Song Select message.
	 */
	public static final int SONG_SELECT=0xF3;

	/**
	 * Status byte for Bus Select message.
	 */
	public static final int BUS_SELECT=0xF5;
	
	/**
	 * Status byte for Tune Request message.
	 */
	public static final int TUNE_REQUEST=0xF6;

	/**
	 * Status byte for End Of Exclusive message.
	 */
	public static final int END_OF_EXCLUSIVE=0xF7;

	/**
	 * Status byte for Timing Clock message.
	 */
	public static final int TIMING_CLOCK=0xF8;

	/**
	 * Status byte for Start message.
	 */
	public static final int START=0xFA;

	/**
	 * Status byte for Continue message.
	 */
	public static final int CONTINUE=0xFB;

	/**
	 * Status byte for Stop message.
	 */
	public static final int STOP=0xFC;

	/**
	 * Status byte for Active Sensing message.
	 */
	public static final int ACTIVE_SENSING=0xFE;

	/**
	 * Status byte for System Reset message.
	 */
	public static final int SYSTEM_RESET=0xFF;

	/**
	 * Status nibble for Note Off message.
	 */
	public static final int NOTE_OFF=0x80;

	/**
	 * Status nibble for Note On message.
	 */
	public static final int NOTE_ON=0x90;

	/**
	 * Status nibble for Poly Pressure message.
	 */
	public static final int POLY_PRESSURE=0xA0;

	/**
	 * Status nibble for Control Change message.
	 */
	public static final int CONTROL_CHANGE=0xB0;

	/**
	 * Status nibble for Program Change message.
	 */
	public static final int PROGRAM_CHANGE=0xC0;

	/**
	 * Statue nibble for Channel Pressure message.
	 */
	public static final int CHANNEL_PRESSURE=0xD0;

	/**
	 * Status nibble for Pitch Bend message.
	 */
	public static final int PITCH_BEND=0xE0;

	/**
	 * Tempo (meta message)
	 */
	public static final int TEMPO=0x51;
	
	/**
	 * End-of-track (meta message)
	 */
	public static final int END_OF_TRACK=0x2F;
}
