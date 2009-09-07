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
package org.tof.songdb;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import org.tof.Config;
import org.tof.song.Song;
import skiba.util.Simply;
import android.content.Context;
import android.util.SparseArray;

public class SongDB {
	
	public interface Record {
		public long getFirstPlayedTime();
		public long getLastPlayedTime();
		public Score getScore(int skill);
	}
	
	public static class Score {
		public int score;
		public float rating;
		
		public Score(int score,float rating) {
			this.score=score;
			this.rating=rating;
		}
	}
	
	/////////////////////////////////// methods

	public static void load(Context context) {
		InputStream stream=null;
		try {
			stream=new FileInputStream(getFilePath(context));
			DataInputStream dataIn=new DataInputStream(stream);
			SparseArray<RecordImpl> records=new SparseArray<RecordImpl>();
			long timestamp=dataIn.readLong();
			if (m_timestamp>=timestamp) {
				return;
			}
			int count=dataIn.readInt();
			for (int i=0;i!=count;++i) {
				int id=dataIn.readInt();
				RecordImpl record=new RecordImpl(dataIn);
				records.append(id,record);
			}
			m_records=records;
		}
		catch (IOException e) {
			//e.printStackTrace();
		}
		finally {
			Simply.close(stream);
		}
	}
	
	public static void store(Context context) {
		OutputStream stream=null;
		try {
			stream=new FileOutputStream(getFilePath(context));
			DataOutputStream dataOut=new DataOutputStream(stream);
			dataOut.writeLong(m_timestamp);
			dataOut.writeInt(m_records.size());
			for (int i=0;i!=m_records.size();++i) {
				dataOut.writeInt(m_records.keyAt(i));
				m_records.valueAt(i).store(dataOut);
			}
			dataOut.flush();
			stream.flush();
			stream.close();
		}
		catch (IOException e) {
			//e.printStackTrace();
		}
		finally {
			Simply.close(stream);
		}
	}
	
	public static Record find(int songID) {
		return m_records.get(songID);
	}
	
	public static void update(int songID,int skill,int score,float rating) {
		int skillIndex=Song.skillToIndex(skill);
		RecordImpl record=m_records.get(songID);
		if (record==null) {
			record=new RecordImpl();
			record.timeFirstPlayed=new Date().getTime();
			m_records.put(songID,record);
			modified();
		}
		record.timeLastPlayed=new Date().getTime();
		Score oldScore=record.scores[skillIndex];
		if (oldScore==null || score>oldScore.score) {
			record.scores[skillIndex]=new Score(score,rating);
			modified();
		}
	}
	
	///////////////////////////////////////////// implementation
	
	private static class RecordImpl implements Record {
		public RecordImpl() {
		}
		public RecordImpl(DataInput dataIn) throws IOException {
			load(dataIn);
		}
		public long getFirstPlayedTime() {
			return timeFirstPlayed;
		}
		public long getLastPlayedTime() {
			return timeLastPlayed;
		}
		public Score getScore(int skill) {
			return scores[Song.skillToIndex(skill)];
		}
		public void store(DataOutput dataOut) throws IOException {
			dataOut.writeLong(timeFirstPlayed);
			dataOut.writeLong(timeLastPlayed);
			for (int i=0;i!=Song.SKILL_COUNT;++i) {
				storeScore(dataOut,scores[i]);
			}
		}
		public void load(DataInput dataIn) throws IOException {
			timeFirstPlayed=dataIn.readLong();
			timeLastPlayed=dataIn.readLong();
			for (int i=0;i!=Song.SKILL_COUNT;++i) {
				scores[i]=loadScore(dataIn);
			}
		}
		
		public long timeFirstPlayed;
		public long timeLastPlayed;
		public Score[] scores=new Score[Song.SKILL_COUNT];
	}
	
	private static void modified() {
		m_timestamp=new Date().getTime();
	}
	
	/////////////////////////////////// helpers
	
	private static File getFilePath(Context context) {
		return new File(context.getFilesDir(),Config.getSongDBFileName());
	}
	
	private static void storeScore(DataOutput dataOut,Score score) throws IOException {
		if (score==null) {
			dataOut.writeBoolean(false);
		} else {
			dataOut.writeBoolean(true);
			dataOut.writeInt(score.score);
			dataOut.writeFloat(score.rating);
		}
	}
	
	private static Score loadScore(DataInput dataIn) throws IOException {
		if (!dataIn.readBoolean()) {
			return null;
		} else {
			return new Score(dataIn.readInt(),dataIn.readFloat());
		}
	}
	
	/////////////////////////////////// data

	private static long m_timestamp;
	private static SparseArray<RecordImpl> m_records=new SparseArray<RecordImpl>();
}
