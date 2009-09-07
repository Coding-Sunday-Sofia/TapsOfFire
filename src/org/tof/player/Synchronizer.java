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

import java.util.ArrayList;
import skiba.util.Simply;

public class Synchronizer {
	
	public Synchronizer() {
		m_handles=new ArrayList<Handle>();
		m_lock=new Object();
	}
	
	public Handle register() {
		Handle handle=new Handle(this);
		synchronized (m_lock) {
			m_handles.add(handle);
		}
		return handle;
	}
	
	/////////////////////////////////// Handle
	
	public static class Handle {
		public void unregister() {
			if (synchronizer!=null) {
				synchronizer.unregister(this);
				synchronizer=null;
			}
		}
		public boolean synchronize() {
			return (synchronizer!=null)?
				synchronizer.synchronize(this):
				true;
		}
		
		private Handle(Synchronizer synchronizer) {
			this.synchronizer=synchronizer;
		}
		private boolean synchronizing;
		private Synchronizer synchronizer;
	}
	
	///////////////////////////////////////////// implementation
	
	private void unregister(Handle handle) {
		synchronized (m_lock) {
			m_handles.remove(handle);
			if (isAllSynchronized()) {
				m_lock.notifyAll();
			}
		}
	}
	
	private boolean synchronize(Handle handle) {
		boolean ok=true;
		synchronized (m_lock) {
			handle.synchronizing=true;
			if (isAllSynchronized()) {
				m_lock.notifyAll();
			} else {
				ok=Simply.waitNoLock(m_lock);
			}
			handle.synchronizing=false;
		}
		return ok;
	}
	
	private boolean isAllSynchronized() {
		for (int i=0,e=m_handles.size();i!=e;++i) {
			if (!m_handles.get(i).synchronizing) {
				return false;
			}
		}
		return true;			
	}
	
	/////////////////////////////////// data
	
	private Object m_lock;
	private ArrayList<Handle> m_handles;
}
