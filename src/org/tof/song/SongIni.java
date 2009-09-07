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

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.CRC32;
import org.tof.midi.InvalidMidiDataException;
import org.tof.midi.MidiCallbackHelper;
import org.tof.midi.MidiReader;
import org.tof.util.DataStreamHelpers;
import org.tof.util.IniFile;
import skiba.util.Simply;
import android.content.res.AssetManager;

public class SongIni {
	
	public SongIni(File songPath) throws InvalidSongException {
		try {
			byte[] iniData=readFileData(new File(songPath,INI_FILE));
			byte[] notesData=readFileData(new File(songPath,NOTES_FILE));
			construct(iniData,notesData);
			calcUnique(iniData,notesData);
		}
		catch (IOException e) {
			throw new InvalidSongException(e);
		}
	}
	
	public SongIni(AssetManager assets,File songPath) throws InvalidSongException {
		try {
			byte[] iniData=readFileData(assets,new File(songPath,INI_FILE));
			byte[] notesData=readFileData(assets,new File(songPath,NOTES_FILE));
			construct(iniData,notesData);
			calcUnique(iniData,notesData);
		}
		catch (IOException e) {
			throw new InvalidSongException(e);
		}
	}
	
	public SongIni(SongIni other) {
		m_id=other.m_id;
		m_name=other.m_name;
		m_artist=other.m_artist;
		m_casetteColor=other.m_casetteColor;
		m_delay=other.m_delay;
		m_skills=other.m_skills;
	}
	
	public SongIni(DataInput dataIn) throws IOException {
		DataStreamHelpers.checkTag(dataIn,DATA_TAG);
		m_id=dataIn.readInt();
		m_name=dataIn.readUTF();
		m_artist=dataIn.readUTF();
		m_casetteColor=dataIn.readInt();
		m_delay=dataIn.readInt();
		m_skills=dataIn.readInt();
	}
	
	public void saveState(DataOutput dataOut) throws IOException {
		dataOut.writeInt(DATA_TAG);
		dataOut.writeInt(m_id);
		dataOut.writeUTF(m_name);
		dataOut.writeUTF(m_artist);
		dataOut.writeInt(m_casetteColor);
		dataOut.writeInt(m_delay);
		dataOut.writeInt(m_skills);
	}
	
	public int getID() {
		return m_id;
	}
	
	public String getName() {
		return m_name;
	}
	
	public String getArtist() {
		return m_artist;
	}
	
	public int getCassetteColor() {
		return m_casetteColor;
	}
	
	public int getSkills() {
		return m_skills;
	}
	
	public int getDelay() {
		return m_delay;
	}
	
	/////////////////////////////////// constants
	
	public static final String INI_FILE="song.ini";
	public static final String NOTES_FILE="notes.mid";
	public static final String SONG_FILE="song.ogg";
	public static final String GUITAR_FILE="guitar.ogg";
	
	public static final int DEFAULT_CASETTE_COLOR=0x00000000;
	
	///////////////////////////////////////////// implementation
	
	private static byte[] readFileData(AssetManager assets,File file) throws IOException {
		InputStream stream=null;
		try {
			stream=assets.open(file.getPath());
			int length=stream.available();
			byte[] data=new byte[length];
			stream.read(data);
			return data;
		}
		finally {
			Simply.close(stream);
		}
	}

	private static byte[] readFileData(File file) throws IOException {
		InputStream stream=null;
		try {
			stream=new FileInputStream(file.getPath());
			int length=(int)file.length();
			byte[] data=new byte[length];
			stream.read(data);
			return data;
		}
		finally {
			Simply.close(stream);
		}
	}
	
	private void construct(byte[] iniData,byte[] notesData) 
		throws InvalidSongException,IOException
	{
		IniFile iniFile=new IniFile(new ByteArrayInputStream(iniData));
		IniFile.Section section=iniFile.getSection(INI_SECTION);
		m_name=section.getStringValue("name","<unknown>");
		m_artist=section.getStringValue("artist","<unknown>");
		m_casetteColor=section.getColorValue("casettecolor",DEFAULT_CASETTE_COLOR);
		m_delay=section.getIntValue("delay",0);
		m_skills=SkillGatherer.gather(new ByteArrayInputStream(notesData));
		if (m_skills==0) {
			throw new InvalidSongException("Song doesn't notes.");
		}
	}
	
	private void calcUnique(byte[] iniData,byte[] notesData)
		throws IOException
	{
		CRC32 crc=new CRC32();
		crc.update(iniData);
		crc.update(notesData);
		m_id=(int)crc.getValue();
	}
	
	/////////////////////////////////// SkillGatherer
	
	private static class SkillGatherer extends MidiCallbackHelper {
		
		public static int gather(InputStream notesFileStream) 
			throws InvalidSongException
		{
			SkillGatherer callback=new SkillGatherer();
			try {
				MidiReader.read(callback,notesFileStream);
				return callback.m_skills;
			}
			catch (InvalidMidiDataException e) {
				throw new InvalidSongException(e);
			}
			catch (IOException e) {
				throw new InvalidSongException(e);
			}
		}

		public void noteOnOff(boolean on,int channel,int note,int velocity) {
			if (!on) {
				return;
			}
			int skill=NoteMap.skillOf(note);
			if (skill!=-1) {
				m_skills|=skill;
			}
		}
		
		private int m_skills;
	}
	
	/////////////////////////////////// data
	
	private int m_id;
	private String m_name;
	private String m_artist;
	private int m_casetteColor;
	private int m_skills;
	private int m_delay;
	
	private static final String INI_SECTION="song";
	private static final int DATA_TAG=0x53494E49;
}
