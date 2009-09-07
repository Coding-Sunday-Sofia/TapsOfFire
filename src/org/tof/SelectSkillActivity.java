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
import java.io.IOException;
import org.tof.R;
import org.tof.player.Vorbis2RawConverter;
import org.tof.song.InvalidSongException;
import org.tof.song.Song;
import org.tof.song.SongIni;
import org.tof.songdb.SongCache;
import org.tof.songdb.SongDB;
import org.tof.stage.SongPlayer;
import org.tof.ui.ActivityBase;
import org.tof.ui.PlayableSkillView;
import org.tof.ui.SongInfo;
import org.tof.ui.UIHelpers;
import org.tof.ui.UISoundEffects;
import org.tof.util.AssetExtractor;
import org.tof.util.DataInputBA;
import org.tof.util.DataOutputBA;
import org.tof.util.MiscHelpers;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Process;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ViewFlipper;

public class SelectSkillActivity extends ActivityBase implements PlayableSkillView.Callback {
	
	protected void onCreate(Bundle savedState) {
		super.onCreate(savedState);
		setContentView(R.layout.select_skill);
		
		Intent intent=getIntent();
		loadSong(intent.getByteArrayExtra(SongInfo.BUNDLE_KEY));
//		try {
//			m_song=new SongInfo(
//					//getAssets(),new File("songs/defy"));
//					new File("/sdcard/API-no-song"));
//			MiscHelpers.cleanup(Config.getSongCachePath());
//		}
//		catch (InvalidSongException e) {
//			throw new RuntimeException(e);
//		}
		
		m_pageFlipper=(ViewFlipper)findViewById(R.id.flipper);
		initializeSkillViews();
		
		UIHelpers.setText(this,R.id.name,m_song.getName());
		UIHelpers.setText(this,R.id.artist,m_song.getArtist());
		UISoundEffects.playInSound();
	}
	
	protected void onResume() {
		super.onResume();
		SongDB.load(this);
		setupSkillViews();
		doPageAction(getCurrentPage(),PAGEACTION_RESUME);
		if (getCurrentPage()==PAGE_MAIN) {
			animate();
		}
	}
	
	protected void onPause() {
		super.onPause();
		doPageAction(getCurrentPage(),PAGEACTION_PAUSE);
	}
	
	protected void onDestroy() {
		super.onDestroy();
		doPageAction(getCurrentPage(),PAGEACTION_STOP);
	}
	
	protected boolean onBackKeyDown() {
		UISoundEffects.playOutSound();
		if (getCurrentPage()==PAGE_MAIN) {
			return false;
		}
		switchToPage(PAGE_MAIN,true);
		return true;
	}
	
//	public boolean onPrepareOptionsMenu(Menu menu) {
//		m_paused=!m_paused;
//		if (m_paused) {
//			doPageAction(getCurrentPage(),PAGEACTION_PAUSE);
//		} else {
//			doPageAction(getCurrentPage(),PAGEACTION_RESUME);
//		}
//			
//		return super.onPrepareOptionsMenu(menu);
//	}
//	boolean m_paused;
	
	/////////////////////////////////////////////////////// logic
	
	private void loadSong(byte[] song) {
		try {
			m_song=new SongInfo(new DataInputBA(song));
			m_originalSong=new SongInfo(m_song);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void onPlaySkill(int skill) {
		m_song.setSelectedSkill(skill);
		prepareSong();
	}
	
	private void prepareSong() {
		if (m_song.isAsset()) {
			if (!checkSDCard()) {
				switchToPage(PAGE_SDCARD,true);
				return;
			}
			if (checkExtracted()) {
				playSong();
			} else {
				switchToPage(PAGE_EXTRACTOR,true);
			}
		} else {
			if (checkConverted()) {
				playSong();
			} else {
				switchToPage(PAGE_CONVERTER,true);
			}
		}
	}
	
	private void playSong() {
		try {
			DataOutputBA dataOut=new DataOutputBA();
			m_song.saveState(dataOut);
			m_song=new SongInfo(m_originalSong);
			Intent intent=new Intent(this,GameActivity.class);
			intent.putExtra(SongInfo.BUNDLE_KEY,dataOut.toByteArray());
			startActivity(intent);
			switchToPage(PAGE_MAIN,false);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void animate() {
		int offset=UIHelpers.startViewAnimation(
			this,
			R.id.head,R.anim.head_in);
		animateSkillViews(offset);
	}
	
	/////////////////////////////////////////////////////// sdcard
	
	private boolean checkSDCard() {
		return Environment.getExternalStorageState().equals(
			Environment.MEDIA_MOUNTED);
	}
	
	private void onSDCardPageAction(int action) {
		if (action==PAGEACTION_INITIALIZE) {
			findViewById(R.id.check_sdcard).setOnClickListener(
				new View.OnClickListener() {
					public void onClick(View view) {
						switchToPage(PAGE_MAIN,true);
						prepareSong();
					}
				}
			);
		}
	}
	
	/////////////////////////////////////////////////////// extractor
	// MARKER extractor
	
	private boolean checkExtracted() {
		File cachePath=SongCache.find(m_song.getID());
		if (cachePath==null) {
			return false;
		}
		boolean extracted=AssetExtractor.isExtracted(
			this,
			m_song.getAssetPath(),
			cachePath);
		if (!extracted) {
			MiscHelpers.cleanup(cachePath);
			return false;
		}
		m_song.setFilesPath(cachePath);
		return checkConverted();
	}
	
	private void onExtractorPageAction(int action) {
		switch (action) {
			case PAGEACTION_INITIALIZE:
				findViewById(R.id.extractorPlay).setOnClickListener(
					new OnClickListener() {
						public void onClick(View view) {
							playSong();
						}
					}
				);
				break;
			case PAGEACTION_START:
			{
				showExtractorProgress(0);
				m_handler.postDelayed(m_extractorStarter,CONVERTER_DELAY);
				m_extractorStarting=true;
				break;
			}
			case PAGEACTION_STOP:
			{
				if (m_extractor!=null) {
					m_extractor.stop();
					m_extractor=null;
					m_handler.removeCallbacks(m_extractorPoller);
				}
				m_handler.removeCallbacks(m_extractorStarter);
				break;
			}
			case PAGEACTION_PAUSE:
			{
				if (m_extractorStarting) {
					m_handler.removeCallbacks(m_extractorStarter);
					m_extractorStarter.run();
				}
				if (m_extractor!=null) {
					m_extractor.pause();
				}
				break;
			}
			case PAGEACTION_RESUME:
			{
				if (m_extractor!=null) {
					m_extractor.pause();
				}
				break;
			}
		}
	}
	
	private void startExtractor() {
		UIHelpers.flipToChild(this,R.id.extractorFlipper,0,false);
		m_extractor=new Extractor(this,m_song);
		m_extractor.start();
		pollExtractor();
	}
	
	private void pollExtractor() {
		m_extractor.check();
		if (m_extractor.isFinished()) {
			Exception finishError=m_extractor.getFinishError();
			m_extractor=null;
			if (finishError!=null) {
				ErrorReportActivity.report(
					this,
					ErrorReportActivity.CAUSE_ERROR,
					"Failed to extract bundled song.",
					null,
					m_song.getErrorDetails(),
					finishError);
				finish();
			} else {
				UIHelpers.flipToChild(this,R.id.extractorFlipper,1,true);
			}
			return;
		}
		showExtractorProgress(m_extractor.getProgress());
		m_handler.postDelayed(m_extractorPoller,100);
	}
	
	private void showExtractorProgress(int progress) {
		String done=UIHelpers.getString(
			this,
			R.string.extracting_song_fmt,
			progress);
		UIHelpers.setText(this,R.id.extractorHead,done);
	}
	
	private static File getExtractedSongFile(SongInfo song) {
		return new File(
			SongCache.getPath(song.getID()),
			SongIni.SONG_FILE);
	}
	private static File getExtractedGuitarFile(SongInfo song) {
		return new File(
			SongCache.getPath(song.getID()),
			SongIni.GUITAR_FILE);
	}
	
	/////////////////////////////////// Extractor
	
	public static class Extractor {
		public Extractor(Context context,SongInfo song) {
			m_context=context;
			m_song=song;
		}
		
		public void start() {
			SongCache.push(m_song.getID());
			m_extractor=new AssetExtractor(
				m_context,
				m_song.getAssetPath(),
				SongCache.getPath(m_song.getID()));
			m_extractor.start();
		}
		public void stop() {
			if (m_finished) {
				return;
			}
			if (m_extractor!=null) {
				m_extractor.stop();
				m_extractor=null;
			}
			if (m_converter!=null) {
				m_converter.stop();
				m_converter=null;
			}
			m_finished=true;
			m_finishError=null;
			SongCache.remove(m_song.getID());
		}
		
		public void pause() {
			if (m_extractor!=null) {
				m_extractor.pause();
			}
			if (m_converter!=null) {
				m_converter.pause();
			}
		}
		public void resume() {
			if (m_extractor!=null) {
				m_extractor.resume();
			}
			if (m_converter!=null) {
				m_converter.resume();
			}
		}
		
		public void check() {
			if (m_finished) {
				return;
			}
			if (m_extractor!=null && m_extractor.isFinished()) {
				m_finishError=m_extractor.getFinishError();
				m_extractor=null;
				if (m_finishError!=null) {
					m_finished=true;
				} else {
					startConverter(true);
				}
				return;
			}
			if (m_converter!=null && m_converter.isFinished()) {
				m_finishError=m_converter.getFinishError();
				m_converter=null;
				if (m_finishError!=null || m_convertingGuitarFile) {
					m_finished=true;
					if (m_finishError==null) {
						setSongFiles();
					} else {
						SongCache.remove(m_song.getID());
					}
				} else {
					startConverter(false);
				}
				return;
			}
		}
		
		public int getProgress() {
			if (m_finished) {
				return 100;
			}
			if (m_extractor!=null) {
				return m_extractor.getProgress()/3;
			}
			if (m_converter!=null) {
				int base=m_convertingGuitarFile?(100*2/3):(100/3);
				return base+m_converter.getProgress()/3;
			}
			return 0;
		}
		
		
		public boolean isFinished() {
			return m_finished;
		}
		public Exception getFinishError() {
			return m_finishError;
		}
		
		///////////////////// implementation
		
		private void startConverter(boolean convertSong) {
			File inputFile=convertSong?
				getExtractedSongFile(m_song):
				getExtractedGuitarFile(m_song);
			File outputFile=convertSong?
				getConvertedSongFile(m_song):
				getConvertedGuitarFile(m_song);
			try {
				m_converter=new Vorbis2RawConverter();
				m_converter.setPriority(CONVERTER_PRIORITY);
				m_converter.start(inputFile,outputFile);
				m_convertingGuitarFile=!convertSong;
			}
			catch (IOException e) {
				m_converter=null;
				m_finished=true;
				m_finishError=e;
			}
		}
		
		private void setSongFiles() {
			m_song.setFilesPath(SongCache.getPath(m_song.getID()));
			m_song.setSongFile(getConvertedSongFile(m_song));
			m_song.setGuitarFile(getConvertedGuitarFile(m_song));
		}
		
		private Context m_context;
		private SongInfo m_song;
		
		private AssetExtractor m_extractor;
		private Vorbis2RawConverter m_converter;
		private boolean m_convertingGuitarFile;
		
		private boolean m_finished;
		private Exception m_finishError;
	}
	
	/////////////////////////////////////////////////////// converter
	// MARKER converter
	
	private boolean checkConverted() {
		File cachePath=SongCache.find(m_song.getID());
		if (cachePath==null) {
			return false;
		}
		File songFile=getConvertedSongFile(m_song);
		File guitarFile=getConvertedGuitarFile(m_song);
		boolean songConverted=checkConverted(
			m_song.getSongFile(),
			songFile);
		boolean guitarConverted=checkConverted(
			m_song.getGuitarFile(),
			guitarFile);
		if (!songConverted || !guitarConverted) {
			return false;
		}
		m_song.setSongFile(songFile);
		m_song.setGuitarFile(guitarFile);
		return true;
	}
	
	private void onConverterPageAction(int action) {
		switch (action) {
			case PAGEACTION_INITIALIZE:
				findViewById(R.id.converterPlay).setOnClickListener(
					new OnClickListener() {
						public void onClick(View view) {
							playSong();
						}
					}
				);
				findViewById(R.id.converterHead).setOnClickListener(
					new OnClickListener() {
						public void onClick(View view) {
							onConverterPageAction(PAGEACTION_STOP);
							playSong();
						}
					}
				);
				break;
			case PAGEACTION_START:
			{
				showConverterProgress(0);
				m_handler.postDelayed(m_converterStarter,CONVERTER_DELAY);
				m_converterStarting=true;
				break;
			}
			case PAGEACTION_STOP:
			{
				if (m_converter!=null) {
					m_converter.stop();
					m_converter=null;
					m_handler.removeCallbacks(m_converterPoller);
				}
				m_handler.removeCallbacks(m_converterStarter);
				break;
			}
			case PAGEACTION_PAUSE:
			{
				if (m_converterStarting) {
					m_handler.removeCallbacks(m_converterStarter);
					m_converterStarter.run();
				}
				if (m_converter!=null) {
					m_converter.pause();
				}
				break;
			}
			case PAGEACTION_RESUME:
			{
				if (m_converter!=null) {
					m_converter.resume();
				}
				break;
			}
		}
	}
	
	private void startConverter() {
		UIHelpers.flipToChild(this,R.id.converterFlipper,0,false);
		m_converter=new Converter(m_song);
		m_converter.start();
		pollConverter();
	}
	
	private void pollConverter() {
		m_converter.check();
		if (m_converter.isFinished()) {
			Exception finishError=m_converter.getFinishError();
			m_converter=null;
			if (finishError!=null) {
				ErrorReportActivity.report(
					this,
					ErrorReportActivity.CAUSE_ERROR,
					"Failed to decode song.",
					null,
					m_song.getErrorDetails(),
					finishError);
				finish();
			} else {
				UIHelpers.flipToChild(this,R.id.converterFlipper,1,true);
			}
			return;
		}
		showConverterProgress(m_converter.getProgress());
		m_handler.postDelayed(m_converterPoller,100);
	}
	
	private void showConverterProgress(int progress) {
		String done=UIHelpers.getString(
			this,
			R.string.converting_song_fmt,
			progress);
		UIHelpers.setText(this,R.id.converterHead,done);
	}
	
	private static boolean checkConverted(File file,File convertedFile) {
		if (!file.exists()) {
			return true;
		}
		if (!convertedFile.exists()) {
			return false;
		}
		return Vorbis2RawConverter.isConvertedFile(file,convertedFile);
	}
	
	private static File getConvertedSongFile(SongInfo song) {
		return new File(
			SongCache.getPath(song.getID()),
			SongPlayer.getRawFileName(SongIni.SONG_FILE));
	}
	private static File getConvertedGuitarFile(SongInfo song) {
		return new File(
			SongCache.getPath(song.getID()),
			SongPlayer.getRawFileName(SongIni.GUITAR_FILE));
	}
	
	/////////////////////////////////// Converter
	
	private static class Converter {
		public Converter(SongInfo song) {
			m_song=song;
		}
		
		public void start() {
			SongCache.push(m_song.getID());
			m_haveSongFile=m_song.getSongFile().exists();
			m_haveGuitarFile=m_song.getGuitarFile().exists();
			if (!m_haveSongFile && !m_haveGuitarFile) {
				m_finished=true;
				return;
			}
			startConverter(m_haveSongFile);
		}
		public void stop() {
			if (m_finished) {
				return;
			}
			if (m_converter!=null) {
				m_converter.stop();
				m_converter=null;
			}
			m_finished=false;
			m_finishError=null;
			SongCache.remove(m_song.getID());
		}
		
		public void pause() {
			if (m_converter!=null) {
				m_converter.pause();
			}
		}
		public void resume() {
			if (m_converter!=null) {
				m_converter.resume();
			}
		}
		
		public void check() {
			if (m_finished) {
				return;
			}
			if (m_converter.isFinished()) {
				m_finishError=m_converter.getFinishError();
				m_converter=null;
				if (m_finishError!=null ||
					m_convertingGuitarFile==m_haveGuitarFile)
				{
					m_finished=true;
					if (m_finishError==null) {
						setSongFiles();
					} else {
						SongCache.remove(m_song.getID());
					}
				} else {
					startConverter(m_convertingGuitarFile);
				}
			}
		}
		
		public int getProgress() {
			if (m_finished) {
				return 100;
			}
			if (m_converter==null) {
				return 0;
			}
			if (!m_haveSongFile || !m_haveGuitarFile) {
				return m_converter.getProgress();
			} else {
				int base=m_convertingGuitarFile?(100/2):0;
				return base+m_converter.getProgress()/2;
			}
		}
		
		public boolean isFinished() {
			return m_finished;
		}
		public Exception getFinishError() {
			return m_finishError;
		}
		
		///////////////////// implementation
		
		private void startConverter(boolean convertSongFile) {
			File inputFile=convertSongFile?
				m_song.getSongFile():
				m_song.getGuitarFile();
			File outputFile=convertSongFile?
				getConvertedSongFile(m_song):
				getConvertedGuitarFile(m_song);
			try {
				m_converter=new Vorbis2RawConverter();
				m_converter.setPriority(CONVERTER_PRIORITY);
				m_converter.start(inputFile,outputFile);
				m_convertingGuitarFile=!convertSongFile;
			}
			catch (IOException e) {
				m_finished=true;
				m_finishError=e;
			}
		}
		
		private void setSongFiles() {
			if (m_haveSongFile) {
				m_song.setSongFile(getConvertedSongFile(m_song));
			}
			if (m_haveGuitarFile) {
				m_song.setGuitarFile(getConvertedGuitarFile(m_song));
			}
		}
		
		private SongInfo m_song;
		private boolean m_haveSongFile;
		private boolean m_haveGuitarFile;
		
		private Vorbis2RawConverter m_converter;
		private boolean m_convertingGuitarFile;
		
		private boolean m_finished;
		private Exception m_finishError;
	}

	/////////////////////////////////////////////////////// skills
	
	private void initializeSkillViews() {
		for (int i=0;i!=Song.SKILL_COUNT;++i) {
			int id=SKILLPAGE_IDS[i*2];
			getSkillView(id).setCallback(this);
		}
	}
	
	private void setupSkillViews() {
		SongDB.Record songRecord=SongDB.find(m_song.getID());
		for (int i=Song.SKILL_COUNT-1;i!=-1;--i) {
			SongDB.Score score=null;
			if (songRecord!=null) {
				score=songRecord.getScore(Song.indexToSkill(i));
			}
			setupSkillView(i,score,SKILLPAGE_IDS[i*2],SKILLPAGE_IDS[i*2+1]);
		}
	}

	private void setupSkillView(int skillIndex,SongDB.Score score,int viewID,int dividerID) {
		int skill=Song.indexToSkill(skillIndex);
		if ((m_song.getSkills() & skill)!=0) {
			UIHelpers.setViewVisibility(this,dividerID,View.VISIBLE);
			PlayableSkillView skillView=getSkillView(viewID);
			skillView.setVisibility(View.VISIBLE);
			skillView.setup(skill,score);
		} else {
			UIHelpers.setViewVisibility(this,dividerID,View.GONE);
			UIHelpers.setViewVisibility(this,viewID,View.GONE);
		}
	}

	private PlayableSkillView getSkillView(int id) {
		return (PlayableSkillView)findViewById(id);
	}
	
	private void animateSkillViews(int offset) {
		int delay=UIHelpers.getInteger(
			this,
			R.integer.anim_body_delay);
		for (int i=Song.SKILL_COUNT-1;i!=-1;--i) {
			int skill=Song.indexToSkill(i);
			if ((m_song.getSkills() & skill)==0) {
				continue;
			}
			int viewID=SKILLPAGE_IDS[i*2];
			int dividerID=SKILLPAGE_IDS[i*2+1];
			UIHelpers.startViewAnimation(
				this,
				dividerID,R.anim.button_in,
				offset);
			UIHelpers.startViewAnimation(
				this,
				viewID,R.anim.button_in,
				offset);
			offset+=delay;
		}
		UIHelpers.startViewAnimation(
			this,
			R.id.lastDivider,R.anim.button_in,
			offset);
	}
	
	private static final int[] SKILLPAGE_IDS=new int[]{
		R.id.amazing,R.id.amazingDivider,
		R.id.medium,R.id.mediumDivider,
		R.id.easy,R.id.easyDivider,
		R.id.supaeasy,R.id.supaeasyDivider,
	};
	
	/////////////////////////////////////////////////////// pages
	
	private int getCurrentPage() {
		return m_pageFlipper.getDisplayedChild();
	}
	
	private void switchToPage(int page,boolean animate) {
		if (page==getCurrentPage()) {
			return;
		}
		View viewView=m_pageFlipper.getChildAt(page);
		if (viewView.getVisibility()!=View.VISIBLE) {
			viewView.setVisibility(View.VISIBLE);
			doPageAction(page,PAGEACTION_INITIALIZE);
		}
		doPageAction(getCurrentPage(),PAGEACTION_STOP);
		UIHelpers.flipToChild(m_pageFlipper,page,animate);
		doPageAction(page,PAGEACTION_START);
		
	}
	
	private void doPageAction(int view,int action) {
		switch (view) {
			case PAGE_SDCARD:
				onSDCardPageAction(action);
				break;
			case PAGE_EXTRACTOR:
				onExtractorPageAction(action);
				break;
			case PAGE_CONVERTER:
				onConverterPageAction(action);
				break;
		}
	}
	
	///////////////////////////////////////////// data
	
	private SongInfo m_song;
	private SongInfo m_originalSong;
	
	private ViewFlipper m_pageFlipper;
	private Handler m_handler=new Handler();
	
	private Extractor m_extractor;
	private boolean m_extractorStarting;
	private Runnable m_extractorPoller=new Runnable() {
		public void run() {
			pollExtractor();
		}
	};
	private Runnable m_extractorStarter=new Runnable() {
		public void run() {
			m_extractorStarting=false;
			startExtractor();
		}
	};

	private Converter m_converter;
	private boolean m_converterStarting;
	private Runnable m_converterPoller=new Runnable() {
		public void run() {
			pollConverter();
		}
	};
	private Runnable m_converterStarter=new Runnable() {
		public void run() {
			m_converterStarting=false;
			startConverter();
		}
	};
	
	/////////////////////////////////// constants
	
	private static final int 
		CONVERTER_PRIORITY		=Process.THREAD_PRIORITY_DISPLAY,
		CONVERTER_DELAY			=700;
	
	private static final int 
		PAGE_MAIN				=0,
		PAGE_SDCARD				=1,
		PAGE_EXTRACTOR			=2,
		PAGE_CONVERTER			=3;
	
	private static final int
		PAGEACTION_INITIALIZE	=0,
		PAGEACTION_START		=1,
		PAGEACTION_STOP			=2,
		PAGEACTION_PAUSE		=3,
		PAGEACTION_RESUME		=4;
}
