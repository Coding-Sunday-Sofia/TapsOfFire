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

import java.io.File;
import java.io.IOException;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import org.tof.R;
import org.tof.gl.GLHelpers;
import org.tof.gl.GLRect;
import org.tof.song.InvalidSongException;
import org.tof.song.Song;
import org.tof.songdb.SongDB;
import org.tof.stage.Stage;
import org.tof.ui.ActivityBase;
import org.tof.ui.FinishedSongInfo;
import org.tof.ui.GameLoadingView;
import org.tof.ui.GameMenuView;
import org.tof.ui.SongInfo;
import org.tof.ui.UISoundEffects;
import org.tof.util.DataInputBA;
import org.tof.util.DataOutputBA;
import org.tof.util.GameFPSTimer;
import skiba.util.Simply;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

public class GameActivity extends ActivityBase implements GameMenuView.Callback {
	
	protected void onCreate(Bundle savedState) {
		//Log.e("TOF","*************** onCreate()");
        super.onCreate(savedState);
        addGLView();
        addMenuView();
        addLoadingView();
        m_loadingView.show();
		try {
			Intent intent=getIntent();
			byte[] songState=intent.getByteArrayExtra(SongInfo.BUNDLE_KEY);
			if (songState!=null) {
				m_songInfo=new SongInfo(new DataInputBA(songState));
//				//Log.e("TOF","Song file: "+m_songInfo.getSongFile().getPath());
//				//Log.e("TOF","Guitar file: "+m_songInfo.getGuitarFile().getPath());
			} else {
				finish();
				
//				m_songInfo=new SongInfo(
//					new File("/sdcard/API")
////					new File("/sdcard/TapsOfFire/cache/39EDB0AB")
////					//new File("/sdcard/Sectoid")
//					//new File("/sdcard/TapsOfFirez/songs/Sonic Clang") 
//					//new File("/sdcard/TapsOfFirez/songs/bangbang")
////					//new File("/sdcard/defy_bpm")
//				);
//				//m_songInfo.setGuitarFile(new File("/sdcard/TapsOfFire/songs/defy/guitar.raw"));
//				//m_songInfo.setSongFile(new File("/sdcard/TapsOfFire/songs/defy/song.raw"));
//				m_songInfo.setSelectedSkill(Song.SKILL_MEDIUM);
			}
			if (savedState!=null) {
				m_stageState=savedState.getByteArray(KEY_ACTIVITY_STATE);
			}
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
//		catch (InvalidSongException e) {
//			throw new RuntimeException(e);
//		}
		CrashHandler.setDetails(m_songInfo.getErrorDetails());
	}
	
	protected void onSaveInstanceState(Bundle state) {
		//Log.e("TOF","*************** onSaveInstanceState()");
		super.onSaveInstanceState(state);
		saveStage(state);
	}
	
	protected void onPause() {
		//Log.e("TOF","*************** onPause()");
		super.onPause();
		destroyGL();
		SongDB.store(this);
	}
	
	protected void onResume() {
		//Log.e("TOF","*************** onResume()");
		super.onResume();
		SongDB.load(this);
		m_menuView.hide();
		m_loadingView.show();
		createGL();
	}
	
	protected void onDestroy() {
		//Log.e("TOF","*************** onDestroy()");
		super.onDestroy();
		if (m_stage!=null) {
			m_stage.destroy();
			m_stage=null;
		}
	}

	///////////////////////////////////////////////////////////////// input

//	public boolean onKeyDown(int keyCode,KeyEvent event) {
//		if (m_stage!=null) {
//			m_stage.onKeyPressed(keyCode,event.getMetaState());
//		}
//		return super.onKeyDown(keyCode,event);
//	}
	
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction()==MotionEvent.ACTION_UP) {
			m_touchX=-1;
			m_touchY=-1;
		} else if (event.getAction()==MotionEvent.ACTION_DOWN) {
			m_touchX=event.getX();
			m_touchY=event.getY();
		}
//		if (m_firstTouchTime==0) {
//			m_firstTouchTime=SystemClock.uptimeMillis();
//		}
//		m_totalTouches++;
//		{
//			int elapsed=Simply.elapsedUptimeMillis(m_firstTouchTime);
//			if (elapsed>1000) {
//				//Log.e("TOF","touches per second: "+(float)m_totalTouches/elapsed*1000);
//				m_totalTouches=0;
//				m_firstTouchTime=SystemClock.uptimeMillis();
//			}
//		}
		Simply.waitSleep(Config.getTouchHandlerSleep());
		return true;
	}
	
	private long m_firstTouchTime=0;
	private int m_totalTouches=0;
	
	///////////////////////////////////////////////////////////////// stage
	
	private void onStageGLCreated(GL10 gl) {
		//Log.e("TOF","++++++++++ onStageGLCreated()");
		GLHelpers.initialize(gl);
		Stage.setDefaults(gl);
		
	    sendMessage(MESSAGE_SHOW_LOADING);
	    boolean startStage=false;
	    try {
	    	if (m_stage==null) {
				Song song=new Song(m_songInfo);
				song.selectAnySkill();
				song.selectSkill(m_songInfo.getSelectedSkill());
				song.glueNoteEvents(Config.getMinNotesDistance());
		    	m_stage=new Stage(this,song);
		    	m_stage.setCallback(new Stage.Callback() {
		    		public void onFinished(Stage.FinalScore info) {
		    			sendMessage(MESSAGE_STAGE_FINISHED,info);
		    		}
		    	});
		    	startStage=(m_stageState==null);
	    	}
	    	m_stage.loadResources(this,gl);
	    	if (m_stageState!=null) {
	    		m_stage.restoreState(m_stageState);
	    		m_stageState=null;
	    	}
	    	m_stage.stop(false);
	    	for (int i=0;i!=3;++i) {
	    		System.gc();
	    	}
	    }
	    catch (Exception e) {
	    	if (m_stage!=null) {
	    		m_stage.destroy();
	    		m_stage=null;
	    	}
	    	reportStageError(R.string.error_loading_song,e);
	    	sendMessage(MESSAGE_HIDE_LOADING);
	    	return;
	    }
	    sendMessage(MESSAGE_HIDE_LOADING);
    	Simply.waitSleep(GameLoadingView.getHideDuratioin(this));
    	if (startStage) {
    		m_stage.start();
    	} else {
    		sendMessage(MESSAGE_SHOW_MENU);
	    }
	}
	
	private void onStageGLDestroyed() {
		//Log.e("TOF","++++++++++ onStageGLDestroyed()");
		if (m_stage!=null) {
			m_stage.stop(true);
			m_stage.unloadResources(null);
		}
		GLHelpers.destroy();
	}
	
	private void onStageGLChanged(GL10 gl,int width,int height) {
		//Log.e("TOF","++++++++++ onStageGLChanged()");
        if (m_stage!=null) {
        	m_stage.setViewport(gl,new GLRect(0,0,width,height));
        }
	}

	private void onStageGLRender(GL10 gl) {
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
		if (m_stage!=null) {
			m_stage.onTouch(m_touchX,m_touchY);
			m_stage.setFPS(m_fpsTimer.getAverageFPS());
			m_stage.render(gl);
		}
	}
	
	private void onStageAction(int action) {
		//Log.e("TOF","++++++++++ onStageAction("+action+")");
		if (m_stage==null) {
			return;
		}
		switch (action) {
			case StageAction.RESTART:
			{
				m_stage.stop(true);
				m_stage.resetState();
				m_stage.start();
//				try {
//					byte[] state=m_stage.saveState();
//					m_stage.stop(false);
//					m_stage.resetState();
//					m_stage.restoreState(state);
//					m_stage.start();
//				}
//				catch (IOException e) {
//					e.printStackTrace();
//				}
				break;
			}
			case StageAction.START:
				m_stage.start();
				break;
			case StageAction.STOP:
				m_stage.stop(true);		
				break;
			case StageAction.PAUSE:
				m_stage.stop(false);		
				break;
		}
	}
	
	private void onSaveStage(Bundle bundle) {
		//Log.e("TOF","++++++++++ onSaveStage()");
		if (m_stage==null) {
			return;
		}
		try {
			bundle.putByteArray(KEY_ACTIVITY_STATE,m_stage.saveState());
		}
		catch (IOException e) {
			throw new RuntimeException(e);			
		}
	}
	
	private void onStageFinished(Stage.FinalScore score) {
		//Log.e("TOF","++++++++++ onStageFinished()");
		if (score.error==null) {
			SongDB.update(
				m_songInfo.getID(),
				m_songInfo.getSelectedSkill(),
				score.score,score.accuracy);
			Intent intent=new Intent(this,SongFinishedActivity.class);
			try {
				DataOutputBA dataOut=new DataOutputBA();
				FinishedSongInfo info=new FinishedSongInfo(m_songInfo);
				info.setScore(score.score);
				info.setLongestStreak(score.longestStreak);
				info.setAccuracy(score.accuracy);
				info.saveState(dataOut);
				intent.putExtra(FinishedSongInfo.BUNDLE_KEY,dataOut.toByteArray());
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
			startActivity(intent);
			finish();
		} else {
			reportStageError(R.string.error_playing_song,score.error);
		}
	}
	
	private void reportStageError(int causeID,Exception error) {
		ErrorReportActivity.report(
			this,
			ErrorReportActivity.CAUSE_ERROR,
			getString(causeID),
			null,
			m_songInfo.getErrorDetails(),
			error);
		finish();
	}
	
	/////////////////////////////////// controls
	
	private void doStageAction(int action,boolean wait) {
		m_glView.queueEvent(new StageAction(action,wait));
		if (wait) {
			Simply.wait(m_glEventProcessedEvent);
		}
	}
	
	private void saveStage(Bundle bundle) { 
		m_glView.queueEvent(new SaveStageRunnable(true,bundle));
		Simply.wait(m_glEventProcessedEvent);
	}
	
	/////////////////////////////////// helpers

	private class StageAction implements Runnable {
		public static final int
			START	=0,
			STOP	=1,
			PAUSE	=2,
			RESTART	=3;
		public StageAction(int action,boolean notify) {
			m_action=action;
			m_notify=notify;
		}
		public void run() {
			onStageAction(m_action);
			if (m_notify) {
				Simply.notify(m_glEventProcessedEvent);
			}
		}
		private int m_action;
		private boolean m_notify;
	}
	
	private class SaveStageRunnable implements Runnable {
		public SaveStageRunnable(boolean save,Bundle bundle) {
			m_bundle=bundle;
			m_save=save;
		}
		public void run() {
			if (m_save) {
				onSaveStage(m_bundle);
			} else {
				//onRestoreStage(m_bundle);
			}
			Simply.notify(m_glEventProcessedEvent);
		}
		private Bundle m_bundle;
		private boolean m_save;
	}
	
	///////////////////////////////////////////////////////////////// views
	
	private void addLoadingView() {
		m_loadingView=(GameLoadingView)getLayoutInflater().
			inflate(R.layout.game_loading,null);
		m_loadingView.setVisibility(View.GONE);
		addContentView(
			m_loadingView,
			new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.FILL_PARENT,
				ViewGroup.LayoutParams.FILL_PARENT
			)
		);
	}
	
	private void addMenuView() {
		m_menuView=(GameMenuView)getLayoutInflater().
			inflate(R.layout.game_menu,null);
		m_menuView.setVisibility(View.GONE);
		addContentView(
			m_menuView,
			new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.FILL_PARENT,
				ViewGroup.LayoutParams.FILL_PARENT
			)
		);
		m_menuView.setCallback(this);
	}

///////////////////////////////////////////////////////////////// GL
	
	private void addGLView() {
        m_glView=new GLSurfaceView(this);
        m_glView.setEGLConfigChooser(false);
        m_glView.setRenderer(new GLSurfaceView.Renderer() {
        	public void onSurfaceCreated(GL10 gl,EGLConfig config) {
        		attachGLCrashHandler();
       			onStageGLCreated(gl);
        	}
        	public void onSurfaceChanged(GL10 gl,int width,int height) {
       			onStageGLChanged(gl,width,height);
        	}
        	public void onDrawFrame(GL10 gl) {
        		m_fpsTimer.onBeforeRender();
       			onStageGLRender(gl);
       			m_fpsTimer.onAfterRender();
        	}
        });
        addContentView(
        	m_glView,
			new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.FILL_PARENT,
				ViewGroup.LayoutParams.FILL_PARENT
			)
        );
	}
	
	private void createGL() {
		m_glView.onResume();
	}
	
	private void destroyGL() {
		m_glView.queueEvent(new Runnable() {
			public void run() {
				m_glView.onPause();
				onStageGLDestroyed();
				Simply.notify(m_glEventProcessedEvent);
			}
		});
		Simply.wait(m_glEventProcessedEvent);
	}
	
	private void attachGLCrashHandler() {
		if (!m_glCrashHandlerAttached) {
			m_glCrashHandlerAttached=true;
			CrashHandler.attachToCurrentThread(this);
			CrashHandler.setDetails(m_songInfo.getErrorDetails());
		}
	}
	
///////////////////////////////////////////////////////////////// menu
	
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		if (!m_menuShown) {
			doStageAction(StageAction.PAUSE,true);
			showMenu(true);
		}
		return true;
	}
	
	public void onGameMenuResume() {
		doStageAction(StageAction.START,true);
		showMenu(false);
	}
	
	public void onGameMenuRestart() {
		doStageAction(StageAction.RESTART,true);
		showMenu(false);
	}
	
	private void showMenu(boolean show) {
		if (m_menuShown==show) {
			return;
		}
		m_menuShown=show;
		if (show) {
			m_menuView.show();
		} else {
			m_menuView.hide();
		}
	}
	
	///////////////////////////////////////////////////////////////// handler
	
	private class LocalHandler extends Handler {
		public void handleMessage(Message message) {
			GameActivity.this.handleMessage(message);
		}
	}
	
	private void handleMessage(Message message) {
		switch (message.what) {
			case MESSAGE_SHOW_LOADING:
			{
				m_loadingView.show();
				break;
			}
			case MESSAGE_HIDE_LOADING:
			{
				m_loadingView.hide();
				break;
			}
			case MESSAGE_SHOW_MENU:
			{
				showMenu(true);
				break;
			}
			case MESSAGE_STAGE_FINISHED:
			{
				onStageFinished((Stage.FinalScore)message.obj);
				break;
			}
		}
	}
	
	private void sendMessage(int message) {
		m_handler.sendEmptyMessage(message);
	}
	private void sendMessage(int message,Object data) {
		m_handler.obtainMessage(message,data).sendToTarget();
	}
	
	private Handler m_handler=new LocalHandler();
	
	private static final int
		MESSAGE_SHOW_LOADING		=1,
		MESSAGE_HIDE_LOADING		=2,
		MESSAGE_SHOW_MENU			=3,
		MESSAGE_STAGE_FINISHED		=4;
	
	/////////////////////////////////////////////////////// data

	private SongInfo m_songInfo;
	private byte[] m_stageState;

	private Stage m_stage;

	private GameLoadingView m_loadingView;
	private GameMenuView m_menuView;
	private boolean m_menuShown=false;

    private GLSurfaceView m_glView;
    private Object m_glEventProcessedEvent=new Object();
    private boolean m_glCrashHandlerAttached=false;
    
    private GameFPSTimer m_fpsTimer=new GameFPSTimer(20);
	
	private float m_touchX=-1;
	private float m_touchY=-1;
}