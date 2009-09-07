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
package org.tof.stage;

import java.io.File;
import java.io.IOException;
import org.tof.Config;
import org.tof.player.PCMDecoder;
import org.tof.player.PCMPlayer;
import org.tof.player.RawDecoder;
import org.tof.player.Synchronizer;
import org.tof.player.VorbisDecoder;
import org.tof.song.InvalidSongException;
import org.tof.song.SongConfig;

public class SongPlayer implements PCMPlayer.Callback {
	
	public SongPlayer() {
		m_songPlayer=new PCMPlayer();
		m_guitarPlayer=new PCMPlayer();
		m_playSynchronizer=new Synchronizer();
		m_finishLock=new Object();
		m_songPlayer.setCallback(this);
		m_guitarPlayer.setCallback(this);
	}
	
	public void open(SongConfig config) throws InvalidSongException {
		close();
		try {
			File file=config.getSongFile();
			if (file.exists()) {
				m_songPlayer.open(createDecoder(file));
			}
			file=config.getGuitarFile();
			if (file.exists()) {
				m_guitarPlayer.open(createDecoder(file));
			}
			m_opened=true;
		}
		catch (IOException e) {
			m_songPlayer.close();
			m_guitarPlayer.close();
			throw new InvalidSongException(e);
		}
	}
	
	public void close() {
		if (!m_opened) {
			return;
		}
		m_songPlayer.close();
		m_guitarPlayer.close();
		m_muted=false;
		m_lastPosition=0;
		m_opened=false;
		resetFinishState();
	}
	
	public void play() {
		stop();
		resetFinishState();
		int position=getPosition();
		m_songPlayer.setPosition(position);
		m_guitarPlayer.setPosition(position);
		
		m_totalDifference=0;
		m_totalDifferenceCounter=0;
		
		Synchronizer.Handle shandle=m_playSynchronizer.register();
		try {
			m_songPlayer.prepare(m_playSynchronizer);
			m_guitarPlayer.prepare(m_playSynchronizer);
			m_songPlayer.play();
			m_guitarPlayer.play();
			shandle.synchronize();
		}
		catch (IOException e) {
			onFinished(null,e);
		}
		finally {
			shandle.unregister();
		}
		m_songPlayer.setVolume(Config.getScaledVolume(Config.VOLUME_SONG));
		m_guitarPlayer.setVolume(Config.getScaledVolume(Config.VOLUME_GUITAR));
		applyMute();
	}
	
	public void stop() {
		m_songPlayer.setVolume(0);
		m_guitarPlayer.setVolume(0);
		m_songPlayer.stop();
		m_guitarPlayer.stop();
	}

	public int getPosition() {
		int position=m_lastPosition;
		if (m_guitarPlayer.isPlaying()) {
			position=m_guitarPlayer.getPosition();
		}
		if (m_songPlayer.isPlaying()) {
			int songPosition=m_songPlayer.getPosition();
			if (m_guitarPlayer.isPlaying()) {
				int difference=(position-songPosition);
				m_totalDifference+=difference;
				m_totalDifferenceCounter++;
				if (m_totalDifferenceCounter==100) {
					//Log.e("TOF","Players difference: "+m_totalDifference/100f);
					m_totalDifference=0;
					m_totalDifferenceCounter=0;
				}
				position-=difference/2;
			} else {
				position=songPosition;
			}
		}
		m_lastPosition=position;
		return position;
	}
	
	public void setPosition(int position) {
		m_songPlayer.setPosition(position);
		m_guitarPlayer.setPosition(position);
		m_lastPosition=position;
	}
	
	public void mute(boolean mute) {
		if (m_muted==mute) {
			return;
		}
		m_muted=mute;
		applyMute();
	}
	
	public boolean isFinished() {
		synchronized (m_finishLock) {
			return m_finished;
		}
	}
	public Exception getFinishError() {
		synchronized (m_finishLock) {
			return m_finishError;
		}
	}
	
	public static String getRawFileName(String vorbisFileName) {
		return vorbisFileName+".raw";
	}
	
	///////////////////////////////////////////// implementation
	
	private void applyMute() {
		float volume=m_muted?
			0:
			Config.getScaledVolume(Config.VOLUME_GUITAR);
		if (m_guitarPlayer.isOpened()) {
			m_guitarPlayer.setVolume(volume);
		} else if (m_songPlayer.isOpened()) {
			m_songPlayer.setVolume(volume);
		}
	}
	
	public void onFinished(PCMPlayer player,Exception error) {
		boolean songPlaying=m_songPlayer.isPlaying();
		boolean guitarPlaying=m_guitarPlayer.isPlaying();
		synchronized (m_finishLock) {
			if (m_finished) {
				return;
			}
			if (error!=null) {
				m_finished=true;
				m_finishError=error;
			} else {
				m_finished=(!songPlaying && !guitarPlaying);
			}
		}
		if (error!=null) {
			stop();
		}
	}
	
	private void resetFinishState() {
		synchronized (m_finishLock) {
			m_finished=false;
			m_finishError=null;
		}
	}
	
	private static PCMDecoder createDecoder(File file) throws IOException {
		String name=file.getName();
		int dot=name.lastIndexOf('.');
		if (dot!=-1) {
			String extension=name.substring(dot+1);
			if (extension.equalsIgnoreCase("ogg")) {
				return new VorbisDecoder(file);
			} else if (extension.equalsIgnoreCase("raw")) {
				return new RawDecoder(file);
			}
		}
		throw new IOException("No decoder for '"+name+"'."); 
	}
	
	/////////////////////////////////// data
	
	private boolean m_opened;
	
	private PCMPlayer m_songPlayer;
	private PCMPlayer m_guitarPlayer;
	private Synchronizer m_playSynchronizer;
	private int m_lastPosition;
	
	private Object m_finishLock;
	private boolean m_finished;
	private Exception m_finishError;
	
	private boolean m_muted;
	
	private int m_totalDifference;
	private int m_totalDifferenceCounter;
}
