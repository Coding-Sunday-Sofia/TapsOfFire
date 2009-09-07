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
package org.tof.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import skiba.util.Simply;
import android.content.Context;
import android.content.res.AssetManager;

public class AssetExtractor {
	
	public AssetExtractor(Context context,File assetPath,File extractPath) {
		m_context=context;
		m_assetPath=assetPath;
		m_extractPath=extractPath;
		m_lock=new Object();
		m_pauseEvent=new Object();
	}
	
	public Context getContext() {
		return m_context;
	}
	
	public File getAssetPath() {
		return m_assetPath;
	}
	
	public File getExtractPath() {
		return m_extractPath;
	}
	
	
	public void start() {
		stop();
		m_thread=new Thread() {
			public void run() {
				threadRun();
			}
		};
		m_finished=false;
		m_finishError=null;
		m_totalLength=0;
		m_extractedLength=0;
		m_running=true;
		m_thread.start();
	}
	
	public void stop() {
		if (m_thread==null) {
			return;
		}
		m_running=false;
		resume();
		Simply.join(m_thread);
		m_thread=null;
	}
	
	public boolean isRunning() {
		return m_running;
	}
	
	public void pause() {
		synchronized (m_pauseEvent) {
			m_paused=true;
		}
	}
	
	public void resume() {
		synchronized (m_pauseEvent) {
			m_paused=false;
			m_pauseEvent.notify();
		}
	}

	public boolean isPaused() {
		synchronized (m_pauseEvent) {
			return m_paused;
		}
	}
	
	public int getProgress() {
		synchronized (m_lock) {
			if (m_totalLength==0) {
				return 0;
			}
			return (int)(100*m_extractedLength/m_totalLength);
		}
	}
	
	public boolean isFinished() {
		synchronized (m_lock) {
			return m_finished;
		}
	}
	
	public Exception getFinishError() {
		synchronized (m_lock) {
			return m_finishError;
		}
	}
	
	public static boolean isExtracted(Context context,File assetPath,File extractPath) {
		try {
			File[] files=listAssetFiles(context,assetPath);
			for (File file: files) {
				long fileLength=getOriginalFileLength(context,file);
				File path=getExtractPath(file,extractPath);
				if (!path.exists() || path.length()!=fileLength) {
					return false;
				}
			}
			return true;
		}
		catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	///////////////////////////////////////////// implementation
	
	private void threadRun() {
		Exception error=null;
		File[] files=null;
		try {
			files=listAssetFiles(m_context,m_assetPath);
			long totalLength=0;
			for (File file: files) {
				totalLength+=getOriginalFileLength(m_context,file);
			}
			synchronized (m_lock) {
				m_totalLength=totalLength;
				m_extractedLength=0;
			}
			m_extractPath.mkdirs();
			for (File file: files) {
				extractFile(file);
				if (!m_running) {
					break;
				}
			}
		}
		catch (IOException e) {
			error=e;
		}
		if (error!=null || !m_running) {
			if (files!=null) {
				for (File file: files) {
					getExtractPath(file,m_extractPath).delete();
				}
			}
		}
		synchronized (m_lock) {
			m_finished=true;
			m_finishError=error;
		}
	}
	
	private void extractFile(File assetFile) throws IOException {
		AssetManager assets=m_context.getAssets();
		InputStream input=null;
		OutputStream output=null;
		try {
			File extractPath=getExtractPath(assetFile,m_extractPath);
			extractPath.getParentFile().mkdirs();
			output=new FileOutputStream(extractPath);
			input=assets.open(getOriginalPath(assetFile));
			byte[] buffer=getBuffer();
			while (m_running) {
				int read=input.read(buffer);
				if (read==-1) {
					return;
				}
				output.write(buffer,0,read);
				synchronized (m_lock) {
					m_extractedLength+=read;
				}
				checkPause();
			}
		}
		finally {
			Simply.close(input);
			Simply.close(output);
		}
	}
	
	private void checkPause() {
		synchronized (m_pauseEvent) {
			if (m_paused) {
				Simply.waitNoLock(m_pauseEvent);
			}
		}
	}
	
	private byte[] getBuffer() {
		if (m_buffer==null) {
			m_buffer=new byte[4*1024];
		}
		return m_buffer;
	}
	
	private static File[] listAssetFiles(Context context,File assetPath) throws IOException {
		String[] fileNames=context.getAssets().list(assetPath.getPath());
		File[] files=new File[fileNames.length];
		for (int i=0;i!=fileNames.length;++i) {
			files[i]=new File(assetPath,fileNames[i]);
		}
		return files;
	}
	
	private static String getOriginalPath(File file) {
		return file.getPath();
	}
	
	private static long getOriginalFileLength(Context context,File file) throws IOException {
		InputStream input=null;
		try {
			input=context.getAssets().open(getOriginalPath(file));
			return input.available();
		}
		finally {
			Simply.close(input);
		}
	}
	
	private static File getExtractPath(File assetPath,File extractPath) {
		return new File(extractPath,assetPath.getName());
	}

	/////////////////////////////////// data
	
	private Context m_context;
	private File m_assetPath;
	private File m_extractPath;
	
	private volatile boolean m_running;
	private Thread m_thread;

	private boolean m_paused;
	private Object m_pauseEvent;
	
	private Object m_lock;
	private long m_totalLength;
	private long m_extractedLength;
	private boolean m_finished;
	private Exception m_finishError;
	
	private byte[] m_buffer;
}
