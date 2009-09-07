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

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import org.tof.Config;
import org.tof.util.MiscHelpers;

public class SongCache {

	public static File push(int songID) {
		File songPath=getPath(songID);
		if (!songPath.exists()) {
			pop();
			songPath.mkdirs();
		}
		return songPath;
	}
	
	public static void remove(int songID) {
		File songPath=getPath(songID);
		MiscHelpers.cleanup(songPath);
		songPath.delete();
	}
	
	public static File find(int songID) {
		File songPath=getPath(songID);
		if (songPath.exists()) {
			return songPath;
		} else {
			return null;
		}
	}
	
	public static void touch(int songID) {
		File songPath=getPath(songID);
		if (songPath.exists()) {
			songPath.setLastModified(new Date().getTime());
		}
	}
	
	public static void removeAll() {
		MiscHelpers.cleanup(Config.getSongCachePath());
	}

	public static File getPath(int songID) {
		return new File(Config.getSongCachePath(),String.format("%08X",songID));
	}
	
	///////////////////////////////////////////// implementation
	
	private static class OldestFilesFirst implements Comparator<File> {
		private OldestFilesFirst() {
		}
		public int compare(File x,File y) {
			long xModified=x.lastModified();
			long yModified=y.lastModified();
			if (xModified==yModified) {
				return 0;
			} else if (xModified>yModified) {
				return +1;
			} else {
				return -1;
			}
		}
		public static final OldestFilesFirst INSTANCE=
			new OldestFilesFirst();
	}

	private static void pop() {
		File path=Config.getSongCachePath();
		File[] files=path.listFiles();
		if (files==null) {
			return;
		}
		Arrays.sort(files,OldestFilesFirst.INSTANCE);
		{
			int deleteCount=files.length+1-
				Math.max(1,Config.getSongCacheLength());
			for (int i=0;i<deleteCount;++i) {
				File file=files[i];
				MiscHelpers.cleanup(file);
				file.delete();
			}
		}
	}
	
}
