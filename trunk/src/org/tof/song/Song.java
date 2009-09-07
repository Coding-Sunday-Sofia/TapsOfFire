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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import org.tof.midi.InvalidMidiDataException;
import org.tof.midi.MidiCallbackHelper;
import org.tof.midi.MidiHeader;
import org.tof.midi.MidiReader;

public class Song {
	
	@SuppressWarnings("unchecked")
	public Song(SongConfig config) throws InvalidSongException {
		m_config=config;
		m_tempoEvents=new EventListBuilder<TempoEvent>();
		m_noteEvents=(EventListBuilder<NoteEvent>[][])
			new EventListBuilder[SKILL_COUNT][];
		for (int i=0;i!=m_noteEvents.length;++i) {
			EventListBuilder<NoteEvent>[] events=
				(EventListBuilder<NoteEvent>[])
				new EventListBuilder[ACTUAL_STRING_COUNT];
			for (int j=0;j!=ACTUAL_STRING_COUNT;++j) {
				events[j]=new EventListBuilder<NoteEvent>();
			}
			m_noteEvents[i]=events;
		}
		m_selectedSkillIndex=-1;
		try {
			MidiReader.read(
				new MidiCallback(this),
				new FileInputStream(config.getNotesFile()));
		}
		catch (InvalidMidiDataException e) {
			throw new InvalidSongException(e);
		}
		catch (IOException e) {
			throw new InvalidSongException(e);
		}
		mergeNoteEvents();
	}
	
	public SongConfig getConfig() {
		return m_config;
	}
	
	public SongIni getIni() {
		return m_config.getIni();
	}

	public int getSelectedSkill() {
		return indexToSkill(m_selectedSkillIndex);
	}
	
	public boolean selectSkill(int skill) {
		if ((skill & getIni().getSkills())==0) {
			return false;
		}
		m_selectedSkillIndex=skillToIndex(skill);
		return (m_selectedSkillIndex!=-1); 
	}
	
	public void selectAnySkill() {
        if (selectSkill(Song.SKILL_SUPAEASY) ||
            selectSkill(Song.SKILL_EASY) ||
            selectSkill(Song.SKILL_MEDIUM) ||
            selectSkill(Song.SKILL_AMAZING));
	}
	
	public EventList<TempoEvent> getTempoEvents() {
		return m_tempoEvents;
	}
	
	public void glueNoteEvents(float minDistance) {
		checkSkillSelected();
		EventListBuilder<NoteEvent> events;
		for (int string=0;string!=Song.STRING_COUNT;++string) {
			events=m_noteEvents[m_selectedSkillIndex][string];
			int eventsRemoved=0;
			for (int i=0;;) {
				if (i>events.count()-2) {
					break;
				}
				NoteEvent event=events.get(i);
				NoteEvent nextEvent=events.get(i+1);
				float distance=(nextEvent.getTime()-event.getTime());
				if (distance>minDistance) {
					++i;
					continue;
				}
				NoteEvent newEvent=new NoteEvent(
					string,
					event.getTime(),
					nextEvent.getTime());
				events.remove(i,2);
				eventsRemoved++;
				events.add(newEvent);
			}
		}
	}
	
	public EventList<NoteEvent> getNoteEvents(int string) {
		checkSkillSelected();
		if (string<0 || string>=STRING_COUNT) {
			throw new IllegalArgumentException(
				String.format("Invalid string index %d.",string)
			);
		}
		return m_noteEvents[m_selectedSkillIndex][string];
	}
	
	public int getTotalNoteEventCount() {
		checkSkillSelected();
		int count=0;
		for (int string=0;string!=Song.STRING_COUNT;++string) {
			count+=m_noteEvents[m_selectedSkillIndex][string].count();
		}
		return count;
	}
	
	/////////////////////////////////// misc
	
	public static int skillToIndex(int skill) {
		if (skill==SKILL_AMAZING) {
			return 0;
		} else if (skill==SKILL_MEDIUM) {
			return 1;
		} else if (skill==SKILL_EASY) {
			return 2;
		} else if (skill==SKILL_SUPAEASY) {
			return 3;
		} else {
			return -1;
		}
	}
	
	public static int indexToSkill(int skillIndex) {
		return 1<<skillIndex;
	}
	
	/////////////////////////////////// constants
	
	public static final int INVALID_SKILL	=0;
	public static final int SKILL_AMAZING	=0x1;
	public static final int SKILL_MEDIUM	=0x2;
	public static final int SKILL_EASY		=0x4;
	public static final int SKILL_SUPAEASY	=0x8;
	
	public static final int SKILL_COUNT		=4;
	public static final int ALL_SKILLS		=(1<<SKILL_COUNT)-1;
	
	public static final int STRING_COUNT	=3;
	
	///////////////////////////////////////////// implementation
	
	private void addTempoEvent(TempoEvent event) {
		m_tempoEvents.add(event);
	}
	
	private void addNoteEvent(int note,float time,float endTime) {
		int noteIndex=NoteMap.indexOf(note);
		if (noteIndex==-1) {
			return;
		}
		int skillIndex=skillToIndex(NoteMap.indexToSkill(noteIndex));
		EventListBuilder<NoteEvent>[] events=m_noteEvents[skillIndex];
		events[NoteMap.indexToString(noteIndex)].add(
			new NoteEvent(NoteMap.indexToString(noteIndex),time,endTime)
		);
	}
	
	/////////////////////////////////// merging
	
	private void mergeNoteEvents() {
		for (int i=0;i!=SKILL_COUNT;++i) {
			if (m_noteEvents[i]!=null) {
				mergeNoteEvents(m_noteEvents[i],m_noteEvents[i][3]);
				mergeNoteEvents(m_noteEvents[i],m_noteEvents[i][4]);
			}
		}
	}
	
	private void mergeNoteEvents(
			EventListBuilder<NoteEvent>[] stringEvents,
			EventListBuilder<NoteEvent> events)
	{
		long[] stringWeights=new long[STRING_COUNT];
		for (int i=0;i!=STRING_COUNT;++i) {
			long count=stringEvents[i].count();
			stringWeights[i]=(count<<32) | i;
		}
		Arrays.sort(stringWeights);
		for (int i=0;i!=events.count();++i) {
			NoteEvent note=events.get(i);
			int index=0;
			for (int j=0;j!=STRING_COUNT;++j) {
				int string=(int)stringWeights[j];
				index=stringEvents[string].add(note);
				if (index>=0) {
					note.setString(string);
					break;
				}
			}
			if (index<0) {
				int string=(int)stringWeights[STRING_COUNT-1];
				mergeNoteEvent(
					stringEvents[string],
					-index-1,
					string,note.getTime(),note.getEndTime());
			}
		}
	}

	private static void mergeNoteEvent(
			EventListBuilder<NoteEvent> events,
			int fromIndex,
			int string,float time,float endTime)
	{
		for (;fromIndex!=0;--fromIndex) {
			NoteEvent mergeEvent=events.get(fromIndex-1);
			if (mergeEvent.getEndTime()<(time-MERGE_MARGIN)) {
				break;
			}
		}
		int toIndex=fromIndex;
		for (;toIndex!=events.count();++toIndex) {
			NoteEvent mergeEvent=events.get(toIndex);
			if (!((endTime+MERGE_MARGIN)>mergeEvent.getTime())) {
				break;
			}
			time=Math.min(time,mergeEvent.getTime());
			endTime=Math.max(endTime,mergeEvent.getEndTime());
		}
		events.remove(fromIndex,toIndex-fromIndex);
		events.add(new NoteEvent(string,time,endTime));
	}
	
	/////////////////////////////////// misc 

	private void checkSkillSelected() {
		if (m_selectedSkillIndex==-1) {
			throw new IllegalStateException("Skill is not selected.");
		}
	}
	
	/////////////////////////////////// data
	
	private SongConfig m_config;
	
	private int m_selectedSkillIndex;
	private EventListBuilder<TempoEvent> m_tempoEvents;
	private EventListBuilder<NoteEvent>[][] m_noteEvents;
		
	private static final int ACTUAL_STRING_COUNT=5;
	private static final float MERGE_MARGIN=10f;
	
	///////////////////////////////////////////////////////////////// MidiCallback
	
	private static class MidiCallback extends MidiCallbackHelper {
		public MidiCallback(Song song) {
			m_song=song;
			m_noteStartTimes=new float[(MAX_CHANNEL+1)*NoteMap.SIZE];
			Arrays.fill(m_noteStartTimes,-1);
			m_tempos=new int[2*10];
			m_tempoSyncIndex=0;
			m_tempoCount=0;
		}
		
		public void onStart(MidiHeader header) {
			m_ticksPerBeat=header.resolution;
		}
		public void onTrackStart(int track) {
			m_currentTrack=track;
			m_currentTicks=0;
			m_lastBPM=0;
			m_lastBPMTicks=0;
			m_lastBPMMillis=0;
			m_tempoSyncIndex=0;
		}
		
		public void onEventDeltaTime(int deltaTime) {
			m_currentTicks+=deltaTime;
			syncTempo();
		}
		
		public void noteOnOff(boolean on,int channel,int note,int velocity)
			throws InvalidMidiDataException
		{
			if (m_currentTrack>1) {
				return;
			}
			int index=NoteMap.indexOf(note);
			if (index==-1) {
				return;
			}
			index+=(channel+1)*NoteMap.SIZE;
			if (on) {
				m_noteStartTimes[index]=getCurrentMillis();
			} else {
				float startMillis=m_noteStartTimes[index];
				{
					if (startMillis==-1) {
						return;
					}
					m_noteStartTimes[index]=-1;
				}
				m_song.addNoteEvent(note,startMillis,getCurrentMillis());
			}
		}
		
		public void tempo(int value)
			throws InvalidMidiDataException
		{
			addTempo(m_currentTicks,value);
			applyTempo(m_currentTicks,value);
			m_song.addTempoEvent(new TempoEvent(m_lastBPM,m_lastBPMMillis));
		}
		
		/////////////////////////////// misc
		
		private void syncTempo() {
			for (;m_tempoSyncIndex!=m_tempoCount;++m_tempoSyncIndex) {
				int tempoTicks=m_tempos[m_tempoSyncIndex*2];
				int tempoValue=m_tempos[m_tempoSyncIndex*2+1];
				if (tempoTicks>m_currentTicks) {
					break;
				}
				applyTempo(tempoTicks,tempoValue);
			}
		}

		private void applyTempo(int tempoTicks,int tempoValue) {
			float bpm=60000000f/tempoValue;
			float currentMillis=m_lastBPMMillis+
				ticksToMillis(tempoTicks-m_lastBPMTicks,m_lastBPM);
			m_lastBPMMillis=currentMillis;
			m_lastBPM=bpm;
			m_lastBPMTicks=tempoTicks;
		}
		
		private void addTempo(int tempoTicks,int tempoValue)
			throws InvalidMidiDataException
		{
			if (m_tempoSyncIndex!=m_tempoCount) {
				throw new InvalidMidiDataException(String.format(
					"Unexpected tempo event at [%d]; "+
					"current tempo sync index is %d [%d] (out of %d).",
					tempoTicks,
					m_tempoSyncIndex,m_tempos[m_tempoSyncIndex*2],
					m_tempoCount));
			}
			int index=m_tempoCount*2;
			if (index==m_tempos.length) {
				int[] copy=new int[index*2];
				System.arraycopy(m_tempos,0,copy,0,m_tempos.length);
				m_tempos=copy;
			}
			m_tempoCount+=1;
			m_tempoSyncIndex+=1;
			m_tempos[index]=tempoTicks;
			m_tempos[index+1]=tempoValue;
		}
		
		private float getCurrentMillis() {
			return m_lastBPMMillis+
				ticksToMillis(m_currentTicks-m_lastBPMTicks,m_lastBPM);
		}
		
		private float ticksToMillis(int ticks,float bpm) {
			if (bpm==0 || m_ticksPerBeat==0) {
				return 0;
			}
			return (60000f*ticks)/(bpm*m_ticksPerBeat);
		}
		
		/////////////////////////////// data
		
		private Song m_song;
		
		private int m_ticksPerBeat;
		private int m_currentTicks;
		private int m_currentTrack;
		
		private float m_lastBPM;
		private int m_lastBPMTicks;
		private float m_lastBPMMillis;

		private int[] m_tempos;
		private int m_tempoSyncIndex;
		private int m_tempoCount;
		
		private float[] m_noteStartTimes;
		
		private static final int MAX_CHANNEL=15;
	}
}
