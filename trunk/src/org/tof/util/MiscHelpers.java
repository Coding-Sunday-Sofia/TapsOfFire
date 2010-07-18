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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import android.net.Uri;

public class MiscHelpers {

	public static final File UriToFile(Uri uri) {
		try {
			return new File(new URI(uri.toString()));
		}
		catch (URISyntaxException e) {
			return new File(uri.toString());
		}
	}
	
	public static final void cleanup(File path) {
		File[] files=path.listFiles();
		if (files!=null) {
			for (File file: files) {
				if (file.isDirectory()) {
					cleanup(file);
				}
				file.delete();
			}
		}
	}
}
