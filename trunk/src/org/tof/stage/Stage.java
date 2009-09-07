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
package org.tof.stage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import javax.microedition.khronos.opengles.GL10;
import org.tof.Config;
import org.tof.R;
import org.tof.gl.GLHelpers;
import org.tof.gl.GLRect;
import org.tof.gl.sprite.Sprite;
import org.tof.gl.sprite.SpriteFont;
import org.tof.gl.sprite.SpriteFontBuilder;
import org.tof.gl.sprite.SpriteRegion;
import org.tof.gl.sprite.SpriteUtil;
import org.tof.song.EventList;
import org.tof.song.InvalidSongException;
import org.tof.song.NoteEvent;
import org.tof.song.Song;
import org.tof.song.TempoEvent;
import org.tof.util.CharString;
import org.tof.util.DataStreamHelpers;
import org.tof.util.MathHelpers;
import skiba.util.Simply;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;

public class Stage {
	
	public Stage(Context context,Song song) throws InvalidSongException {
		m_song=song;
		initializeBPM();
		
		m_guitar=new Guitar(song);
		
		m_soundEffects=new StageSoundEffects();
		m_soundEffects.load(context);

		m_player=new SongPlayer();
		m_player.open(m_song.getConfig());
		m_player.mute(true);

		m_state=STATE_STOPPED;
		resetPlayingState();
		m_resourcesLoaded=false;
		m_renderingStopped=true;
	}
	
	public void destroy() {
		m_player.close();
		m_soundEffects.destroy();
		m_state=STATE_DESTROYED;
	}
	
	/////////////////////////////////// state
	
	public void resetState() {
		checkNotDestroyed();
		if (m_state!=STATE_STOPPED) {
			throw invalidStateException();
		}
		m_countdownTimer.reset();
		resetEffects();
		resetScore();
		resetNotes();
		m_player.setPosition(0);
		resetPlayingState();
	}
	
	public void saveState(DataOutputStream stream) throws IOException {
		checkNotDestroyed();
		stream.writeInt(STREAM_TAG);
		saveLocalTime(stream);
		m_countdownTimer.save(stream);
		m_cooldownTimer.save(stream);
		saveEffects(stream);
		saveScore(stream);
		saveBPM(stream);
		saveNotes(stream);
		stream.writeInt(m_player.getPosition());
		
		stream.writeInt(m_position);
		stream.writeFloat(m_readiness);
	}
	
	public void restoreState(DataInputStream stream) throws IOException {
		checkNotDestroyed();
		if (m_state!=STATE_STOPPED) {
			throw invalidStateException();
		}
		DataStreamHelpers.checkTag(stream,STREAM_TAG);
		restoreLocalTime(stream);
		m_countdownTimer.restore(stream);
		m_cooldownTimer.restore(stream);
		restoreEffects(stream);
		restoreScore(stream);
		restoreBPM(stream);
		restoreNotes(stream);
		m_player.setPosition(stream.readInt());
		
		m_position=stream.readInt();
		m_readiness=stream.readFloat();
		m_guitar.setReadiness(m_readiness);
		m_guitar.setPosition(m_position,m_bpm);
		m_effects.setReadiness(m_readiness);
		m_effects.setPosition(m_position,m_bpm);
	}
	
	public byte[] saveState() throws IOException {
		ByteArrayOutputStream stream=new ByteArrayOutputStream(4*1024);
		DataOutputStream dataStream=new DataOutputStream(stream);
		saveState(dataStream);
		dataStream.flush();
		return stream.toByteArray();
	}
	
	public void restoreState(byte[] state) throws IOException {
		ByteArrayInputStream stream=new ByteArrayInputStream(state);
		DataInputStream dataStream=new DataInputStream(stream);
		restoreState(dataStream);
	}
	
	/////////////////////////////////// controls
	
	public void start() {
		checkNotDestroyed();
		if (m_state!=STATE_STOPPED) {
			throw invalidStateException();
		}
		if (m_state==STATE_STARTED) {
			return;
		}
		resumeLocalTime();
		if (!m_countdownTimer.isStarted()) {
			startPlaying();
		}
		m_state=STATE_STARTED;
		m_renderingStopped=false;
	}
	
	public void stop(boolean stopRendering) {
		checkNotDestroyed();
		if (m_state==STATE_STARTED) {
			m_player.stop();
			pauseLocalTime();
			m_state=STATE_STOPPED;
		}
		m_renderingStopped=stopRendering;
	}
	
	/////////////////////////////////// callback
	
	public static class FinalScore {
		public Exception error;
		public int score;
		public int longestStreak;
		public float accuracy;
	}
	
	public interface Callback {
		public void onFinished(FinalScore score);
	}
	
	public void setCallback(Callback callback) {
		m_callback=callback;
	}
	
	/////////////////////////////////// input
	
	public void onTouch(float screenX,float screenY) {
		touchKeys(screenX,screenY);
		m_guitar.setActiveStrings(m_keyStrings);
	}
	
	public void onMultitouch(float[] screenCoordinates) {
		multitouchKeys(screenCoordinates);
		m_guitar.setActiveStrings(m_keyStrings);
	}
	
	public void setFPS(int fps) {
		m_averageFPS=fps;
	}
	
	// DEBUG
	public void onKeyPressed(int keyCode,int metaState) {
		//m_guitar.onKeyPressed(keyCode);
		if (m_effects!=null) {
			m_effects.onKeyPressed(keyCode,metaState);
		}
	}
	
	/////////////////////////////////// gl

	public void loadResources(Context context,GL10 gl) throws IOException {
		checkNotDestroyed();
		try {
			m_guitar.loadResources(context,gl);
			loadCountdownResources(gl);
			loadScoreResources(context,gl);
			loadFPSResources(gl);
			loadKeyResources(context,gl);
			loadEffects(context,gl);
			m_resourcesLoaded=true;
		}
		catch (IOException e) {
			unloadResources(gl);
			throw e;
		}
	}
	
	public void unloadResources(GL10 gl) {
		m_guitar.unloadResources(gl);
		unloadCountdownResources(gl);
		unloadScoreResources(gl);
		unloadFPSResources(gl);
		unloadKeyResources(gl);
		unloadEffects(gl);
		m_resourcesLoaded=false;
	}
	
	public void setViewport(GL10 gl,GLRect viewport) {
		checkNotDestroyed();
		m_viewport=new GLRect(viewport);
		GLRect guitarViewport=new GLRect();
		if (m_viewport.height>m_viewport.width) {
			guitarViewport.x=0;
			guitarViewport.y=KEYS_HEIGHT;
			guitarViewport.width=m_viewport.width;
			guitarViewport.height=m_viewport.width;
			
			m_scoreCenter=m_viewport.width/2;
			setKeysWidth(gl,guitarViewport.width);
		} else {
			guitarViewport.x=m_viewport.width/2-(m_viewport.height-KEYS_HEIGHT)/2;
			guitarViewport.y=KEYS_HEIGHT;
			guitarViewport.width=m_viewport.height-KEYS_HEIGHT;
			guitarViewport.height=m_viewport.height-KEYS_HEIGHT;
			
			m_scoreCenter=m_viewport.width-m_viewport.width/4;
			setKeysWidth(gl,guitarViewport.width);
		}
		m_guitar.setViewport(guitarViewport);
		m_effects.setViewport(viewport);
	}
	
	public void render(GL10 gl) {
		checkNotDestroyed();
		advance();
		if (m_renderingStopped || !m_resourcesLoaded) {
			return;
		}
		renderBackground(gl);
		m_guitar.render(gl);
		renderForeground(gl);
	}
	
	public static void setDefaults(GL10 gl) {
		gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT,GL10.GL_FASTEST);
		gl.glClearColor(0,0,0,1);
		
		gl.glEnable(GL10.GL_BLEND);
		gl.glBlendFunc(GL10.GL_SRC_ALPHA,GL10.GL_ONE_MINUS_SRC_ALPHA);
		gl.glEnable(GL10.GL_TEXTURE_2D);
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		gl.glTexEnvx(GL10.GL_TEXTURE_ENV,GL10.GL_TEXTURE_ENV_MODE,GL10.GL_MODULATE);
	}
	
	/////////////////////////////////////////////////////////////////////////// implementation
	// MARKER logic
	
	private void resetPlayingState() {
		resetBPM();
		resetPauseLocalTime();
		startCountdown();
	}
	
	private void startPlaying() {
		m_player.play();
	}
	
	private void finish(Exception error) {
		stop(true);
		if (m_callback!=null) {
			FinalScore info=new FinalScore();
			info.error=error;
			info.score=m_score;
			info.longestStreak=m_longestStreak;
			info.accuracy=(float)m_pickedNoteCount/m_song.getTotalNoteEventCount();
			m_callback.onFinished(info);
		}
	}
	
	private void advance() {
		if (m_state==STATE_STOPPED) {
			return;
		}
		if (finishCountdown()) {
			startPlaying();
		}
		finishCooldown();
		changeBPM();
		setPosition();
		setReadiness();
		pickNotes();
	}
	
	private void setPosition() {
		int position=m_player.getPosition();
		if (m_countdownTimer.isRunning()) {
			position-=m_countdownTimer.getRemainingTime();
		} else if (m_cooldownTimer.isRunning()) {
			position+=m_cooldownTimer.getElapsedTime();
		}
		position-=Config.getNotesDelay()+m_song.getIni().getDelay();
		m_position=position;
		m_guitar.setPosition(position,m_bpm);
		m_effects.setPosition(position,m_bpm);		
	}
	
	private void setReadiness() {
		float readiness=1;
		if (m_countdownTimer.isRunning()) {
			readiness=m_countdownTimer.getProgress();
		} else if (m_cooldownTimer.isRunning()) {
			readiness=1-m_cooldownTimer.getProgress();
		}
		m_readiness=readiness;
		m_guitar.setReadiness(readiness);
		m_effects.setReadiness(Math.min(1,readiness*3));
	}
	
	/////////////////////////////////////////////////////////////////////////// rendering
	
	private void renderForeground(GL10 gl) {
		beginRender(gl);
		renderKeys(gl);
		m_effects.renderForeground(gl);
		if (m_countdownTimer.isRunning()) {
			renderCountdown(gl);
		}
		renderScore(gl);
		renderFPS(gl);
		endRender(gl);
	}
	
	private void renderBackground(GL10 gl) {
		beginRender(gl);
		m_effects.renderBackground(gl);
		endRender(gl);
	}
	
	private void beginRender(GL10 gl) {
		GLHelpers.setViewport(
			gl,
			m_viewport.x,m_viewport.y,
			m_viewport.width,m_viewport.height);
		{
			gl.glMatrixMode(GL10.GL_PROJECTION);
			gl.glLoadIdentity();
			gl.glOrthof(
				0,m_viewport.width,
				0,m_viewport.height,
				0,1);
			gl.glMatrixMode(GL10.GL_MODELVIEW);
			gl.glLoadIdentity();
		}
		gl.glColor4f(1,1,1,1);
	}
	
	private void endRender(GL10 gl) {
	}
	
	/////////////////////////////////////////////////////////////////////////// effects
	
	private void resetEffects() {
		if (m_effects!=null) {
			m_effects.resetState();
		}
	}
	
	private void loadEffects(Context context,GL10 gl) throws IOException {
		m_effects=new StageEffects(context,gl);
	}
	
	private void unloadEffects(GL10 gl) {
		if (m_effects!=null) {
			m_effects.destroy(gl);
			m_effects=null;
		}
	}
	
	private void saveEffects(DataOutputStream stream) throws IOException {
		if (m_effects!=null) {
			stream.writeBoolean(true);
			m_effects.saveState(stream);
		} else {
			stream.writeBoolean(false);
		}
	}
	
	private void restoreEffects(DataInputStream stream) throws IOException {
		boolean saved=stream.readBoolean();
		if (saved!=(m_effects!=null)) {
			throw DataStreamHelpers.inconsistentStateException();
		}
		if (m_effects!=null) {
			m_effects.restoreState(stream);
		}
	}
	
	/////////////////////////////////////////////////////////////////////////// countdown

	private void startCountdown() {
		int stepPeriod=(int)(60000/m_bpm);
		m_countdownTimer.start(COUNTDOWN_STEPS*stepPeriod);
	}
	
	private boolean finishCountdown() {
		if (!m_countdownTimer.isStarted() || m_countdownTimer.isRunning()) {
			return false;
		}
		m_countdownTimer.stop();
		return true;
	}
	
	private void renderCountdown(GL10 gl) {
		float centerX=m_viewport.width/2;
		float centerY=m_viewport.height/2;
		
		gl.glColor4f(1,1,1,1);
		{
			int step=(int)((1-m_countdownTimer.getProgress())*COUNTDOWN_STEPS);
			String stepString=String.valueOf(step);
			m_countdownFont.setScale(3);
			m_countdownFont.setCenter(
				centerX,
				centerY-m_countdownFont.getHeight()*0.2f);
			m_countdownFont.render(gl,stepString);
		}
		
		m_countdownText.setScale(1.3f);
		m_countdownText.setCenter(
			centerX,
			centerY+m_countdownText.getHeight()/2);
		m_countdownText.render(gl);
	}
	
	private void loadCountdownResources(GL10 gl) {
		SpriteFontBuilder fontBuilder=new SpriteFontBuilder();
		fontBuilder.setSize(50);
		fontBuilder.setTypeface(Config.getFireTypeface());
		fontBuilder.setColor(Config.getSelectedColor());
		fontBuilder.setShadowColor(Config.getShadowColor());
		fontBuilder.resetCharacters();
		fontBuilder.addCharacters("0123456789");
		m_countdownFont=fontBuilder.buildFont(gl);
		
		fontBuilder.setSize(20);
		fontBuilder.setTypeface(Config.getDefaultTypeface());
		fontBuilder.setColor(Config.getBaseColor());
		m_countdownText=fontBuilder.buildText(gl,"Get ready to rock!");
	}
	
	private void unloadCountdownResources(GL10 gl) {
		if (m_countdownFont!=null) {
			m_countdownFont.destroy(gl);
			m_countdownFont=null;
		}
		if (m_countdownText!=null) {
			m_countdownText.destroy(gl);
			m_countdownText=null;
		}
	}
	
	private SpriteFont m_countdownFont;
	private SpriteRegion m_countdownText;
	
	private LocalTimer m_countdownTimer=new LocalTimer();

	private static final int COUNTDOWN_STEPS=6;
	
	/////////////////////////////////////////////////////////////////////////// cooldown
	
	private void finishCooldown() {
		if (m_state!=STATE_STARTED) {
			return;
		}
		if (!m_player.isFinished()) {
			return;
		}
		if (m_cooldownTimer.isStarted()) {
			if (!m_cooldownTimer.isRunning()) {
				m_cooldownTimer.stop();
				finish(null);
			} 
		} else {
			Exception error=m_player.getFinishError();
			if (error!=null) {
				finish(error);
			} else {
				m_cooldownTimer.start(COOLDOWN_DURATION);
			}
		}
	}
	
	private LocalTimer m_cooldownTimer=new LocalTimer();
	
	private static final int COOLDOWN_DURATION=1000;
	
	/////////////////////////////////////////////////////////////////////////// score
	// MARKER score
	
	private void resetScore() {
		m_score=0;
		m_currentStreak=0;
		m_longestStreak=0;
		m_pickedNoteCount=0;
		m_chordBeginPosition=0;
		m_chordEndPosition=0;
	}
	
	private void loadScoreResources(Context context,GL10 gl) throws IOException {
		SpriteFontBuilder fontBuilder=new SpriteFontBuilder();
		fontBuilder.setTypeface(Config.getDefaultTypeface());
		fontBuilder.setSize(20);
		fontBuilder.setColor(Config.getBaseColor());
		fontBuilder.setShadowColor(Config.getShadowColor());
		fontBuilder.addCharacters("0123456789");
		fontBuilder.addCharacters(" hits");
		m_scoreFont=fontBuilder.buildFont(gl);
		
		m_scoreMultiplier2x=new Sprite(context,gl,R.drawable.multiplier_2x);
		m_scoreMultiplier3x=new Sprite(context,gl,R.drawable.multiplier_3x);
		m_scoreMultiplier4x=new Sprite(context,gl,R.drawable.multiplier_4x);
	}
	
	private void unloadScoreResources(GL10 gl) {
		if (m_scoreFont!=null) {
			m_scoreFont.destroy(gl);
			m_scoreFont=null;
		}
		if (m_scoreMultiplier2x!=null) {
			m_scoreMultiplier2x.destroy(gl);
			m_scoreMultiplier2x=null;
		}
		if (m_scoreMultiplier3x!=null) {
			m_scoreMultiplier3x.destroy(gl);
			m_scoreMultiplier3x=null;
		}
		if (m_scoreMultiplier4x!=null) {
			m_scoreMultiplier4x.destroy(gl);
			m_scoreMultiplier4x=null;
		}
	}
	
	private void saveScore(DataOutputStream stream) throws IOException {
		stream.writeInt(m_score);
		stream.writeInt(m_currentStreak);
		stream.writeInt(m_longestStreak);
		stream.writeInt(m_pickedNoteCount);
		m_scoreMultiplierTimer.save(stream);
	}

	private void restoreScore(DataInputStream stream) throws IOException {
		m_score=stream.readInt();
		m_currentStreak=stream.readInt();
		m_longestStreak=stream.readInt();
		m_pickedNoteCount=stream.readInt();
		m_scoreMultiplierTimer.restore(stream);
	}
	
	private void renderScore(GL10 gl) {
		gl.glColor4f(1,1,1,1);

		m_scoreFont.setScale(SCORE_HEIGHT/m_scoreFont.getUnscaledHeight());
		float textY=m_viewport.height-m_scoreFont.getHeight()/2;
		
		m_stringBuilder.clear();
		m_stringBuilder.append(m_score+getBonusScore());
		m_scoreFont.setCenter(m_scoreCenter,textY);
		m_scoreFont.render(gl,m_stringBuilder);
		
		textY-=m_scoreFont.getHeight()*3/2;
		
		if (m_currentStreak>1) {
			m_stringBuilder.clear();
			m_stringBuilder.append(m_currentStreak);
			m_stringBuilder.append(" hits");
			m_scoreFont.setCenter(m_scoreCenter,textY);
			m_scoreFont.setScale(HITS_HEIGHT/m_scoreFont.getUnscaledHeight());
			m_scoreFont.render(gl,m_stringBuilder);
		}
		
		if (m_scoreMultiplierTimer.isRunning()) {
			Sprite sprite=null;
			switch (getScoreMultiplier()) {
				case 2: 
					sprite=m_scoreMultiplier2x;
					break;
				case 3: 
					sprite=m_scoreMultiplier3x;
					break;
				case 4: 
					sprite=m_scoreMultiplier4x;
					break;
			}
			if (sprite!=null) {
				float f=m_scoreMultiplierTimer.getProgress();
				f=f*f;
				gl.glColor4f(1,1,1,1-f*2/3);
				sprite.setCenter(m_viewport.width/2,m_viewport.height*3/4);
				sprite.setScale(4*f);
				gl.glBlendFunc(GL10.GL_SRC_ALPHA,GL10.GL_ONE);
				sprite.render(gl);
				gl.glBlendFunc(GL10.GL_SRC_ALPHA,GL10.GL_ONE_MINUS_SRC_ALPHA);
			}
		}
	}
	
	/////////////////////////////////// logic
	
	private void updateScoreOnPick(NoteEvent[] chord,int chordLength) {
		int chordEnd=m_chordEndPosition;
		int repickedCount=0;
		for (int i=0;i!=chordLength;++i) {
			NoteEvent note=chord[i];
			if (note.isRepicked()) {
				repickedCount++;
			}
			chordEnd=Math.max(chordEnd,(int)note.getEndTime());
		}
		if (repickedCount==0) {
			m_score+=getBonusScore();
			m_chordBeginPosition=m_position;
		}
		m_chordEndPosition=chordEnd;
		m_currentStreak+=1;
		m_pickedNoteCount+=(chordLength-repickedCount);
		m_longestStreak=Math.max(m_longestStreak,m_currentStreak);
		if ((m_currentStreak % 10)==0) {
			m_scoreMultiplierTimer.start(SCORE_MULTIPLIER_SHOWTIME);
		}
		m_score+=chordLength*50*getScoreMultiplier();
	}
	
	private void updateScoreOnMiss() {
		m_currentStreak=0;
		m_chordEndPosition=0;
	}
	
	private void updateScoreOnUnpick() {
		m_chordEndPosition=0;
	}
	
	private void updateScoreOnSlip() {
		m_currentStreak=0;
	}
	
	private int getScoreMultiplier() {
		return (m_currentStreak+9)/10;
	}
	
	private int getBonusScore() {
		int length=Math.min(m_position,m_chordEndPosition)-
			m_chordBeginPosition;
		if (length==0) {
			return 0;
		}
		if (length>(60000/m_bpm/4)) {
			return length/10;
		} else {
			return 0;
		}
	}
	
	/////////////////////////////////// data
	
	private int m_score;
	private int m_currentStreak;
	private int m_longestStreak;
	private int m_pickedNoteCount;

	private int m_chordEndPosition;
	private int m_chordBeginPosition;
	
	private LocalTimer m_scoreMultiplierTimer=new LocalTimer();
	
	private float m_scoreCenter;
	private SpriteFont m_scoreFont;
	private Sprite m_scoreMultiplier2x;
	private Sprite m_scoreMultiplier3x;
	private Sprite m_scoreMultiplier4x;

	private static final float SCORE_HEIGHT=40;
	private static final float HITS_HEIGHT=30;
	private static final int SCORE_MULTIPLIER_SHOWTIME=700;
	
	/////////////////////////////////////////////////////////////////////////// fps
	// MARKER fps
	
	private void loadFPSResources(GL10 gl) {
		SpriteFontBuilder fontBuilder=new SpriteFontBuilder();
		fontBuilder.setTypeface(Typeface.MONOSPACE);
		fontBuilder.setSize(20);
		fontBuilder.setColor(0xFF000000);
		fontBuilder.setBackgroundColor(0xFFFF0000);
		fontBuilder.addCharacters("0123456789/");
		m_fpsFont=fontBuilder.buildFont(gl);
	}
	
	private void unloadFPSResources(GL10 gl) {
		if (m_fpsFont!=null) {
			m_fpsFont.destroy(gl);
			m_fpsFont=null;
		}
	}
	
	private void renderFPS(GL10 gl) {
		if (!Config.showDebugInfo()) {
			return;
		}
		m_stringBuilder.clear();
		m_stringBuilder.append(m_averageFPS);
		
		float width=m_fpsFont.measureWidth(m_stringBuilder);
		m_fpsFont.setCenter(width/2,m_viewport.height-m_fpsFont.getHeight()/2);
		m_fpsFont.render(gl,m_stringBuilder);
	}
	
	private int m_averageFPS;
	
	private SpriteFont m_fpsFont;
	
	/////////////////////////////////////////////////////////////////////////// keys
	// MARKER keys
	
	private void loadKeyResources(Context context,GL10 gl) {
		if (m_keyDrawable==null) {
			m_keyDrawable=context.getResources().getDrawable(R.drawable.key);
		}
	}
	
	private void unloadKeyResources(GL10 gl) {
		if (m_keySprite!=null) {
			m_keySprite.destroy(gl);
			m_keySprite=null;
		}
		if (m_leafKeySprite!=null) {
			m_leafKeySprite.destroy(gl);
			m_leafKeySprite=null;
		}
	}
	
	private void setKeysWidth(GL10 gl,float keysWidth) {
		m_keysWidth=keysWidth;
		unloadKeyResources(gl);
		createKeySprites(gl);
	}
	
	private void touchKeys(float screenX,float screenY) {
		float x=screenX;
		float y=m_viewport.height-screenY;
		float tapzoneWidth=m_keyTapzoneWidth;
		float middleTapzoneWidth=tapzoneWidth*MIDDLE_KEY_FRACTION;
		int strings=0;
		if (y<=(KEY_HEIGHT*2)) {
			float right=tapzoneWidth;
			for (int i=0;i!=(Song.STRING_COUNT*2-1);++i) {
				if (x<right) {
					if ((i % 2)==0) {
						strings=Guitar.stringsAdd(strings,i/2);
					} else {
						strings=Guitar.stringsAdd(strings,i/2);
						strings=Guitar.stringsAdd(strings,i/2+1);
					}
					break;
				}
				if ((i % 2)==0) {
					right+=middleTapzoneWidth;
				} else {
					right+=tapzoneWidth;
				}
			}
		}
		m_pickedKeyStrings=(strings &~ m_keyStrings);
		m_keyStrings=strings;
		
//		if (m_pickedKeyStrings!=m_lastPickedKeyStrings) {
//			m_lastPickedKeyStrings=m_pickedKeyStrings;
//			m_stringBuilder.clear();
//			m_stringBuilder.append("[");
//			for (int i=0;i!=Song.STRING_COUNT;++i) {
//				m_stringBuilder.append(
//					Guitar.stringsCheck(m_pickedKeyStrings,i)?
//						"##":"__"
//				);
//			}
//			m_stringBuilder.append("]");
//			Log.e("TOF","                                           "+m_stringBuilder);
//		}
//		private int m_lastPickedKeyStrings;
	}
	
	private void multitouchKeys(float[] screenCoordinates) {
		float keyWidth=m_keyTapzoneWidth*(MIDDLE_KEY_FRACTION+1);
		float leafKeyWidth=m_keyTapzoneWidth*(MIDDLE_KEY_FRACTION+0.5f);
		int strings=0;
		for (int i=2;i<=screenCoordinates.length;i+=2) {
			float x=screenCoordinates[i-2];
			float y=m_viewport.height-screenCoordinates[i-1];
			if (y<=(KEY_HEIGHT*2)) {
				float right=leafKeyWidth;
				for (int string=0;string!=Song.STRING_COUNT;++string) {
					if (x<right) {
						strings=Guitar.stringsAdd(strings,string);
					}
					if (string!=(Song.STRING_COUNT-1)) {
						right+=keyWidth;
					} else {
						right+=leafKeyWidth;
					}
				}
			}
		}
		m_pickedKeyStrings=(strings &~ m_keyStrings);
		m_keyStrings=strings;
	}
	
	private void renderKeys(GL10 gl) {
		float baseX=(m_viewport.width/2-m_keysWidth/2);
		for (int string=0;string!=Song.STRING_COUNT;++string) {
			SpriteRegion keySprite;
			if (string==0 || string==(Song.STRING_COUNT-1)) {
				keySprite=m_leafKeySprite;
			} else {
				keySprite=m_keySprite;
			}
			keySprite.setCenter(baseX+keySprite.getWidth()/2,KEY_HEIGHT/2);
			int color=Config.getStringColor(string);
			if (Guitar.stringsCheck(m_keyStrings,string)) {
				color=GLHelpers.multiplyColor(color,ACTIVE_KEY_DIM);
				keySprite.translateCenter(0,ACTIVE_KEY_DY);
			}
			GLHelpers.setColor(gl,color,1);
			keySprite.render(gl);
			baseX+=keySprite.getWidth();
		}
	}
	
	private void createKeySprites(GL10 gl) {
		float tapzoneWidth=m_keysWidth/(
			Song.STRING_COUNT*(1+MIDDLE_KEY_FRACTION)-
			MIDDLE_KEY_FRACTION
		);
		float middleTapzoneWidth=tapzoneWidth*MIDDLE_KEY_FRACTION;
		m_keyTapzoneWidth=tapzoneWidth;

		float leafKeyWidth=tapzoneWidth+middleTapzoneWidth/2;
		float keyWidth=tapzoneWidth+middleTapzoneWidth;
		m_keySprite=SpriteUtil.createSprite(
			gl,
			m_keyDrawable,
			MathHelpers.round(keyWidth),KEY_HEIGHT);
		m_leafKeySprite=SpriteUtil.createSprite(
			gl,
			m_keyDrawable,
			MathHelpers.round(leafKeyWidth),KEY_HEIGHT);
	}
	
	private int m_keyStrings;
	private int m_pickedKeyStrings;
	
	private float m_keysWidth;
	private float m_keyTapzoneWidth;
	
	private Drawable m_keyDrawable;
	private SpriteRegion m_keySprite;
	private SpriteRegion m_leafKeySprite;
	
	private static final int 
		KEY_HEIGHT				=60,
		ACTIVE_KEY_DY			=-2,
		KEYS_HEIGHT				=KEY_HEIGHT+ACTIVE_KEY_DY;
	private static final float
		ACTIVE_KEY_DIM			=0.8f,
		MIDDLE_KEY_FRACTION		=0.4f;
	
	/////////////////////////////////////////////////////////////////////////// bpm
	
	private void initializeBPM() throws InvalidSongException {
		EventList<TempoEvent> tempoEvents=m_song.getTempoEvents();
		if (tempoEvents.count()==0) {
			throw new InvalidSongException("Song doesn't have tempo events.");
		}
		TempoEvent tempo=tempoEvents.get(0);
		m_initialBPM=tempo.getBPM();
	}
	
	private void resetBPM() {
		m_bpm=m_initialBPM;
		m_targetBPM=m_initialBPM;
		m_bpmChangePosition=0;
	}
	
	private void saveBPM(DataOutputStream stream) throws IOException {
		stream.writeFloat(m_bpm);
		stream.writeFloat(m_targetBPM);
		stream.writeInt(m_bpmChangePosition);
	}
	
	private void restoreBPM(DataInputStream stream) throws IOException {
		m_bpm=stream.readFloat();
		m_targetBPM=stream.readFloat();
		m_bpmChangePosition=stream.readInt();
	}
	
	private void changeBPM() {
		m_bpm+=(m_targetBPM-m_bpm)*BPM_CHANGE_SPEED;
		EventList<TempoEvent> tempos=m_song.getTempoEvents();
		int index=tempos.lowerBound(m_position);
		if (index==-1) {
			return;
		}
		TempoEvent tempo=tempos.get(index);
		float beatPeriod=60000/m_bpm;
		if ((m_position-tempo.getTime())<beatPeriod &&
			tempo.getTime()>m_bpmChangePosition)
		{
			m_bpm=m_targetBPM;
			m_targetBPM=tempo.getBPM();
			m_bpmChangePosition=m_position;

			m_bpm=m_targetBPM;
		}
	}
	
	private float m_initialBPM;
	private float m_bpm;
	private float m_targetBPM;
	private int m_bpmChangePosition;
	
	private static final float BPM_CHANGE_SPEED=0.3f;
	
	/////////////////////////////////////////////////////////////////////////// notes
	// MARKER: notes 
	
	private void onNotesPicked(NoteEvent[] chord,int chordLength) {
		//Log.e("TOF","onNotesPicked");
		m_player.mute(false);
		m_effects.onNotesPicked(chord,chordLength);
		updateScoreOnPick(chord,chordLength);
	}
	
	private void onNotesMissed() {
		//Log.e("TOF","onNotesMissed");
		m_soundEffects.playScrewUpSound();
		m_effects.onNotesMissed();
		updateScoreOnMiss();
	}
	
	private void onNoteUnpicked() {
		//Log.e("TOF","onNotesUnpicked");
		updateScoreOnUnpick();	
	}
	
	private void onNotesSlipped() {
		//Log.e("TOF","onNotesSlipped");	
		m_player.mute(true);
		updateScoreOnSlip();
	}
	
	private void pickNotes() {
		float beatPeriod=60000f/m_bpm;
		float earlyMargin=beatPeriod*Config.getEarlyPickMargin();
		float lateMargin=beatPeriod*Config.getLatePickMargin();
		float repickMargin=beatPeriod*Config.getRepickMargin();
		
		boolean noteSlipped=false;
		NoteEvent[] chord=m_noteBuffer;
		float chordMinTime=Float.MAX_VALUE;
		for (int string=0;string!=Song.STRING_COUNT;++string) {
			chord[string]=null;
			EventList<NoteEvent> notes=m_song.getNoteEvents(string);
			long range=notes.range(m_position,m_position+earlyMargin);
			int rangeBegin=EventList.rangeBegin(range);
			int rangeEnd=EventList.rangeEnd(range);
			if (rangeBegin==rangeEnd) {
				continue;
			}
			if (rangeBegin!=0) {
				NoteEvent note=notes.get(rangeBegin-1);
				if (note.isIntact()) {
					note.setMissed();
					noteSlipped=true;
				}
			}
			NoteEvent note=notes.get(rangeBegin);
			if (note.isUnpicked()) {
				int elapsed=(m_position-note.getUnpickPosition());
				if (m_position>note.getEndTime() || elapsed>repickMargin) {
					note.endPick();
				} else if (Guitar.stringsCheck(m_keyStrings,string)) {
					note.pick();
				}
			}
			if (note.isPicked()) {
				if ((note.getEndTime()-m_position)>earlyMargin) {
					chord[string]=note;
					continue;
				}
			}
			if (!note.isIntact()) {
				rangeBegin++;
			} else {
				if (note.getTime()<(m_position-lateMargin)) {
					if (!note.isMissed()) {
						note.setMissed();
						noteSlipped=true;
					}
					rangeBegin++;
				}
			}
			if (rangeBegin==rangeEnd) {
				continue;
			}
			note=notes.get(rangeBegin);
			chordMinTime=Math.min(chordMinTime,note.getTime());
			chord[string]=note;
		}
		int chordStrings=0;
		int repickedStrings=0;
		int requiredStrings=0;
		for (int string=0;string!=Song.STRING_COUNT;++string) {
			NoteEvent note=chord[string];
			if (note==null) {
				continue;
			}
			if (note.isIntact()) {
				if (Math.abs(chordMinTime-note.getTime())>CHORD_MARGIN) {
					chord[string]=null;
					continue;
				}
				requiredStrings=Guitar.stringsAdd(requiredStrings,string);
			} else {
				repickedStrings=Guitar.stringsAdd(repickedStrings,string);
			}
			chordStrings=Guitar.stringsAdd(chordStrings,string);
		}
		int chordLength=0;
		for (int string=0;string!=Song.STRING_COUNT;++string) {
			NoteEvent note=chord[string];
			if (note!=null) {
				chord[chordLength]=note;
				chordLength++;
			}
		}
		if (m_pickedKeyStrings!=0) {
			if (requiredStrings==0 ||
				!checkStrings(chordStrings,m_pickedKeyStrings))
			{
				for (int i=0;i!=chordLength;++i) {
					NoteEvent note=chord[i];
					if (note.isPicked()) {
						note.endPick();
					}
				}
				onNotesMissed();
			} else {
				for (int i=0;i!=chordLength;++i) {
					chord[i].pick();
				}
				onNotesPicked(chord,chordLength);
			}
		} else {
			boolean noteUnpicked=false;
			if (m_keyStrings==0 && repickedStrings!=0) {
				for (int i=0;i!=chordLength;++i) {
					NoteEvent note=chord[i];
					if (note.isPicked() &&
						(note.getEndTime()-m_position)>repickMargin)
					{
						note.unpick(m_position);
						noteUnpicked=true;
					}
				}
			}
			if (noteUnpicked) {
				onNoteUnpicked();
			}
			if (noteSlipped) {
				onNotesSlipped();
			}
		}
	}
	
	private void resetNotes() {
		for (int string=0;string!=Song.STRING_COUNT;++string) {
			EventList<NoteEvent> notes=m_song.getNoteEvents(string);
			for (int i=0;i!=notes.count();++i) {
				notes.get(i).makeIntact();
			}
		}
	}

	private void saveNotes(DataOutputStream stream) throws IOException {
		for (int string=0;string!=Song.STRING_COUNT;++string) {
			EventList<NoteEvent> notes=m_song.getNoteEvents(string);
			stream.writeInt(notes.count());
			for (int i=0;i!=notes.count();++i) {
				NoteEvent note=notes.get(i);
				stream.writeInt(note.state);
				stream.writeInt(note.unpickPosition);
			}
		}
	}

	private void restoreNotes(DataInputStream stream) throws IOException {
		for (int string=0;string!=Song.STRING_COUNT;++string) {
			EventList<NoteEvent> notes=m_song.getNoteEvents(string);
			int count=stream.readInt();
			if (count!=notes.count()) {
				throw DataStreamHelpers.inconsistentStateException();
			}
			for (int i=0;i!=count;++i) {
				NoteEvent note=notes.get(i);
				note.state=stream.readInt();
				note.unpickPosition=stream.readInt();
			}
		}
	}

	private static boolean checkStrings(int chordStrings,int activeStrings) {
		if (chordStrings==STRINGS_02 || chordStrings==STRINGS_012) {
			return activeStrings==Guitar.STRING_1;
		} else {
			return (chordStrings & activeStrings)==chordStrings;
		}
	}
	
	private NoteEvent[] m_noteBuffer=new NoteEvent[3*Song.STRING_COUNT];

	private static final float 
		CHORD_MARGIN	=1f;
	
	private static final int
		STRINGS_02	=Guitar.STRING_0 | Guitar.STRING_2, 
		STRINGS_012	=Guitar.STRING_0 | Guitar.STRING_1 | Guitar.STRING_2;
	
	/////////////////////////////////////////////////////////////////////////// local time
	
	private void saveLocalTime(DataOutputStream stream) throws IOException {
		stream.writeInt(getLocalTime());
		stream.writeInt(m_localTimeAtPause);
	}

	private void restoreLocalTime(DataInputStream stream) throws IOException {
		m_localTimeBase=SystemClock.uptimeMillis()+stream.readInt();
		m_localTimeAtPause=stream.readInt();
	}
	
	private void resetPauseLocalTime() {
		m_localTimeBase=SystemClock.uptimeMillis()-1;
		m_localTimeAtPause=1;
	}
	
	private int getLocalTime() {
		if (m_localTimeAtPause!=0) {
			return m_localTimeAtPause;
		} else {
			return Simply.elapsedUptimeMillis(m_localTimeBase);
		}
	}
	
	private void pauseLocalTime() {
		if (m_localTimeAtPause==0) {
			m_localTimeAtPause=getLocalTime();
		}
	}
	
	private void resumeLocalTime() {
		if (m_localTimeAtPause==0) {
			return;
		}
		m_localTimeBase=SystemClock.uptimeMillis()-m_localTimeAtPause;
		m_localTimeAtPause=0;
	}
	
	private long m_localTimeBase;
	private int m_localTimeAtPause;
	
	/////////////////////////////////////////////////////////////////////////// LocalTimer
	
	private final class LocalTimer {
		public LocalTimer() {
			reset();
		}
		public void save(DataOutputStream stream) throws IOException {
			stream.writeInt(m_startTime);
			stream.writeInt(m_duration);
		}
		public void restore(DataInputStream stream) throws IOException {
			m_startTime=stream.readInt();
			m_duration=stream.readInt();
		}
		public final void reset() {
			m_startTime=Integer.MAX_VALUE;
			m_duration=0;
		}
		public final void start(int duration) {
			m_startTime=getLocalTime();
			m_duration=duration;
		}
		public final boolean isStarted() {
			return m_startTime!=Integer.MAX_VALUE;
		}
		public final void stop() {
			reset();
		}
		public final int getDuration() {
			return m_duration;
		}
		public final int getElapsedTime() {
			if (!isStarted()) {
				return 0;
			}
			return Math.min(m_duration,getLocalTime()-m_startTime);
		}
		public final int getRemainingTime() {
			return m_duration-getElapsedTime();
		}
		public final float getProgress() {
			if (!isStarted()) {
				return 0;
			}
			return (float)getElapsedTime()/m_duration;
		}
		public final boolean isRunning() {
			return isStarted() && getElapsedTime()<m_duration;
		}
		
		private int m_startTime;
		private int m_duration;
	}
	
	/////////////////////////////////////////////////////////////////////////// state
	
	private void checkNotDestroyed() {
		if (m_state==STATE_DESTROYED) {
			throw invalidStateException();
		}
	}
	
	private RuntimeException invalidStateException() {
		throw new IllegalStateException(
			String.format("Invalid state %s.",formatState(m_state))
		);
	}
	
	private static String formatState(int state) {
		switch (state) {
			case STATE_DESTROYED:	return "DESTROYED";
			case STATE_STOPPED:		return "STOPPED";
			case STATE_STARTED:		return "STARTED";
		}
		return "<INVALID STATE>";
	}	
	
	/////////////////////////////////////////////////////////////////////////// globals
	
	private Song m_song;
	private Guitar m_guitar;
	private SongPlayer m_player;
	private StageEffects m_effects;
	private StageSoundEffects m_soundEffects;

	private GLRect m_viewport;

	private int m_position;
	private float m_readiness;

	private CharString m_stringBuilder=new CharString(64);
	
	private Callback m_callback;
	
	private boolean m_renderingStopped;
	private boolean m_resourcesLoaded;
	private int m_state;
	
	private static final int
		STATE_STOPPED		=1,
		STATE_STARTED		=2,
		STATE_DESTROYED		=3;

	private static final int STREAM_TAG=0x53544147;
}
