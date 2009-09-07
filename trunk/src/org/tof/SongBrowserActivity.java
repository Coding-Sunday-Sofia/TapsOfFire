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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import org.tof.R;
import org.tof.song.InvalidSongException;
import org.tof.ui.ActivityBase;
import org.tof.ui.SongInfo;
import org.tof.ui.UIHelpers;
import org.tof.ui.UISoundEffects;
import org.tof.util.DataInputBA;
import org.tof.util.DataOutputBA;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class SongBrowserActivity extends ActivityBase implements ListAdapter, OnItemClickListener {
	
	protected void onCreate(Bundle savedState) {
		super.onCreate(savedState);
		setContentView(R.layout.song_browser);

		m_recentSongs=new ArrayList<SongInfo>();
		m_songs=new ArrayList<SongInfo>();
		{
			ListView list=(ListView)findViewById(R.id.list);
			list.setAdapter(this);
			list.setOnItemClickListener(this);
		}
		loadSongs(savedState);
	}
	
	protected void onResume() {
		super.onResume();
	}
	
	protected void onSaveInstanceState(Bundle state) {
		super.onSaveInstanceState(state);
		saveSongs(state);
	}
	
	protected boolean onBackKeyDown() {
		UISoundEffects.playOutSound();
		return false;
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
//		if (song.getName().startsWith("D")) {
//			addSorted(m_recentSongs,song);
//		}
		notifyDataSetChanged();
	}
	
	private void onLoadingFinished() {
		m_loading=false;
		notifyDataSetChanged();
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
	
	/////////////////////////////////////////////////////// loading
	
	private void saveSongs(Bundle bundle) {
		if (m_loading) {
			return;
		}
		DataOutputBA dataOut=new DataOutputBA();
		try {
			dataOut.writeInt(m_songs.size());
			for (SongInfo song: m_songs) {
				song.saveState(dataOut);
			}
			bundle.putByteArray(KEY_ACTIVITY_STATE,dataOut.toByteArray());
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private boolean restoreSongs(Bundle bundle) {
		if (bundle==null) {
			return false;
		}
		byte[] songs=bundle.getByteArray(KEY_ACTIVITY_STATE);
		if (songs==null) {
			return false;
		}
		m_recentSongs.clear();
		m_songs.clear();
		try {
			DataInputBA dataIn=new DataInputBA(songs);
			int count=dataIn.readInt();
			for (int i=0;i!=count;++i) {
				onSongLoaded(new SongInfo(dataIn));
			}
			return true;
		}
		catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	private void loadSongs(Bundle bundle) {
		if (restoreSongs(bundle)) {
			return;
		}
		m_recentSongs.clear();
		m_songs.clear();
		m_loading=true;
		m_loadingThread=new Thread() {
			public void run() {
				loadBuiltinSongs(Config.getBuiltinSongsPath());
				if (!Thread.currentThread().isInterrupted()) {
					loadSongs(Config.getSongsPath());
				}
				if (!Thread.currentThread().isInterrupted()) {
					reportLoadingFinished();
				}
			}
		};
		m_loadingThread.start();
	}
	
	private void loadBuiltinSongs(File path) {
		try {
			reportSongLoaded(new SongInfo(getAssets(),path));
		}
		catch (InvalidSongException e) {
			//
		}
		try {
			if (Thread.currentThread().isInterrupted()) {
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
			if (Thread.currentThread().isInterrupted()) {
				return;
			}
			if (!file.isDirectory()) {
				continue;
			}
			try {
				reportSongLoaded(new SongInfo(file));
			}
			catch (InvalidSongException e) {
				e.printStackTrace();
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
			onSongLoaded(m_song);
		}
		private SongInfo m_song;
	}
	
	/////////////////////////////////////////////////////// items
	
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
				return ITEM_EXTERNAL_HEADER;
			} else if (position<=m_songs.size()) {
				return ITEM_EXTERNAL;
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
				view=getLayoutInflater().inflate(R.layout.song_browser_item,null);
			}
			SongInfo song=getItem(position);
			UIHelpers.setText(view,R.id.name,song.getName());
			UIHelpers.setText(view,R.id.artist,song.getArtist());
		} else /*if (itemViewType==ITEMVIEW_HEADER)*/ {
			if (view==null) {
				view=getLayoutInflater().inflate(R.layout.song_browser_item_header,null);
			}
			switch (getItemType(position)) {
				case ITEM_RECENT_HEADER:
					UIHelpers.setText(view,R.id.name,"Recently played songs");
					break;
				case ITEM_EXTERNAL_HEADER:
					UIHelpers.setText(view,R.id.name,"All songs");
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
	
	private ArrayList<DataSetObserver> m_observers;
	
	private static final int
		ITEMVIEW_HEADER			=0x0000,
		ITEMVIEW_ITEM			=0x0001,
		ITEM_ITEMVIEW_MASK		=0x00FF,
		ITEM_ITEMVIEW_COUNT		=2,
		
		ITEM_UNKNOWN			=0xFF00,
		ITEM_RECENT_HEADER		=0x0000 | ITEMVIEW_HEADER,
		ITEM_RECENT				=0x0100 | ITEMVIEW_ITEM,
		ITEM_EXTERNAL_HEADER	=0x0200 | ITEMVIEW_HEADER,
		ITEM_EXTERNAL			=0x0300 | ITEMVIEW_ITEM;
	
	/////////////////////////////////////////////////////// ui
	
	public boolean onKeyDown(int keyCode,KeyEvent event) {
		if (keyCode==KeyEvent.KEYCODE_BACK && event.getRepeatCount()==0) {
			UISoundEffects.playOutSound();
		}
		return super.onKeyDown(keyCode,event);
	}
	
	/////////////////////////////////////////////////////// data

	private ArrayList<SongInfo> m_recentSongs;
	private ArrayList<SongInfo> m_songs;
	
	private Thread m_loadingThread;
	private boolean m_loading=false;
}
