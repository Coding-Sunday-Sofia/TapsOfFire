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
package skiba.util;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import android.os.Process;
import android.os.SystemClock;

public class Simply {
	
	public static final int elapsedUptimeMillis(long start) {
		return (int)(SystemClock.uptimeMillis()-start);
	}
	
	public static final void setThreadPriority(int priority) {
		try {
			Process.setThreadPriority(priority);
		}
		catch (SecurityException e) {
		}
	}
	public static final void setThreadPriority(int tid,int priority) {
		try {
			Process.setThreadPriority(tid,priority);
		}
		catch (SecurityException e) {
		}
	}
	
	public static final void notify(Object object) {
		if (object!=null) {
			synchronized (object) {
				object.notify();
			}
		}
	}
	public static final void notifyAll(Object object) {
		if (object!=null) {
			synchronized (object) {
				object.notifyAll();
			}
		}
	}
	
	public static final boolean waitNoLock(Object object) {
		if (object==null) {
			return true;
		}
		try {
			object.wait();
			return true;
		}
		catch (InterruptedException e) {
			return false;
		}
	}
	public static final boolean wait(Object object) {
		if (object==null) {
			return true;
		}
		try {
			synchronized (object) {
				object.wait();
			}
			return true;
		}
		catch (InterruptedException e) {
			return false;
		}
	}
	public static final boolean wait(Object object,long timeout) {
		if (object==null) {
			return true;
		}
		try {
			synchronized (object) {
				object.wait(timeout);
			}
			return true;
		}
		catch (InterruptedException e) {
			return false;
		}
	}
	
	public static final boolean join(Thread thread) {
		if (thread==null) {
			return true;
		}
		try {
			thread.join();
			return true;
		}
		catch (InterruptedException e) {
			return false;
		}
	}
	
	public static final boolean sleep(int milliseconds) {
		try {
			Thread.sleep(milliseconds);
			return true;
		}
		catch (InterruptedException e) {
			return false;
		}
	}
	public static final boolean waitSleep(int milliseconds) {
		return wait(WAITSLEEP_OBJECT,Math.max(1,milliseconds));
	}
	
	public static final void close(Socket socket) {
		if (socket!=null) {
			try {
				socket.close();
			}
			catch (IOException e) {
			}
		}
	}	
	public static final void close(Closeable closeable) {
		if (closeable!=null) {
			try {
				closeable.close();
			}
			catch (IOException e) {
			}
		}
	}
	
	///////////////////////////////////////////// implementation
	
	private static final Object WAITSLEEP_OBJECT=new Object();
}