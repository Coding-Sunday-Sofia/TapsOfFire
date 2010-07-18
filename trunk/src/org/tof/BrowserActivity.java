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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import org.tof.song.InvalidSongException;
import org.tof.song.SongInfo;
import org.tof.ui.ActivityBase;
import org.tof.ui.UIHelpers;
import org.tof.util.DataOutputBA;
import skiba.util.Simply;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class BrowserActivity extends ActivityBase implements ListAdapter, OnItemClickListener {
	
	protected void onCreate(Bundle savedState) {
		super.onCreate(savedState);
		setContentView(R.layout.browser);
		usePageFlipper(savedState);
		
		m_recentSongs=new ArrayList<SongInfo>();
		m_songs=new ArrayList<SongInfo>();
		
		if (obtainSongsCookie()) {
			if (!restoreSongs()) {
				flipToPage(PAGE_LOADER,true);
				startLoading();
			}
		} else {
			startLoading();
		}
		
		ListView list=(ListView)findViewById(R.id.list);
		list.setAdapter(this);
		list.setOnItemClickListener(this);
	}
	
	protected void onPause() {
		super.onPause();
		pauseLoading();
	}
	
	protected void onResume() {
		super.onResume();
		resumeLoading();
	}
	
	protected void onDestroy() {
		super.onDestroy();
		stopLoading(true);
	}
	
	protected View onCreateMenuView() {
		if (getCurrentPage()!=PAGE_MAIN) {
			return null;
		}
		return getLayoutInflater().inflate(R.layout.menu_browser,null);
	}
	
	public void onMenuItemClick(int id) {
		if (id==R.id.scan) {
			flipToPage(PAGE_LOADER,true);
			obtainSongsCookie();
			startLoading();
			return;
		}
	}
	
	protected boolean onBackToMainPage() {
		return true;
	}
	
	/////////////////////////////////////////////////////// logic
	
	public void onItemClick(AdapterView<?> parent,View view,int position,long id) {
		SongInfo song=getItem(position);
		Intent intent=new Intent(this,SelectSkillActivity.class);
		try {
			DataOutputBA dataOut=new DataOutputBA();
			song.saveState(dataOut);
			intent.putExtra(SongInfo.BUNDLE_KEY,dataOut.toByteArray());
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		startActivity(intent);
	}
	
	private void onSongLoaded(SongInfo song) {
		addSorted(m_songs,song);
		notifyDataSetChanged();
	}
	
	private void onLoadingFinished() {
		saveSongs();
		flipToPage(PAGE_MAIN,true);
	}
	
	/////////////////////////////////////////////////////// songs
	
	private void saveSongs() {
		try {
			OutputStream stream=openFileOutput(SONGS_FILE,0);
			DataOutputStream dataOut=new DataOutputStream(stream);
			dataOut.writeLong(m_songsCookie);
			dataOut.writeInt(m_songs.size());
			for (int i=0,e=m_songs.size();i!=e;++i) {
				m_songs.get(i).saveState(dataOut);
			}
			dataOut.flush();
			stream.flush();
			stream.close();
		}
		catch (IOException e) {
			//
		}
	}
	
	private boolean obtainSongsCookie() {
		m_songsCookie=0;
		File path=Config.getSongsPath();
		if (path.exists()) {
			String[] files=path.list();
			if (files!=null && files.length!=0) {
				m_songsCookie=path.lastModified()*files.length;
				return true;
			}
		}
		return false;
	}
	
	private boolean restoreSongs() {
		try {
			InputStream stream=openFileInput(SONGS_FILE);
			DataInputStream dataIn=new DataInputStream(stream);
			long cacheTime=dataIn.readLong();
			if (m_songsCookie!=cacheTime) {
				return false;
			}
			int count=dataIn.readInt();
			for (int i=0;i!=count;++i) {
				onSongLoaded(new SongInfo(dataIn));				
			}
			return true;
		}
		catch (IOException e) {
			return false;
		}
	}
	
	///////////////////////////////////////////////////////////////// loading
	
	private void startLoading() {
		stopLoading(true);
		resetLoadingProgress();
		m_recentSongs.clear();
		m_songs.clear();
		notifyDataSetChanged();
		
		m_stopLoading=false;
		m_loadingPaused=false;
		m_loadingThread=new Thread() {
			public void run() {
				loadBuiltinSongs(Config.getBuiltinSongsPath());
				if (!checkLoadingStopped()) {
					loadSongs(Config.getSongsPath());
				}
				if (!checkLoadingStopped()) {
					System.gc();
//					int delay=UIHelpers.getInteger(
//						BrowserActivity.this,
//						R.integer.comprehension_delay);
//					Simply.wait(m_loadingPauseEvent,delay);
					reportLoadingFinished();
				}
			}
		};
		m_loadingThread.start();
	}
	
	private void stopLoading(boolean join) {
		m_stopLoading=true;
		resumeLoading();
		if (join) {
			Simply.join(m_loadingThread);
		}
	}
	
	private void pauseLoading() {
		synchronized (m_loadingPauseEvent) {
			m_loadingPaused=true;
		}
	}
	
	private void resumeLoading() {
		synchronized (m_loadingPauseEvent) {
			m_loadingPaused=false;
			Simply.notify(m_loadingPauseEvent);
		}
	}
	
	private boolean checkLoadingStopped() {
		synchronized (m_loadingPauseEvent) {
			if (m_loadingPaused) {
				Simply.waitNoLock(m_loadingPauseEvent);
			}
		}
		return m_stopLoading;
	}
	
	private void loadBuiltinSongs(File path) {
		try {
			reportSongLoaded(new SongInfo(getAssets(),path));
		}
		catch (InvalidSongException e) {
			//
		}
		try {
			if (checkLoadingStopped()) {
				return;
			}
			String[] fileNames=getAssets().list(path.getPath());
			if (fileNames==null) {
				return;
			}
			for (String fileName: fileNames) {
				loadBuiltinSongs(new File(path,fileName));
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void loadSongs(File path) {
		if (!path.exists()) {
			return;
		}
		File[] files=path.listFiles();
		if (files==null) {
			return;
		}
		for (File file: files) {
			if (checkLoadingStopped()) {
				return;
			}
			if (!file.isDirectory()) {
				continue;
			}
			try {
				reportSongLoaded(new SongInfo(file));
			}
			catch (InvalidSongException e) {
				//
			}
			loadSongs(file);
		}
	}
	
	private void reportSongLoaded(SongInfo song) {
		runOnUiThread(new SongLoadedRunnable(song));
	}
	
	private void reportLoadingFinished() {
		runOnUiThread(new Runnable() {
			public void run() {
				onLoadingFinished();
			}
		});
	}
	
	private class SongLoadedRunnable implements Runnable {
		public SongLoadedRunnable(SongInfo song) {
			m_song=song;
		}
		public void run() {
			updateLoadingProgress(m_song);
			onSongLoaded(m_song);
		}
		private SongInfo m_song;
	}
	
	private void updateLoadingProgress(SongInfo song) {
		m_songsLoaded++;
		reportSongsLoaded();
		if (m_songTraceViews!=null) {
			String trace=getString(
				R.string.song_trace_fmt,
				song.getName(),song.getArtist());
			for (int i=m_songTraceViews.length-1;i>0;--i) {
				m_songTraceViews[i].setText(m_songTraceViews[i-1].getText());
			}
			m_songTraceViews[0].setText(trace);
		}
	}

	private void resetLoadingProgress() {
		m_songsLoaded=0;
		reportSongsLoaded();
		if (m_songTraceViews!=null) {
			for (TextView view: m_songTraceViews) {
				view.setText(null);
			}
		}
	}
	
	private void reportSongsLoaded() {
		if (m_songsLoadedView!=null) {
			m_songsLoadedView.setText(
				getString(R.string.songs_loaded_fmt,m_songsLoaded)
			);
		}
	}
	
	protected void doPageAction(int page,int action) {
		if (page==PAGE_LOADER && action==PAGEACTION_INITIALIZE) {
			m_songsLoadedView=(TextView)findViewById(R.id.songs_loaded);
			m_songTraceViews=new TextView[4];
			m_songTraceViews[0]=(TextView)findViewById(R.id.song_0);
			m_songTraceViews[1]=(TextView)findViewById(R.id.song_1);
			m_songTraceViews[2]=(TextView)findViewById(R.id.song_2);
			m_songTraceViews[3]=(TextView)findViewById(R.id.song_3);
		}
	}
	
	///////////////////////////////////////////////////////////////// items
	
	public SongInfo getItem(int position) {
		if (m_recentSongs.size()!=0) {
			if (position==0) {
				return null;
			} else if (position<=m_recentSongs.size()) {
				return m_recentSongs.get(position-1);
			} else {
				position-=(1+m_recentSongs.size());
			}
		}
		if (m_songs.size()!=0) {
			if (position==0) {
				return null;
			} else if (position<=m_songs.size()) {
				return m_songs.get(position-1);
			} else {
				position-=(1+m_songs.size());
			}
		}
		return null;
	}
	
	private int getItemType(int position) {
		if (m_recentSongs.size()!=0) {
			if (position==0) {
				return ITEM_RECENT_HEADER;
			} else if (position<=m_recentSongs.size()) {
				return ITEM_RECENT;
			} else {
				position-=(1+m_recentSongs.size());
			}
		}
		if (m_songs.size()!=0) {
			if (position==0) {
				return ITEM_ALL_HEADER;
			} else if (position<=m_songs.size()) {
				return ITEM_ALL;
			} else {
				position-=(1+m_songs.size());
			}
		}
		return ITEM_UNKNOWN;
	}
	
	public boolean areAllItemsEnabled() {
		return false;
	}

	public boolean isEnabled(int position) {
		return getItemViewType(position)==ITEMVIEW_ITEM; 
	}

	public int getCount() {
		int count=0;
		if (m_recentSongs.size()!=0) {
			count+=(1+m_recentSongs.size());
		}
		if (m_songs.size()!=0) {
			count+=(1+m_songs.size());
		}
		return count;		
	}

	public long getItemId(int position) {
		return position;
	}

	public int getItemViewType(int position) {
		return (ITEM_ITEMVIEW_MASK & getItemType(position));
	}

	public int getViewTypeCount() {
		return ITEM_ITEMVIEW_COUNT;
	}
	
	public boolean hasStableIds() {
		return false;
	}

	public boolean isEmpty() {
		return getCount()==0;
	}

	public void registerDataSetObserver(DataSetObserver observer) {
		if (m_observers==null) {
			m_observers=new ArrayList<DataSetObserver>();
		}
		m_observers.add(observer);
	}

	public void unregisterDataSetObserver(DataSetObserver observer) {
		if (m_observers!=null) {
			m_observers.remove(observer);
		}
	}

	public View getView(int position,View view,ViewGroup parent) {
		int itemViewType=getItemViewType(position);
		if (itemViewType==ITEMVIEW_ITEM) {
			if (view==null) {
				view=getLayoutInflater().inflate(R.layout.browser_item,null);
			}
			SongInfo song=getItem(position);
			UIHelpers.setText(view,R.id.name,song.getName());
			UIHelpers.setText(view,R.id.artist,song.getArtist());
		} else /*if (itemViewType==ITEMVIEW_HEADER)*/ {
			if (view==null) {
				view=getLayoutInflater().inflate(R.layout.browser_item_header,null);
			}
			switch (getItemType(position)) {
				case ITEM_RECENT_HEADER:
					UIHelpers.setText(view,R.id.name,getString(R.string.recent_songs));
					break;
				case ITEM_ALL_HEADER:
					UIHelpers.setText(view,R.id.name,getString(R.string.all_songs));
					break;
			}
		}
		return view;
	}
	
	private void notifyDataSetChanged() {
		if (m_observers!=null) {
			for (DataSetObserver observer: m_observers) {
				observer.onChanged();
			}
		}
	}
	
	/////////////////////////////////////////////////////// sorting

	private static void addSorted(ArrayList<SongInfo> songs,SongInfo song) {
		int i=Collections.binarySearch(songs,song,SONG_COMPARATOR);
		if (i<0) {
			i=-i-2;
		}
		songs.add(i+1,song);
	}
	
	private static class SongComparator implements Comparator<SongInfo> {
		public int compare(SongInfo x,SongInfo y) { 
			return x.getName().compareTo(y.getName());
		}
		public boolean equals(SongComparator other) {
			return true;
		}
	}
	private static final SongComparator SONG_COMPARATOR=new SongComparator();
	
	///////////////////////////////////////////////////////////////// data

	private ArrayList<DataSetObserver> m_observers;
	
	private ArrayList<SongInfo> m_recentSongs;
	private ArrayList<SongInfo> m_songs;
	private long m_songsCookie;
	
	private Thread m_loadingThread;
	private volatile boolean m_stopLoading;
	private boolean m_loadingPaused;
	private Object m_loadingPauseEvent=new Object();
	
	private int m_songsLoaded;
	private TextView m_songsLoadedView;
	private TextView[] m_songTraceViews;
	
	/////////////////////////////////// constants
	
	private static final int
		PAGE_LOADER				=1;
	
	private static final int
		ITEMVIEW_HEADER			=0x0000,
		ITEMVIEW_ITEM			=0x0001,
		ITEM_ITEMVIEW_MASK		=0x00FF,
		ITEM_ITEMVIEW_COUNT		=2,
		
		ITEM_UNKNOWN			=0xFF00,
		ITEM_RECENT_HEADER		=0x0000 | ITEMVIEW_HEADER,
		ITEM_RECENT				=0x0100 | ITEMVIEW_ITEM,
		ITEM_ALL_HEADER			=0x0200 | ITEMVIEW_HEADER,
		ITEM_ALL				=0x0300 | ITEMVIEW_ITEM;
	
	private static final String SONGS_FILE="browser.cache";
}
