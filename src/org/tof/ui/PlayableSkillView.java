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
package org.tof.ui;

import org.tof.R;
import org.tof.song.Song;
import org.tof.songdb.SongDB;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

public class PlayableSkillView extends LinearLayout implements OnClickListener {

	public PlayableSkillView(Context context) {
		super(context);
	}
	public PlayableSkillView(Context context,AttributeSet attributes) {
		super(context,attributes);
	}
	
	public void setup(int skill,SongDB.Score score) {
		m_skill=skill;
		m_playButton.setText(getSkillName(skill));
		if (score==null) {
			m_notPlayed.setVisibility(View.VISIBLE);
			m_score.setVisibility(View.INVISIBLE);
			m_rating.setRating(0);
		} else {
			m_notPlayed.setVisibility(View.INVISIBLE);
			m_score.setVisibility(View.VISIBLE);
			m_score.setText(String.valueOf(score.score));
			m_rating.setRating(score.rating*m_rating.getNumStars());
		}
	}
	
	/////////////////////////////////// Callback
	
	public interface Callback {
		public void onPlaySkill(int skill);
	}
	
	public void setCallback(Callback callback) {
		m_callback=callback;
	}
	
	///////////////////////////////////////////// implementation
	
	protected void onFinishInflate() {
		super.onFinishInflate();
		m_playButton=(Button)findViewById(R.id.play);
		m_playButton.setOnClickListener(this);
		
		m_notPlayed=(TextView)findViewById(R.id.notPlayed);
		m_score=(TextView)findViewById(R.id.score);
		m_rating=(RatingBar)findViewById(R.id.rating);
	}
	
	public void onClick(View view) {
		if (view==m_playButton) {
			if (m_callback!=null) {
				m_callback.onPlaySkill(m_skill);
			}
		}
	}
	
	private static String getSkillName(int skill) {
		switch (skill) {
			case Song.SKILL_SUPAEASY:	return "Supaeasy";
			case Song.SKILL_EASY:		return "Easy";
			case Song.SKILL_MEDIUM:		return "Medium";
			case Song.SKILL_AMAZING:	return "Amazing";
			default:					return "<Unknown>";
		}
	}
	
	/////////////////////////////////// data
	
	private int m_skill;
	private Callback m_callback;

	private Button m_playButton;
	private TextView m_notPlayed;
	private TextView m_score;
	private RatingBar m_rating;
}
