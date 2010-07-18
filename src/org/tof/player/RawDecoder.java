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
package org.tof.player;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import skiba.util.Simply;

public class RawDecoder implements PCMDecoder {
	
	public RawDecoder() {
	}
	
	public RawDecoder(File file) throws IOException {
		open(file);
	}

	public void open(File file) throws IOException {
		close();
		try {
			m_file=new RandomAccessFile(file,"r");
			readHeader();
			seekToTime(1000);
		}
		catch (IOException e) {
			Simply.close(m_file);
			m_file=null;
			throw e;
		}
	}

	public void close() {
		Simply.close(m_file);
		m_file=null;
	}
	
	public boolean isOpened() {
		return m_file!=null;
	}
	
	public boolean isSeekable() {
		return true;
	}
	
	public int getRate() {
		return m_rate;
	}
	
	public int getChannels() {
		return m_channels;
	}
	
	public void seekToTime(int time) throws IOException {
		if (time<0) {
			throw new IllegalArgumentException();
		}
		m_file.seek(timeToOffset(time,m_rate,m_channels));
	}
	
	public int read(byte[] buffer,int offset,int length) throws IOException {
		return m_file.read(buffer,offset,length);
	}
	
	/////////////////////////////////// helpers
	
	public static void writeHeader(OutputStream stream,int rate,int channels)
		throws IOException
	{
		DataOutputStream dataStream=new DataOutputStream(stream);
		dataStream.writeInt(FILE_SIGNATURE);
		dataStream.writeInt(rate);
		dataStream.writeInt(channels);
	}
	
	public static int getFileSize(int timeLength,int rate,int channels) {
		return timeToOffset(timeLength,rate,channels);
	}
	
	///////////////////////////////////////////// implementation
	
	private void readHeader() throws IOException {
		m_file.seek(0);
		int signature=m_file.readInt();
		if (signature!=FILE_SIGNATURE) {
			throw new IOException(
				String.format("Invalid signature (%08X).",signature)
			);
		}
		m_rate=m_file.readInt();
		m_channels=m_file.readInt();
	}
	
	private static int timeToOffset(int time,int rate,int channels) {
		long offset=(long)rate*channels*SAMPLE_BYTES*time/1000;
		offset-=(offset%(channels*SAMPLE_BYTES));
		return (int)(HEADER_SIZE+offset);
	}
	
	/////////////////////////////////// data
	
	private int m_rate;
	private int m_channels;
	private RandomAccessFile m_file;

	private static final int SAMPLE_BYTES=2; 
	private static final int FILE_SIGNATURE=0x52415721;
	private static final int HEADER_SIZE=3*4;
}
