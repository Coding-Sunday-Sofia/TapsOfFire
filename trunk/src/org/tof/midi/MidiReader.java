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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class MidiReader {
	
	/////////////////////////////////// Callback
	
	public interface Callback {
		void onStart(MidiHeader header) 
			throws InvalidMidiDataException;
		
		void onTrackStart(int track) 
			throws InvalidMidiDataException;
		
		void onEventDeltaTime(int time) 
			throws InvalidMidiDataException;
		
		void onMidiEvent(int command,int channel,int data1,int data2) 
			throws InvalidMidiDataException;
		
		void onSysexEvent(byte[] dataBuffer,int dataLength)
			throws InvalidMidiDataException;
		
		void onMetaEvent(int type,byte[] dataBuffer,int dataLength)
			throws InvalidMidiDataException;
		
		void onTrackEnd()
			throws InvalidMidiDataException;
		
		void onEnd()
			throws InvalidMidiDataException;
	}
	
	/////////////////////////////////// read()

	public static void read(Callback callback,File file)
		throws InvalidMidiDataException,IOException
	{
		int length=(int)file.length();
		byte[] data=new byte[length];
		new FileInputStream(file).read(data);
		read(callback,new ByteArrayInputStream(data));
	}
	
	public static void read(Callback callback,InputStream is) 
		throws InvalidMidiDataException,IOException
	{
		MidiDataInputStream din=new MidiDataInputStream(is);
		MidiHeader header=readHeader(din);
		callback.onStart(header);

		byte[] dataBuffer=null;
		for (int track=0;track!=header.tracks;++track) {
			if (din.readInt()!=MidiConstant.TRACK_HEADER) {
				throw new InvalidMidiDataException("Invalid MIDI track header.");
			}
			callback.onTrackStart(track);
			/*length*/din.readInt();
			int runningStatus=-1;
			for (boolean done=false;!done;) {
				int deltaTime=din.readVariableLengthInt();
				callback.onEventDeltaTime(deltaTime);

				int sbyte=(din.readByte() & 0xFF);
				if (sbyte<0xF0) {
					// Midi event
					int status=sbyte;
					int data1=0,data2=0;

					switch (sbyte & 0xF0) {
						case MidiConstant.NOTE_OFF:
						case MidiConstant.NOTE_ON:
						case MidiConstant.POLY_PRESSURE:
						case MidiConstant.CONTROL_CHANGE:
						case MidiConstant.PITCH_BEND:
							data1=(din.readByte() & 0xFF);
							data2=(din.readByte() & 0xFF);
							runningStatus=sbyte;
							break;
	
						case MidiConstant.PROGRAM_CHANGE:
						case MidiConstant.CHANNEL_PRESSURE:
							data1=(din.readByte() & 0xFF);
							runningStatus=sbyte;
							break;
	
						default:
							if ((sbyte & 0x80)!=0) {
								continue;
							}
							if (runningStatus!=-1) {
								switch (runningStatus & 0xF0) {
									case MidiConstant.NOTE_OFF:
									case MidiConstant.NOTE_ON:
									case MidiConstant.POLY_PRESSURE:
									case MidiConstant.CONTROL_CHANGE:
									case MidiConstant.PITCH_BEND:
										status=runningStatus;
										data1=sbyte;
										data2=(din.readByte() & 0xFF);
										break;
		
									case MidiConstant.PROGRAM_CHANGE:
									case MidiConstant.CHANNEL_PRESSURE:
										status=runningStatus;
										data1=sbyte;
										break;
		
									default:
										throw new InvalidMidiDataException(
											"Invalid Short MIDI Event: "+sbyte
										);
									
								}
							} else {
								throw new InvalidMidiDataException(
									"Invalid Short MIDI Event: "+sbyte
								);
							}
					}
					callback.onMidiEvent((status & 0xF0),(status & 0x0F),data1,data2);
				} else if (sbyte==0xF0 || sbyte==0xF7) {
					// System Exclusive event
					int dataLength=din.readVariableLengthInt();
					dataBuffer=ensureBufferLength(dataBuffer,dataLength);
					din.readFully(dataBuffer,0,dataLength);
					callback.onSysexEvent(dataBuffer,dataLength);
				} else if (sbyte==0xFF) {
					// Meta event
					byte mtype=din.readByte();
					int dataLength=din.readVariableLengthInt();
					dataBuffer=ensureBufferLength(dataBuffer,dataLength);
					din.readFully(dataBuffer,0,dataLength);
					callback.onMetaEvent((mtype & 0xFF),dataBuffer,dataLength);
					if (mtype==MidiConstant.END_OF_TRACK) {
						done=true;
					}
				} else {
					throw new InvalidMidiDataException("Invalid status byte: "+sbyte);
				}
			}
			callback.onTrackEnd();
		}
		callback.onEnd();
	}
	
	///////////////////////////////////////////// implementation
	
	private static MidiHeader readHeader(InputStream in) 
		throws InvalidMidiDataException,IOException
	{
		DataInputStream din;
		if (in instanceof DataInputStream) {
			din=(DataInputStream)in;
		} else {
			din=new DataInputStream(in);
		}
		int type,tracks,division,resolution,bytes;
		float divisionType;
		if (din.readInt()!=MidiConstant.FILE_HEADER) {
			throw new InvalidMidiDataException(
				"Invalid MIDI chunk header."
			);
		}
		bytes=din.readInt();
		if (bytes<6) {
			throw new InvalidMidiDataException(
				"Invalid MIDI chunk header length: "+bytes
			);
		}
		type=din.readShort();
		if (type<0 || type>2) {
			throw new InvalidMidiDataException(
				"Invalid MIDI file type value: "+type
			);
		}
		tracks=din.readShort();
		if (tracks<=0) {
			throw new InvalidMidiDataException(
				"Invalid number of MIDI tracks: "+tracks
			);
		}
		division=din.readShort();
		if ((division & 0x8000)!=0) {
			division=-((division>>>8) & 0xFF);
			switch (division) {
				case 24:
					divisionType=MidiConstant.SMPTE_24;
					break;
				case 25:
					divisionType=MidiConstant.SMPTE_25;
					break;
				case 29:
					divisionType=MidiConstant.SMPTE_30DROP;
					break;
				case 30:
					divisionType=MidiConstant.SMPTE_30;
					break;
				default:
					throw new InvalidMidiDataException(
						"Invalid MIDI frame division type: "+division
					);
			}
			resolution=division&0xff;
		} else {
			divisionType=MidiConstant.PPQ;
			resolution=division&0x7fff;
		}
		din.skip(bytes-6);
		return new MidiHeader(type,divisionType,resolution,tracks);
	}
	
	private static byte[] ensureBufferLength(byte[] buffer,int length) {
		if (buffer==null || buffer.length<length) {
			return new byte[length];
		} else {
			return buffer;
		}
	}
}
