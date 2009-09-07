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

import java.io.IOException;
import org.tof.R;
import org.tof.ui.ActivityBase;
import org.tof.ui.FinishedSongInfo;
import org.tof.ui.UIHelpers;
import org.tof.util.DataInputBA;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;

public class SongFinishedActivity extends ActivityBase {
	
	protected void onCreate(Bundle savedState) {
		super.onCreate(savedState);
		
//		try {
//			SongInfo song=new SongInfo(new File("/sdcard/TapsOfFire/songs/defy"));
//			m_info=new FinishedSongInfo(song);
//			m_info.setAccuracy(0.3f);
//			m_info.setScore(121223);
//			m_info.setLongestStreak(12);
//		}
//		catch (InvalidSongException e) {
//			throw new RuntimeException(e);
//		}

		try {
			byte[] state=getIntent().getByteArrayExtra(FinishedSongInfo.BUNDLE_KEY);
			m_info=new FinishedSongInfo(new DataInputBA(state));
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		setContentView(R.layout.final_score);
		m_scoreView=(TextView)findViewById(R.id.score);
		m_rating=(RatingBar)findViewById(R.id.rating);
	}
	
	protected void onResume() {
		super.onResume();
		UIHelpers.startViewAnimation(this,R.id.head,R.anim.body_alpha);
		UIHelpers.startViewAnimation(this,R.id.score,R.anim.body_alpha);
		UIHelpers.startViewAnimation(this,R.id.rating,R.anim.body_alpha);

		UIHelpers.setViewVisibility(this,R.id.accuracy,View.INVISIBLE);
		UIHelpers.setViewVisibility(this,R.id.streak,View.INVISIBLE);

		animateScore();
	}
	
	///////////////////////////////////////////// animation
	
	private void animateScore() {
		m_currentScore=0;
		m_scoreIncrement=7;
		stepScoreAnimation();
	}
	
	private void stepScoreAnimation() {
		m_scoreView.setText(String.valueOf(m_currentScore));
		m_rating.setRating(m_rating.getNumStars()*
			m_info.getAccuracy()*m_currentScore/m_info.getScore()
		);
		if (m_currentScore!=m_info.getScore()) {
			m_currentScore=Math.min(m_info.getScore(),m_currentScore+m_scoreIncrement);
			m_scoreIncrement=m_scoreIncrement*3/2;
			m_handler.postDelayed(m_stepScoreAnimation,50);
		} else {
			TextView accuracy=(TextView)findViewById(R.id.accuracy);
			accuracy.setVisibility(View.VISIBLE);
			accuracy.setText(UIHelpers.getString(
				this,
				R.string.accuracy_fmt,(int)(m_info.getAccuracy()*100)));
				
			TextView streak=(TextView)findViewById(R.id.streak);
			streak.setVisibility(View.VISIBLE);
			streak.setText(UIHelpers.getString(
				this,
				R.string.longest_streak_fmt,m_info.getLongestStreak()));
			
			int offset=UIHelpers.getInteger(this,R.integer.anim_body_duration);
			UIHelpers.startViewAnimation(this,R.id.accuracy,R.anim.button_in);
			UIHelpers.startViewAnimation(this,R.id.streak,R.anim.button_in,offset/2);
		}
	}
	
	///////////////////////////////////////////// data

	private FinishedSongInfo m_info;
	
	private TextView m_scoreView;
	private RatingBar m_rating;
	
	private int m_currentScore;
	private int m_scoreIncrement;
	
	private Handler m_handler=new Handler();
	private Runnable m_stepScoreAnimation=new Runnable() {
		public void run() {
			stepScoreAnimation();
		}
	};
}
