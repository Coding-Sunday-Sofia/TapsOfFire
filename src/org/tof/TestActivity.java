package org.tof;

import java.io.File;
import java.io.IOException;
import org.tof.player.PCMPlayer;
import org.tof.player.RawDecoder;
import org.tof.player.Vorbis2RawConverter;
import org.tof.player.VorbisDecoder;
import org.tof.song.Song;
import org.tof.song.SongInfo;
import org.tof.stage.SongPlayer;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.FrameLayout.LayoutParams;

public class TestActivity extends Activity implements View.OnClickListener {
	
	public void onCreate(Bundle savedState) {
		super.onCreate(savedState);
		setContentView(R.layout.test);
		Config.load(this);
		
		findViewById(R.id.open).setOnClickListener(this);
		findViewById(R.id.close).setOnClickListener(this);
		findViewById(R.id.play).setOnClickListener(this);
		findViewById(R.id.stop).setOnClickListener(this);
		findViewById(R.id.seekTo0).setOnClickListener(this);
		
		m_text=(TextView)findViewById(R.id.text);
		
		
//		try {
//			m_converter=new Vorbis2RawConverter();
//			m_converter.start(
//				new File("/sdcard/TapsOfFire/songs/defy/guitar.ogg"),
//				new File("/sdcard/TapsOfFire/songs/defy/guitar.raw"));
//			checkConverter();
//			
//		}
//		catch (IOException e) {
//			e.printStackTrace();
//			throw new RuntimeException(e);
//		}
//		
//		new Thread() {
//			public void run() {
//				while (!Thread.interrupted()) {
//					m_player.getPosition();
//					SystemClock.sleep(1000/50);
//				}
//			}
//		}.start();
	}
	
	private void checkConverter() {
		if (m_converter.isFinished()) {
			m_text.setText("Finished!");
		} else {
			m_text.setText("In progress, "+m_converter.getProgress()+" done");
			m_handler.postDelayed(
				new Runnable() {
					public void run() {
						checkConverter();
					}
				},
				100
			);
		}
	}
	
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.open:
				try {
					//m_player.open(
						//new VorbisDecoder(new File("/sdcard/TapsOfFire/songs/defy/song.ogg"))
					//	new RawDecoder(new File("/sdcard/song.raw"))
					//);
					//m_player.open(new File("/data/data/org.tof/files/defy/song.ogg"));
					//m_player.open(new File("/sdcard/menu.ogg"));
					
					SongInfo song=new SongInfo(new File("/sdcard/defy"));
					song.setSongFile(new File("/sdcard/defy/song.raw"));
					song.setGuitarFile(new File("/sdcard/defy/guitar.rawzz"));
					m_player.open(song);
					//m_player.load(this);
					
					m_playerPosition=0;
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				break;
			case R.id.close:
				m_player.close();
				//m_player.destroy();
				break;
			case R.id.play:
				try {
					m_playerPosition=23193;
					m_player.setPosition(m_playerPosition);
					m_player.play();
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				//m_player.playScrewUpSound();
				break;
			case R.id.stop:
				m_playerPosition=m_player.getPosition();
				Log.e("TOF","Player position: "+m_playerPosition);
				m_player.stop();
				break;
			case R.id.seekTo0:
				m_playerPosition=0;
				m_player.setPosition(0);
				//m_player.resetPosition();
				break;
		}
	}
	
	private Vorbis2RawConverter m_converter;	
	//private PCMPlayer m_player=new PCMPlayer();
	private SongPlayer m_player=new SongPlayer();
	//private StageSoundEffects m_player=new StageSoundEffects();
	private int m_playerPosition;

	private TextView m_text;
	private Handler m_handler=new Handler();
}
