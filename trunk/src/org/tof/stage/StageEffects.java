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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import javax.microedition.khronos.opengles.GL10;
import org.tof.Config;
import org.tof.gl.GLHelpers;
import org.tof.gl.GLRect;
import org.tof.gl.sprite.Sprite;
import org.tof.song.NoteEvent;
import org.tof.song.Song;
import org.tof.util.DataStreamHelpers;
import org.tof.util.IniFile;
import org.tof.util.MathHelpers;
import android.content.Context;
import android.graphics.Color;
import android.util.FloatMath;
import android.util.Log;
import android.view.KeyEvent;

class StageEffects {
	
	public StageEffects(Context context,GL10 gl) throws IOException {
		m_layers=new ArrayList<Layer>();
		IniFile ini=new IniFile(context.getAssets().open("stage.ini"));
		int maxLightIndex=0;
		for (int layerNumber=1;layerNumber<=MAX_LAYER;++layerNumber) {
			String layerName="layer"+layerNumber;
			IniFile.Section layerSection=ini.getSection(layerName);
			if (layerSection==null) {
				continue;
			}
			Layer layer=new Layer(layerNumber,this);
			layer.load(context,gl,layerSection);
			m_layers.add(layer);
			for (int effectNumber=1;effectNumber<=MAX_LAYER_EFFECT;++effectNumber) {
				String effectName=layerName+":fx"+effectNumber;
				IniFile.Section effectSection=ini.getSection(effectName);
				if (effectSection==null) {
					continue;
				}
				LayerEffect effect=LayerEffect.create(this,effectSection);
				if (effect!=null) {
					layer.addEffect(effect);
				}
				if (effect instanceof LightLayerEffect) {
					maxLightIndex=Math.max(
						maxLightIndex,
						((LightLayerEffect)effect).getLightIndex());
				}
			}
		}
		m_lightAverageStrings=new float[maxLightIndex+1];
		m_averageStringHistory=new float[Song.STRING_COUNT];
		resetState();
	}
	
	public void destroy(GL10 gl) {
		for (int i=0,e=m_layers.size();i!=e;++i) {
			m_layers.get(i).destroy(gl);
		}
		m_layers.clear();
	}
	
	/////////////////////////////////// state
	
	public void resetState() {
		m_readiness=1;
		m_position=0;
		m_beatPeriod=0;
		m_beatPosition=0;
		m_beats=0;
		m_quarterbeatPosition=0;
		m_quarterbeats=0;
		m_notesPickPosition=0;
		m_notesMissPosition=0;
		Arrays.fill(m_lightAverageStrings,0);
		Arrays.fill(m_averageStringHistory,0);
		for (int i=0,e=m_layers.size();i!=e;++i) {
			m_layers.get(i).resetState();
		}
	}
	
	public void saveState(DataOutputStream stream) throws IOException {
		stream.writeInt(STREAM_TAG);
		stream.writeFloat(m_beatPeriod);
		stream.writeInt(m_beatPosition);
		stream.writeInt(m_beats);
		stream.writeInt(m_quarterbeatPosition);
		stream.writeInt(m_quarterbeats);
		stream.writeInt(m_notesPickPosition);
		stream.writeInt(m_notesMissPosition);
		{
			stream.writeInt(m_lightAverageStrings.length);
			for (float value: m_lightAverageStrings) {
				stream.writeFloat(value);
			}
		}
		{
			stream.writeInt(m_averageStringHistory.length);
			for (float value: m_averageStringHistory) {
				stream.writeFloat(value);
			}
		}
		{
			stream.writeInt(m_layers.size());
			for (int i=0;i!=m_layers.size();++i) {
				m_layers.get(i).saveState(stream);
			}
		}
	}
	
	public void restoreState(DataInputStream stream) throws IOException {
		DataStreamHelpers.checkTag(stream,STREAM_TAG);
		m_beatPeriod=stream.readFloat();
		m_beatPosition=stream.readInt();
		m_beats=stream.readInt();
		m_quarterbeatPosition=stream.readInt();
		m_quarterbeats=stream.readInt();
		m_notesPickPosition=stream.readInt();
		m_notesMissPosition=stream.readInt();
		{
			int length=stream.readInt();
			if (length!=m_lightAverageStrings.length) {
				throw DataStreamHelpers.inconsistentStateException();
			}
			for (int i=0;i!=length;++i) {
				m_lightAverageStrings[i]=stream.readFloat();
			}
		}
		{
			int length=stream.readInt();
			if (length!=m_averageStringHistory.length) {
				throw DataStreamHelpers.inconsistentStateException();
			}
			for (int i=0;i!=length;++i) {
				m_averageStringHistory[i]=stream.readFloat();
			}
		}
		{
			int size=stream.readInt();
			if (size!=m_layers.size()) {
				throw DataStreamHelpers.inconsistentStateException();
			}
			for (int i=0;i!=size;++i) {
				m_layers.get(i).restoreState(stream);
			}
		}
	}
	
	/////////////////////////////////// setup
	
	public void setViewport(GLRect viewport) {
		if (viewport==null) {
			m_viewport=null;
		} else {
			m_viewport=new GLRect(viewport);
		}
	}
	
	public void setReadiness(float readiness) {
		m_readiness=readiness;
	}
	
	public void setPosition(int position,float bpm) {
		float beatPeriod=60000/bpm;
		m_position=position;
		m_beatPeriod=beatPeriod;
		
		int quarterbeats=(int)(4*position/beatPeriod);
		if (quarterbeats>m_quarterbeats) {
			m_quarterbeatPosition=position;
			m_quarterbeats=quarterbeats;
		}
		int beats=quarterbeats/4;
		if (beats>m_beats) {
			m_beatPosition=position;
			m_beats=beats;
			mixLightStrings();
		}
	}
	
	public void onNotesPicked(NoteEvent[] notes,int notesLength) {
		if (notesLength==0) {
			return;
		}
		{
			float average=0;
			for (int i=0;i!=notesLength;++i) {
				average+=notes[i].getString();
			}
			average/=notesLength;
			int lastIndex=m_averageStringHistory.length-1;
			System.arraycopy(
				m_averageStringHistory,1,
				m_averageStringHistory,0,
				lastIndex);
			m_averageStringHistory[lastIndex]=average;
		}
		m_notesPickPosition=m_position;
		{
			float average=0;
			for (int i=0;i!=m_averageStringHistory.length;++i) {
				average+=m_averageStringHistory[i];
			}
			average/=m_averageStringHistory.length;
			int lastIndex=m_lightAverageStrings.length-1;
			m_lightAverageStrings[lastIndex]=average;
		}
	}
	
	public void onNotesMissed() {
		m_notesMissPosition=m_position;
	}
	
	/////////////////////////////////// rendering
	
	public void renderBackground(GL10 gl) {
		if (m_viewport==null) {
			return;
		}
		for (int i=0,e=m_layers.size();i!=e;++i) {
			Layer layer=m_layers.get(i);
			if (!layer.isForeground()) {
				layer.render(gl);
			}
		}
	}

	public void renderForeground(GL10 gl) {
		if (m_viewport==null) {
			return;
		}
		for (int i=0,e=m_layers.size();i!=e;++i) {
			Layer layer=m_layers.get(i);
			if (layer.isForeground()) {
				layer.render(gl);
			}
		}
	}
	
	/////////////////////////////////// package
	
	/*package*/ void setPosition(Sprite sprite,float x,float y) {
		if (m_viewport==null) {
			sprite.setCenter(0,0);
			return;
		}
		if (m_readiness!=1) {
			if (x<0) {
				x=-1+(1+x)*m_readiness;
			} else if (x>0) {
				x=1-(1-x)*m_readiness;
			}
		}
		x*=m_viewport.width/2;
		y*=m_viewport.height/2;
		x=m_viewport.centerX()+x;
		y=m_viewport.centerY()+y;
		sprite.setCenter(x,y);
	}
	
	/*package*/ float getBeatPeriod() {
		return m_beatPeriod;
	}
	
	/*package*/ int getPosition() {
		return m_position;
	}
	
	/*package*/ int getBeats() {
		return m_beats;
	}
	
	/*package*/ int getBeatPosition() {
		return m_beatPosition;
	}

	/*package*/ int getQuarterbeats() {
		return m_quarterbeats;
	}
	
	/*package*/ int getQuarterbeatPosition() {
		return m_quarterbeatPosition;
	}
	
	/*package*/ int getNotesPickPosition() {
		return m_notesPickPosition;
	}
	
	/*package*/ int getNotesMissPosition() {
		return m_notesMissPosition;
	}
	
	/*package*/ float getLightAverageString(int index) {
		if (index<0 || index>=m_lightAverageStrings.length) {
			return 0;
		}
		return m_lightAverageStrings[index];
	}
	
	///////////////////////////////////////////// debug

	void onKeyPressed(int keyCode,int metaState) {
		if (keyCode>=KeyEvent.KEYCODE_0 && keyCode<=KeyEvent.KEYCODE_9) {
			int index=(keyCode-KeyEvent.KEYCODE_1);
			if (index==-1) {
				index=9;
			}
			if ((metaState & KeyEvent.META_SHIFT_ON)!=0) {
				index+=10;
			}
			if (index<0 || index>=m_layers.size()) {
				Log.e("TOF","******* Invalid layer index "+index);
				m_selectedLayer=null;
			} else {
				Log.e("TOF","******* Selected layer "+index);
				m_selectedLayer=m_layers.get(index);
			}
			return;
		}
		if (m_selectedLayer==null) {
			return;
		}
		int precision=m_layerAdjustmentPrecision;
		switch (keyCode) {
			case KeyEvent.KEYCODE_Q:
				m_selectedLayer.m_originalX-=0.01f/precision;
				break;
			case KeyEvent.KEYCODE_W:
				m_selectedLayer.m_originalX+=0.01f/precision;
				break;
			case KeyEvent.KEYCODE_A:
				m_selectedLayer.m_originalY-=0.01f/precision;
				break;
			case KeyEvent.KEYCODE_S:
				m_selectedLayer.m_originalY+=0.01f/precision;
				break;
			case KeyEvent.KEYCODE_E:
				m_selectedLayer.m_originalScaleX-=0.01f/precision;
				break;
			case KeyEvent.KEYCODE_R:
				m_selectedLayer.m_originalScaleX+=0.01f/precision;
				break;
			case KeyEvent.KEYCODE_D:
				m_selectedLayer.m_originalScaleY-=0.01f/precision;
				break;
			case KeyEvent.KEYCODE_F:
				m_selectedLayer.m_originalScaleY+=0.01f/precision;
				break;
			case KeyEvent.KEYCODE_Z:
				m_selectedLayer.m_originalAngle-=1;				
				break;
			case KeyEvent.KEYCODE_X:
				m_selectedLayer.m_originalAngle+=1;				
				break;
			case KeyEvent.KEYCODE_K:
				precision-=1;
				if (precision<1) {
					precision=1;
				}
				m_layerAdjustmentPrecision=precision;
				Log.e("TOF","******* Precision: "+precision);
				break;
			case KeyEvent.KEYCODE_L:
				precision+=1;
				m_layerAdjustmentPrecision=precision;
				Log.e("TOF","******* Precision: "+precision);
				break;
				
			case KeyEvent.KEYCODE_P:
				Log.e("TOF","******* Position: "+m_selectedLayer.m_originalX+", "+m_selectedLayer.m_originalY);
				Log.e("TOF","******* Scale: "+m_selectedLayer.m_originalScaleX+", "+m_selectedLayer.m_originalScaleY);
				Log.e("TOF","******* Angle: "+m_selectedLayer.m_originalAngle);
				break;
		}
	}
	
	private int m_layerAdjustmentPrecision=1;
	private Layer m_selectedLayer;
	
	///////////////////////////////////////////// implementation
	
	private void mixLightStrings() {
		System.arraycopy(
			m_lightAverageStrings,1,
			m_lightAverageStrings,0,
			m_lightAverageStrings.length-1);
	}
	
	/////////////////////////////////// data
	
	private GLRect m_viewport;
	private ArrayList<Layer> m_layers;
	
	private float m_readiness;
	private int m_position;
	private float m_beatPeriod;
	private int m_beatPosition;
	private int m_beats;
	private int m_quarterbeatPosition;
	private int m_quarterbeats;
	private int m_notesPickPosition;
	private int m_notesMissPosition;
	
	private float[] m_lightAverageStrings;
	private float[] m_averageStringHistory;
	
	private static final int 
		MAX_LAYER			=32,
		MAX_LAYER_EFFECT	=32;
	
	private static final int STREAM_TAG=0x53544658;
}

///////////////////////////////////////////////////////////////////// Layer

class Layer {
	public Layer(int number,StageEffects stage) {
		m_number=number;
		m_stage=stage;
		m_effects=new ArrayList<LayerEffect>();
	}
	
	public void destroy(GL10 gl) {
		m_sprite.destroy(gl);
	}
	
	/////////////////////////////////// state
	
	public void resetState() {
		applyOriginalValues();
	}
	
	public void saveState(DataOutputStream stream) throws IOException {
		stream.writeInt(STREAM_TAG);
		stream.writeInt(m_originalColor);
		stream.writeFloat(m_originalX);
		stream.writeFloat(m_originalY);
		stream.writeFloat(m_originalScaleX);
		stream.writeFloat(m_originalScaleY);
		stream.writeFloat(m_originalAngle);
		stream.writeBoolean(m_glowing);
		stream.writeBoolean(m_foreground);
		
		stream.writeInt(m_color);
		stream.writeFloat(m_x);
		stream.writeFloat(m_y);
		stream.writeFloat(m_sprite.getScaleX());
		stream.writeFloat(m_sprite.getScaleY());
		stream.writeFloat(m_sprite.getAngle());
		
		stream.writeInt(m_effects.size());
		for (int i=0;i!=m_effects.size();++i) {
			m_effects.get(i).saveState(stream);			
		}
	}
	
	public void restoreState(DataInputStream stream) throws IOException {
		DataStreamHelpers.checkTag(stream,STREAM_TAG);
		m_originalColor=stream.readInt();
		m_originalX=stream.readFloat();
		m_originalY=stream.readFloat();
		m_originalScaleX=stream.readFloat();
		m_originalScaleY=stream.readFloat();
		m_originalAngle=stream.readFloat();
		m_glowing=stream.readBoolean();
		m_foreground=stream.readBoolean();
		
		m_color=stream.readInt();
		m_x=stream.readFloat();
		m_y=stream.readFloat();
		m_sprite.setScale(stream.readFloat(),stream.readFloat());
		m_sprite.setAngle(stream.readFloat());
		
		int effectsSize=stream.readInt();
		if (effectsSize!=m_effects.size()) {
			throw DataStreamHelpers.inconsistentStateException();
		}
		for (int i=0;i!=effectsSize;++i) {
			m_effects.get(i).restoreState(stream);
		}
	}
	
	public void load(Context context,GL10 gl,IniFile.Section iniSection) 
		throws IOException
	{
		String texture=iniSection.getValue("texture");
		if (texture==null) {
			throw new IOException(String.format(
				"Invalid layer %d: no texture specified.",
				m_number
			));
		}
		m_sprite=new Sprite(context,gl,texture);
		m_originalColor=iniSection.getColorValue("color",0xFFFFFFFF);
		m_originalX=iniSection.getFloatValue("xpos",0.0f);
		m_originalY=iniSection.getFloatValue("ypos",0.0f);
		m_originalScaleX=iniSection.getFloatValue("xscale",1.0f);
		m_originalScaleY=iniSection.getFloatValue("yscale",1.0f);
		m_originalAngle=iniSection.getFloatValue("angle",0.0f);
		m_glowing=(iniSection.getValue("glowing")!=null);
		m_foreground=(iniSection.getValue("foreground")!=null);
		applyOriginalValues();
	}
	
	public void addEffect(LayerEffect effect) {
		m_effects.add(effect);
	}
	
	public int getNumber() {
		return m_number;
	}
	
	public boolean isForeground() {
		return m_foreground;
	}
	
	public void applyOriginalValues() {
		m_x=m_originalX;
		m_y=m_originalY;
		m_sprite.setScale(m_originalScaleX,m_originalScaleY);
		m_sprite.setAngle(m_originalAngle);
		m_color=m_originalColor;
	}
	
	public int getColor() {
		return m_color;
	}
	public void setColor(int color) {
		m_color=color;
	}
	
	public float getX() {
		return m_x;
	}
	public float getY() {
		return m_y;
	}
	public void setPosition(float x,float y) {
		m_x=x;
		m_y=y;
	}
	public void translate(float dx,float dy) {
		m_x+=dx;
		m_y+=dy;
	}
	
	public float getScaleX() {
		return m_sprite.getScaleX();
	}
	public float getScaleY() {
		return m_sprite.getScaleY();
	}
	public void setScale(float scaleX,float scaleY) {
		m_sprite.setScale(scaleX,scaleY);
	}
	public void scale(float scaleX,float scaleY) {
		m_sprite.scale(scaleX,scaleY);
	}
	
	public float getAngle() {
		return m_sprite.getAngle();
	}
	public void setAngle(float angle) {
		m_sprite.setAngle(angle);
	}
	public void rotate(float angle) {
		m_sprite.rotate(angle);
	}
	
	public void render(GL10 gl) {
		applyOriginalValues();
		applyEffects();
		m_stage.setPosition(m_sprite,m_x,m_y);
		if (m_glowing) {
			gl.glBlendFunc(GL10.GL_ONE,GL10.GL_ONE);
		}
		GLHelpers.setColor(gl,m_color,1);
		m_sprite.render(gl);
		if (m_glowing) {
			gl.glBlendFunc(GL10.GL_SRC_ALPHA,GL10.GL_ONE_MINUS_SRC_ALPHA);
		}
	}
	
	///////////////////////////////////////////// implementation
	
	private void applyEffects() {
		for (int i=0,e=m_effects.size();i!=e;++i) {
			m_effects.get(i).apply(this);
		}
	}
	
	/////////////////////////////////// data

	private int m_number;
	private StageEffects m_stage;
	private Sprite m_sprite;
	private float m_x;
	private float m_y;
	private int m_color;
	private boolean m_glowing;
	private boolean m_foreground;
	private int m_originalColor;
	//TODO: change to private
	/*package*/ float m_originalX;
	/*package*/ float m_originalY;
	/*package*/ float m_originalScaleX;
	/*package*/ float m_originalScaleY;
	/*package*/ float m_originalAngle;
	private ArrayList<LayerEffect> m_effects;
	
	private static final int STREAM_TAG=0x5346584C;
}

///////////////////////////////////////////////////////////////////// Effects

class LayerEffect {
	
	public LayerEffect(StageEffects stage,IniFile.Section iniSection) {
		m_stage=stage;
		m_intensity=iniSection.getFloatValue("intensity",1.0f);
		m_trigger=parseTrigger(iniSection.getValue("trigger"));
		m_profile=parseProfile(iniSection.getValue("profile"));
		m_period=iniSection.getFloatValue("period",500.0f);
		m_delay=iniSection.getFloatValue("delay",0.0f);
	}
	
	public void saveState(DataOutputStream stream) throws IOException {
		stream.writeInt(STREAM_TAG);
		stream.writeInt(m_trigger);
		stream.writeInt(m_profile);
		stream.writeFloat(m_intensity);
		stream.writeFloat(m_period);
		stream.writeFloat(m_delay);
	}
	
	public void restoreState(DataInputStream stream) throws IOException {
		DataStreamHelpers.checkTag(stream,STREAM_TAG);
		m_trigger=stream.readInt();
		m_profile=stream.readInt();
		m_intensity=stream.readFloat();
		m_period=stream.readFloat();
		m_delay=stream.readFloat();
	}
	
	public StageEffects getStage() {
		return m_stage;
	}
	
	public void apply(Layer layer) {
	}
	
	public static LayerEffect create(StageEffects stage,IniFile.Section iniSection) {
		String type=iniSection.getValue("type");
		if (type==null) {
			return null;
		}
		if (type.equals("wiggle")) {
			return new WiggleLayerEffect(stage,iniSection);
		}
		if (type.equals("scale")) {
			return new ScaleLayerEffect(stage,iniSection);
		}
		if (type.equals("rotate")) {
			return new RotateLayerEffect(stage,iniSection);
		}
		if (type.equals("light")) {
			return new LightLayerEffect(stage,iniSection);
		}
		return null;
	}
	
	/////////////////////////////////// protected
	
	protected float getIntensity() {
		return m_intensity;
	}
	
	protected float getPeriod() {
		return m_period;
	}
	
	protected float getDelay() {
		return m_delay;
	}
	
	protected float trigger() {
		switch (m_trigger) {
			case TRIGGER_BEAT:
			case TRIGGER_QUARTERBIT:
			{
				int position=(m_trigger==TRIGGER_BEAT)?
					m_stage.getBeatPosition():
					m_stage.getQuarterbeatPosition();
				if (position==0) {
					return 0;
				}
				float period=m_stage.getBeatPeriod();
				if (m_trigger==TRIGGER_QUARTERBIT) {
					period/=4;
				}
				float p=m_stage.getPosition()-m_delay*period-position;
				return m_intensity*(1-step(0,period,p));
			}
			case TRIGGER_PICK:
			case TRIGGER_MISS:
			{
				int position=(m_trigger==TRIGGER_PICK)?
					m_stage.getNotesPickPosition():
					m_stage.getNotesMissPosition();
				if (position==0) {
					return 0;
				}
				float p=m_stage.getPosition()-m_delay*m_period-position;
				return m_intensity*(1-step(0,m_period,p));
			}
			default:
			case TRIGGER_NONE:
				return 0;
		}
	}
	
	///////////////////////////////////////////// implementation
	
	private float step(float min,float max,float x) {
		if (x<min) {
			return 0;
		}
		if (x>max) {
			return 1;
		}
		x=(x-min)/(max-min);
		if (m_profile==PROFILE_SMOOTHSTEP) {
			x=-2*x*x*x+3*x*x;
		} else if (m_profile==PROFILE_SINSTEP) {
			x=FloatMath.cos(MathHelpers.PI*(1-x));
		}
		return x;
	}
	
	private static int parseTrigger(String triggerName) {
		if (triggerName==null) {
			return TRIGGER_DEFAULT;
		}
		if (triggerName.equalsIgnoreCase("beat")) {
			return TRIGGER_BEAT;
		}
		if (triggerName.equalsIgnoreCase("quarterbeat")) {
			return TRIGGER_QUARTERBIT;
		}
		if (triggerName.equalsIgnoreCase("pick")) {
			return TRIGGER_PICK;
		}
		if (triggerName.equalsIgnoreCase("miss")) {
			return TRIGGER_MISS;
		}
		return TRIGGER_DEFAULT;
	}
	
	private static int parseProfile(String profileName) {
		if (profileName==null) {
			return PROFILE_DEFAULT;
		}
		if (profileName.equalsIgnoreCase("linstep")) {
			return PROFILE_LINSTEP;
		}
		if (profileName.equalsIgnoreCase("sinstep")) {
			return PROFILE_SINSTEP;
		}
		if (profileName.equalsIgnoreCase("smoothstep")) {
			return PROFILE_SMOOTHSTEP;
		}
		return PROFILE_DEFAULT;
	}
	
	/////////////////////////////////// data
	
	private StageEffects m_stage;
	private int m_trigger;
	private int m_profile;
	private float m_intensity;
	private float m_period;
	private float m_delay;
	
	private static final int
		TRIGGER_NONE			=0,
		TRIGGER_BEAT			=1,
		TRIGGER_QUARTERBIT		=2,
		TRIGGER_PICK			=3,
		TRIGGER_MISS			=4,
		TRIGGER_DEFAULT			=TRIGGER_NONE;
	
	private static final int
		PROFILE_LINSTEP			=1,
		PROFILE_SMOOTHSTEP		=2,
		PROFILE_SINSTEP			=3,
		PROFILE_DEFAULT			=PROFILE_LINSTEP;

	private static final int STREAM_TAG=0x53465845;
}

/////////////////////////////////////////////////////////// WiggleLayerEffect

class WiggleLayerEffect extends LayerEffect {
	
	public WiggleLayerEffect(StageEffects stage,IniFile.Section iniSection) {
		super(stage,iniSection);
		m_frequency=iniSection.getFloatValue("frequency",6);
		m_xMagnitude=iniSection.getFloatValue("magnitude",0.1f);
		m_yMagnitude=iniSection.getFloatValue("magnitude",0.1f);
	}
	
	public void saveState(DataOutputStream stream) throws IOException {
		stream.writeInt(STREAM_TAG);
		stream.writeFloat(m_frequency);
		stream.writeFloat(m_xMagnitude);
		stream.writeFloat(m_yMagnitude);
	}
	
	public void restoreState(DataInputStream stream) throws IOException {
		DataStreamHelpers.checkTag(stream,STREAM_TAG);
		m_frequency=stream.readFloat();
		m_xMagnitude=stream.readFloat();
		m_yMagnitude=stream.readFloat();
	}
	
	public void apply(Layer layer) {
		float p=trigger();
		float angle=2*p*MathHelpers.PI*m_frequency;
		float dx=p*FloatMath.sin(angle)*m_xMagnitude;
		float dy=p*FloatMath.cos(angle)*m_yMagnitude;
		layer.translate(dx,dy);
	}
	
	///////////////////////////////////////////// implementation
	
	private float m_frequency;
	private float m_xMagnitude;
	private float m_yMagnitude;
	
	private static final int STREAM_TAG=0x46584557;
}

/////////////////////////////////////////////////////////// ScaleLayerEffect

class ScaleLayerEffect extends LayerEffect {
	
	public ScaleLayerEffect(StageEffects stage,IniFile.Section iniSection) {
		super(stage,iniSection);
		m_xMagnitude=iniSection.getFloatValue("magnitude",0.1f);
		m_yMagnitude=iniSection.getFloatValue("magnitude",0.1f);
	}

	public void saveState(DataOutputStream stream) throws IOException {
		stream.writeInt(STREAM_TAG);
		stream.writeFloat(m_xMagnitude);
		stream.writeFloat(m_yMagnitude);
	}
	
	public void restoreState(DataInputStream stream) throws IOException {
		DataStreamHelpers.checkTag(stream,STREAM_TAG);
		m_xMagnitude=stream.readFloat();
		m_yMagnitude=stream.readFloat();
	}
	
	public void apply(Layer layer) {
		float p=trigger();
		layer.scale(1+p*m_xMagnitude,1+p*m_yMagnitude);
	}
	
	///////////////////////////////////////////// implementation
	
	private float m_xMagnitude;
	private float m_yMagnitude;
	
	private static final int STREAM_TAG=0x46584553;
}

/////////////////////////////////////////////////////////// RotateLayerEffect

class RotateLayerEffect extends LayerEffect {
	
	public RotateLayerEffect(StageEffects stage,IniFile.Section iniSection) {
		super(stage,iniSection);
		m_angle=iniSection.getFloatValue("angle",0.0f);
	}

	public void saveState(DataOutputStream stream) throws IOException {
		stream.writeInt(STREAM_TAG);
		stream.writeFloat(m_angle);
	}
	
	public void restoreState(DataInputStream stream) throws IOException {
		DataStreamHelpers.checkTag(stream,STREAM_TAG);
		m_angle=stream.readFloat();
	}
	
	public void apply(Layer layer) {
		float p=trigger();
		layer.rotate(p*m_angle);
	}
	
	///////////////////////////////////////////// implementation
	
	private float m_angle;
	
	private static final int STREAM_TAG=0x46584552;
}

/////////////////////////////////////////////////////////// LightLayerEffect

class LightLayerEffect extends LayerEffect {
	
	public LightLayerEffect(StageEffects stage,IniFile.Section iniSection) {
		super(stage,iniSection);
		m_lightIndex=iniSection.getIntValue("light_index",0);
		m_startBeat=iniSection.getIntValue("start_beat",0);
		m_ambient=iniSection.getFloatValue("ambient",0.5f);
		m_contrast=iniSection.getFloatValue("contrast",0.5f);
	}

	public void saveState(DataOutputStream stream) throws IOException {
		stream.writeInt(STREAM_TAG);
		stream.writeInt(m_lightIndex);
		stream.writeInt(m_startBeat);
		stream.writeFloat(m_ambient);
		stream.writeFloat(m_contrast);
	}
	
	public void restoreState(DataInputStream stream) throws IOException {
		DataStreamHelpers.checkTag(stream,STREAM_TAG);
		m_lightIndex=stream.readInt();
		m_startBeat=stream.readInt();
		m_ambient=stream.readFloat();
		m_contrast=stream.readFloat();
	}
	
	public int getLightIndex() {
		return m_lightIndex;
	}
	
	public void apply(Layer layer) {
		if (getStage().getBeats()<m_startBeat) {
			layer.setColor(0x00000000);
			return;
		}
		float p=trigger();
		p=m_ambient+m_contrast*p;
		float astring=getStage().getLightAverageString(m_lightIndex);
		int color=getColor(astring);
		layer.setColor(Color.argb(
			clamp255(getIntensity()*255),
			clamp255(Color.red(color)*p),
			clamp255(Color.green(color)*p),
			clamp255(Color.blue(color)*p)
		));
	}
	
	private static int getColor(float averageString) {
		if (averageString>=(Song.STRING_COUNT-1)) {
			return Config.getStringColor(Song.STRING_COUNT-1);			
		} else if (averageString<=0) {
			return Config.getStringColor(0);
		}
		int string=(int)averageString;
		float rightFraction=(averageString % 1);
		float leftFraction=1-rightFraction;
		int leftColor=Config.getStringColor(string);
		int rightColor=Config.getStringColor(string+1);
		int red=(int)(
			Color.red(leftColor)*leftFraction+
			Color.red(rightColor)*rightFraction);
		int green=(int)(
			Color.green(leftColor)*leftFraction+
			Color.green(rightColor)*rightFraction);
		int blue=(int)(
			Color.blue(leftColor)*leftFraction+
			Color.blue(rightColor)*rightFraction);
		return Color.rgb(red,green,blue);
	}
	
	private static int clamp255(float value) {
		return value>255?255:(int)value;
	}
	
	///////////////////////////////////////////// implementation
	
	private int m_lightIndex;
	private int m_startBeat;
	private float m_ambient;
	private float m_contrast;
	
	private static final int STREAM_TAG=0x4658454C;
}
