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
package org.tof.song;

@SuppressWarnings("unchecked")
public class EventListBuilder<E extends Event> extends EventList<E> {
	
	public EventListBuilder() {
		m_events=new Event[GROW_LENGTH];
		m_eventCount=0;
	}
	
	public int count() {
		return m_eventCount;
	}
	
	public E get(int index) {
		return (E)m_events[index];
	}
	
	public void remove(int index,int count) {
		if (index<0 || index>=m_eventCount || count<=0) {
			return;
		}
		int lastIndex=Math.min(index+count,m_eventCount);
		System.arraycopy(m_events,lastIndex,m_events,index,m_eventCount-lastIndex);
		m_eventCount-=(lastIndex-index);
	}
	
	public int add(E event) {
		float time=event.getTime();
		float endTime=event.getEndTime();
		if (isInvalid(time) || isInvalid(endTime) || time>endTime) {
			throw new IllegalArgumentException();
		}
		int low=lowerUpperBound(true,m_events,m_eventCount,time);
		if (low!=-1) {
			Event lowEvent=m_events[low];
			if (lowEvent.getEndTime()>time) {
				return -low-1;
			}
		}
		int high=low+1;
		if (high!=m_eventCount) {
			Event highEvent=m_events[high];
			if (time==highEvent.getTime() ||
				endTime>highEvent.getTime())
			{
				return -high-1;
			}
		}
		if (m_eventCount==m_events.length) {
			Event[] events=new Event[m_eventCount+GROW_LENGTH];
			System.arraycopy(m_events,0,events,0,m_eventCount);
			m_events=events;
		}
		System.arraycopy(m_events,high,m_events,high+1,m_eventCount-high);
		m_events[high]=event;
		m_eventCount++;
		return high;
	}
	
	public int lowerBound(float time) {
		return lowerUpperBound(true,m_events,m_eventCount,time);
	}

	public int upperBound(float time) {
		return lowerUpperBound(false,m_events,m_eventCount,time);
	}
	
	public long range(float time,float endTime) {
		if (Float.isNaN(time) || Float.isNaN(endTime)) {
			throw new IllegalArgumentException();
		}
		if (time>endTime) {
			return 0;
		}
		int i=lowerUpperBound(true,m_events,m_eventCount,time);
		if (i==-1) {
			i=0;
		} else {
			for (;i!=m_eventCount;++i) {
				if (m_events[i].getEndTime()>time) {
					break;
				}
			}
		}
		int j=i;
		for (;j!=m_eventCount;++j) {
			if (m_events[j].getTime()>=endTime) {
				break;
			}
		}
		return rangeMake(i,j);
	}

	/////////////////////////////////// Iterator
	
	public interface Iterator<E extends Event> extends 
		java.util.Iterator<E>,
		java.lang.Iterable<E>
	{
	}
	
	///////////////////////////////////////////// implementation
	
    private static int lowerUpperBound(boolean lower,Event[] events,int eventCount,float time) {
		int high=eventCount;
		int low=-1;
		while (high-low>1) {
			int guess=(high+low)/2;
			if (events[guess].getTime()<time) {
				low=guess;
			} else {
				high=guess;
			}
		}
		return lower?low:high;
	}
    
    private static boolean isInvalid(float value) {
    	return Float.isNaN(value) || Float.isInfinite(value);
    }
	
	/////////////////////////////////// data
    
	private Event[] m_events;
	private int m_eventCount;
	
	private static final int GROW_LENGTH=32;
}
