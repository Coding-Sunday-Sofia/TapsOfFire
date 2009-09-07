package org.tof.song;

public class TempoEvent extends Event {
	
	public TempoEvent(float bpm,float time) {
		super(time,time);
		m_bpm=bpm;
	}
	
	public float getBPM() {
		return m_bpm;
	}
	
	///////////////////////////////////////////// implementation
	
	private float m_bpm;
}
