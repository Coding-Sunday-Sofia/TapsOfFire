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
package org.tof;

import java.io.File;
import java.util.Date;
import org.tof.R;
import org.tof.ui.UIHelpers;
import org.tof.util.MathHelpers;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.os.Environment;

public class Config {

	public static void load(Context context) {
		loadResources(context);
		loadPreferences(context);
		loadMasterVolume(context);
		fixMasterVolume(context,false);
	}
	
	public static void store(Context context) {
		storePreferences(context);
	}
	
	public static void reset() {
		resetPreferences();
	}

	/////////////////////////////////// paths
	
	public static File getRootPath() {
		File base=Environment.getExternalStorageDirectory();
		return new File(base,"TapsOfFire");
	}
	public static File getBuiltinSongsPath() {
		return new File("songs");
	}
	public static File getSongsPath() {
		return new File(getRootPath(),"songs");
	}
	public static String getSongDBFileName() {
		return "songs.db";
	}
	
	/////////////////////////////////// resources
	
	public static int[] getStringColors() {
		return m_stringColors;
	}
	public static int getStringColor(int string) {
		return m_stringColors[string];
	}
	public static int getBaseColor() {
		return m_baseColor;
	}
	public static int getBackgroundColor() {
		return m_backgroundColor;
	}
	public static int getSelectedColor() {
		return m_selectedColor;
	}
	public static int getShadowColor() {
		return m_shadowColor;
	}
	
	public static Typeface getFireTypeface() {
		return m_fireTypeface;
	}
	public static Typeface getDefaultTypeface() {
		return m_defaultTypeface;
	}
	
	/////////////////////////////////// master volume
	
	public static float getMasterVolume() {
		return m_masterVolume;
	}
	public static void setMasterVolume(float volume) {
		setMasterVolume(volume,null);
	}
	
	public static void fixMasterVolume(Context context,boolean force) {
		if ((!m_masterVolumeFixed || force) && Config.getMasterVolume()<0.3f) {
			Config.setMasterVolume(0.3f,context);
		}
		m_masterVolumeFixed=true;
	}
	
	public static float loadMasterVolume(Context context) {
		AudioManager audio=(AudioManager)context.getSystemService(
			Context.AUDIO_SERVICE);
		int volume=audio.getStreamVolume(
			AudioManager.STREAM_MUSIC);
		int maxVolume=audio.getStreamMaxVolume(
			AudioManager.STREAM_MUSIC);
		if (maxVolume==0) {
			m_masterVolume=0;
		} else {
			m_masterVolume=(float)volume/maxVolume;
		}
		return m_masterVolume;
	}
	
	public static void setMasterVolume(float volume,Context context) {
		m_masterVolume=Math.min(Math.max(0,volume),1);
		if (context!=null) {
			AudioManager audio=(AudioManager)context.getSystemService(
				Context.AUDIO_SERVICE);
			int maxVolume=audio.getStreamMaxVolume(
				AudioManager.STREAM_MUSIC);
			audio.setStreamVolume(
				AudioManager.STREAM_MUSIC,
				MathHelpers.round(volume*maxVolume),
				0);
		}
	}
	
	/////////////////////////////////// volume
	
	public static float getAbsoluteVolume(int volumeIndex) {
		if (volumeIndex<0 || volumeIndex>=COUNTOF_VOLUMES) {
			return 0;
		}
		return m_absoluteVolumes[volumeIndex];
	}
	public static void setAbsoluteVolume(int volumeIndex,float volume) {
		if (volumeIndex<0 || volumeIndex>=COUNTOF_VOLUMES) {
			return;
		}
		setModified();
		volume=Math.min(Math.max(0,volume),1);
		m_absoluteVolumes[volumeIndex]=volume;
	}
	
	public static float getScaledVolume(int volumeIndex) {
		if (volumeIndex<0 || volumeIndex>=COUNTOF_VOLUMES) {
			return 0;
		}
		if (m_masterVolume==0) {
			return 0;
		}
		return Math.min(m_absoluteVolumes[volumeIndex]/m_masterVolume,1);
	}
	
	public static final int
		VOLUME_MENU			=0,
		VOLUME_SONG			=1,
		VOLUME_GUITAR		=2,
		VOLUME_SCREWUP		=3,
		COUNTOF_VOLUMES		=4;
	
	/////////////////////////////////// song cache
	
	public static File getSongCachePath() {
		return new File(getRootPath(),"cache");
	}
	
	public static int getSongCacheLength() {
		return m_songCacheLength;
	}
	public static void setSongCacheLength(int length) {
		m_songCacheLength=length;
		setModified();
	}
	
	/////////////////////////////////// other values
	
	public static int getNotesDelay() {
		return m_notesDelay;
	}
	public static void setNotesDelay(int delay) {
		m_notesDelay=delay;
		setModified();
	}
	
	public static float getEarlyPickMargin() {
		return m_earlyPickMargin;
	}
	public static void setEarlyPickMargin(float margin) {
		m_earlyPickMargin=margin;
		setModified();
	}
	
	public static float getLatePickMargin() {
		return m_latePickMargin;
	}
	public static void setLatePickMargin(float margin) {
		m_latePickMargin=margin;
		setModified();
	}

	public static float getRepickMargin() {
		return m_repickMargin;
	}
	public static void setRepickMargin(float margin) {
		m_repickMargin=margin;
		setModified();
	}
	
	public static int getMinNotesDistance() {
		return m_minNotesDistance;
	}
	public static void setMinNotesDistance(int distance) {
		m_minNotesDistance=distance;
		setModified();
	}
	
	public static int getTargetFPS() {
		return m_targetFPS;
	}
	public static void setTargetFPS(int fps) {
		m_targetFPS=fps;
		setModified();
	}
	
	public static int getTouchHandlerSleep() {
		return m_touchHandlerSleep;
	}
	public static void setTouchHandlerSleep(int sleep) {
		m_touchHandlerSleep=sleep;
		setModified();
	}
	
	public static boolean showDebugInfo() {
		return m_showDebugInfo;
	}
	public static void showDebugInfo(boolean show) {
		m_showDebugInfo=show;
		setModified();
	}
	
	/////////////////////////////////// defaults
	
	public static final int DEFAULT_NOTES_DELAY=0;
	public static final int DEFAULT_SONG_CACHE_LENGTH=5;
	
	public static final float DEFAULT_EARLY_PICK_MARGIN=0.25f;
	public static final float DEFAULT_LATE_PICK_MARGIN=0.25f;
	public static final float DEFAULT_REPICK_MARGIN=0.25f;
	public static final int DEFAULT_MIN_NOTES_DISTANCE=100;
	
	public static final int DEFAULT_TOUCH_HANDLER_SLEEP=20;
	public static final int DEFAULT_TARGET_FPS=30;
	
	public static final boolean DEFAULT_SHOW_DEBUG_INFO=false;
	
	public static final float[] DEFAULT_ABSOLUTE_VOLUMES={
		0.2f,0.7f,0.7f,0.2f
	};
	
	/////////////////////////////////// helpers
	
	public static float getMasterVolume(Context context) {
		AudioManager audio=(AudioManager)context.getSystemService(
				Context.AUDIO_SERVICE);
		int volume=audio.getStreamVolume(
			AudioManager.STREAM_MUSIC);
		int maxVolume=audio.getStreamMaxVolume(
			AudioManager.STREAM_MUSIC);
		if (maxVolume==0) {
			return 0;
		} else {
			return (float)volume/maxVolume;
		}
	}
	
	/////////////////////////////////////////////////////// implementation
	
	private static void resetPreferences() {
		m_notesDelay=DEFAULT_NOTES_DELAY;
		m_songCacheLength=DEFAULT_SONG_CACHE_LENGTH;
		
		m_earlyPickMargin=DEFAULT_EARLY_PICK_MARGIN;
		m_latePickMargin=DEFAULT_LATE_PICK_MARGIN;
		m_repickMargin=DEFAULT_REPICK_MARGIN;
		m_minNotesDistance=DEFAULT_MIN_NOTES_DISTANCE;
		
		m_targetFPS=DEFAULT_TARGET_FPS;
		m_touchHandlerSleep=DEFAULT_TOUCH_HANDLER_SLEEP;
		
		m_showDebugInfo=DEFAULT_SHOW_DEBUG_INFO;
		
		for (int i=0;i!=COUNTOF_VOLUMES;++i) {
			m_absoluteVolumes[i]=DEFAULT_ABSOLUTE_VOLUMES[i];
		}
		setModified();
	}
	
	private static void loadResources(Context context) {
    	m_stringColors=new int[3];
    	m_stringColors[0]=UIHelpers.getColor(context,R.color.string0);
    	m_stringColors[1]=UIHelpers.getColor(context,R.color.string1);
    	m_stringColors[2]=UIHelpers.getColor(context,R.color.string2);
    	
    	m_baseColor=UIHelpers.getColor(context,R.color.base);
    	m_backgroundColor=UIHelpers.getColor(context,R.color.background);
    	m_selectedColor=UIHelpers.getColor(context,R.color.selected);
    	m_shadowColor=UIHelpers.getColor(context,R.color.shadow);
    	
    	m_fireTypeface=UIHelpers.getTypeface(context,"fonts/title.ttf");
    	m_defaultTypeface=UIHelpers.getTypeface(context,"fonts/default.ttf");
	}
	
	private static void loadPreferences(Context context) {
		SharedPreferences preferences=context.getSharedPreferences(
			PREFERENCES_NAME,0);
		
//		SharedPreferences.Editor editor=preferences.edit();
//		editor.clear();
//		editor.commit();
		
		long modificationTime=preferences.getLong(KEY_MODIFICATION_TIME,0);
		if (m_modificationTime>=modificationTime) {
			return;
		}
		m_modificationTime=modificationTime;
		
		m_notesDelay=preferences.getInt(
			KEY_NOTES_DELAY,DEFAULT_NOTES_DELAY);
		m_songCacheLength=preferences.getInt(
			KEY_SONG_CACHE_LENGTH,DEFAULT_SONG_CACHE_LENGTH);
		
		m_earlyPickMargin=preferences.getFloat(
			KEY_EARLY_PICK_MARGIN,DEFAULT_EARLY_PICK_MARGIN);
		m_latePickMargin=preferences.getFloat(
			KEY_LATE_PICK_MARGIN,DEFAULT_LATE_PICK_MARGIN);
		m_repickMargin=preferences.getFloat(
			KEY_REPICK_MARGIN,DEFAULT_REPICK_MARGIN);
		m_minNotesDistance=preferences.getInt(
			KEY_MIN_NOTES_DISTANCE,DEFAULT_MIN_NOTES_DISTANCE);
		
		m_targetFPS=preferences.getInt(
			KEY_TARGET_FPS,DEFAULT_TARGET_FPS);
		m_touchHandlerSleep=preferences.getInt(
			KEY_TOUCH_HANDLER_SLEEP,DEFAULT_TOUCH_HANDLER_SLEEP);
		
		m_showDebugInfo=preferences.getBoolean(
			KEY_SHOW_DEBUG_INFO,DEFAULT_SHOW_DEBUG_INFO);
		
		for (int i=0;i!=COUNTOF_VOLUMES;++i) {
			m_absoluteVolumes[i]=preferences.getFloat(
				KEY_VOLUME_+i,
				DEFAULT_ABSOLUTE_VOLUMES[i]);
		}
	}
	
	private static void storePreferences(Context context) {
		SharedPreferences preferences=context.getSharedPreferences(
			PREFERENCES_NAME,0);
		SharedPreferences.Editor editor=preferences.edit();
		
		editor.putLong(KEY_MODIFICATION_TIME,m_modificationTime);
		editor.putInt(KEY_NOTES_DELAY,m_notesDelay);
		editor.putInt(KEY_SONG_CACHE_LENGTH,m_songCacheLength);
		
		editor.putFloat(KEY_EARLY_PICK_MARGIN,m_earlyPickMargin);
		editor.putFloat(KEY_LATE_PICK_MARGIN,m_latePickMargin);
		editor.putFloat(KEY_REPICK_MARGIN,m_repickMargin);
		editor.putInt(KEY_MIN_NOTES_DISTANCE,m_minNotesDistance);
		
		editor.putInt(KEY_TARGET_FPS,m_targetFPS);
		editor.putInt(KEY_TOUCH_HANDLER_SLEEP,m_touchHandlerSleep);
		editor.putBoolean(KEY_SHOW_DEBUG_INFO,m_showDebugInfo);
		
		for (int i=0;i!=COUNTOF_VOLUMES;++i) {
			editor.putFloat(KEY_VOLUME_+i,m_absoluteVolumes[i]);
		}
		
		editor.commit();
	}
	
	private static void setModified() {
		m_modificationTime=new Date().getTime();
	}
	
	/////////////////////////////////// resources
	
	private static int[] m_stringColors;
	private static int m_baseColor;
	private static int m_backgroundColor;
	private static int m_selectedColor;
	private static int m_shadowColor;
	
	private static Typeface m_fireTypeface;
	private static Typeface m_defaultTypeface;
	
	/////////////////////////////////// preferences

	private static long m_modificationTime=-1;
	
	private static int m_notesDelay;
	private static int m_songCacheLength;
	
	private static float m_earlyPickMargin;
	private static float m_latePickMargin;
	private static float m_repickMargin;
	private static int m_minNotesDistance;

	private static int m_targetFPS;
	private static int m_touchHandlerSleep;
	
	private static boolean m_showDebugInfo;
	
	private static float m_masterVolume;
	private static boolean m_masterVolumeFixed;
	
	private static float[] m_absoluteVolumes=new float[COUNTOF_VOLUMES];
	
	private static final String 
		PREFERENCES_NAME			="config2",
		KEY_MODIFICATION_TIME		="modificationTime",
		KEY_VOLUME_					="volume#",
		KEY_NOTES_DELAY				="notesDelay",
		KEY_SONG_CACHE_LENGTH		="songCacheLength",
		KEY_EARLY_PICK_MARGIN		="earlyPickMargin",
		KEY_LATE_PICK_MARGIN		="latePickMargin",
		KEY_REPICK_MARGIN			="repickMargin",
		KEY_MIN_NOTES_DISTANCE		="minNotesDistance",
		KEY_TARGET_FPS				="targetFPS",
		KEY_TOUCH_HANDLER_SLEEP		="touchHandlerSleep",
		KEY_SHOW_DEBUG_INFO			="showDebugInfo";
}
